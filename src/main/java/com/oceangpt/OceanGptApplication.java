package com.oceangpt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * OceanGPT海水水质监测系统主启动类
 * 实现海洋遥感数据分析和环境变化预测
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class OceanGptApplication {

    public static void main(String[] args) {
        SpringApplication.run(OceanGptApplication.class, args);
    }
}