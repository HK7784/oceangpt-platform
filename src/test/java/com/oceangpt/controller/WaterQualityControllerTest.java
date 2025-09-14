package com.oceangpt.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceangpt.dto.PredictionRequest;
import com.oceangpt.dto.PredictionResponse;
import com.oceangpt.dto.TrendAnalysisRequest;
import com.oceangpt.dto.TrendAnalysisResponse;
import com.oceangpt.model.OceanData;
import com.oceangpt.model.PredictionResult;
import com.oceangpt.service.InferenceService;
import com.oceangpt.service.ModelService;
import com.oceangpt.service.DataProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WaterQualityController单元测试
 */
@WebMvcTest(WaterQualityController.class)
class WaterQualityControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ModelService modelService;
    
    @MockBean
    private InferenceService inferenceService;
    
    @MockBean
    private DataProcessingService dataProcessingService;
    
    private PredictionRequest validPredictionRequest;
    private TrendAnalysisRequest validTrendRequest;
    private PredictionResult mockPredictionResult;
    private OceanData mockOceanData;
    
    @BeforeEach
    void setUp() {
        // 设置有效的预测请求
        validPredictionRequest = new PredictionRequest();
        validPredictionRequest.setTimestamp(System.currentTimeMillis());
        validPredictionRequest.setLatitude(30.5);
        validPredictionRequest.setLongitude(-114.2);
        validPredictionRequest.setTurbidity(2.5);
        validPredictionRequest.setChlorophyllConcentration(0.8);
        
        // 设置有效的趋势分析请求
        validTrendRequest = new TrendAnalysisRequest();
        validTrendRequest.setLatitude(30.5);
        validTrendRequest.setLongitude(-114.2);
        validTrendRequest.setStartDate(LocalDate.now().minusDays(30));
        validTrendRequest.setEndDate(LocalDate.now());
        validTrendRequest.setParameters(Arrays.asList("temperature", "salinity", "ph"));
        validTrendRequest.setAggregationType("daily");
        validTrendRequest.setIncludeForecast(false);
        validTrendRequest.setForecastDays(0);
        
        // 设置模拟预测结果
        mockPredictionResult = new PredictionResult();
        mockPredictionResult.setNutrientLevel(0.5);
        mockPredictionResult.setPhLevel(7.8);
        mockPredictionResult.setConfidence(0.95);
        mockPredictionResult.setSeaSurfaceTemperature(15.5);
        mockPredictionResult.setSalinity(35.0);
        mockPredictionResult.setDissolvedOxygen(8.5);
        mockPredictionResult.setChlorophyllConcentration(0.8);
        mockPredictionResult.setPollutionIndex(0.3);
        
        // 设置模拟海洋数据
        mockOceanData = new OceanData();
        mockOceanData.setLatitude(30.5);
        mockOceanData.setLongitude(-114.2);
        mockOceanData.setTimestamp(LocalDateTime.now());
        mockOceanData.setSeaSurfaceTemperature(15.5);
        mockOceanData.setSalinity(35.0);
        mockOceanData.setPhLevel(8.1);
        mockOceanData.setDissolvedOxygen(8.5);
        mockOceanData.setChlorophyllConcentration(0.8);
    }
    
    @Test
    void testPredictWaterQuality_Success() throws Exception {
        // 模拟服务行为
        when(modelService.isModelReady()).thenReturn(true);
        when(dataProcessingService.preprocessData(anyList()))
            .thenReturn(Arrays.asList(mockOceanData));
        when(inferenceService.predictAsync(any(OceanData.class), any(LocalDateTime.class)))
            .thenReturn(CompletableFuture.completedFuture(mockPredictionResult));
        
        // 执行请求
        mockMvc.perform(post("/api/v1/water-quality/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPredictionRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nutrient").value(0.5))
                .andExpect(jsonPath("$.ph").value(7.8))
                .andExpect(jsonPath("$.confidence").value(0.95))
                .andExpect(jsonPath("$.qualityLevel").value("良好"))
                .andExpect(jsonPath("$.modelVersion").value("v1.0"));
    }
    
    @Test
    void testPredictWaterQuality_ModelNotReady() throws Exception {
        // 模拟模型未就绪
        when(modelService.isModelReady()).thenReturn(false);
        
        mockMvc.perform(post("/api/v1/water-quality/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPredictionRequest)))
                .andExpect(status().isServiceUnavailable());
    }
    
    @Test
    void testPredictWaterQuality_InvalidRequest() throws Exception {
        // 创建无效请求（缺少必需字段）
        PredictionRequest invalidRequest = new PredictionRequest();
        invalidRequest.setLatitude(91.0); // 无效纬度
        
        mockMvc.perform(post("/api/v1/water-quality/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testAnalyzeMonthlyTrend_Success() throws Exception {
        // 模拟服务行为
        when(dataProcessingService.loadHistoricalDataFromCsv(anyDouble(), anyDouble(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(mockOceanData));
        when(dataProcessingService.preprocessData(anyList()))
            .thenReturn(Arrays.asList(mockOceanData));
        
        mockMvc.perform(post("/api/v1/water-quality/analyze/monthly-trend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTrendRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.analysisInfo").exists())
                .andExpect(jsonPath("$.analysisInfo.latitude").value(30.5))
                .andExpect(jsonPath("$.analysisInfo.longitude").value(-114.2));
    }
    
    @Test
    void testAnalyzeMonthlyTrend_InvalidDateRange() throws Exception {
        // 创建无效日期范围的请求
        TrendAnalysisRequest invalidRequest = new TrendAnalysisRequest();
        invalidRequest.setLatitude(30.5);
        invalidRequest.setLongitude(-114.2);
        invalidRequest.setStartDate(LocalDate.now());
        invalidRequest.setEndDate(LocalDate.now().minusDays(10)); // 结束日期早于开始日期
        invalidRequest.setParameters(Arrays.asList("temperature"));
        invalidRequest.setAggregationType("daily");
        
        mockMvc.perform(post("/api/v1/water-quality/analyze/monthly-trend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testAnalyzeMonthlyTrend_WithForecast() throws Exception {
        // 设置包含预测的请求
        validTrendRequest.setIncludeForecast(true);
        validTrendRequest.setForecastDays(7);
        
        when(dataProcessingService.loadHistoricalDataFromCsv(anyDouble(), anyDouble(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(mockOceanData));
        when(dataProcessingService.preprocessData(anyList()))
            .thenReturn(Arrays.asList(mockOceanData));
        
        mockMvc.perform(post("/api/v1/water-quality/analyze/monthly-trend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTrendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecastTrends").exists())
                .andExpect(jsonPath("$.forecastTrends").isArray());
    }
    
    @Test
    void testGetHealthStatus_Success() throws Exception {
        // 模拟服务状态
        when(modelService.isModelReady()).thenReturn(true);
        when(modelService.getModelInfo()).thenReturn(Map.of("version", "v1.0", "status", "loaded"));
        
        // Map<String, Object> noaaStatus = new HashMap<>();
        // noaaStatus.put("baseUrl", "https://api.noaa.gov");
        // noaaStatus.put("status", "available");
        // when(noaaDataService.getServiceStatus()).thenReturn(noaaStatus); // 已移除NOAA依赖
        
        mockMvc.perform(get("/api/v1/water-quality/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.modelReady").value(true))
                .andExpect(jsonPath("$.modelInfo.version").value("v1.0"))
                .andExpect(jsonPath("$.dataSource").value("S2/S3 CSV Files")); // 使用CSV数据源
    }
    
    @Test
    void testGetHealthStatus_ServiceDown() throws Exception {
        // 模拟服务异常
        when(modelService.isModelReady()).thenThrow(new RuntimeException("模型服务异常"));
        
        mockMvc.perform(get("/api/v1/water-quality/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.error").exists());
    }
    
    @Test
    void testValidationAnnotations() throws Exception {
        // 测试各种验证注解
        PredictionRequest invalidRequest = new PredictionRequest();
        invalidRequest.setLatitude(91.0); // 超出范围
        invalidRequest.setLongitude(-181.0); // 超出范围
        invalidRequest.setTurbidity(-1.0); // 负值无效
        invalidRequest.setChlorophyllConcentration(-1.0); // 负值无效
        
        mockMvc.perform(post("/api/v1/water-quality/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }
}