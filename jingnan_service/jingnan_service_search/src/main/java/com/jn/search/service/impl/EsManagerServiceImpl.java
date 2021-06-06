package com.jn.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.jn.entity.Result;
import com.jn.feign.SkuFeign;
import com.jn.goods.pojo.Sku;
import com.jn.pojo.SkuInfo;
import com.jn.search.dao.SearchDao;
import com.jn.search.service.EsManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * @Author yaxiongliu
 **/
@Service
public class EsManagerServiceImpl implements EsManagerService {

    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private SearchDao searchDao;

    /**
     * 导入全部数据到ES索引库
     */
    @Override
    public void importAll() {
        //1.查到所有商品数据
        Map paramMap = new HashMap();
        paramMap.put("status", "1");
        Result result = skuFeign.findList(paramMap);
        //2.将数据类型转换【sku数据转换为ES索引库中的类型SkuInfo】
        String skuJsonStr = JSON.toJSONString(result.getData());
        List<SkuInfo> skuInfos = JSON.parseArray(skuJsonStr, SkuInfo.class);
        for (SkuInfo skuInfo : skuInfos) {
            skuInfo.setPrice(skuInfo.getPrice());
            skuInfo.setSpecMap(JSON.parseObject(skuInfo.getSpec(), Map.class));
        }
        //4.将sku数据导入到es中
        searchDao.saveAll(skuInfos);
    }

    @Override
    public void importDataToESBySpuId(String spuId) {
        //1.根据spuId查询到sku列表（通过feign调用）
        List<Sku> skuList = skuFeign.findBySpuId(spuId);
        //2.将skuList转为json字符串
        String skuListJson = JSON.toJSONString(skuList);
        //3.将json字符串转为List<SkuInfo>
        List<SkuInfo> skuInfoList = JSON.parseArray(skuListJson, SkuInfo.class);
        for (SkuInfo skuInfo : skuInfoList) {
            String specJSON = skuInfo.getSpec();//规格json字符串
            //这里要转换为map类型，map类型ES存储时会将所有的key当成field，那么这些field就可以用来实现精确搜索
            Map specMap = JSON.parseObject(specJSON, Map.class);
            skuInfo.setSpecMap(specMap);
        }

        //4.将sku数据导入到es中
        searchDao.saveAll(skuInfoList);
    }
}
