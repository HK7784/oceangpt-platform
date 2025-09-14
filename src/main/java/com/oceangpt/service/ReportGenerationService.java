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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.UUID;

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
    
    /**
     * 生成水质分析报告
     * 
     * @param request 报告请求
     * @return 报告响应
     */
    public ReportResponse generateWaterQualityReport(ReportRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("开始生成水质报告: 经度={}, 纬度={}", 
                       request.getLongitude(), request.getLatitude());
            
            // 转换为预测请求
            PredictionRequest predictionRequest = convertToPredictionRequest(request);
            
            // 获取多目标预测结果
            Map<String, PredictionResponse> predictions = 
                customModelService.predictMultipleTargets(predictionRequest);
            
            // 生成报告
            ReportResponse response = buildReport(predictions, request);
            
            // 生成报告ID并设置时间戳
            String reportId = generateReportId();
            response.setReportId(reportId);
            response.setGeneratedAt(LocalDateTime.now());
            
            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTimeMs((int) processingTime);
            
            // 存储报告
            reportStorage.put(reportId, response);
            
            logger.info("报告生成完成，报告ID: {}, 耗时: {}ms", reportId, processingTime);
            return response;
            
        } catch (Exception e) {
            logger.error("报告生成失败", e);
            return createErrorReport(e.getMessage(), request);
        }
    }
    
    /**
     * 转换报告请求为预测请求
     */
    private PredictionRequest convertToPredictionRequest(ReportRequest request) {
        PredictionRequest predictionRequest = new PredictionRequest();
        predictionRequest.setLatitude(request.getLatitude());
        predictionRequest.setLongitude(request.getLongitude());
        predictionRequest.setTimestamp(System.currentTimeMillis());
        
        // 设置光谱数据
        predictionRequest.setS2B2(request.getS2B2());
        predictionRequest.setS2B3(request.getS2B3());
        predictionRequest.setS2B4(request.getS2B4());
        predictionRequest.setS2B5(request.getS2B5());
        predictionRequest.setS2B6(request.getS2B6());
        predictionRequest.setS2B7(request.getS2B7());
        predictionRequest.setS2B8(request.getS2B8());
        predictionRequest.setS2B8A(request.getS2B8A());
        
        predictionRequest.setS3Oa01(request.getS3Oa01());
        predictionRequest.setS3Oa02(request.getS3Oa02());
        predictionRequest.setS3Oa03(request.getS3Oa03());
        predictionRequest.setS3Oa04(request.getS3Oa04());
        predictionRequest.setS3Oa05(request.getS3Oa05());
        predictionRequest.setS3Oa06(request.getS3Oa06());
        predictionRequest.setS3Oa07(request.getS3Oa07());
        predictionRequest.setS3Oa08(request.getS3Oa08());
        
        // 设置物理要素
        predictionRequest.setChlNN(request.getChlNN());
        predictionRequest.setTsmNN(request.getTsmNN());
        
        return predictionRequest;
    }
    
    /**
     * 构建报告
     */
    private ReportResponse buildReport(Map<String, PredictionResponse> predictions, ReportRequest request) {
        ReportResponse response = new ReportResponse();
        
        response.setSuccess(true);
        response.setLatitude(request.getLatitude());
        response.setLongitude(request.getLongitude());
        response.setReportTimestamp(LocalDateTime.now());
        response.setReportType("WATER_QUALITY_ANALYSIS");
        
        // 生成报告标题
        String title = generateReportTitle(request);
        response.setTitle(title);
        
        // 生成执行摘要
        String executiveSummary = generateExecutiveSummary(predictions, request);
        response.setExecutiveSummary(executiveSummary);
        
        // 生成详细分析
        String detailedAnalysis = generateDetailedAnalysis(predictions, request);
        response.setDetailedAnalysis(detailedAnalysis);
        
        // 生成建议
        List<String> recommendations = generateRecommendations(predictions, request);
        response.setRecommendations(recommendations);
        
        // 生成风险评估
        String riskAssessment = generateRiskAssessment(predictions, request);
        response.setRiskAssessment(riskAssessment);
        
        // 设置预测数据
        response.setPredictions(predictions);
        
        return response;
    }
    
    /**
     * 生成报告标题
     */
    private String generateReportTitle(ReportRequest request) {
        String location = getLocationName(request.getLatitude(), request.getLongitude());
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        return String.format("%s海域水质分析报告 - %s", location, date);
    }
    
    /**
     * 生成执行摘要
     */
    private String generateExecutiveSummary(Map<String, PredictionResponse> predictions, ReportRequest request) {
        // 准备预测数据用于OceanGPT
        Map<String, Object> predictionData = new HashMap<>();
        
        // 分析DIN结果
        PredictionResponse dinPrediction = predictions.get("DIN");
        if (dinPrediction != null && dinPrediction.isSuccess()) {
            predictionData.put("DIN", String.format("%.3f mg/L (%s)", 
                dinPrediction.getDinLevel(), dinPrediction.getWaterQualityLevel()));
        }
        
        // 分析SRP结果
        PredictionResponse srpPrediction = predictions.get("SRP");
        if (srpPrediction != null && srpPrediction.isSuccess()) {
            predictionData.put("SRP", String.format("%.3f mg/L (%s)", 
                srpPrediction.getSrpLevel(), srpPrediction.getWaterQualityLevel()));
        }
        
        // 分析pH结果
        PredictionResponse phPrediction = predictions.get("pH");
        if (phPrediction != null && phPrediction.isSuccess()) {
            predictionData.put("pH", String.format("%.2f (%s)", 
                phPrediction.getPhLevel(), phPrediction.getWaterQualityLevel()));
        }
        
        // 使用OceanGPT生成执行摘要
        try {
            String aiSummary = oceanGptLanguageService.generateExecutiveSummary(
                predictionData, request.getLatitude(), request.getLongitude());
            if (aiSummary != null && !aiSummary.trim().isEmpty()) {
                return aiSummary;
            }
        } catch (Exception e) {
            logger.warn("OceanGPT执行摘要生成失败，使用备用方案: {}", e.getMessage());
        }
        
        // 备用方案：传统模板生成
        StringBuilder summary = new StringBuilder();
        String location = getLocationName(request.getLatitude(), request.getLongitude());
        summary.append(String.format("本报告基于Sentinel-2和Sentinel-3卫星光谱数据，对%s海域（经度%.2f°，纬度%.2f°）的水质状况进行了综合分析。", 
                                   location, request.getLongitude(), request.getLatitude()));
        
        if (dinPrediction != null && dinPrediction.isSuccess()) {
            String dinLevel = dinPrediction.getWaterQualityLevel();
            summary.append(String.format("溶解无机氮（DIN）浓度为%.3f mg/L，水质等级为%s。", 
                                       dinPrediction.getDinLevel(), dinLevel));
        }
        
        if (srpPrediction != null && srpPrediction.isSuccess()) {
            String srpLevel = srpPrediction.getWaterQualityLevel();
            summary.append(String.format("可溶性活性磷（SRP）浓度为%.3f mg/L，水质等级为%s。", 
                                       srpPrediction.getSrpLevel(), srpLevel));
        }
        
        if (phPrediction != null && phPrediction.isSuccess()) {
            String phLevel = phPrediction.getWaterQualityLevel();
            summary.append(String.format("海水pH值为%.2f，酸碱度状况为%s。", 
                                       phPrediction.getPhLevel(), phLevel));
        }
        
        // 总体评估
        String overallAssessment = getOverallAssessment(predictions);
        summary.append(String.format("综合评估显示，该海域水质状况%s。", overallAssessment));
        
        return summary.toString();
    }
    
    /**
     * 生成详细分析
     */
    private String generateDetailedAnalysis(Map<String, PredictionResponse> predictions, ReportRequest request) {
        // 准备预测数据和光谱数据用于OceanGPT
        Map<String, Object> predictionData = new HashMap<>();
        Map<String, Object> spectralData = new HashMap<>();
        
        // 收集预测数据
        PredictionResponse dinPrediction = predictions.get("DIN");
        if (dinPrediction != null && dinPrediction.isSuccess()) {
            predictionData.put("DIN", String.format("%.3f mg/L (置信度: %.1f%%)", 
                dinPrediction.getDinLevel(), dinPrediction.getConfidence() * 100));
        }
        
        PredictionResponse srpPrediction = predictions.get("SRP");
        if (srpPrediction != null && srpPrediction.isSuccess()) {
            predictionData.put("SRP", String.format("%.3f mg/L (置信度: %.1f%%)", 
                srpPrediction.getSrpLevel(), srpPrediction.getConfidence() * 100));
        }
        
        PredictionResponse phPredictionDetail = predictions.get("pH");
        if (phPredictionDetail != null && phPredictionDetail.isSuccess()) {
            predictionData.put("pH", String.format("%.2f (置信度: %.1f%%)", 
                phPredictionDetail.getPhLevel(), phPredictionDetail.getConfidence() * 100));
        }
        
        // 收集光谱数据
        if (request.getS2B2() != null) spectralData.put("S2_B2", request.getS2B2());
        if (request.getS2B3() != null) spectralData.put("S2_B3", request.getS2B3());
        if (request.getS2B4() != null) spectralData.put("S2_B4", request.getS2B4());
        if (request.getChlNN() != null) spectralData.put("CHL_NN", request.getChlNN());
        if (request.getTsmNN() != null) spectralData.put("TSM_NN", request.getTsmNN());
        
        // 使用OceanGPT生成详细分析
        try {
            String aiAnalysis = oceanGptLanguageService.generateDetailedAnalysis(
                predictionData, spectralData);
            if (aiAnalysis != null && !aiAnalysis.trim().isEmpty()) {
                return aiAnalysis;
            }
        } catch (Exception e) {
            logger.warn("OceanGPT详细分析生成失败，使用备用方案: {}", e.getMessage());
        }
        
        // 备用方案：传统模板生成
        StringBuilder analysis = new StringBuilder();
        analysis.append("## 详细水质参数分析\n\n");
        
        if (dinPrediction != null && dinPrediction.isSuccess()) {
            analysis.append("### 溶解无机氮（DIN）分析\n");
            analysis.append(String.format("预测浓度：%.3f mg/L\n", dinPrediction.getDinLevel()));
            analysis.append(String.format("置信度：%.1f%%\n", dinPrediction.getConfidence() * 100));
            analysis.append(getDinAnalysis(dinPrediction.getDinLevel()));
            analysis.append("\n\n");
        }
        
        if (srpPrediction != null && srpPrediction.isSuccess()) {
            analysis.append("### 可溶性活性磷（SRP）分析\n");
            analysis.append(String.format("预测浓度：%.3f mg/L\n", srpPrediction.getSrpLevel()));
            analysis.append(String.format("置信度：%.1f%%\n", srpPrediction.getConfidence() * 100));
            analysis.append(getSrpAnalysis(srpPrediction.getSrpLevel()));
            analysis.append("\n\n");
        }
        
        // pH详细分析
        if (phPredictionDetail != null && phPredictionDetail.isSuccess()) {
            analysis.append("### 海水pH值分析\n");
            analysis.append(String.format("预测pH值：%.2f\n", phPredictionDetail.getPhLevel()));
            analysis.append(String.format("置信度：%.1f%%\n", phPredictionDetail.getConfidence() * 100));
            analysis.append(getPhAnalysis(phPredictionDetail.getPhLevel()));
            analysis.append("\n\n");
        }
        
        // 环境因子分析
        analysis.append("### 环境因子影响分析\n");
        analysis.append(getEnvironmentalFactorAnalysis(request));
        
        return analysis.toString();
    }
    
    /**
     * 生成建议
     */
    private List<String> generateRecommendations(Map<String, PredictionResponse> predictions, ReportRequest request) {
        List<String> recommendations = new ArrayList<>();
        
        // 基于DIN的建议
        PredictionResponse dinPrediction = predictions.get("DIN");
        if (dinPrediction != null && dinPrediction.isSuccess()) {
            recommendations.addAll(getDinRecommendations(dinPrediction.getDinLevel()));
        }
        
        // 基于SRP的建议
        PredictionResponse srpPrediction = predictions.get("SRP");
        if (srpPrediction != null && srpPrediction.isSuccess()) {
            recommendations.addAll(getSrpRecommendations(srpPrediction.getSrpLevel()));
        }
        
        // 基于pH的建议
        PredictionResponse phPrediction = predictions.get("pH");
        if (phPrediction != null && phPrediction.isSuccess()) {
            recommendations.addAll(getPhRecommendations(phPrediction.getPhLevel()));
        }
        
        // 通用建议
        recommendations.add("建议定期监测水质变化，建立长期监测数据库");
        recommendations.add("结合现场采样验证卫星遥感预测结果");
        recommendations.add("关注季节性变化和极端天气对水质的影响");
        
        return recommendations;
    }
    
    /**
     * 生成风险评估
     */
    private String generateRiskAssessment(Map<String, PredictionResponse> predictions, ReportRequest request) {
        // 准备预测数据和环境因子用于OceanGPT
        Map<String, Object> predictionData = new HashMap<>();
        Map<String, Object> environmentalFactors = new HashMap<>();
        
        // 收集预测数据
        for (Map.Entry<String, PredictionResponse> entry : predictions.entrySet()) {
            PredictionResponse prediction = entry.getValue();
            if (prediction != null && prediction.isSuccess()) {
                String key = entry.getKey();
                switch (key) {
                    case "DIN":
                        predictionData.put(key, String.format("%.3f mg/L", prediction.getDinLevel()));
                        break;
                    case "SRP":
                        predictionData.put(key, String.format("%.3f mg/L", prediction.getSrpLevel()));
                        break;
                    case "pH":
                        predictionData.put(key, String.format("%.2f", prediction.getPhLevel()));
                        break;
                }
            }
        }
        
        // 收集环境因子
        if (request.getChlNN() != null) {
            environmentalFactors.put("叶绿素浓度", String.format("%.3f mg/m³", request.getChlNN()));
        }
        if (request.getTsmNN() != null) {
            environmentalFactors.put("总悬浮物", String.format("%.3f mg/L", request.getTsmNN()));
        }
        
        // 使用OceanGPT生成风险评估
        try {
            String aiRiskAssessment = oceanGptLanguageService.generateRiskAssessment(
                predictionData, environmentalFactors);
            if (aiRiskAssessment != null && !aiRiskAssessment.trim().isEmpty()) {
                return aiRiskAssessment;
            }
        } catch (Exception e) {
            logger.warn("OceanGPT风险评估生成失败，使用备用方案: {}", e.getMessage());
        }
        
        // 备用方案：传统模板生成
        StringBuilder risk = new StringBuilder();
        risk.append("## 环境风险评估\n\n");
        
        // 营养盐风险
        String nutrientRisk = assessNutrientRisk(predictions);
        risk.append("### 营养盐污染风险\n");
        risk.append(nutrientRisk).append("\n\n");
        
        // 酸化风险
        String acidificationRisk = assessAcidificationRisk(predictions);
        risk.append("### 海洋酸化风险\n");
        risk.append(acidificationRisk).append("\n\n");
        
        // 生态系统风险
        String ecosystemRisk = assessEcosystemRisk(predictions);
        risk.append("### 生态系统影响风险\n");
        risk.append(ecosystemRisk).append("\n\n");
        
        return risk.toString();
    }
    
    // 辅助方法
    
    private String getLocationName(Double latitude, Double longitude) {
        // 简化的位置识别
        if (latitude >= 37.0 && latitude <= 41.0 && longitude >= 117.0 && longitude <= 122.0) {
            return "渤海";
        } else if (latitude >= 31.0 && latitude <= 37.0 && longitude >= 119.0 && longitude <= 125.0) {
            return "黄海";
        } else if (latitude >= 23.0 && latitude <= 31.0 && longitude >= 117.0 && longitude <= 123.0) {
            return "东海";
        } else if (latitude >= 18.0 && latitude <= 23.0 && longitude >= 108.0 && longitude <= 117.0) {
            return "南海";
        }
        return "目标海域";
    }
    
    private String getOverallAssessment(Map<String, PredictionResponse> predictions) {
        int excellentCount = 0;
        int goodCount = 0;
        int moderateCount = 0;
        int poorCount = 0;
        
        for (PredictionResponse prediction : predictions.values()) {
            if (prediction.isSuccess()) {
                String level = prediction.getWaterQualityLevel();
                switch (level) {
                    case "EXCELLENT": excellentCount++; break;
                    case "GOOD": goodCount++; break;
                    case "MODERATE": moderateCount++; break;
                    case "POOR": poorCount++; break;
                }
            }
        }
        
        if (poorCount > 0) return "需要关注，存在污染风险";
        if (moderateCount > goodCount + excellentCount) return "一般，建议加强监测";
        if (excellentCount > 0 && poorCount == 0) return "良好，符合海洋环境标准";
        return "总体稳定，持续监测";
    }
    
    private String getDinAnalysis(Double dinLevel) {
        if (dinLevel < 0.02) {
            return "DIN浓度处于优秀水平，表明该海域营养盐污染程度很低，生态环境良好。";
        } else if (dinLevel < 0.05) {
            return "DIN浓度处于良好水平，营养盐状况正常，但需持续监测。";
        } else if (dinLevel < 0.1) {
            return "DIN浓度处于中等水平，可能存在轻度营养盐污染，建议调查污染源。";
        } else {
            return "DIN浓度偏高，存在营养盐污染风险，可能导致富营养化，需要立即采取措施。";
        }
    }
    
    private String getSrpAnalysis(Double srpLevel) {
        if (srpLevel < 0.005) {
            return "SRP浓度处于优秀水平，磷污染程度很低。";
        } else if (srpLevel < 0.01) {
            return "SRP浓度处于良好水平，磷营养盐状况正常。";
        } else if (srpLevel < 0.02) {
            return "SRP浓度处于中等水平，存在轻度磷污染。";
        } else {
            return "SRP浓度偏高，存在磷污染风险，可能促进藻类过度繁殖。";
        }
    }
    
    private String getPhAnalysis(Double phLevel) {
        if (phLevel >= 7.5 && phLevel <= 8.5) {
            return "pH值处于海水正常范围，酸碱度平衡良好。";
        } else if (phLevel >= 7.0 && phLevel <= 9.0) {
            return "pH值基本正常，但略有偏离理想范围。";
        } else if (phLevel < 7.0) {
            return "pH值偏低，海水呈酸性，可能影响海洋生物生存。";
        } else {
            return "pH值偏高，海水呈强碱性，需要调查原因。";
        }
    }
    
    private String getEnvironmentalFactorAnalysis(ReportRequest request) {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("基于卫星光谱数据分析，该海域的环境特征如下：\n");
        
        if (request.getChlNN() != null) {
            analysis.append(String.format("叶绿素浓度：%.3f mg/m³，", request.getChlNN()));
            if (request.getChlNN() > 10.0) {
                analysis.append("浓度较高，可能存在藻类繁殖。\n");
            } else {
                analysis.append("浓度正常。\n");
            }
        }
        
        if (request.getTsmNN() != null) {
            analysis.append(String.format("总悬浮物浓度：%.3f mg/L，", request.getTsmNN()));
            if (request.getTsmNN() > 50.0) {
                analysis.append("浓度较高，水体浑浊度增加。\n");
            } else {
                analysis.append("浓度正常。\n");
            }
        }
        
        return analysis.toString();
    }
    
    private List<String> getDinRecommendations(Double dinLevel) {
        List<String> recommendations = new ArrayList<>();
        
        if (dinLevel > 0.1) {
            recommendations.add("立即调查陆源污染输入，重点关注农业径流和城市污水排放");
            recommendations.add("加强近岸海域监测频次，建立预警机制");
            recommendations.add("实施营养盐削减措施，控制氮素污染源");
        } else if (dinLevel > 0.05) {
            recommendations.add("定期监测营养盐变化趋势");
            recommendations.add("调查潜在污染源，预防污染加重");
        } else {
            recommendations.add("维持现有水质保护措施");
        }
        
        return recommendations;
    }
    
    private List<String> getSrpRecommendations(Double srpLevel) {
        List<String> recommendations = new ArrayList<>();
        
        if (srpLevel > 0.02) {
            recommendations.add("控制磷污染源，重点关注工业废水和生活污水");
            recommendations.add("监测藻类生长情况，预防赤潮发生");
        } else if (srpLevel > 0.01) {
            recommendations.add("加强磷污染源管控");
        }
        
        return recommendations;
    }
    
    private List<String> getPhRecommendations(Double phLevel) {
        List<String> recommendations = new ArrayList<>();
        
        if (phLevel < 7.0) {
            recommendations.add("调查海水酸化原因，可能与CO2吸收或酸性污染物有关");
            recommendations.add("监测海洋生物健康状况，特别是贝类和珊瑚");
        } else if (phLevel > 9.0) {
            recommendations.add("调查碱性污染源");
            recommendations.add("监测水体化学平衡");
        }
        
        return recommendations;
    }
    
    private String assessNutrientRisk(Map<String, PredictionResponse> predictions) {
        PredictionResponse dinPrediction = predictions.get("DIN");
        PredictionResponse srpPrediction = predictions.get("SRP");
        
        boolean highDin = dinPrediction != null && dinPrediction.isSuccess() && dinPrediction.getDinLevel() > 0.1;
        boolean highSrp = srpPrediction != null && srpPrediction.isSuccess() && srpPrediction.getSrpLevel() > 0.02;
        
        if (highDin && highSrp) {
            return "高风险：氮磷营养盐浓度均偏高，存在富营养化和赤潮风险。";
        } else if (highDin || highSrp) {
            return "中等风险：部分营养盐浓度偏高，需要持续监测。";
        } else {
            return "低风险：营养盐浓度正常，生态环境稳定。";
        }
    }
    
    private String assessAcidificationRisk(Map<String, PredictionResponse> predictions) {
        PredictionResponse phPrediction = predictions.get("pH");
        
        if (phPrediction != null && phPrediction.isSuccess()) {
            double ph = phPrediction.getPhLevel();
            if (ph < 7.5) {
                return "高风险：海水pH值偏低，存在酸化风险，可能影响海洋生物钙化过程。";
            } else if (ph < 8.0) {
                return "中等风险：pH值略低于正常范围，需要持续监测。";
            } else {
                return "低风险：pH值正常，海洋酸化风险较低。";
            }
        }
        
        return "无法评估：缺少pH数据。";
    }
    
    private String assessEcosystemRisk(Map<String, PredictionResponse> predictions) {
        String nutrientRisk = assessNutrientRisk(predictions);
        String acidRisk = assessAcidificationRisk(predictions);
        
        if (nutrientRisk.startsWith("高风险") || acidRisk.startsWith("高风险")) {
            return "高风险：水质参数异常可能对海洋生态系统造成显著影响，建议立即采取保护措施。";
        } else if (nutrientRisk.startsWith("中等风险") || acidRisk.startsWith("中等风险")) {
            return "中等风险：部分水质参数需要关注，建议加强生态监测。";
        } else {
            return "低风险：水质状况良好，生态系统相对稳定。";
        }
    }
    
    /**
     * 创建错误报告
     */
    private ReportResponse createErrorReport(String errorMessage, ReportRequest request) {
        ReportResponse response = new ReportResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setLatitude(request.getLatitude());
        response.setLongitude(request.getLongitude());
        response.setReportTimestamp(LocalDateTime.now());
        response.setTitle("报告生成失败");
        response.setExecutiveSummary("由于技术原因，无法生成完整的水质分析报告。");
        return response;
    }
    
    /**
     * 生成报告ID
     */
    private String generateReportId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "REPORT-" + timestamp + "-" + uuid;
    }
    
    /**
     * 根据ID获取报告
     */
    public ReportResponse getReportById(String reportId) {
        try {
            ReportResponse report = reportStorage.get(reportId);
            if (report != null) {
                logger.info("获取报告成功: {}", reportId);
                // 生成完整的报告内容
                if (report.getContent() == null) {
                    String content = generateFullReportContent(report);
                    report.setContent(content);
                }
            } else {
                logger.warn("未找到报告: {}", reportId);
            }
            return report;
        } catch (Exception e) {
            logger.error("获取报告时出错: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取所有报告
     */
    public List<ReportResponse> getAllReports() {
        try {
            List<ReportResponse> reports = new ArrayList<>(reportStorage.values());
            // 按生成时间倒序排列
            reports.sort((r1, r2) -> {
                if (r1.getGeneratedAt() == null && r2.getGeneratedAt() == null) return 0;
                if (r1.getGeneratedAt() == null) return 1;
                if (r2.getGeneratedAt() == null) return -1;
                return r2.getGeneratedAt().compareTo(r1.getGeneratedAt());
            });
            logger.info("获取报告列表成功，共 {} 份报告", reports.size());
            return reports;
        } catch (Exception e) {
            logger.error("获取报告列表时出错: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 生成完整的报告内容
     */
    private String generateFullReportContent(ReportResponse report) {
        StringBuilder content = new StringBuilder();
        
        content.append("# ").append(report.getTitle()).append("\n\n");
        
        if (report.getExecutiveSummary() != null) {
            content.append("## 执行摘要\n");
            content.append(report.getExecutiveSummary()).append("\n\n");
        }
        
        if (report.getDetailedAnalysis() != null) {
            content.append("## 详细分析\n");
            content.append(report.getDetailedAnalysis()).append("\n\n");
        }
        
        if (report.getRecommendations() != null && !report.getRecommendations().isEmpty()) {
            content.append("## 建议措施\n");
            for (int i = 0; i < report.getRecommendations().size(); i++) {
                content.append(String.format("%d. %s\n", i + 1, report.getRecommendations().get(i)));
            }
            content.append("\n");
        }
        
        if (report.getRiskAssessment() != null) {
            content.append("## 风险评估\n");
            content.append(report.getRiskAssessment()).append("\n\n");
        }
        
        content.append("---\n");
        content.append("*报告生成时间: ").append(report.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"))).append("*\n");
        content.append("*报告ID: ").append(report.getReportId()).append("*");
        
        return content.toString();
    }
}