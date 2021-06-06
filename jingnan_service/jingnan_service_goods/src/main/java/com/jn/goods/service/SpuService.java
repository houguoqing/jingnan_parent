package com.jn.goods.service;

import com.jn.goods.pojo.Goods;
import com.jn.goods.pojo.Spu;
import com.github.pagehelper.Page;

import java.util.List;
import java.util.Map;

public interface SpuService {
    /**
     * 物理删除
     * @param id
     */
    public void realDelete(String id);
    /**
     * 恢复数据
     * @param id
     */
    public void restore(String id);
    /**
     * 上架商品
     * @param id
     */
    public void put(String id);
    /**
     * 下架商品
     * @param id
     */
    public void pull(String id);
    /**
     * 审核
     * @param id
     */
    public void audit(String id);
    /***
     * 修改数据
     * @param goods
     */
    void update(Goods goods);
    /**
     * 根据ID查询商品
     * @param id
     * @return
     */
    public Goods findGoodsById(String id);
    /***
     * 查询所有
     * @return
     */
    List<Spu> findAll();

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    Spu findById(String id);

    /***
     * 新增
     * @param goods
     */
    void add(Goods goods);

    /***
     * 修改
     * @param spu
     */
    void update(Spu spu);

    /***
     * 删除
     * @param id
     */
    void delete(String id);

    /***
     * 多条件搜索
     * @param searchMap
     * @return
     */
    List<Spu> findList(Map<String, Object> searchMap);

    /***
     * 分页查询
     * @param page
     * @param size
     * @return
     */
    Page<Spu> findPage(int page, int size);

    /***
     * 多条件分页查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    Page<Spu> findPage(Map<String, Object> searchMap, int page, int size);




}
