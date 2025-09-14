package com.oceangpt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceangpt.dto.PredictionRequest;
import com.oceangpt.dto.PredictionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 自定义模型服务
 * 负责调用用户的EndToEndRegressionModel进行水质预测
 */
@Service
public class CustomModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomModelService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private WaterQualityClassificationService waterQualityClassificationService;
    
    @Value("${oceangpt.python.executable:python}")
    private String pythonExecutable;
    
    @Value("${oceangpt.model.path:}")
    private String modelPath;
    
    @Value("${oceangpt.model.timeout:30}")
    private int timeoutSeconds;
    
    /**
     * 使用自定义模型进行水质预测
     * 
     * @param request 预测请求
     * @return 预测响应
     */
    public PredictionResponse predictWaterQuality(PredictionRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("开始使用自定义模型进行预测: 经度={}, 纬度={}", 
                       request.getLongitude(), request.getLatitude());
            
            // 准备输入数据
            Map<String, Object> inputData = prepareInputData(request);
            
            // 调用Python模型
            Map<String, Object> result = callPythonModel(inputData);
            
            // 构建响应
            PredictionResponse response = buildResponse(result, request);
            
            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTimeMs((int) processingTime);
            
            logger.info("预测完成，耗时: {}ms", processingTime);
            return response;
            
        } catch (Exception e) {
            logger.error("预测失败", e);
            return createErrorResponse(e.getMessage(), request);
        }
    }
    
    /**
     * 准备Python模型的输入数据
     */
    private Map<String, Object> prepareInputData(PredictionRequest request) {
        Map<String, Object> inputData = new HashMap<>();
        
        // 基本信息
        inputData.put("latitude", request.getLatitude());
        inputData.put("longitude", request.getLongitude());
        
        // S2光谱数据 (13个波段) - 组装成数组
        java.util.List<Double> s2Data = new java.util.ArrayList<>();
        s2Data.add(request.getS2B2() != null ? request.getS2B2() : 0.0);
        s2Data.add(request.getS2B3() != null ? request.getS2B3() : 0.0);
        s2Data.add(request.getS2B4() != null ? request.getS2B4() : 0.0);
        s2Data.add(request.getS2B5() != null ? request.getS2B5() : 0.0);
        s2Data.add(request.getS2B6() != null ? request.getS2B6() : 0.0);
        s2Data.add(request.getS2B7() != null ? request.getS2B7() : 0.0);
        s2Data.add(request.getS2B8() != null ? request.getS2B8() : 0.0);
        s2Data.add(request.getS2B8A() != null ? request.getS2B8A() : 0.0);
        // 补充到13个波段
        while (s2Data.size() < 13) {
            s2Data.add(0.0);
        }
        inputData.put("s2Data", s2Data);
        
        // S3光谱数据 (21个波段) - 组装成数组
        java.util.List<Double> s3Data = new java.util.ArrayList<>();
        s3Data.add(request.getS3Oa01() != null ? request.getS3Oa01() : 0.0);
        s3Data.add(request.getS3Oa02() != null ? request.getS3Oa02() : 0.0);
        s3Data.add(request.getS3Oa03() != null ? request.getS3Oa03() : 0.0);
        s3Data.add(request.getS3Oa04() != null ? request.getS3Oa04() : 0.0);
        s3Data.add(request.getS3Oa05() != null ? request.getS3Oa05() : 0.0);
        s3Data.add(request.getS3Oa06() != null ? request.getS3Oa06() : 0.0);
        s3Data.add(request.getS3Oa07() != null ? request.getS3Oa07() : 0.0);
        s3Data.add(request.getS3Oa08() != null ? request.getS3Oa08() : 0.0);
        // 补充到21个波段
        while (s3Data.size() < 21) {
            s3Data.add(0.0);
        }
        inputData.put("s3Data", s3Data);
        
        // 神经网络预测值（可选字段）
        inputData.put("chlNN", request.getChlNN() != null ? request.getChlNN() : 0.0);
        inputData.put("tsmNN", request.getTsmNN() != null ? request.getTsmNN() : 0.0);
        
        return inputData;
    }
    
    /**
     * 调用Python模型进行预测
     */
    private Map<String, Object> callPythonModel(Map<String, Object> inputData) throws Exception {
        // 获取Python脚本路径
        Path scriptPath = Paths.get("src", "main", "resources", "model_predictor.py")
                              .toAbsolutePath();
        
        // 创建临时JSON文件来避免命令行参数转义问题
        Path tempJsonFile = Paths.get("temp", "input_" + System.currentTimeMillis() + ".json");
        tempJsonFile.getParent().toFile().mkdirs(); // 确保temp目录存在
        
        try {
            // 将JSON数据写入临时文件
            objectMapper.writeValue(tempJsonFile.toFile(), inputData);
            
            // 构建命令 - 传递临时文件路径
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(
                pythonExecutable,
                scriptPath.toString(),
                tempJsonFile.toString()
            );
        
        // 设置工作目录
        processBuilder.directory(Paths.get(".").toAbsolutePath().toFile());
        
        logger.debug("执行命令: {}", String.join(" ", processBuilder.command()));
        
        // 启动进程
        Process process = processBuilder.start();
        
        // 读取输出
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }
        
            // 等待进程完成
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python模型调用超时");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("Python模型调用失败，退出码: {}, 错误输出: {}", exitCode, errorOutput.toString());
                throw new RuntimeException("Python模型调用失败: " + errorOutput.toString());
            }
            
            // 解析输出
            String outputStr = output.toString().trim();
            logger.debug("Python模型输出: {}", outputStr);
            
            try {
                JsonNode jsonNode = objectMapper.readTree(outputStr);
                return objectMapper.convertValue(jsonNode, Map.class);
            } catch (Exception e) {
                logger.error("解析Python模型输出失败: {}", outputStr, e);
                throw new RuntimeException("解析模型输出失败: " + e.getMessage());
            }
            
        } finally {
            // 清理临时文件
            try {
                if (tempJsonFile.toFile().exists()) {
                    tempJsonFile.toFile().delete();
                }
            } catch (Exception e) {
                logger.warn("删除临时文件失败: {}", tempJsonFile, e);
            }
        }
    }
    
    /**
     * 构建预测响应
     */
    private PredictionResponse buildResponse(Map<String, Object> result, PredictionRequest request) {
        PredictionResponse response = new PredictionResponse();
        
        // 检查是否成功
        Boolean success = (Boolean) result.get("success");
        if (success == null || !success) {
            response.setSuccess(false);
            response.setErrorMessage(result.get("error") != null ? result.get("error").toString() : "预测失败");
            return response;
        }
        
        // 设置预测结果
        response.setSuccess(true);
        response.setLatitude(request.getLatitude());
        response.setLongitude(request.getLongitude());
        response.setPredictionTimestamp(LocalDateTime.now());
        
        // 获取预测值
        @SuppressWarnings("unchecked")
        Map<String, Object> predictions = (Map<String, Object>) result.get("predictions");
        
        if (predictions != null) {
            // 设置DIN预测值
            if (predictions.containsKey("DIN")) {
                Double dinValue = ((Number) predictions.get("DIN")).doubleValue();
                response.setDinLevel(dinValue);
                response.setDinUnit("mg/L");
            }
            
            // 设置SRP预测值
            if (predictions.containsKey("SRP")) {
                Double srpValue = ((Number) predictions.get("SRP")).doubleValue();
                response.setSrpLevel(srpValue);
                response.setSrpUnit("mg/L");
            }
            
            // 设置pH预测值
            if (predictions.containsKey("pH")) {
                Double phValue = ((Number) predictions.get("pH")).doubleValue();
                response.setPhLevel(phValue);
                response.setPhUnit("");
            }
            
            // 进行水质等级分类
            try {
                WaterQualityClassificationService.WaterQualityClassification classification = 
                    waterQualityClassificationService.classifyWaterQuality(
                        response.getDinLevel(), 
                        response.getSrpLevel(), 
                        response.getPhLevel()
                    );
                
                // 设置水质等级信息
                response.setWaterQualityLevel(classification.getGrade().getName());
                response.setQualityLevel(classification.getGrade().getDescription());
                
                // 添加额外信息
                Map<String, Object> additionalInfo = new HashMap<>();
                additionalInfo.put("waterQualityGrade", classification.getGrade().getName());
                additionalInfo.put("waterQualityColor", classification.getGrade().getColor());
                additionalInfo.put("waterQualityDescription", classification.getGrade().getDescription());
                additionalInfo.put("waterQualityUsage", classification.getGrade().getUsage());
                additionalInfo.put("classificationReason", classification.getReason());
                additionalInfo.put("overallScore", classification.getOverallScore());
                response.setAdditionalInfo(additionalInfo);
                
                logger.info("水质分类完成: {}, 评分: {}", 
                    classification.getGrade().getName(), classification.getOverallScore());
                    
            } catch (Exception e) {
                logger.warn("水质分类失败，使用默认等级", e);
                response.setWaterQualityLevel("三级");
                response.setQualityLevel("一般");
            }
        }
        
        // 设置置信度
        if (result.containsKey("confidence")) {
            response.setConfidence(((Number) result.get("confidence")).doubleValue());
        }
        
        // 设置模型版本
        if (result.containsKey("modelVersion")) {
            response.setModelVersion((String) result.get("modelVersion"));
        }
        
        // 设置水质等级
        if (result.containsKey("qualityLevel")) {
            response.setWaterQualityLevel((String) result.get("qualityLevel"));
        } else {
            // 基于DIN值确定水质等级（如果没有提供）
            if (response.getDinLevel() != null) {
                response.setWaterQualityLevel(determineWaterQualityLevel(response.getDinLevel(), "DIN"));
            }
        }
        
        return response;
    }
    
    /**
     * 根据预测值确定水质等级
     */
    private String determineWaterQualityLevel(Double value, String targetType) {
        if (value == null) return "UNKNOWN";
        
        switch (targetType) {
            case "DIN":
                if (value < 0.02) return "EXCELLENT";
                if (value < 0.05) return "GOOD";
                if (value < 0.1) return "MODERATE";
                return "POOR";
                
            case "SRP":
                if (value < 0.005) return "EXCELLENT";
                if (value < 0.01) return "GOOD";
                if (value < 0.02) return "MODERATE";
                return "POOR";
                
            case "pH":
                if (value >= 7.5 && value <= 8.5) return "EXCELLENT";
                if (value >= 7.0 && value <= 9.0) return "GOOD";
                if (value >= 6.5 && value <= 9.5) return "MODERATE";
                return "POOR";
                
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * 创建错误响应
     */
    private PredictionResponse createErrorResponse(String errorMessage, PredictionRequest request) {
        PredictionResponse response = new PredictionResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setLatitude(request.getLatitude());
        response.setLongitude(request.getLongitude());
        response.setPredictionTimestamp(LocalDateTime.now());
        return response;
    }
    
    /**
     * 预测多个目标（DIN, SRP, pH）
     * 注意：我们的Python模型一次性返回所有三个预测值
     */
    public Map<String, PredictionResponse> predictMultipleTargets(PredictionRequest request) {
        Map<String, PredictionResponse> results = new HashMap<>();
        
        try {
            // 调用Python模型获取所有预测值
            Map<String, Object> inputData = prepareInputData(request);
            Map<String, Object> result = callPythonModel(inputData);
            
            // 构建主响应
            PredictionResponse mainResponse = buildResponse(result, request);
            
            if (mainResponse.isSuccess()) {
                // 为每个目标创建单独的响应
                String[] targets = {"DIN", "SRP", "pH"};
                
                for (String target : targets) {
                    PredictionResponse targetResponse = new PredictionResponse();
                    targetResponse.setSuccess(true);
                    targetResponse.setLatitude(request.getLatitude());
                    targetResponse.setLongitude(request.getLongitude());
                    targetResponse.setPredictionTimestamp(LocalDateTime.now());
                    targetResponse.setConfidence(mainResponse.getConfidence());
                    targetResponse.setModelVersion(mainResponse.getModelVersion());
                    
                    // 根据目标类型设置相应的值
                    switch (target) {
                        case "DIN":
                            targetResponse.setDinLevel(mainResponse.getDinLevel());
                            targetResponse.setDinUnit(mainResponse.getDinUnit());
                            targetResponse.setWaterQualityLevel(
                                determineWaterQualityLevel(mainResponse.getDinLevel(), "DIN"));
                            break;
                        case "SRP":
                            targetResponse.setSrpLevel(mainResponse.getSrpLevel());
                            targetResponse.setSrpUnit(mainResponse.getSrpUnit());
                            targetResponse.setWaterQualityLevel(
                                determineWaterQualityLevel(mainResponse.getSrpLevel(), "SRP"));
                            break;
                        case "pH":
                            targetResponse.setPhLevel(mainResponse.getPhLevel());
                            targetResponse.setPhUnit(mainResponse.getPhUnit());
                            targetResponse.setWaterQualityLevel(
                                determineWaterQualityLevel(mainResponse.getPhLevel(), "pH"));
                            break;
                    }
                    
                    results.put(target, targetResponse);
                }
            } else {
                // 如果预测失败，为所有目标返回错误响应
                String[] targets = {"DIN", "SRP", "pH"};
                for (String target : targets) {
                    results.put(target, createErrorResponse(mainResponse.getErrorMessage(), request));
                }
            }
            
        } catch (Exception e) {
            logger.error("预测失败", e);
            String[] targets = {"DIN", "SRP", "pH"};
            for (String target : targets) {
                results.put(target, createErrorResponse(e.getMessage(), request));
            }
        }
        
        return results;
    }
}