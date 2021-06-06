package com.jn.search.dao;

import com.jn.pojo.SkuInfo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/*
 * @Author yaxiongliu
 **/
public interface SearchDao extends ElasticsearchRepository<SkuInfo,Long> {
}
