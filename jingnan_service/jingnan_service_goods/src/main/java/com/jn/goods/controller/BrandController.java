package com.jn.goods.controller;

import com.jn.entity.Result;
import com.jn.entity.StatusCode;
import com.jn.goods.pojo.Brand;
import com.jn.goods.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 品牌的controller
 * */
@RestController
@RequestMapping("brand")
public class BrandController {
    /**
     * 第一个任务，查询所有品牌的数据
     * 1.创建实体类
     * 2.控制层
     * 3.业务层
     * 4.dao
     * */
    @Autowired
    private BrandService brandService;

    @GetMapping
    public Result<Brand> findAll(){
        //调用业务层接口查询所有
        List<Brand> list = brandService.findAll();
        return new Result<Brand>(true, StatusCode.OK,"查询成功",list);
    }

    //第二个任务:根据id查询品牌信息
    @GetMapping("{id}")
    public Result<Brand> findById(@PathVariable("id")  Integer id){
        //调用业务层接口查询所有
        Brand brand = brandService.findById(id);
        return new Result<Brand>(true, StatusCode.OK,"查询成功",brand);
    }
}
