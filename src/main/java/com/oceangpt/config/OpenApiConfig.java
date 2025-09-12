package com.oceangpt.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI配置类
 * 配置Swagger UI文档
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Bean
    public OpenAPI oceanGptOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OceanGPT API")
                        .description("海洋数据智能分析与预测API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("OceanGPT Team")
                                .email("support@oceangpt.com")
                                .url("https://oceangpt.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("本地开发服务器"),
                        new Server()
                                .url("https://api.oceangpt.com")
                                .description("生产环境服务器")
                ));
    }
}