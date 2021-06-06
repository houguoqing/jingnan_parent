package com.jn.search.service;

import java.util.Map;

public interface SearchService {
    //搜索接口：
    Map search(Map<String, String> paramMap)throws Exception;
}
