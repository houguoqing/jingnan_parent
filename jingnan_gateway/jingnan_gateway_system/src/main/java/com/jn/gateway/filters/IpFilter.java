package com.jn.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/*
 * @Author yaxiongliu
 * IP黑名单 & 特定域名封禁
 * 过滤器：过滤所有入微服务系统的请求：
 * 判断是否是指定ip，如果是，则拒绝访问
 **/
@Component
public class IpFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("经过第1个过滤器IpFilter");
        ServerHttpRequest request = exchange.getRequest();
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String ip = remoteAddress.getHostName();
        System.out.println("ip:"+ ip);
        //拦截的逻辑
//        if (!ip.equals("123")){
//            //拦截
//            ServerHttpResponse response = exchange.getResponse();
//            response.setStatusCode(HttpStatus.UNAUTHORIZED);//提示用户未授权错误
//            return response.setComplete();//结束当前请求
//        }
        return chain.filter(exchange);
    }
    //配置过滤器在过滤器链中的优先级
    @Override
    public int getOrder() {
        return 0;
    }
}
