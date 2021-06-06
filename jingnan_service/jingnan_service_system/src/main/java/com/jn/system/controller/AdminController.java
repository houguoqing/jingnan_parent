package com.jn.system.controller;
import com.jn.entity.PageResult;
import com.jn.entity.Result;
import com.jn.entity.StatusCode;
import com.jn.system.service.AdminService;
import com.jn.system.pojo.Admin;
import com.github.pagehelper.Page;
import com.jn.system.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping("/admin")
public class AdminController {


    @Autowired
    private AdminService adminService;

    /**
     * 目标：实现网关鉴权
     * 主要步骤：
     * 第一步：登陆成功用户，签发token
     *   1.创建一个map用来返回用户信息：用户名称、token
     *   2.使用Jwt签发token
     * 第二步：网关判断用户传递的token是否有效
     * @param admin
     * @return
     */
    //登陆功能
    @RequestMapping("login")
    public Result login(@RequestBody Admin admin) {
        boolean isLogin = adminService.login(admin);
        if (isLogin){
            //1.创建一个map用来返回用户信息：用户名称、token
            Map<String, String> info = new HashMap<>();
            info.put("username", admin.getLoginName());//设置登陆名称
            //2.使用Jwt签发token
            String token = JwtUtil.createJWT(UUID.randomUUID().toString(), admin.getLoginName());
            info.put("token", token);
            //3.将信息返回给登陆的用户
            return new Result(true, StatusCode.OK, "登陆成功", info);
        }
        return new Result(false, StatusCode.LOGINERROR, "登陆失败");
    }

    /**
     * 查询全部数据
     * @return
     */
    @GetMapping
    public Result findAll(){
        List<Admin> adminList = adminService.findAll();
        return new Result(true, StatusCode.OK,"查询成功",adminList) ;
    }

    /***
     * 根据ID查询数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result findById(@PathVariable Integer id){
        Admin admin = adminService.findById(id);
        return new Result(true,StatusCode.OK,"查询成功",admin);
    }


    /***
     * 新增数据
     * @param admin
     * @return
     */
    @PostMapping
    public Result add(@RequestBody Admin admin){
        adminService.add(admin);
        return new Result(true,StatusCode.OK,"添加成功");
    }


    /***
     * 修改数据
     * @param admin
     * @param id
     * @return
     */
    @PutMapping(value="/{id}")
    public Result update(@RequestBody Admin admin,@PathVariable Integer id){
        admin.setId(id);
        adminService.update(admin);
        return new Result(true,StatusCode.OK,"修改成功");
    }


    /***
     * 根据ID删除品牌数据
     * @param id
     * @return
     */
    @DeleteMapping(value = "/{id}" )
    public Result delete(@PathVariable Integer id){
        adminService.delete(id);
        return new Result(true,StatusCode.OK,"删除成功");
    }

    /***
     * 多条件搜索品牌数据
     * @param searchMap
     * @return
     */
    @GetMapping(value = "/search" )
    public Result findList(@RequestParam Map searchMap){
        List<Admin> list = adminService.findList(searchMap);
        return new Result(true,StatusCode.OK,"查询成功",list);
    }


    /***
     * 分页搜索实现
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    @GetMapping(value = "/search/{page}/{size}" )
    public Result findPage(@RequestParam Map searchMap, @PathVariable  int page, @PathVariable  int size){
        Page<Admin> pageList = adminService.findPage(searchMap, page, size);
        PageResult pageResult=new PageResult(pageList.getTotal(),pageList.getResult());
        return new Result(true,StatusCode.OK,"查询成功",pageResult);
    }


}
