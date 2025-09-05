package com.oceangpt.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 配置静态资源处理和路径映射
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置根路径静态资源映射
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true);
        
        // 明确配置静态资源路径，避免与API路径冲突
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        registry.addResourceHandler("/public/**")
                .addResourceLocations("classpath:/public/");
        
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("classpath:/resources/");
        
        // 确保不将v1路径作为静态资源处理
        // 不添加对v1/**的资源处理器
    }
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 配置根路径重定向到index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}