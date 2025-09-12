package com.oceangpt.service;

import com.oceangpt.dto.ChatRequest;
import com.oceangpt.dto.ChatResponse;
import com.oceangpt.dto.QueryRequest;
import com.oceangpt.dto.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

@Service
public class ChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    @Autowired
    private QueryExplanationService queryExplanationService;
    
    @Autowired
    private OceanGptLanguageService oceanGptLanguageService;
    
    @Autowired
    private ParameterMappingService parameterMappingService;
    
    @Autowired
    private CustomModelService customModelService;
    
    @Autowired
    private UserFriendlyService userFriendlyService;
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private ReportGenerationService reportGenerationService;
    
    // 内存中存储聊天历史（生产环境应使用数据库）
    private final Map<String, List<ChatResponse>> chatHistories = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> sessionLastActivity = new ConcurrentHashMap<>();
    
    // 会话超时时间（小时）
    private static final int SESSION_TIMEOUT_HOURS = 24;
    
    /**
     * 处理聊天消息
     */
    public ChatResponse processMessage(ChatRequest request) {
        logger.info("处理聊天消息: sessionId={}, message={}", request.getSessionId(), request.getMessage());
        
        try {
            // 更新会话活动时间
            updateSessionActivity(request.getSessionId());
            
            // 检查是否为帮助请求
            if (userFriendlyService.isHelpRequest(request.getMessage())) {
                return createHelpResponse(request.getSessionId());
            }
            
            // 检查是否为参数解释请求
            String parameterName = userFriendlyService.extractParameterExplanationRequest(request.getMessage());
            if (parameterName != null) {
                return createParameterExplanationResponse(request.getSessionId(), parameterName);
            }
            
            // 检查是否为单位解释请求
            String unitName = userFriendlyService.extractUnitExplanationRequest(request.getMessage());
            if (unitName != null) {
                return createUnitExplanationResponse(request.getSessionId(), unitName);
            }
            
            // 检查是否为报告查看请求
            if (isReportViewRequest(request.getMessage())) {
                return handleReportViewRequest(request);
            }
            
            // 检查是否为报告列表请求
            if (isReportListRequest(request.getMessage())) {
                return handleReportListRequest(request);
            }
            
            // 检查是否为水质预测请求
            if (isPredictionRequest(request.getMessage())) {
                return handlePredictionRequest(request);
            }
            
            // 检查是否为大连监测点分布请求
            if (isDalianMonitoringPointsRequest(request.getMessage())) {
                return handleDalianMonitoringPointsRequest(request);
            }
            
            // 构建查询请求
            QueryRequest queryRequest = buildQueryRequest(request);
            
            // 调用现有的查询解释服务
            QueryResponse queryResponse = queryExplanationService.processQuery(queryRequest);
            
            // 转换为聊天响应
            ChatResponse chatResponse = convertToChatResponse(queryResponse, request.getSessionId());
            
            // 保存到聊天历史
            saveChatHistory(request.getSessionId(), chatResponse);
            
            // 增强回复内容
            enhanceResponse(chatResponse, request);
            
            return chatResponse;
            
        } catch (Exception e) {
            logger.error("处理聊天消息时出错: {}", e.getMessage(), e);
            return createFriendlyErrorResponse(request.getSessionId(), e.getMessage());
        }
    }
    
    /**
     * 检查是否为报告查看请求
     */
    private boolean isReportViewRequest(String message) {
        String lowerMessage = message.toLowerCase();
        return (lowerMessage.contains("查看报告") || lowerMessage.contains("报告详情") || 
                lowerMessage.contains("显示报告") || lowerMessage.contains("view report")) &&
               lowerMessage.matches(".*[a-zA-Z0-9\\-]+.*"); // 包含报告ID
    }
    
    /**
     * 检查是否为报告列表请求
     */
    private boolean isReportListRequest(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("报告列表") || lowerMessage.contains("历史报告") ||
               lowerMessage.contains("所有报告") || lowerMessage.contains("report list") ||
               lowerMessage.contains("list reports");
    }
    
    /**
     * 检查是否为预测请求
     */
    private boolean isPredictionRequest(String message) {
        String lowerMessage = message.toLowerCase();
        
        // 预测相关关键词
        boolean hasPredictionKeywords = lowerMessage.contains("预测") || 
                                      lowerMessage.contains("预报") ||
                                      lowerMessage.contains("未来") ||
                                      lowerMessage.contains("明天") ||
                                      lowerMessage.contains("下周") ||
                                      lowerMessage.contains("下月") ||
                                      lowerMessage.contains("predict") ||
                                      lowerMessage.contains("forecast");
        
        // 坐标相关关键词（更宽松的匹配）
        boolean hasCoordinateKeywords = lowerMessage.contains("经度") ||
                                      lowerMessage.contains("纬度") ||
                                      lowerMessage.contains("经纬度") ||
                                      lowerMessage.contains("坐标") ||
                                      lowerMessage.contains("位置") ||
                                      lowerMessage.contains("地点") ||
                                      lowerMessage.contains("longitude") ||
                                      lowerMessage.contains("latitude") ||
                                      lowerMessage.contains("coordinate") ||
                                      lowerMessage.contains("location") ||
                                      lowerMessage.matches(".*\\d+\\.\\d+.*") || // 包含小数格式的数字
                                      lowerMessage.matches(".*\\d+\\s*,\\s*\\d+.*") || // 包含逗号分隔的数字
                                      lowerMessage.matches(".*\\d+[.,]\\d+.*\\d+[.,]\\d+.*") || // 经纬度格式
                                      lowerMessage.matches(".*\\d+°.*\\d+°.*"); // 度数格式
        
        // 地名关键词（扩展中国主要海域和城市）
        boolean hasLocationKeywords = lowerMessage.contains("渤海") ||
                                     lowerMessage.contains("黄海") ||
                                     lowerMessage.contains("东海") ||
                                     lowerMessage.contains("南海") ||
                                     lowerMessage.contains("青岛") ||
                                     lowerMessage.contains("大连") ||
                                     lowerMessage.contains("上海") ||
                                     lowerMessage.contains("厦门") ||
                                     lowerMessage.contains("深圳") ||
                                     lowerMessage.contains("海南") ||
                                     lowerMessage.contains("舟山") ||
                                     lowerMessage.contains("威海") ||
                                     lowerMessage.contains("烟台") ||
                                     lowerMessage.contains("天津") ||
                                     lowerMessage.contains("宁波") ||
                                     lowerMessage.contains("福州");
        
        // 水质参数关键词（扩展识别DIN等参数）
        boolean hasWaterQualityKeywords = lowerMessage.contains("din") ||
                                         lowerMessage.contains("srp") ||
                                         lowerMessage.contains("ph") ||
                                         lowerMessage.contains("叶绿素") ||
                                         lowerMessage.contains("chl") ||
                                         lowerMessage.contains("chlorophyll") ||
                                         lowerMessage.contains("悬浮物") ||
                                         lowerMessage.contains("tsm") ||
                                         lowerMessage.contains("水质") ||
                                         lowerMessage.contains("water quality") ||
                                         lowerMessage.contains("营养盐") ||
                                         lowerMessage.contains("nutrient") ||
                                         lowerMessage.contains("氮") ||
                                         lowerMessage.contains("磷") ||
                                         lowerMessage.contains("无机氮") ||
                                         lowerMessage.contains("活性磷") ||
                                         lowerMessage.contains("溶解无机氮") ||
                                         lowerMessage.contains("可溶性活性磷") ||
                                         lowerMessage.contains("温度") ||
                                         lowerMessage.contains("水温") ||
                                         lowerMessage.contains("盐度") ||
                                         lowerMessage.contains("浊度") ||
                                         lowerMessage.contains("藻类");
        
        // 数值查询关键词
        boolean hasNumericalQuery = lowerMessage.contains("多少") ||
                                   lowerMessage.contains("数值") ||
                                   lowerMessage.contains("浓度") ||
                                   lowerMessage.contains("含量") ||
                                   lowerMessage.contains("水平") ||
                                   lowerMessage.contains("值") ||
                                   lowerMessage.contains("level") ||
                                   lowerMessage.contains("concentration") ||
                                   lowerMessage.contains("检测") ||
                                   lowerMessage.contains("监测") ||
                                   lowerMessage.contains("分析") ||
                                   lowerMessage.matches(".*(\\d+|几|什么|如何).*");
        
        // 时间相关关键词（结合水质或海洋相关词汇）
        boolean hasTimeKeywords = (lowerMessage.contains("月") || lowerMessage.contains("日") ||
                                 lowerMessage.contains("时间") || lowerMessage.contains("time") ||
                                 lowerMessage.contains("年") || lowerMessage.contains("季") ||
                                 lowerMessage.contains("今天") || lowerMessage.contains("明天") ||
                                 lowerMessage.contains("昨天")) &&
                                (lowerMessage.contains("海") || lowerMessage.contains("ocean") ||
                                 lowerMessage.contains("水") || lowerMessage.contains("water") ||
                                 lowerMessage.contains("质量") || lowerMessage.contains("quality"));
        
        // 更智能的判断逻辑：
        // 1. 如果有明确的预测关键词，且有位置信息，则认为是预测请求
        // 2. 如果有坐标信息和水质参数，则认为是预测请求
        // 3. 如果有地名和水质参数，则认为是预测请求
        // 4. 如果有时间和水质参数，则认为是预测请求
        boolean isLocationBased = hasCoordinateKeywords || hasLocationKeywords;
        boolean isWaterQualityRelated = hasWaterQualityKeywords || hasNumericalQuery;
        
        return (hasPredictionKeywords && isLocationBased) ||
               (isLocationBased && isWaterQualityRelated) ||
               (hasTimeKeywords && isWaterQualityRelated);
    }
    
    /**
     * 处理水质预测请求
     */
    private ChatResponse handlePredictionRequest(ChatRequest request) {
        try {
            logger.info("处理水质预测请求: {}", request.getMessage());
            
            // 使用参数映射服务解析用户输入
            Map<String, Object> parameters = parameterMappingService.mapUserInputToParameters(request.getMessage());
            
            if (parameters.containsKey("latitude") && parameters.containsKey("longitude")) {
                // 验证坐标
                Double latitude = (Double) parameters.get("latitude");
                Double longitude = (Double) parameters.get("longitude");
                
                if (latitude < -90 || latitude > 90) {
                    String errorMessage = userFriendlyService.getFriendlyErrorMessage(
                        "invalid_latitude", 
                        "纬度值超出有效范围"
                    );
                    return createFriendlyErrorResponse(request.getSessionId(), errorMessage);
                }
                
                if (longitude < -180 || longitude > 180) {
                    String errorMessage = userFriendlyService.getFriendlyErrorMessage(
                        "invalid_longitude", 
                        "经度值超出有效范围"
                    );
                    return createFriendlyErrorResponse(request.getSessionId(), errorMessage);
                }
                
                // 构建预测请求
                com.oceangpt.dto.PredictionRequest predictionRequest = buildPredictionRequest(parameters);
                
                // 首先尝试从缓存获取预测结果
                com.oceangpt.dto.PredictionResponse predictionResponse = cacheService.getCachedPrediction(predictionRequest);
                
                if (predictionResponse == null) {
                    // 缓存中没有，调用预测模型
                    logger.info("缓存未命中，调用预测模型");
                    predictionResponse = customModelService.predictWaterQuality(predictionRequest);
                    
                    if (predictionResponse != null && predictionResponse.isSuccess()) {
                        // 缓存预测结果
                        cacheService.cachePrediction(predictionRequest, predictionResponse);
                    }
                } else {
                    logger.info("从缓存获取预测结果");
                }
                
                // 构建友好的回复
                ChatResponse chatResponse = buildPredictionChatResponse(predictionResponse, request.getSessionId(), parameters);
                
                // 如果预测成功，自动生成详细报告
                if (predictionResponse != null && predictionResponse.isSuccess()) {
                    try {
                        // 构建报告请求
                        com.oceangpt.dto.ReportRequest reportRequest = new com.oceangpt.dto.ReportRequest();
                        reportRequest.setLatitude(predictionResponse.getLatitude());
                        reportRequest.setLongitude(predictionResponse.getLongitude());
                        // ReportRequest没有setDateTime方法，时间戳在ReportGenerationService中设置
                        reportRequest.setReportType("WATER_QUALITY");
                        
                        // 生成报告
                        com.oceangpt.dto.ReportResponse reportResponse = reportGenerationService.generateWaterQualityReport(reportRequest);
                        
                        if (reportResponse != null && reportResponse.isSuccess()) {
                            // 将报告信息添加到聊天响应中
                            String originalMessage = chatResponse.getMessage();
                            StringBuilder enhancedMessage = new StringBuilder(originalMessage);
                            
                            enhancedMessage.append("\n\n📋 **详细分析报告已生成**\n");
                            enhancedMessage.append("• 报告ID: ").append(reportResponse.getReportId()).append("\n");
                            enhancedMessage.append("• 生成时间: ").append(reportResponse.getGeneratedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
                            enhancedMessage.append("• 输入 '查看报告 ").append(reportResponse.getReportId()).append("' 获取完整报告\n");
                            enhancedMessage.append("• 输入 '报告列表' 查看所有历史报告");
                            
                            chatResponse.setMessage(enhancedMessage.toString());
                            
                            // 将报告信息添加到相关数据中
                            Map<String, Object> relatedData = chatResponse.getRelatedData();
                            if (relatedData == null) {
                                relatedData = new HashMap<>();
                            }
                            relatedData.put("report", reportResponse);
                            chatResponse.setRelatedData(relatedData);
                            
                            logger.info("自动生成水质报告成功: {}", reportResponse.getReportId());
                        }
                    } catch (Exception e) {
                        logger.warn("自动生成报告失败: {}", e.getMessage());
                        // 报告生成失败不影响预测结果的返回
                    }
                }
                
                // 保存到聊天历史
                saveChatHistory(request.getSessionId(), chatResponse);
                
                return chatResponse;
                
            } else {
                // 尝试提供更智能的错误提示
                String message = request.getMessage();
                StringBuilder suggestions = new StringBuilder();
                suggestions.append("❌ 无法识别有效的地理坐标信息。\n\n");
                suggestions.append("💡 **请尝试以下格式：**\n");
                suggestions.append("• 经度 120.5, 纬度 36.2\n");
                suggestions.append("• 纬度 36.2, 经度 120.5\n");
                suggestions.append("• 120.5, 36.2\n");
                suggestions.append("• 青岛、上海、厦门等城市名称\n\n");
                
                // 检查是否包含数字，给出更具体的建议
                if (message.matches(".*\\d+.*")) {
                    suggestions.append("🔍 **检测到数字，请确认：**\n");
                    suggestions.append("• 坐标格式是否正确\n");
                    suggestions.append("• 经纬度范围：经度 -180°~180°，纬度 -90°~90°\n\n");
                }
                
                suggestions.append("💡 如需帮助，请输入 '帮助' 获取使用指南。");
                
                return createFriendlyErrorResponse(request.getSessionId(), suggestions.toString());
            }
            
        } catch (Exception e) {
            logger.error("处理预测请求时出错: {}", e.getMessage(), e);
            return createFriendlyErrorResponse(request.getSessionId(), e.getMessage());
        }
    }
    
    /**
     * 构建预测请求
     */
    private com.oceangpt.dto.PredictionRequest buildPredictionRequest(Map<String, Object> parameters) {
        com.oceangpt.dto.PredictionRequest request = new com.oceangpt.dto.PredictionRequest();
        request.setLatitude((Double) parameters.get("latitude"));
        request.setLongitude((Double) parameters.get("longitude"));
        
        // 设置光谱数据
        @SuppressWarnings("unchecked")
        Map<String, Double> s2Data = (Map<String, Double>) parameters.get("s2Data");
        @SuppressWarnings("unchecked")
        Map<String, Double> s3Data = (Map<String, Double>) parameters.get("s3Data");
        
        if (s2Data != null) {
            request.setS2B1(s2Data.get("s2B1"));
            request.setS2B2(s2Data.get("s2B2"));
            request.setS2B3(s2Data.get("s2B3"));
            request.setS2B4(s2Data.get("s2B4"));
            request.setS2B5(s2Data.get("s2B5"));
            request.setS2B6(s2Data.get("s2B6"));
            request.setS2B7(s2Data.get("s2B7"));
            request.setS2B8(s2Data.get("s2B8"));
            request.setS2B8A(s2Data.get("s2B8A"));
        }
        if (s3Data != null) {
            request.setS3Oa01(s3Data.get("s3Oa01"));
            request.setS3Oa02(s3Data.get("s3Oa02"));
            request.setS3Oa03(s3Data.get("s3Oa03"));
            request.setS3Oa04(s3Data.get("s3Oa04"));
            request.setS3Oa05(s3Data.get("s3Oa05"));
            request.setS3Oa06(s3Data.get("s3Oa06"));
            request.setS3Oa07(s3Data.get("s3Oa07"));
            request.setS3Oa08(s3Data.get("s3Oa08"));
        }
        
        // 设置时间信息
        if (parameters.containsKey("dateTime")) {
            request.setDateTime((java.time.LocalDateTime) parameters.get("dateTime"));
        } else {
            // 如果没有指定时间，使用当前时间
            request.setDateTime(java.time.LocalDateTime.now());
        }
        
        // 设置神经网络预测值
        if (parameters.containsKey("chlNN")) {
            request.setChlNN((Double) parameters.get("chlNN"));
        }
        if (parameters.containsKey("tsmNN")) {
            request.setTsmNN((Double) parameters.get("tsmNN"));
        }
        
        return request;
    }
    
    /**
     * 构建预测结果的聊天响应
     */
    private ChatResponse buildPredictionChatResponse(com.oceangpt.dto.PredictionResponse predictionResponse, 
                                                   String sessionId, Map<String, Object> parameters) {
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setSessionId(sessionId);
        chatResponse.setTimestamp(LocalDateTime.now());
        
        if (predictionResponse.isSuccess()) {
            StringBuilder message = new StringBuilder();
            message.append("🌊 **水质预测结果**\n\n");
            
            // 位置信息
            message.append(String.format("📍 **预测位置**: 经度 %.4f°, 纬度 %.4f°\n", 
                parameters.get("longitude"), parameters.get("latitude")));
            
            if (parameters.containsKey("dateTime")) {
                message.append(String.format("📅 **预测时间**: %s\n\n", parameters.get("dateTime")));
            }
            
            // 水质等级信息
            if (predictionResponse.getWaterQualityLevel() != null) {
                message.append("🏆 **水质等级**: ").append(predictionResponse.getWaterQualityLevel());
                if (predictionResponse.getQualityLevel() != null) {
                    message.append(" (").append(predictionResponse.getQualityLevel()).append(")\n");
                } else {
                    message.append("\n");
                }
                
                // 添加水质等级说明
                if (predictionResponse.getAdditionalInfo() != null) {
                    Map<String, Object> additionalInfo = predictionResponse.getAdditionalInfo();
                    if (additionalInfo.containsKey("waterQualityUsage")) {
                        message.append("📋 **适用范围**: ").append(additionalInfo.get("waterQualityUsage")).append("\n");
                    }
                    if (additionalInfo.containsKey("overallScore")) {
                        Double score = (Double) additionalInfo.get("overallScore");
                        message.append(String.format("📊 **综合评分**: %.1f/100\n", score));
                    }
                }
                message.append("\n");
            }
            
            // 预测结果
            message.append("🔬 **营养盐浓度预测**:\n");
            if (predictionResponse.getDinLevel() != null) {
                message.append(String.format("• DIN (溶解无机氮): %.3f mg/L\n", predictionResponse.getDinLevel()));
            }
            if (predictionResponse.getSrpLevel() != null) {
                message.append(String.format("• SRP (可溶性活性磷): %.3f mg/L\n", predictionResponse.getSrpLevel()));
            }
            if (predictionResponse.getPhLevel() != null) {
                message.append(String.format("• pH值: %.2f\n", predictionResponse.getPhLevel()));
            }
            
            // 水质等级
            if (predictionResponse.getQualityLevel() != null) {
                message.append(String.format("\n🏆 **水质等级**: %s\n", getQualityLevelDescription(predictionResponse.getQualityLevel())));
            }
            
            // 置信度
            if (predictionResponse.getConfidence() != null) {
                message.append(String.format("\n📊 **预测置信度**: %.1f%%\n", predictionResponse.getConfidence() * 100));
            }
            
            // 数据源信息
            if (parameters.containsKey("dataSource")) {
                message.append(String.format("\n📡 **数据源**: %s\n", parameters.get("dataSource")));
            }
            if (parameters.containsKey("qualityScore")) {
                message.append(String.format("📈 **数据质量**: %.1f%%\n", (Double) parameters.get("qualityScore") * 100));
            }
            
            message.append("\n💡 **说明**: 预测基于Sentinel-2/3卫星数据和EndToEndRegressionModel机器学习模型");
            
            chatResponse.setMessage(message.toString());
            chatResponse.setConfidence(predictionResponse.getConfidence() != null ? predictionResponse.getConfidence() : 0.85);
            
            // 设置相关数据
            Map<String, Object> relatedData = new HashMap<>();
            relatedData.put("prediction", predictionResponse);
            relatedData.put("parameters", parameters);
            
            // 添加地图数据用于前端可视化
            List<Map<String, Object>> mapData = new ArrayList<>();
            Map<String, Object> locationPoint = new HashMap<>();
            locationPoint.put("latitude", parameters.get("latitude"));
            locationPoint.put("longitude", parameters.get("longitude"));
            locationPoint.put("name", "预测位置");
            
            // 根据水质等级设置地图点的质量标识
            String qualityLevel = predictionResponse.getQualityLevel();
            if (qualityLevel != null) {
                switch (qualityLevel.toLowerCase()) {
                    case "excellent":
                    case "优秀":
                        locationPoint.put("quality", "excellent");
                        break;
                    case "good":
                    case "良好":
                        locationPoint.put("quality", "good");
                        break;
                    case "moderate":
                    case "中等":
                        locationPoint.put("quality", "moderate");
                        break;
                    case "poor":
                    case "较差":
                        locationPoint.put("quality", "poor");
                        break;
                    default:
                        locationPoint.put("quality", "moderate");
                }
            } else {
                locationPoint.put("quality", "moderate");
            }
            
            // 添加详细信息
            StringBuilder details = new StringBuilder();
            if (predictionResponse.getDinLevel() != null) {
                details.append(String.format("DIN: %.3f mg/L\n", predictionResponse.getDinLevel()));
            }
            if (predictionResponse.getSrpLevel() != null) {
                details.append(String.format("SRP: %.3f mg/L\n", predictionResponse.getSrpLevel()));
            }
            if (predictionResponse.getPhLevel() != null) {
                details.append(String.format("pH: %.2f\n", predictionResponse.getPhLevel()));
            }
            if (predictionResponse.getConfidence() != null) {
                details.append(String.format("置信度: %.1f%%", predictionResponse.getConfidence() * 100));
            }
            locationPoint.put("details", details.toString());
            
            mapData.add(locationPoint);
            relatedData.put("mapData", mapData);
            
            chatResponse.setRelatedData(relatedData);
            
        } else {
            chatResponse.setMessage("❌ 水质预测失败: " + predictionResponse.getErrorMessage() + 
                "\n\n请检查输入的经纬度是否正确，或稍后重试。");
            chatResponse.setConfidence(0.0);
        }
        
        return chatResponse;
    }
    
    /**
     * 获取水质等级描述
     */
    private String getQualityLevelDescription(String level) {
        switch (level.toLowerCase()) {
            case "excellent": return "优秀 🌟";
            case "good": return "良好 ✅";
            case "moderate": return "中等 ⚠️";
            case "poor": return "较差 ❌";
            case "very_poor": return "很差 🚫";
            default: return level;
        }
    }
    
    /**
     * 格式化预测响应
     */
    private ChatResponse formatPredictionResponse(com.oceangpt.dto.PredictionResponse predictionResponse, String sessionId) {
        StringBuilder answer = new StringBuilder();
        
        answer.append("🌊 **水质预测结果**\n\n");
        
        // 位置信息
        if (predictionResponse.getLatitude() != null && predictionResponse.getLongitude() != null) {
            answer.append(String.format("📍 **位置**: 经度 %.4f°, 纬度 %.4f°\n", 
                predictionResponse.getLongitude(), predictionResponse.getLatitude()));
        }
        
        // 时间信息
        if (predictionResponse.getPredictionTimestamp() != null) {
            answer.append(String.format("📅 **预测时间**: %s\n\n", 
                predictionResponse.getPredictionTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日"))));
        }
        
        // 水质等级信息
        if (predictionResponse.getWaterQualityLevel() != null) {
            answer.append("🏆 **水质等级**: ").append(predictionResponse.getWaterQualityLevel());
            if (predictionResponse.getQualityLevel() != null) {
                answer.append(" (").append(predictionResponse.getQualityLevel()).append(")\n");
            } else {
                answer.append("\n");
            }
            
            // 添加水质等级说明
            if (predictionResponse.getAdditionalInfo() != null) {
                Map<String, Object> additionalInfo = predictionResponse.getAdditionalInfo();
                if (additionalInfo.containsKey("waterQualityUsage")) {
                    answer.append("📋 **适用范围**: ").append(additionalInfo.get("waterQualityUsage")).append("\n");
                }
                if (additionalInfo.containsKey("overallScore")) {
                    Double score = (Double) additionalInfo.get("overallScore");
                    answer.append(String.format("📊 **综合评分**: %.1f/100\n", score));
                }
                if (additionalInfo.containsKey("classificationReason")) {
                    answer.append("💡 **分类依据**: ").append(additionalInfo.get("classificationReason")).append("\n");
                }
            }
            answer.append("\n");
        }
        
        // 预测结果
        answer.append("🔬 **预测结果**:\n");
        if (predictionResponse.getDinLevel() != null) {
            answer.append(String.format("• DIN (溶解无机氮): %.3f mg/L\n", predictionResponse.getDinLevel()));
            answer.append("  ℹ️ 输入 '参数说明 DIN' 了解详情\n");
        }
        if (predictionResponse.getSrpLevel() != null) {
            answer.append(String.format("• SRP (可溶性活性磷): %.3f mg/L\n", predictionResponse.getSrpLevel()));
            answer.append("  ℹ️ 输入 '参数说明 SRP' 了解详情\n");
        }
        if (predictionResponse.getPhLevel() != null) {
            answer.append(String.format("• pH值: %.2f\n", predictionResponse.getPhLevel()));
            answer.append("  ℹ️ 输入 '参数说明 pH' 了解详情\n");
        }
        
        // 水质等级评估
        if (predictionResponse.getQualityLevel() != null) {
            answer.append(String.format("\n🏆 **水质等级**: %s\n", getWaterQualityLevelWithEmoji(predictionResponse.getQualityLevel())));
        }
        
        // 置信度
        if (predictionResponse.getConfidence() != null) {
            double confidence = predictionResponse.getConfidence() * 100;
            String confidenceLevel = getConfidenceLevelDescription(confidence);
            answer.append(String.format("\n📊 **预测置信度**: %.1f%% %s\n", confidence, confidenceLevel));
        }
        
        // 数据质量说明
        String qualityExplanation = userFriendlyService.generateDataQualityExplanation(
            predictionResponse.getConfidence(), 
            "Satellite Data"
        );
        answer.append("\n").append(qualityExplanation).append("\n");
        
        // 使用建议
        answer.append("\n💡 **使用建议**:\n");
        if (predictionResponse.getConfidence() != null && predictionResponse.getConfidence() >= 0.8) {
            answer.append("• 预测结果可靠，可作为决策参考\n");
        } else {
            answer.append("• 预测结果仅供参考，建议结合实地监测数据\n");
        }
        answer.append("• 输入 '帮助' 获取更多使用指南\n");
        answer.append("• 输入 '单位说明 mg/L' 了解单位含义");
        
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setMessage(answer.toString());
        response.setConfidence(predictionResponse.getConfidence() != null ? predictionResponse.getConfidence() : 0.8);
        response.setTimestamp(LocalDateTime.now());
        response.setSuccess(true);
        
        return response;
    }
    
    /**
     * 获取带表情符号的水质等级
     */
    private String getWaterQualityLevelWithEmoji(String level) {
        if (level == null) return "未知";
        
        switch (level.toLowerCase()) {
            case "excellent":
            case "优秀":
                return "优秀 🌟";
            case "good":
            case "良好":
                return "良好 ✅";
            case "moderate":
            case "中等":
                return "中等 ⚠️";
            case "poor":
            case "较差":
                return "较差 ❌";
            default:
                return level;
        }
    }
    
    /**
     * 获取置信度等级描述
     */
    private String getConfidenceLevelDescription(double confidence) {
        if (confidence >= 90) {
            return "(非常可靠)";
        } else if (confidence >= 80) {
            return "(可靠)";
        } else if (confidence >= 70) {
            return "(较可靠)";
        } else if (confidence >= 60) {
            return "(一般)";
        } else {
            return "(不确定)";
        }
    }
    
    /**
     * 创建帮助响应
     */
    private ChatResponse createHelpResponse(String sessionId, String helpMessage) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setMessage(helpMessage);
        response.setConfidence(0.9);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    /**
     * 获取聊天历史
     */
    public List<ChatResponse> getChatHistory(String sessionId) {
        cleanupExpiredSessions();
        return chatHistories.getOrDefault(sessionId, new ArrayList<>());
    }
    
    /**
     * 清除聊天历史
     */
    public void clearChatHistory(String sessionId) {
        chatHistories.remove(sessionId);
        sessionLastActivity.remove(sessionId);
        logger.info("已清除会话历史: sessionId={}", sessionId);
    }
    
    /**
     * 获取活跃会话列表
     */
    public List<String> getActiveSessions() {
        cleanupExpiredSessions();
        return new ArrayList<>(chatHistories.keySet());
    }
    
    /**
     * 构建查询请求
     */
    private QueryRequest buildQueryRequest(ChatRequest chatRequest) {
        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setQuery(chatRequest.getMessage());
        queryRequest.setLanguage(chatRequest.getLanguage() != null ? chatRequest.getLanguage() : "zh");
        queryRequest.setSessionId(chatRequest.getSessionId());
        
        // 设置位置信息
        if (chatRequest.getLocation() != null) {
            queryRequest.setLatitude(chatRequest.getLocation().getLatitude());
            queryRequest.setLongitude(chatRequest.getLocation().getLongitude());
        }
        
        return queryRequest;
    }
    
    /**
     * 转换查询响应为聊天响应
     */
    private ChatResponse convertToChatResponse(QueryResponse queryResponse, String sessionId) {
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setSessionId(sessionId);
        chatResponse.setMessage(queryResponse.getAnswer());
        chatResponse.setConfidence(queryResponse.getConfidence());
        chatResponse.setSupportingEvidence(queryResponse.getSupportingEvidence());
        chatResponse.setRecommendations(queryResponse.getRecommendations());
        chatResponse.setRelatedData(queryResponse.getRelatedData());
        chatResponse.setFollowUpQueries(queryResponse.getSuggestedFollowUpQueries());
        // 转换TechnicalDetails为Map
        if (queryResponse.getTechnicalDetails() != null) {
            Map<String, Object> techDetailsMap = new HashMap<>();
            var techDetails = queryResponse.getTechnicalDetails();
            techDetailsMap.put("modelUsed", techDetails.getModelUsed());
            techDetailsMap.put("algorithmVersion", techDetails.getAlgorithmVersion());
            techDetailsMap.put("dataQualityScore", techDetails.getDataQualityScore());
            techDetailsMap.put("calculationMethod", techDetails.getCalculationMethod());
            techDetailsMap.put("uncertaintyRange", techDetails.getUncertaintyRange());
            chatResponse.setTechnicalDetails(techDetailsMap);
        }
        chatResponse.setSuccess(true);
        
        return chatResponse;
    }
    
    /**
     * 增强响应内容
     */
    private void enhanceResponse(ChatResponse response, ChatRequest request) {
        // 添加上下文相关的建议问题
        if (response.getFollowUpQueries() == null || response.getFollowUpQueries().isEmpty()) {
            response.setFollowUpQueries(generateContextualQuestions(request.getMessage()));
        }
        
        // 添加数据来源信息
        if (response.getSources() == null) {
            response.setSources(Arrays.asList(
                "OceanGPT-14B 海洋大语言模型",
                "海洋科学知识库",
                "实时海洋监测数据"
            ));
        }
    }
    
    /**
     * 生成上下文相关的问题
     */
    private List<String> generateContextualQuestions(String userMessage) {
        List<String> questions = new ArrayList<>();
        
        String lowerMessage = userMessage.toLowerCase();
        
        if (lowerMessage.contains("ph") || lowerMessage.contains("酸碱")) {
            questions.addAll(Arrays.asList(
                "海洋酸化的主要原因是什么？",
                "pH值变化对海洋生物有什么影响？",
                "如何监测海洋pH值的变化？"
            ));
        } else if (lowerMessage.contains("温度") || lowerMessage.contains("气候")) {
            questions.addAll(Arrays.asList(
                "海水温度上升对生态系统的影响？",
                "气候变化如何影响海洋环流？",
                "海洋热浪的成因和影响是什么？"
            ));
        } else if (lowerMessage.contains("污染") || lowerMessage.contains("水质")) {
            questions.addAll(Arrays.asList(
                "海洋污染的主要来源有哪些？",
                "如何评估海洋水质状况？",
                "海洋塑料污染的解决方案？"
            ));
        } else {
            questions.addAll(Arrays.asList(
                "海洋生态系统的健康状况如何？",
                "海洋保护有哪些有效措施？",
                "海洋科学研究的最新进展？"
            ));
        }
        
        return questions.subList(0, Math.min(3, questions.size()));
    }
    
    /**
     * 保存聊天历史
     */
    private void saveChatHistory(String sessionId, ChatResponse response) {
        chatHistories.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(response);
        
        // 限制历史记录数量（最多保留50条）
        List<ChatResponse> history = chatHistories.get(sessionId);
        if (history.size() > 50) {
            history.remove(0);
        }
    }
    
    /**
     * 更新会话活动时间
     */
    private void updateSessionActivity(String sessionId) {
        sessionLastActivity.put(sessionId, LocalDateTime.now());
    }
    
    /**
     * 清理过期会话
     */
    private void cleanupExpiredSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(SESSION_TIMEOUT_HOURS);
        
        sessionLastActivity.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(cutoffTime)) {
                chatHistories.remove(entry.getKey());
                logger.info("清理过期会话: sessionId={}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 创建错误响应
     */
    private ChatResponse createErrorResponse(String sessionId, String errorMessage) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setMessage(errorMessage);
        response.setSuccess(false);
        response.setConfidence(0.0);
        return response;
    }
    
    /**
     * 创建友好的错误响应
     */
    private ChatResponse createFriendlyErrorResponse(String sessionId, String originalError) {
        String friendlyMessage = userFriendlyService.getFriendlyErrorMessage("general_error", originalError);
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setMessage(friendlyMessage);
        response.setSuccess(false);
        response.setConfidence(0.0);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    /**
     * 创建帮助响应
     */
    private ChatResponse createHelpResponse(String sessionId) {
        String helpGuide = userFriendlyService.generateUsageGuide();
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setMessage(helpGuide);
        response.setSuccess(true);
        response.setConfidence(1.0);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    /**
     * 创建参数解释响应
     */
    private ChatResponse createParameterExplanationResponse(String sessionId, String parameterName) {
        String explanation = userFriendlyService.getParameterExplanation(parameterName);
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setMessage(explanation);
        response.setSuccess(true);
        response.setConfidence(1.0);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    /**
     * 创建单位解释响应
     */
    private ChatResponse createUnitExplanationResponse(String sessionId, String unitName) {
        String explanation = userFriendlyService.getUnitExplanation(unitName);
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setMessage(explanation);
        response.setSuccess(true);
        response.setConfidence(1.0);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    /**
     * 处理报告查看请求
     */
    private ChatResponse handleReportViewRequest(ChatRequest request) {
        try {
            // 提取报告ID
            String reportId = extractReportId(request.getMessage());
            if (reportId == null) {
                return createFriendlyErrorResponse(request.getSessionId(), "请提供有效的报告ID，例如：查看报告 REPORT-20240115-001");
            }
            
            // 获取报告详情
            com.oceangpt.dto.ReportResponse reportResponse = reportGenerationService.getReportById(reportId);
            
            if (reportResponse == null) {
                return createFriendlyErrorResponse(request.getSessionId(), "未找到报告ID: " + reportId + "。请检查报告ID是否正确，或输入'报告列表'查看所有可用报告。");
            }
            
            // 构建报告详情响应
            StringBuilder message = new StringBuilder();
            message.append("📋 **水质分析报告详情**\n\n");
            message.append("🆔 **报告ID**: ").append(reportResponse.getReportId()).append("\n");
            message.append("📍 **位置**: 经度 ").append(reportResponse.getLongitude()).append("°, 纬度 ").append(reportResponse.getLatitude()).append("°\n");
            message.append("📅 **生成时间**: ").append(reportResponse.getGeneratedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
            
            if (reportResponse.getContent() != null) {
                message.append("📊 **报告内容**:\n").append(reportResponse.getContent()).append("\n\n");
            }
            
            message.append("💡 **操作提示**:\n");
            message.append("• 输入 '报告列表' 查看所有历史报告\n");
            message.append("• 输入新的经纬度坐标进行新的水质预测");
            
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setSessionId(request.getSessionId());
            chatResponse.setMessage(message.toString());
            chatResponse.setSuccess(true);
            chatResponse.setConfidence(1.0);
            chatResponse.setTimestamp(LocalDateTime.now());
            
            // 添加相关数据
            Map<String, Object> relatedData = new HashMap<>();
            relatedData.put("report", reportResponse);
            chatResponse.setRelatedData(relatedData);
            
            // 保存到聊天历史
            saveChatHistory(request.getSessionId(), chatResponse);
            
            return chatResponse;
            
        } catch (Exception e) {
            logger.error("处理报告查看请求时出错: {}", e.getMessage(), e);
            return createFriendlyErrorResponse(request.getSessionId(), "获取报告详情时出现错误，请稍后重试。");
        }
    }
    
    /**
     * 处理报告列表请求
     */
    private ChatResponse handleReportListRequest(ChatRequest request) {
        try {
            // 获取报告列表
            java.util.List<com.oceangpt.dto.ReportResponse> reports = reportGenerationService.getAllReports();
            
            StringBuilder message = new StringBuilder();
            message.append("📋 **历史报告列表**\n\n");
            
            if (reports == null || reports.isEmpty()) {
                message.append("暂无历史报告。\n\n");
                message.append("💡 **开始使用**:\n");
                message.append("• 输入经纬度坐标进行水质预测，系统将自动生成详细报告\n");
                message.append("• 例如：经度 120.5, 纬度 36.2");
            } else {
                message.append("共找到 ").append(reports.size()).append(" 份报告:\n\n");
                
                // 按时间倒序显示最近的10份报告
                reports.stream()
                    .sorted((r1, r2) -> r2.getGeneratedAt().compareTo(r1.getGeneratedAt()))
                    .limit(10)
                    .forEach(report -> {
                        message.append("🔹 **").append(report.getReportId()).append("**\n");
                        message.append("   📍 位置: 经度 ").append(report.getLongitude()).append("°, 纬度 ").append(report.getLatitude()).append("°\n");
                        message.append("   📅 时间: ").append(report.getGeneratedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
                        message.append("   💡 输入 '查看报告 ").append(report.getReportId()).append("' 查看详情\n\n");
                    });
                
                if (reports.size() > 10) {
                    message.append("... 还有 ").append(reports.size() - 10).append(" 份更早的报告\n\n");
                }
            }
            
            message.append("💡 **操作提示**:\n");
            message.append("• 输入 '查看报告 [报告ID]' 查看具体报告\n");
            message.append("• 输入新的经纬度坐标进行新的水质预测");
            
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setSessionId(request.getSessionId());
            chatResponse.setMessage(message.toString());
            chatResponse.setSuccess(true);
            chatResponse.setConfidence(1.0);
            chatResponse.setTimestamp(LocalDateTime.now());
            
            // 添加相关数据
            Map<String, Object> relatedData = new HashMap<>();
            relatedData.put("reports", reports);
            chatResponse.setRelatedData(relatedData);
            
            // 保存到聊天历史
            saveChatHistory(request.getSessionId(), chatResponse);
            
            return chatResponse;
            
        } catch (Exception e) {
            logger.error("处理报告列表请求时出错: {}", e.getMessage(), e);
            return createFriendlyErrorResponse(request.getSessionId(), "获取报告列表时出现错误，请稍后重试。");
        }
    }
    
    /**
     * 从消息中提取报告ID
     */
    private String extractReportId(String message) {
        // 匹配报告ID格式：REPORT-YYYYMMDD-XXX 或类似格式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([A-Z0-9\\-]{10,})");
        java.util.regex.Matcher matcher = pattern.matcher(message.toUpperCase());
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 检查是否为大连监测点分布请求
     */
    private boolean isDalianMonitoringPointsRequest(String message) {
        String lowerMessage = message.toLowerCase();
        return (lowerMessage.contains("大连") || lowerMessage.contains("沿海城市") || 
                lowerMessage.contains("全国") || lowerMessage.contains("中国")) && 
               (lowerMessage.contains("监测点") || lowerMessage.contains("分布") || 
                lowerMessage.contains("位置") || lowerMessage.contains("点位"));
    }
    
    /**
     * 处理监测点分布请求（大连或全国沿海城市）
     */
    private ChatResponse handleDalianMonitoringPointsRequest(ChatRequest request) {
        logger.info("处理监测点分布请求: {}", request.getMessage());
        
        try {
            String message = request.getMessage().toLowerCase();
            List<Map<String, Object>> monitoringPoints;
            String responseMessage;
            
            // 判断是大连还是全国沿海城市请求
            if (message.contains("大连")) {
                monitoringPoints = createDalianMonitoringPoints();
                responseMessage = "以下是大连海域的监测点分布情况，共" + monitoringPoints.size() + "个监测点：";
            } else {
                monitoringPoints = createChinaCoastalCitiesMonitoringPoints();
                responseMessage = "以下是中国主要沿海城市的海洋监测点分布情况，共" + monitoringPoints.size() + "个监测点：";
            }
            
            ChatResponse response = new ChatResponse();
            response.setSessionId(request.getSessionId());
            response.setMessage(responseMessage);
            response.setTimestamp(LocalDateTime.now());
            response.setMessageType("assistant");
            
            // 设置地图数据
            Map<String, Object> relatedData = new HashMap<>();
            relatedData.put("mapData", monitoringPoints);
            response.setRelatedData(relatedData);
            
            // 保存到聊天历史
            saveChatHistory(request.getSessionId(), response);
            
            return response;
            
        } catch (Exception e) {
            logger.error("处理监测点分布请求时出错: {}", e.getMessage(), e);
            return createErrorResponse(request.getSessionId(), "获取监测点分布信息时出错，请稍后重试。");
        }
    }
    
    /**
     * 创建大连附近的监测点数据
     */
    private List<Map<String, Object>> createDalianMonitoringPoints() {
        List<Map<String, Object>> points = new ArrayList<>();
        
        // 大连市中心坐标：38.914, 121.6147
        // 在大连附近创建多个监测点
        Object[][] coordinates = {
            {38.914, 121.6147, "大连湾监测点", "excellent"},
            {38.920, 121.620, "金州湾监测点", "good"},
            {38.908, 121.608, "甘井子海域监测点", "moderate"},
            {38.925, 121.635, "开发区海域监测点", "good"},
            {38.900, 121.600, "旅顺口海域监测点", "excellent"},
            {38.930, 121.640, "普兰店湾监测点", "moderate"},
            {38.895, 121.590, "长海县海域监测点", "good"},
            {38.940, 121.650, "瓦房店海域监测点", "moderate"}
        };
        
        for (Object[] coord : coordinates) {
            Map<String, Object> point = new HashMap<>();
            point.put("latitude", (Double)coord[0]);
            point.put("longitude", (Double)coord[1]);
            point.put("name", (String)coord[2]);
            point.put("quality", (String)coord[3]);
            
            // 根据水质等级生成模拟的详细信息
            String details = generateMonitoringPointDetails((String)coord[3]);
            point.put("details", details);
            
            points.add(point);
        }
        
        return points;
    }
    
    /**
     * 根据水质等级生成监测点详细信息
     */
    /**
     * 创建中国主要沿海城市的海洋监测点数据
     */
    private List<Map<String, Object>> createChinaCoastalCitiesMonitoringPoints() {
        List<Map<String, Object>> points = new ArrayList<>();
        
        // 中国主要沿海城市的海洋监测点坐标（选择离城市最近的海洋点）
        Object[][] coastalCities = {
            {"上海", 121.8, 31.0, "good", "长江口海域监测点"},
            {"深圳", 114.3, 22.4, "excellent", "珠江口海域监测点"},
            {"青岛", 120.4, 36.1, "good", "胶州湾海域监测点"},
            {"厦门", 118.2, 24.4, "excellent", "厦门湾海域监测点"},
            {"天津", 117.8, 39.2, "moderate", "渤海湾海域监测点"},
            {"大连", 121.8, 38.8, "good", "大连湾海域监测点"},
            {"宁波", 121.8, 29.7, "good", "杭州湾海域监测点"},
            {"烟台", 121.5, 37.6, "excellent", "烟台海域监测点"},
            {"威海", 122.2, 37.6, "excellent", "威海海域监测点"},
            {"秦皇岛", 119.8, 39.8, "moderate", "秦皇岛海域监测点"},
            {"连云港", 119.3, 34.8, "good", "连云港海域监测点"},
            {"南通", 121.2, 32.1, "good", "南通海域监测点"},
            {"温州", 120.9, 27.9, "good", "温州湾海域监测点"},
            {"福州", 119.5, 26.2, "excellent", "闽江口海域监测点"},
            {"汕头", 116.8, 23.4, "good", "汕头海域监测点"},
            {"湛江", 110.2, 21.0, "excellent", "湛江海域监测点"},
            {"北海", 109.2, 21.6, "excellent", "北海海域监测点"},
            {"海口", 110.4, 20.2, "excellent", "琼州海峡监测点"}
        };
        
        for (Object[] city : coastalCities) {
            Map<String, Object> point = new HashMap<>();
            point.put("name", (String) city[0]);
            point.put("lat", (Double) city[2]);
            point.put("lng", (Double) city[1]);
            point.put("quality", (String) city[3]);
            point.put("details", generateMonitoringPointDetails((String) city[3]) + "\n监测站点：" + (String) city[4]);
            
            // 添加模拟的海洋参数数据
            Random random = new Random();
            point.put("temperature", 15 + random.nextDouble() * 10); // 15-25°C
            point.put("salinity", 30 + random.nextDouble() * 5);     // 30-35 PSU
            point.put("ph", 7.8 + random.nextDouble() * 0.4);        // 7.8-8.2
            point.put("dissolvedOxygen", 6 + random.nextDouble() * 3); // 6-9 mg/L
            
            points.add(point);
        }
        
        return points;
    }
    
    private String generateMonitoringPointDetails(String quality) {
        switch (quality) {
            case "excellent":
                return "DIN: 0.030 mg/L\nSRP: 0.015 mg/L\npH: 8.10\n水质状况: 优秀";
            case "good":
                return "DIN: 0.045 mg/L\nSRP: 0.018 mg/L\npH: 8.05\n水质状况: 良好";
            case "moderate":
                return "DIN: 0.055 mg/L\nSRP: 0.022 mg/L\npH: 7.95\n水质状况: 中等";
            default:
                return "DIN: 0.040 mg/L\nSRP: 0.020 mg/L\npH: 8.00\n水质状况: 良好";
        }
    }
}