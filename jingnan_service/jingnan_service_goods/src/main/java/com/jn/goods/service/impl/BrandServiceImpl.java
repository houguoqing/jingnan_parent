package com.jn.goods.service.impl;

import com.jn.goods.dao.BrandDao;
import com.jn.goods.pojo.Brand;
import com.jn.goods.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class BrandServiceImpl implements BrandService {
    @Autowired
    private BrandDao brandDao;

    @Override
    public List<Brand> findAll() {
        return brandDao.selectAll();
    }

    @Override
    public Brand findById(Integer id) {
        throw new RuntimeException("哎呦，报错了！");
        //return brandDao.selectByPrimaryKey(id);
    }
}
