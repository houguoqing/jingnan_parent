package com.jn.search.service;

public interface EsManagerService {
    //根据spuid导入商品数据到索引库
    void importDataToESBySpuId(String spuId);
    //导入所有商品数据
    void importAll();

}
