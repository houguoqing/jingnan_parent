package com.jn.goods.controller;

/*
 * @Author yaxiongliu
 **/

import com.jn.entity.Result;
import com.jn.entity.StatusCode;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 统一异常处理类
 */
@ControllerAdvice//基于Spring AOP实现的
public class BaseExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public Result error(Exception e) {
        e.printStackTrace();//打印当前异常
        return new Result(false, StatusCode.ERROR, e.getMessage());
    }
}
