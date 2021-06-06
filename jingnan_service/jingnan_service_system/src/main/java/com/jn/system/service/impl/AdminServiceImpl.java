package com.jn.system.service.impl;

import com.jn.system.dao.AdminMapper;
import com.jn.system.service.AdminService;
import com.jn.system.pojo.Admin;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    /**
     * 实现登陆功能：
     * 1.查出系统中的Admin账户
     * 2.获取账户中加密的明文
     * 3.判断是否与用户传入的admin的原始密码匹配
     * 4.如果匹配：返回true
     *   否则返回false
     * @param admin
     * @return
     */
    @Override
    public boolean login(Admin admin) {
        Admin adminQuery = new Admin();
        adminQuery.setLoginName(admin.getLoginName());//设置查询用户登陆名称
        adminQuery.setStatus("1");//用户未删除
        //1.查出系统中的Admin账户
        Admin adminDo = adminMapper.selectOne(adminQuery);
        //2.获取账户中加密的明文
        String password = adminDo.getPassword();
        //3.判断是否与用户传入的admin的原始密码匹配
        boolean checkpw = BCrypt.checkpw(admin.getPassword(), password);
        //4.如果匹配：返回true
        if (checkpw){
            return true;
        }
        //  否则返回false
        return false;
    }

    /**
     * 查询全部列表
     * @return
     */
    @Override
    public List<Admin> findAll() {
        return adminMapper.selectAll();
    }

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    @Override
    public Admin findById(Integer id){
        return  adminMapper.selectByPrimaryKey(id);
    }


    /**
     * 增加
     * @param admin
     */
    @Override
    public void add(Admin admin){
        //对管理员密码进行加密
        //获取明文
        String password = admin.getPassword();
        //加密获得密文
        String hashpw = BCrypt.hashpw(password, BCrypt.gensalt());
        //设置加密后的密码
        admin.setPassword(hashpw);
        adminMapper.insert(admin);
    }


    /**
     * 修改
     * @param admin
     */
    @Override
    public void update(Admin admin){
        adminMapper.updateByPrimaryKey(admin);
    }

    /**
     * 删除
     * @param id
     */
    @Override
    public void delete(Integer id){
        adminMapper.deleteByPrimaryKey(id);
    }


    /**
     * 条件查询
     * @param searchMap
     * @return
     */
    @Override
    public List<Admin> findList(Map<String, Object> searchMap){
        Example example = createExample(searchMap);
        return adminMapper.selectByExample(example);
    }

    /**
     * 分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public Page<Admin> findPage(int page, int size){
        PageHelper.startPage(page,size);
        return (Page<Admin>)adminMapper.selectAll();
    }

    /**
     * 条件+分页查询
     * @param searchMap 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public Page<Admin> findPage(Map<String,Object> searchMap, int page, int size){
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        return (Page<Admin>)adminMapper.selectByExample(example);
    }

    /**
     * 构建查询对象
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Admin.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 用户名
            if(searchMap.get("loginName")!=null && !"".equals(searchMap.get("loginName"))){
                criteria.andLike("loginName","%"+searchMap.get("loginName")+"%");
           	}
            // 密码
            if(searchMap.get("password")!=null && !"".equals(searchMap.get("password"))){
                criteria.andLike("password","%"+searchMap.get("password")+"%");
           	}
            // 状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andEqualTo("status",searchMap.get("status"));
           	}

            // id
            if(searchMap.get("id")!=null ){
                criteria.andEqualTo("id",searchMap.get("id"));
            }

        }
        return example;
    }

}
