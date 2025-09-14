package com.oceangpt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步配置类
 * 配置异步任务执行器
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 异步任务执行器
     * 用于处理异步预测和数据获取任务
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(5);
        
        // 最大线程数
        executor.setMaxPoolSize(20);
        
        // 队列容量
        executor.setQueueCapacity(100);
        
        // 线程名前缀
        executor.setThreadNamePrefix("OceanGPT-Async-");
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：调用者运行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
    
    /**
     * 数据处理专用执行器
     * 用于处理大量数据处理任务
     */
    @Bean(name = "dataProcessingExecutor")
    public Executor dataProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(3);
        
        // 最大线程数
        executor.setMaxPoolSize(10);
        
        // 队列容量
        executor.setQueueCapacity(50);
        
        // 线程名前缀
        executor.setThreadNamePrefix("OceanGPT-DataProcessing-");
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(120);
        
        // 拒绝策略：丢弃最旧的任务
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
        
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }
}