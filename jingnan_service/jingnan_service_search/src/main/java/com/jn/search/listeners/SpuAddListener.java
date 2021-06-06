package com.jn.search.listeners;

import com.jn.search.service.EsManagerService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/*
 * @Author yaxiongliu
 **/
@Component
@RabbitListener(queues = "search_add_queue")
public class SpuAddListener {

    @Autowired
    private EsManagerService esManagerService;

    @RabbitHandler
    public void addDataToES(String spuId) {
        System.out.println("===接收到需要商品上架的spuId为======" + spuId);
        //根据SpuID导入，对应商品数据
        esManagerService.importDataToESBySpuId(spuId);
    }
}
