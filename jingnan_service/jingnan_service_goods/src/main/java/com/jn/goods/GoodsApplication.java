package com.jn.goods;

import com.jn.util.IdWorker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import tk.mybatis.spring.annotation.MapperScan;

/*
 * @Author yaxiongliu
 **/
@SpringBootApplication
@EnableEurekaClient
@MapperScan(basePackages = {"com.jn.goods.dao"})
public class GoodsApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoodsApplication.class);
    }

    @Value("${workId}")
    private Integer workId;
    @Value("${dataCenterId}")
    private Integer dataCenterId;
    @Bean
    public IdWorker idWorker(){
        return new IdWorker(workId,dataCenterId);
    }
}
