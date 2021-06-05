package com.jn.goods.service;

import com.jn.goods.pojo.Brand;

import java.util.List;

public interface BrandService {
    /*
    * 查询所有
    * */
    List<Brand> findAll();


    //根据id查询品牌信息
    Brand findById(Integer id);
}
