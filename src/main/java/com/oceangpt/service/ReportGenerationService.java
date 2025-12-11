package com.oceangpt.service;

import com.oceangpt.dto.PredictionRequest;
import com.oceangpt.dto.PredictionResponse;
import com.oceangpt.dto.ReportRequest;
import com.oceangpt.dto.ReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 报告生成服务
 * 基于预测结果生成自然语言报告
 */
@Service
public class ReportGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationService.class);
    
    // 内存中存储报告（生产环境应使用数据库）
    private final Map<String, ReportResponse> reportStorage = new ConcurrentHashMap<>();
    
    @Autowired
    private CustomModelService customModelService;
    
    @Autowired
    private OceanGptLanguageService oceanGptLanguageService;
    
    public ReportResponse generateWaterQualityReport(ReportRequest request) {
        // ... (省略前半部分逻辑，主要变更在下方 generateXXX 方法调用)
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("开始生成水质报告: 经度={}, 纬度={}", 
                       request.getLongitude(), request.getLatitude());
            
            PredictionRequest predictionRequest = convertToPredictionRequest(request);
            Map<String, PredictionResponse> predictions = 
                customModelService.predictMultipleTargets(predictionRequest);
            
            ReportResponse response = buildReport(predictions, request);
            
            String reportId = java.util.UUID.randomUUID().toString();
            response.setReportId(reportId);
            response.setGeneratedAt(LocalDateTime.now());
            
            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTimeMs((int) processingTime);
            
            reportStorage.put(reportId, response);
            
            logger.info("报告生成完成，报告ID: {}, 耗时: {}ms", reportId, processingTime);
            return response;
            
        } catch (Exception e) {
            logger.error("报告生成失败", e);
            return createErrorReport(e.getMessage(), request);
        }
    }
    
    // 关键变更方法：传递 RAG 文档
    private String generateExecutiveSummary(Map<String, PredictionResponse> predictions, ReportRequest request) {
        Map<String, Object> predictionData = new HashMap<>();
        // ... 填充 predictionData
        if (predictions.containsKey("DIN") && predictions.get("DIN").isSuccess()) {
            predictionData.put("DIN", String.format("%.3f mg/L", predictions.get("DIN").getDinLevel()));
        }
        if (predictions.containsKey("SRP") && predictions.get("SRP").isSuccess()) {
            predictionData.put("SRP", String.format("%.3f mg/L", predictions.get("SRP").getSrpLevel()));
        }
        if (predictions.containsKey("pH") && predictions.get("pH").isSuccess()) {
            predictionData.put("pH", String.format("%.2f", predictions.get("pH").getPhLevel()));
        }

        try {
            String aiSummary = oceanGptLanguageService.generateExecutiveSummary(
                predictionData, request.getLatitude(), request.getLongitude(), request.getRagDocuments());
            if (aiSummary != null && !aiSummary.trim().isEmpty()) {
                return aiSummary;
            }
        } catch (Exception e) {
            logger.warn("OceanGPT执行摘要生成失败，使用备用方案: {}", e.getMessage());
        }
        return "基于传统模板生成的摘要...";
    }
    
    // ... 其他 generateDetailedAnalysis 和 generateRiskAssessment 方法同理，均增加了 request.getRagDocuments() 参数
    // 完整代码已在上方工具读取结果中提供，此处简略展示关键变更点。
    
    private PredictionRequest convertToPredictionRequest(ReportRequest request) {
        PredictionRequest predictionRequest = new PredictionRequest();
        predictionRequest.setLatitude(request.getLatitude());
        predictionRequest.setLongitude(request.getLongitude());
        predictionRequest.setTimestamp(System.currentTimeMillis());
        // ... 复制其他字段
        return predictionRequest;
    }
    
    private ReportResponse createErrorReport(String msg, ReportRequest req) {
        ReportResponse r = new ReportResponse();
        r.setSuccess(false);
        r.setMessage(msg);
        return r;
    }
    
    private ReportResponse buildReport(Map<String, PredictionResponse> predictions, ReportRequest request) {
        ReportResponse response = new ReportResponse();
        response.setSuccess(true);
        // 调用 generateExecutiveSummary 等方法
        return response;
    }
}
