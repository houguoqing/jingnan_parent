package com.jn.search.controller;

import com.jn.entity.Result;
import com.jn.pojo.SkuInfo;
import com.jn.search.service.EsManagerService;
import com.jn.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/*
 * @Author yaxiongliu
 **/
@RestController
@RequestMapping("search")
public class SearchController {
    //导入ES模板对象
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private EsManagerService esManagerService;

    @Autowired
    private SearchService searchService;

    /**
     * 创建索引库结构
     */
    @RequestMapping("/createIndex")
    public Result createIndex(){
        //创建索引库
        elasticsearchTemplate.createIndex(SkuInfo.class);
        //配置映射
        elasticsearchTemplate.putMapping(SkuInfo.class);
        return new Result();
    }
    //导入所有的商品数据
    @RequestMapping("/importAll")
    public Result importAll(){
        esManagerService.importAll();
        return new Result();
    }
    //搜索接口
    @GetMapping()
    public Map search(@RequestParam Map<String,String> paramMap) throws Exception {
        Map search = searchService.search(paramMap);
        return search;
    }
}
