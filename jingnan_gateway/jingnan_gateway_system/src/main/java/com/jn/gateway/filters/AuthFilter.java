package com.jn.gateway.filters;

import com.jn.gateway.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @Author 雄哥
 * 目标：实现网关鉴权
 * 主要步骤：
 * 第一步：登陆成功用户，签发token
 *   1.创建一个map用来返回用户信息：用户名称、token
 *   2.使用Jwt签发token
 * 第二步：网关判断用户传递的token是否有效
 * @return
 */
@Component//注入到Spring容器
public class AuthFilter implements GlobalFilter, Ordered {

    /**
     * 目标：网关判断用户传递的token是否有效
     * 1. 获取请求、响应
     * 2. 判断如果是登录请求则放行
     * 3. 获取请求头，并取出其中的token参数
     * 4. 判断请求头中是否有令牌
     * 5. 判断如果没有则：
     *   5.1 响应中放入返回的状态吗, 提示没有权限访问
     *   5.2 返回
     * 6. 如果请求头中有令牌则解析令牌
     *   6.1 解析jwt令牌出错, 说明令牌过期或者伪造等不合法情况出现
     *   6.2 返回
     * 7. 能走到这一步，说明没有登陆没有问题，则放行
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1. 获取请求、响应
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //2. 判断如果是登录请求则放行
        String path = request.getURI().getPath();
        //goods/brand
        //system/admin
        ///admin/login登陆请求
        if (path.contains("admin/login")) {
            return chain.filter(exchange);//放行
        }
        //3. 获取请求头，并取出其中的token参数
        String token = request.getHeaders().getFirst("token");
        //4. 判断请求头中是否有令牌
        //5. 判断如果没有则：
        if (StringUtils.isEmpty(token)) {//是否为空
            //5.1 响应中放入返回的状态吗, 提示没有权限访问
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            //5.2 拦截返回
            return response.setComplete();
        }
        //6. 如果请求头中有令牌则解析令牌
        try {
            Claims claims = JwtUtil.parseJWT(token);//令牌抛异常情况：1.令牌有效期到了，2.假令牌
        } catch (Exception e) {
            e.printStackTrace();//抛异常
            //  6.1 解析jwt令牌出错, 说明令牌过期或者伪造等不合法情况出现
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            //  6.2 返回
            return response.setComplete();
        }

        //7. 能走到这一步，说明没有登陆没有问题，则放行
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
