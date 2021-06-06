package com.jn.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.jn.entity.Page;
import com.jn.pojo.SkuInfo;
import com.jn.search.service.SearchService;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * @Author yaxiongliu
 **/
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ElasticsearchTemplate esTemplate;

    /**
     * 第二步：品牌筛选，过滤品牌商品，聚合查询品牌信息
     *  1.添加品牌过滤条件
     *  2.构建品牌字符串聚合对象：设置聚合字段brandName
     *  3.原生查询对象设置聚合条件
     *  4.获取聚合结果并转换为字符串List集合
     *  5.设置品牌的查询结果集
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public Map search(Map<String, String> paramMap) throws Exception {
        //1.定义查询结果封装集合
        Map<String, Object> resultMap = new HashMap<>();
        //2.判断是否含有查询条件，有条件才执行ES查询
        //  2.1 有条件才执行ES查询
        if (paramMap == null || paramMap.size() == 0) {
            return resultMap; //  2.2 没有条件返回null
        }
        //3.创建原生搜索对象
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //4.创建bool组合查询条件对象【match查询，term查询，布尔查询，范围查询，模糊查询....】
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //  4.1 组合查询条件的关键词
        String keywords = paramMap.get("keywords");
        if (StringUtils.isNotEmpty(keywords)) {
            //  4.2 设置查询组合条件
            boolQuery.must(QueryBuilders.matchQuery("name", keywords));
        }

        queryBuilder.withQuery(boolQuery);

        //第二步：品牌筛选，过滤品牌商品，聚合查询品牌信息
        // 添加品牌过滤条件 http://localhost:9009/search?brandName=华为&keywords=手机
        String brandName = paramMap.get("brandName");
        if (StringUtils.isNotEmpty(brandName)) {
            boolQuery.filter(QueryBuilders.termQuery("brandName", brandName));
        }
        // 构建品牌字符串聚合对象：设置聚合字段brandName
        String skuBrand = "skuBrand";
        // 原生查询对象设置聚合条件
        queryBuilder.addAggregation(AggregationBuilders.terms(skuBrand).field("brandName"));

        /**
         * 第三步： 规格过滤
         *    1.设置查询条件规格：spec_
         *    2.构建规格字符串聚合对象：设置聚合字段spec.keyword
         *      GET方式传值的时候,+号会被浏览器处理为空, 所以在这里转换回来
               注意：浏览器对get请求参数有一个处理，就会将+装换为空格
         *    3.获取规格聚合结果
         */
        //设置查询条件规格：spec_
        for (String key : paramMap.keySet()) {
            //构建规格字符串聚合对象：设置聚合字段spec.keyword
            if (key.contains("spec_")) {
                //  GET方式传值的时候,+号会被浏览器处理为空, 所以在这里转换回来
                String value = paramMap.get(key);
                value = value.replace(" ", "+");
                // 注意：浏览器对get请求参数有一个处理，就会将+装换为空格
                //http://localhost:9009/search?spec_颜色=黑色&spec_版本=6GB+64GB&keywords=手机

                boolQuery.filter(QueryBuilders
                        .termQuery("specMap." + key.substring(5) + ".keyword", value));//6GB+64GB
            }

        }
        // 设置查询规格的聚合对象
        String skuSpec = "skuSpec";
        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(skuSpec).field("spec.keyword");
        // 原生查询对象设置聚合条件
        queryBuilder.addAggregation(aggregationBuilder);

        /**
         * 第四步：价格区间查询
         * 1.判断是否含有价格查询字段
         * 2.截取价格查询
         * 3.设置大于等于最低价格
         * 4.设置小于等于最高价格
         */
        //判断是否含有价格查询字段
        String price = paramMap.get("price");
        if (StringUtils.isNotEmpty(price)) {
            //截取价格查询: 200-2000
            String[] prices = price.split("-");
            String small = prices[0];//最低价格
            //设置大于等于最低价格
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(small));
            if (prices.length == 2) {
                String big = prices[1];//最高价格
                //设置小于等于最高价格
                boolQuery.filter(QueryBuilders.rangeQuery("price").lte(big));
            }
        }
        /**
         * 第五步：搜索结果分页
         * 1.获取显示页数：如果页数为空，则设置为第一页
         * 2.设置分页查询
         * 3.当前页回显
         */
        //获取显示页数：如果页数为空，则设置为第一页
        String pageNum = paramMap.get("pageNum");
        if (StringUtils.isEmpty(pageNum)) {
            pageNum = "1";
        }
        //设置分页查询: 在代码中，其实页数一般是从0开始，但是人的认知中，起始页是从1
        queryBuilder.withPageable(PageRequest.of(Integer.parseInt(pageNum) - 1, 3));//20


        /**
         * 第六步：搜索结果排序
         *    1.判断是否含有排序字段
         *    2.获取排序的规则：判断升序还是降序
         *    3.设置排序字段 + 排序规则ASC
         *    4.设置排序字段 + 排序规则DESC
         */
        //判断是否含有排序字段
        String sortField = paramMap.get("sortField");//price
        if (StringUtils.isNotEmpty(sortField)) {
            //获取排序的规则：判断升序还是降序
            String sortRule = paramMap.get("sortRule");
            if ("ASC".equals(sortRule)) {//设置排序字段 + 排序规则ASC
                queryBuilder.withSort(SortBuilders.fieldSort(sortField).order(SortOrder.ASC));
            }
            if ("DESC".equals(sortRule)) {//设置排序字段 + 排序规则DESC
                queryBuilder.withSort(SortBuilders.fieldSort(sortField).order(SortOrder.DESC));
            }
        }
        /**
         * 第七步：搜索结果关键字高亮显示
         * 1.配置关键字高亮的字段
         * 2.设置高亮显示字段
         * 3.非高亮字段，替换为高亮字段
         */
        //配置关键字高亮的字段
        HighlightBuilder.Field field = new HighlightBuilder
                .Field("name")
                .preTags("<span style='color:red'>")
                .postTags("</span>");

        queryBuilder.withHighlightFields(field);
        //5.执行查询返回结果信息
        //查询结果映射匿名内部类：看起来像是new接口的对象
        SearchResultMapper mapper = new SearchResultMapper() {
            //查询结果的映射
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                //将查询结果包装转换的List集合中
                List<T> skuList = new ArrayList<>();
                //查询结果命中的对象
                SearchHits hits = response.getHits();
                for (SearchHit hit : hits) {
                    SkuInfo skuInfo = JSON.parseObject(hit.getSourceAsString(), SkuInfo.class);
                    //非高亮字段，替换为高亮字段
                    Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                    //如果highlightFields集合不为空，那么替换
                    if (highlightFields != null && highlightFields.size() > 0) {
                        HighlightField highlightFieldName = highlightFields.get("name");
                        //将高亮的内容设置到非高亮内容中
                        skuInfo.setName(highlightFieldName.getFragments()[0].toString());
                    }

                    skuList.add((T) skuInfo);
                }
                //定义聚合对象，设置查询结果集
                /**
                 * 参数1：查询结果List集合
                 * 参数2：分页对象
                 * 参数3：查询命中数
                 * 参数4：聚合对象
                 */
                AggregatedPageImpl<T> aggregatedPage = new AggregatedPageImpl<>(
                        skuList, pageable, hits.getTotalHits(), response.getAggregations());
                return aggregatedPage;
            }
        };
        //  5.1 查询结果映射对象
        AggregatedPage<SkuInfo> aggregatedPage = esTemplate.queryForPage(queryBuilder.build(), SkuInfo.class, mapper);
        //  5.2 设置查询结果：总条数，总页数，查询结果集
        //总条数
        resultMap.put("totalNum", aggregatedPage.getTotalElements());
        //总页数
        resultMap.put("totalPage", aggregatedPage.getTotalPages());
        //查询结果集
        resultMap.put("rows", aggregatedPage.getContent());
        // 获取聚合结果并转换为字符串List集合
        StringTerms stringTerms = (StringTerms) aggregatedPage.getAggregation(skuBrand);
        List<String> brandList = stringTerms
                .getBuckets()
                .stream()
                .map(bucket -> bucket.getKeyAsString())
                .collect(Collectors.toList());
        resultMap.put("brandList", brandList);
        // 获取规格聚合结果
        StringTerms specTerms = (StringTerms) aggregatedPage.getAggregation(skuSpec);
        List<String> specList = specTerms //使用JDK1.8当中的Stream流操作
                .getBuckets()
                .stream()
                .map(bucket -> bucket.getKeyAsString())
                .collect(Collectors.toList());
        resultMap.put("specList", specList);
        //当前页回显
        resultMap.put("pageNum", pageNum);
        //  5.3 返回查询结果信息
        return resultMap;
    }
}
