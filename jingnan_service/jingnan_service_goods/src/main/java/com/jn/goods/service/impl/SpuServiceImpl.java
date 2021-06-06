package com.jn.goods.service.impl;

import com.alibaba.fastjson.JSON;
import com.jn.entity.GoodsStatus;
import com.jn.goods.dao.*;
import com.jn.goods.pojo.*;
import com.jn.goods.service.SpuService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.jn.util.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private BrandMapper brandMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CategoryBrandMapper categoryBrandMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    @Autowired
    private IdWorker idWorker;
    //物理删除
    @Override
    public void realDelete(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //检查是否删除的商品
        if(!spu.getIsDelete().equals(GoodsStatus.DELETE)){
            throw new RuntimeException("此商品未删除！");
        }
        spuMapper.deleteByPrimaryKey(id);

    }
    /**
     * 恢复数据
     * @param id
     */
    @Override
    public void restore(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //检查是否删除的商品
        if(spu.getIsDelete().equals(GoodsStatus.NOT_DELETE)){
            throw new RuntimeException("此商品未删除！");
        }
        spu.setIsDelete(GoodsStatus.NOT_DELETE);//未删除
        spu.setStatus(GoodsStatus.NOT_AUDIT);//未审核
        spuMapper.updateByPrimaryKeySelective(spu);
    }
    /**
     * 上架商品：并且发通知告诉消息队列
     * @param id
     */
    @Override
    public void put(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if(spu.getStatus().equals(GoodsStatus.NOT_AUDIT)){
            throw new RuntimeException("未通过审核的商品不能上架！");
        }
        spu.setIsMarketable(GoodsStatus.UP_MARKET);//上架状态
        spuMapper.updateByPrimaryKeySelective(spu);
        //并且发通知告诉消息队列: Fanout模式，所以不用写路由键
        rabbitTemplate.convertAndSend("goods_up_exchange", "", spu.getId());
    }
    /**
     * 下架商品
     * @param id
     */
    public void pull(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        spu.setIsMarketable(GoodsStatus.DOWN_MARKET);//下架状态
        spuMapper.updateByPrimaryKeySelective(spu);
    }
    /**
     * 审核
     * @param id
     */
    public void audit(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        spu.setStatus(GoodsStatus.AUDIT_OK);//已审核
        spuMapper.updateByPrimaryKeySelective(spu);
    }
    //开启事务
    @Transactional
    @Override
    public void update(Goods goods ) {
        //取出spu部分
        Spu spu = goods.getSpu();
        spuMapper.updateByPrimaryKey(spu);
        //删除原sku列表
        Example example=new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId",spu.getId());
        skuMapper.deleteByExample(example);

        addSkuList(goods);//保存sku列表
    }
    /**
     * 根据ID查询商品
     * @param id
     * @return
     */
    public Goods findGoodsById(String id){
        //查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);

        //查询SKU 列表
        Example example=new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId",id);
        List<Sku> skuList = skuMapper.selectByExample(example);

        //封装，返回
        Goods goods=new Goods();
        goods.setSpu(spu);
        goods.setSkuList(skuList);
        return goods;
    }
    /**
     * 目标：保存商品:4.保存spu同时保存sku列表【比较复杂】，分布式id解决方案【IdWorker】
     * 实现步骤：
     *  1.生成spuid：主键id
     *  2.spuid存储在goods的spu对象中
     *  3.添加spu标准产品单位数据
     *  4.保存sku标准库存量单位数据列表
     *
     * 注意：添加商品列表，在多个方法中执行，需要加入事务操作
     */
    @Transactional//配置事务：要成功都成功，要失败都失败
    @Override
    public void add(Goods goods){
        //1.生成spuid：主键id
        long spuid = idWorker.nextId();
        //2.spuid存储在goods的spu对象中
        goods.getSpu().setId(String.valueOf(spuid));
        //3.添加spu标准产品单位数据
        spuMapper.insert(goods.getSpu());
        //4.保存sku标准库存量单位数据列表
        addSkuList(goods);
    }

    /**
     * 目标：保存sku标准库存量单位数据列表
     * 实现步骤：
     *   1.获取spu对象
     *   2.查询商品对应品牌对象
     *   3.查询商品对应分类对象
     *   4.获取所有sku对象的list集合
     *   5.遍历集合：构建Sku对象，并新增
     *     5.1 设置sku主键ID：采用分布式ID生成器
     *     5.2 设置sku规格信息：如果规格为空，则初始化{}，防止JSON解析异常
     *     5.3 设置sku名称(商品名称 + 规格)
     *     5.4 设置：名称、spuid、修改日期、创建日期、商品分类ID、商品分类名称、品牌
     *     5.5 新增sku对象
     */
    private void addSkuList(Goods goods) {
        //1.获取spu对象
        Spu spu = goods.getSpu();
        //2.查询商品对应品牌对象
        Integer brandId = spu.getBrandId();//获取品牌id
        Brand brand = brandMapper.selectByPrimaryKey(brandId);
        //3.查询商品对应分类对象
        Integer categoryId = spu.getCategory3Id();
        Category category = categoryMapper.selectByPrimaryKey(categoryId);

        //保存品牌和分类的关联关系：
        CategoryBrand categoryBrand = new CategoryBrand();
        categoryBrand.setBrandId(spu.getBrandId());
        categoryBrand.setCategoryId(spu.getCategory3Id());
        int count = categoryBrandMapper.selectCount(categoryBrand);
        //判断是否有这个品牌和分类的关系数据
        if(count == 0) {
            //如果没有关系数据则添加品牌和分类关系数据
            categoryBrandMapper.insert(categoryBrand);
        }



        //4.获取所有sku对象的list集合
        List<Sku> skuList = goods.getSkuList();
        //5.遍历集合：构建Sku对象，并新增
        for (Sku skuVo : skuList) {//前端传来信息，
            //  5.1 设置sku主键ID：采用分布式ID生成器
            skuVo.setId(String.valueOf(idWorker.nextId()));//分布式id
            //  5.2 设置sku规格信息：如果规格为空，则初始化{}，防止JSON解析异常
            if (skuVo.getSpec() == null) {
                skuVo.setSpec("{}");
            }
            //  5.3 设置sku名称(商品名称 + 规格)
            String name = spu.getName();
            //拼接规格到商品sku名称中
            String spec = skuVo.getSpec();
            Map<String, String> map = JSON.parseObject(spec, Map.class);
            for (String value : map.values()) {
                name += " " + value;
            }
            //华为p30翡翠冷64GB
            //华为p30 翡翠冷 64GB
            skuVo.setName(name);
            skuVo.setSpuId(spu.getId());
            //  5.4 设置：名称、spuid、修改日期、创建日期、商品分类ID、商品分类名称、品牌
            skuVo.setCreateTime(new Date());
            skuVo.setUpdateTime(new Date());
            skuVo.setCategoryId(categoryId);
            skuVo.setCategoryName(category.getName());
            skuVo.setBrandName(brand.getName());
            skuVo.setSn(skuVo.getSn());
            //  5.5 新增sku对象
            skuMapper.insert(skuVo);
        }

    }

    /**
     * 查询全部列表
     * @return
     */
    @Override
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    @Override
    public Spu findById(String id){
        return  spuMapper.selectByPrimaryKey(id);
    }





    /**
     * 修改
     * @param spu
     */
    @Override
    public void update(Spu spu){
        spuMapper.updateByPrimaryKey(spu);
    }

    /**
     * 删除
     * @param id
     */
    /**
     * 删除
     * @param id
     */
    @Override
    public void delete(String id){
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //检查是否下架的商品
        if(spu.getIsMarketable().equals(GoodsStatus.UP_MARKET)){
            throw new RuntimeException("必须先下架再删除！");
        }
        spu.setIsDelete(GoodsStatus.DELETE);//删除
        spu.setStatus(GoodsStatus.NOT_AUDIT);//未审核
        spuMapper.updateByPrimaryKeySelective(spu);
    }


    /**
     * 条件查询
     * @param searchMap
     * @return
     */
    @Override
    public List<Spu> findList(Map<String, Object> searchMap){
        Example example = createExample(searchMap);
        return spuMapper.selectByExample(example);
    }

    /**
     * 分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public Page<Spu> findPage(int page, int size){
        PageHelper.startPage(page,size);
        return (Page<Spu>)spuMapper.selectAll();
    }

    /**
     * 条件+分页查询
     * @param searchMap 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public Page<Spu> findPage(Map<String,Object> searchMap, int page, int size){
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        return (Page<Spu>)spuMapper.selectByExample(example);
    }

    /**
     * 构建查询对象
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 主键
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andEqualTo("id",searchMap.get("id"));
           	}
            // 货号
            if(searchMap.get("sn")!=null && !"".equals(searchMap.get("sn"))){
                criteria.andEqualTo("sn",searchMap.get("sn"));
           	}
            // SPU名
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
           	}
            // 副标题
            if(searchMap.get("caption")!=null && !"".equals(searchMap.get("caption"))){
                criteria.andLike("caption","%"+searchMap.get("caption")+"%");
           	}
            // 图片
            if(searchMap.get("image")!=null && !"".equals(searchMap.get("image"))){
                criteria.andLike("image","%"+searchMap.get("image")+"%");
           	}
            // 图片列表
            if(searchMap.get("images")!=null && !"".equals(searchMap.get("images"))){
                criteria.andLike("images","%"+searchMap.get("images")+"%");
           	}
            // 售后服务
            if(searchMap.get("saleService")!=null && !"".equals(searchMap.get("saleService"))){
                criteria.andLike("saleService","%"+searchMap.get("saleService")+"%");
           	}
            // 介绍
            if(searchMap.get("introduction")!=null && !"".equals(searchMap.get("introduction"))){
                criteria.andLike("introduction","%"+searchMap.get("introduction")+"%");
           	}
            // 规格列表
            if(searchMap.get("specItems")!=null && !"".equals(searchMap.get("specItems"))){
                criteria.andLike("specItems","%"+searchMap.get("specItems")+"%");
           	}
            // 参数列表
            if(searchMap.get("paraItems")!=null && !"".equals(searchMap.get("paraItems"))){
                criteria.andLike("paraItems","%"+searchMap.get("paraItems")+"%");
           	}
            // 是否上架
            if(searchMap.get("isMarketable")!=null && !"".equals(searchMap.get("isMarketable"))){
                criteria.andEqualTo("isMarketable",searchMap.get("isMarketable"));
           	}
            // 是否启用规格
            if(searchMap.get("isEnableSpec")!=null && !"".equals(searchMap.get("isEnableSpec"))){
                criteria.andEqualTo("isEnableSpec", searchMap.get("isEnableSpec"));
           	}
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andEqualTo("isDelete",searchMap.get("isDelete"));
           	}
            // 审核状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andEqualTo("status",searchMap.get("status"));
           	}

            // 品牌ID
            if(searchMap.get("brandId")!=null ){
                criteria.andEqualTo("brandId",searchMap.get("brandId"));
            }
            // 一级分类
            if(searchMap.get("category1Id")!=null ){
                criteria.andEqualTo("category1Id",searchMap.get("category1Id"));
            }
            // 二级分类
            if(searchMap.get("category2Id")!=null ){
                criteria.andEqualTo("category2Id",searchMap.get("category2Id"));
            }
            // 三级分类
            if(searchMap.get("category3Id")!=null ){
                criteria.andEqualTo("category3Id",searchMap.get("category3Id"));
            }
            // 模板ID
            if(searchMap.get("templateId")!=null ){
                criteria.andEqualTo("templateId",searchMap.get("templateId"));
            }
            // 运费模板id
            if(searchMap.get("freightId")!=null ){
                criteria.andEqualTo("freightId",searchMap.get("freightId"));
            }
            // 销量
            if(searchMap.get("saleNum")!=null ){
                criteria.andEqualTo("saleNum",searchMap.get("saleNum"));
            }
            // 评论数
            if(searchMap.get("commentNum")!=null ){
                criteria.andEqualTo("commentNum",searchMap.get("commentNum"));
            }

        }
        return example;
    }

}
