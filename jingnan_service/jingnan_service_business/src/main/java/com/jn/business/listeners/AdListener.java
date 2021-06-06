package com.jn.business.listeners;

import okhttp3.*;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * @Author yaxiongliu
 *  目标：监听从MQ中提取消息执行更新
 **/
@RabbitListener(queues = "ad_update_queue")
@Component//注意：必须将监听器注入Spring的容器，否则不生效
public class AdListener {
    /**
     * 获取更新通知内容
     */
    @RabbitHandler
    public void updateAd(String msg){//msg == position
        //1.输出日志
        System.out.println("msg :: " + msg);
        //2.创建Http客户端对象，构建发送请求对象，发送请求
        //2.1 URl地址：http://192.168.200.128/ad_load?position=
        String url = String.format("http://192.168.200.128/ad_load?position=%s", msg);
        //2.2 创建Http客户端对象
        OkHttpClient client = new OkHttpClient();
        //2.3 构建者模式，创建请求对象，设置url
        Request request = new Request.Builder().url(url).build();
        //客户端发送请求，返回响应回调
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();//打印失败日志
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //输出成功的日志
                System.out.println("调用成功：" + response.message());
            }
        });
        //RestTemplate简单，而其他的客户端OkHttpClient难！
    }
}
