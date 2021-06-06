package com.jn.goods.dao;

import com.jn.goods.pojo.Brand;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
import java.util.Map;

public interface BrandMapper extends Mapper<Brand> {
    //根据商品分类名称查询品牌列表【参数是分类名称】
    //select name,image from tb_brand where id in (select brand_id from tb_category_brand where category_id = (select id from tb_category where name = '手机'));
    //注意：需要将参数拼接到SQL语句中，使用@Param注解
    @Select("select name,image from tb_brand where id in (select brand_id from tb_category_brand where category_id in (select id from tb_category where name = #{categoryName}));")
    List<Map> findBrandListByCategoryName(@Param("categoryName") String categoryName);
}
