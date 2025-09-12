package com.oceangpt.controller;

import com.oceangpt.dto.*;
import com.oceangpt.model.OceanData;
import com.oceangpt.model.PredictionResult;
import com.oceangpt.service.CustomModelService;
import com.oceangpt.service.InferenceService;
import com.oceangpt.service.ModelService;
// import com.oceangpt.service.NoaaDataService; // 已移除NOAA依赖
import com.oceangpt.service.TrendAnalysisService;
import com.oceangpt.service.ReportGenerationService;
import com.oceangpt.service.QueryExplanationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 水质监测和预测API控制器
 * 提供海洋水质预测和趋势分析功能
 */
@RestController
@RequestMapping("/v1/water-quality")
@CrossOrigin(origins = "*")
@Validated
@Tag(name = "水质监测API", description = "海洋水质预测和趋势分析接口")
public class WaterQualityController {
    
    private static final Logger logger = LoggerFactory.getLogger(WaterQualityController.class);
    
    // @Autowired
    // private NoaaDataService noaaDataService; // 已移除NOAA依赖
    
    @Autowired
    private ModelService modelService;
    
    @Autowired
    private InferenceService inferenceService;
    
    @Autowired
    private TrendAnalysisService trendAnalysisService;
    
    @Autowired
    private CustomModelService customModelService;
    
    @Autowired
    private ReportGenerationService reportGenerationService;
    
    @Autowired
    private QueryExplanationService queryExplanationService;

    
    /**
     * 实时水质预测接口
     * @param request 预测请求参数
     * @return 预测结果
     */
    @PostMapping("/predict")
    @Operation(summary = "实时水质预测", description = "基于输入的海洋数据进行实时水质预测")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "预测成功", 
                    content = @Content(schema = @Schema(implementation = PredictionResponse.class))),
        @ApiResponse(responseCode = "400", description = "请求参数无效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<PredictionResponse> predictWaterQuality(
            @Parameter(description = "预测请求参数", required = true)
            @Valid @RequestBody PredictionRequest request) {
        
        long startTime = System.currentTimeMillis();
        logger.info("收到水质预测请求: {}", request);
        
        try {
            // 使用自定义模型进行预测
            PredictionResponse response = customModelService.predictWaterQuality(request);
            
            // 设置处理时间
            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTimeMs((int) processingTime);
            
            logger.info("水质预测完成，处理时间: {}ms", processingTime);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("水质预测过程中发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("预测服务异常: " + e.getMessage(), startTime));
        }
    }
    
    /**
     * 月度趋势分析接口
     * @param request 趋势分析请求参数
     * @return 趋势分析结果
     */
    @PostMapping("/analyze/monthly-trend")
    @Operation(summary = "月度趋势分析", description = "分析指定区域和时间段的海洋水质变化趋势")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "分析成功", 
                    content = @Content(schema = @Schema(implementation = TrendAnalysisResponse.class))),
        @ApiResponse(responseCode = "400", description = "请求参数无效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<TrendAnalysisResponse> analyzeMonthlyTrend(
            @Parameter(description = "趋势分析请求参数", required = true)
            @Valid @RequestBody TrendAnalysisRequest request) {
        
        long startTime = System.currentTimeMillis();
        logger.info("收到月度趋势分析请求: {}", request);
        
        try {
            // 验证日期范围
            if (request.getStartDate().isAfter(request.getEndDate())) {
                return ResponseEntity.badRequest()
                    .body(createTrendErrorResponse("开始日期不能晚于结束日期", startTime));
            }
            
            // 使用TrendAnalysisService执行分析
            TrendAnalysisResponse response = trendAnalysisService.analyzeMonthlyTrend(request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("趋势分析过程中发生错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createTrendErrorResponse("趋势分析服务异常: " + e.getMessage(), startTime));
        }
    }
    
    /**
     * 简单的健康检查端点
     * @return 基本状态信息
     */
    @GetMapping("/health")
    @Operation(summary = "服务健康检查", description = "检查水质预测服务的健康状态")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("service", "OceanGPT Water Quality Prediction");
            health.put("version", "1.0.0");
            health.put("dataSource", "S2/S3 CSV Files");
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
    
    /**
     * 详细的模型状态检查端点
     * @return 模型状态信息
     */
    @GetMapping("/model-status")
    @Operation(summary = "模型状态检查", description = "检查模型服务的详细状态")
    public ResponseEntity<Map<String, Object>> getModelStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("timestamp", LocalDateTime.now());
            
            // 安全地检查模型状态
            try {
                status.put("modelReady", modelService.isModelReady());
            } catch (Exception e) {
                logger.error("Error checking model readiness: {}", e.getMessage(), e);
                status.put("modelReady", false);
                status.put("modelError", e.getMessage());
            }
            
            // 安全地获取模型信息
            try {
                status.put("modelInfo", modelService.getModelInfo());
            } catch (Exception e) {
                logger.error("Error getting model info: {}", e.getMessage(), e);
                status.put("modelInfoError", e.getMessage());
            }
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Model status check failed: {}", e.getMessage(), e);
            status.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(status);
        }
    }
    
    // 私有辅助方法
    
    /**
     * 将请求转换为OceanData对象
     */
    private OceanData convertToOceanData(PredictionRequest request) {
        OceanData oceanData = new OceanData();
        oceanData.setLatitude(request.getLatitude());
        oceanData.setLongitude(request.getLongitude());
        oceanData.setTimestamp(LocalDateTime.ofEpochSecond(request.getTimestamp() / 1000, 0, java.time.ZoneOffset.UTC));
        oceanData.setChlorophyllConcentration(request.getChlorophyllConcentration());
        oceanData.setDataSource("API_REQUEST");
        
        return oceanData;
    }
    
    /**
     * 构建预测响应
     */
    private PredictionResponse buildPredictionResponse(PredictionResult predictionResult, long startTime) {
        PredictionResponse response = new PredictionResponse();
        
        // 设置预测结果
        response.setNutrient(predictionResult.getNutrientLevel());
        response.setPh(predictionResult.getPhLevel());
        response.setConfidence(predictionResult.getConfidence());
        response.setSeaSurfaceTemperature(predictionResult.getSeaSurfaceTemperature());
        response.setSalinity(predictionResult.getSalinity());
        response.setDissolvedOxygen(predictionResult.getDissolvedOxygen());
        response.setChlorophyllConcentration(predictionResult.getChlorophyllConcentration());
        response.setPollutionIndex(predictionResult.getPollutionIndex());
        
        // 设置水质等级
        response.setQualityLevel(determineQualityLevel(predictionResult.getPollutionIndex()));
        
        // 设置元数据
        response.setModelVersion("v1.0");
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        response.setPredictionTimestamp(LocalDateTime.now());
        
        // 添加警告信息
        List<String> warnings = new ArrayList<>();
        if (response.getConfidence() < 0.7) {
            warnings.add("预测置信度较低，建议结合其他数据源验证");
        }
        if (response.getPollutionIndex() > 0.7) {
            warnings.add("检测到较高污染指数，建议进一步监测");
        }
        response.setWarnings(warnings.toArray(new String[0]));
        
        return response;
    }
    
    /**
     * 执行趋势分析
     */
    private TrendAnalysisResponse performTrendAnalysis(TrendAnalysisRequest request, 
                                                     List<OceanData> processedData, long startTime) {
        TrendAnalysisResponse response = new TrendAnalysisResponse();
        
        // 设置分析信息
        TrendAnalysisResponse.AnalysisInfo analysisInfo = new TrendAnalysisResponse.AnalysisInfo(
            request.getLatitude(), request.getLongitude(),
            request.getStartDate().toString(), request.getEndDate().toString(),
            request.getAggregationType(), processedData.size()
        );
        response.setAnalysisInfo(analysisInfo);
        
        // 生成历史趋势数据点
        List<TrendAnalysisResponse.TrendDataPoint> historicalTrends = 
            generateHistoricalTrends(processedData, request.getAggregationType());
        response.setHistoricalTrends(historicalTrends);
        
        // 计算统计摘要
        TrendAnalysisResponse.StatisticalSummary summary = calculateStatisticalSummary(processedData);
        response.setStatisticalSummary(summary);
        
        // 计算趋势指标
        Map<String, TrendAnalysisResponse.TrendIndicator> indicators = calculateTrendIndicators(processedData);
        response.setTrendIndicators(indicators);
        
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        return response;
    }
    
    /**
     * 生成历史趋势数据点
     */
    private List<TrendAnalysisResponse.TrendDataPoint> generateHistoricalTrends(
            List<OceanData> data, String aggregationType) {
        List<TrendAnalysisResponse.TrendDataPoint> trends = new ArrayList<>();
        
        // 简化实现：按时间排序并生成数据点
        data.sort(Comparator.comparing(OceanData::getTimestamp));
        
        for (OceanData oceanData : data) {
            Map<String, Double> values = new HashMap<>();
            values.put("temperature", oceanData.getSeaSurfaceTemperature());
            values.put("salinity", oceanData.getSalinity());
            values.put("ph", oceanData.getPhLevel());
            values.put("oxygen", oceanData.getDissolvedOxygen());
            values.put("chlorophyll", oceanData.getChlorophyllConcentration());
            
            TrendAnalysisResponse.TrendDataPoint point = new TrendAnalysisResponse.TrendDataPoint(
                oceanData.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                values, 0.9 // 历史数据置信度较高
            );
            trends.add(point);
        }
        
        return trends;
    }
    
    /**
     * 计算统计摘要
     */
    private TrendAnalysisResponse.StatisticalSummary calculateStatisticalSummary(List<OceanData> data) {
        TrendAnalysisResponse.StatisticalSummary summary = new TrendAnalysisResponse.StatisticalSummary();
        
        Map<String, TrendAnalysisResponse.ParameterStats> parameterStats = new HashMap<>();
        
        // 计算温度统计
        List<Double> temperatures = data.stream()
            .map(OceanData::getSeaSurfaceTemperature)
            .filter(Objects::nonNull)
            .toList();
        if (!temperatures.isEmpty()) {
            parameterStats.put("temperature", calculateParameterStats(temperatures));
        }
        
        // 计算盐度统计
        List<Double> salinities = data.stream()
            .map(OceanData::getSalinity)
            .filter(Objects::nonNull)
            .toList();
        if (!salinities.isEmpty()) {
            parameterStats.put("salinity", calculateParameterStats(salinities));
        }
        
        // 计算pH统计
        List<Double> phLevels = data.stream()
            .map(OceanData::getPhLevel)
            .filter(Objects::nonNull)
            .toList();
        if (!phLevels.isEmpty()) {
            parameterStats.put("ph", calculateParameterStats(phLevels));
        }
        
        summary.setParameterStats(parameterStats);
        summary.setOverallQualityTrend("stable"); // 简化实现
        summary.setAnomalyCount(0); // 简化实现
        
        return summary;
    }
    
    /**
     * 计算参数统计信息
     */
    private TrendAnalysisResponse.ParameterStats calculateParameterStats(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
        double standardDeviation = Math.sqrt(variance);
        
        return new TrendAnalysisResponse.ParameterStats(mean, min, max, standardDeviation, "stable");
    }
    
    /**
     * 计算趋势指标
     */
    private Map<String, TrendAnalysisResponse.TrendIndicator> calculateTrendIndicators(List<OceanData> data) {
        Map<String, TrendAnalysisResponse.TrendIndicator> indicators = new HashMap<>();
        
        // 简化实现：为主要参数创建趋势指标
        indicators.put("temperature", new TrendAnalysisResponse.TrendIndicator("stable", 0.5, 0.05));
        indicators.put("salinity", new TrendAnalysisResponse.TrendIndicator("stable", 0.3, 0.1));
        indicators.put("ph", new TrendAnalysisResponse.TrendIndicator("stable", 0.4, 0.08));
        
        return indicators;
    }
    
    /**
     * 添加预测数据
     */
    private void addForecastData(TrendAnalysisResponse response, TrendAnalysisRequest request, 
                               List<OceanData> historicalData) {
        // 简化实现：生成模拟预测数据
        List<TrendAnalysisResponse.TrendDataPoint> forecastTrends = new ArrayList<>();
        
        LocalDateTime lastDate = historicalData.stream()
            .map(OceanData::getTimestamp)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        for (int i = 1; i <= request.getForecastDays(); i++) {
            LocalDateTime forecastDate = lastDate.plusDays(i);
            
            Map<String, Double> values = new HashMap<>();
            values.put("temperature", 15.0 + Math.random() * 5); // 模拟预测值
            values.put("salinity", 35.0 + Math.random() * 2);
            values.put("ph", 8.0 + Math.random() * 0.5);
            values.put("oxygen", 8.0 + Math.random() * 1);
            values.put("chlorophyll", 0.5 + Math.random() * 0.3);
            
            TrendAnalysisResponse.TrendDataPoint point = new TrendAnalysisResponse.TrendDataPoint(
                forecastDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                values, 0.7 - (i * 0.01) // 预测置信度随时间递减
            );
            forecastTrends.add(point);
        }
        
        response.setForecastTrends(forecastTrends);
    }
    
    /**
     * 确定水质等级
     */
    private String determineQualityLevel(Double pollutionIndex) {
        if (pollutionIndex == null) return "未知";
        
        if (pollutionIndex < 0.3) return "优秀";
        else if (pollutionIndex < 0.5) return "良好";
        else if (pollutionIndex < 0.7) return "一般";
        else return "较差";
    }
    
    /**
     * 创建错误响应
     */
    private PredictionResponse createErrorResponse(String errorMessage, long startTime) {
        PredictionResponse response = new PredictionResponse();
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        response.setWarnings(new String[]{errorMessage});
        return response;
    }
    
    /**
     * 创建趋势分析错误响应
     */
    private TrendAnalysisResponse createTrendErrorResponse(String errorMessage, long startTime) {
        TrendAnalysisResponse response = new TrendAnalysisResponse();
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        return response;
    }
    
    /**
     * 创建空的趋势响应
     */
    private TrendAnalysisResponse createEmptyTrendResponse(TrendAnalysisRequest request, long startTime) {
        TrendAnalysisResponse response = new TrendAnalysisResponse();
        
        TrendAnalysisResponse.AnalysisInfo analysisInfo = new TrendAnalysisResponse.AnalysisInfo(
            request.getLatitude(), request.getLongitude(),
            request.getStartDate().toString(), request.getEndDate().toString(),
            request.getAggregationType(), 0
        );
        response.setAnalysisInfo(analysisInfo);
        response.setHistoricalTrends(new ArrayList<>());
        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        return response;
    }
    
    /**
     * 生成水质分析报告
     */
    @PostMapping("/analyze/report")
    @Operation(summary = "生成水质分析报告", description = "基于预测结果生成自然语言报告")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "报告生成成功"),
        @ApiResponse(responseCode = "400", description = "请求参数无效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<ReportResponse> generateReport(
            @Valid @RequestBody ReportRequest request) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Generating water quality report for location: ({}, {})", 
                       request.getLatitude(), request.getLongitude());
            
            // 生成报告
            ReportResponse response = reportGenerationService.generateWaterQualityReport(request);
            
            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTimeMs((int) processingTime);
            
            logger.info("Report generated successfully in {}ms", processingTime);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            
            ReportResponse errorResponse = new ReportResponse(false);
            errorResponse.setErrorMessage("请求参数无效: " + e.getMessage());
            errorResponse.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error generating report: {}", e.getMessage(), e);
            
            ReportResponse errorResponse = new ReportResponse(false);
            errorResponse.setErrorMessage("报告生成失败: " + e.getMessage());
            errorResponse.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 自然语言查询和解释
     */
    @PostMapping("/query/explain")
    @Operation(summary = "自然语言查询和解释", description = "支持自然语言查询，提供智能解释和回答")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询处理成功"),
        @ApiResponse(responseCode = "400", description = "请求参数无效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<QueryResponse> explainQuery(
            @Valid @RequestBody QueryRequest request) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Processing natural language query: {}", request.getQuery());
            
            // 处理查询
            QueryResponse response = queryExplanationService.processQuery(request);
            
            long processingTime = System.currentTimeMillis() - startTime;
            if (response.getProcessingTimeMs() == null) {
                response.setProcessingTimeMs((int) processingTime);
            }
            
            logger.info("Query processed successfully in {}ms", processingTime);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid query parameters: {}", e.getMessage());
            
            QueryResponse errorResponse = new QueryResponse(false);
            errorResponse.setErrorMessage("查询参数无效: " + e.getMessage());
            errorResponse.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Error processing query: {}", e.getMessage(), e);
            
            QueryResponse errorResponse = new QueryResponse(false);
            errorResponse.setErrorMessage("查询处理失败: " + e.getMessage());
            errorResponse.setProcessingTimeMs((int) (System.currentTimeMillis() - startTime));
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}