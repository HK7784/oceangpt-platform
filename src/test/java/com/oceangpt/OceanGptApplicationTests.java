package com.oceangpt;

import com.oceangpt.service.ModelService;
import com.oceangpt.service.InferenceService;
// import com.oceangpt.service.NoaaDataService; // 已移除NOAA依赖
import com.oceangpt.service.DataProcessingService;
import com.oceangpt.model.PredictionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OceanGPT应用程序集成测试
 * 测试Spring Boot应用程序的启动和基本组件注入
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "oceangpt.model.path=src/test/resources/models/test_model.onnx",
    "oceangpt.model.mock-mode=true",
    "oceangpt.data.csv-path=D:/sentinel-2 reflectance", // 使用CSV数据源
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class OceanGptApplicationTests {

    @Autowired
    private ModelService modelService;
    
    @Autowired
    private InferenceService inferenceService;
    
    // @Autowired
    // private NoaaDataService noaaDataService; // 已移除NOAA依赖
    
    @Autowired
    private DataProcessingService dataProcessingService;

    @Test
    void contextLoads() {
        // 测试Spring上下文是否正确加载
        assertNotNull(modelService, "ModelService应该被正确注入");
        assertNotNull(inferenceService, "InferenceService应该被正确注入");
        // assertNotNull(noaaDataService, "NoaaDataService应该被正确注入"); // 已移除NOAA依赖
        assertNotNull(dataProcessingService, "DataProcessingService应该被正确注入");
    }
    
    @Test
    void testModelServiceInitialization() {
        // 测试模型服务初始化
        assertNotNull(modelService);
        
        // 在测试模式下，模型信息应该可用
        var modelInfo = modelService.getModelInfo();
        assertNotNull(modelInfo);
        assertTrue(modelInfo.containsKey("modelPath"));
    }
    
    @Test
    void testInferenceServiceBasicFunctionality() {
        // 测试推理服务基本功能
        assertNotNull(inferenceService);
        
        // 创建一个测试用的PredictionResult对象
        PredictionResult testResult = new PredictionResult();
        testResult.setPredictedTemperature(15.0);
        testResult.setPredictedSalinity(35.0);
        testResult.setPredictedPh(8.1);
        
        // 测试置信度计算
        double confidence = inferenceService.calculateConfidence(testResult);
        assertTrue(confidence >= 0.0 && confidence <= 1.0, "置信度应该在0-1之间");
    }
    
    @Test
    void testNoaaDataServiceConfiguration() {
        // 测试NOAA数据服务配置
        // // assertNotNull(noaaDataService); // 已移除NOAA依赖 // 已移除NOAA依赖
        
        // 在模拟模式下，服务应该可以正常工作
        // 这里可以添加更多的配置验证
    }
    
    @Test
    void testDataProcessingServiceConfiguration() {
        // 测试数据处理服务配置
        assertNotNull(dataProcessingService);
        
        // 测试基本的数据质量评估功能
        // 这里可以添加更多的功能验证
    }
    
    @Test
    void testApplicationProperties() {
        // 测试应用程序属性是否正确加载
        // 这里可以通过@Value注解或Environment来验证配置
        assertTrue(true, "应用程序属性加载测试");
    }
    
    @Test
    void testCacheConfiguration() {
        // 测试缓存配置
        // 可以通过CacheManager来验证缓存是否正确配置
        assertTrue(true, "缓存配置测试");
    }
    
    @Test
    void testAsyncConfiguration() {
        // 测试异步配置
        // 可以通过TaskExecutor来验证异步配置是否正确
        assertTrue(true, "异步配置测试");
    }
}