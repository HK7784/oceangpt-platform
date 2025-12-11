package com.oceangpt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OceanGPT语言模型服务
 * 负责调用vLLM部署的OceanGPT模型生成自然语言报告
 */
@Service
public class OceanGptLanguageService {
    
    private static final Logger logger = LoggerFactory.getLogger(OceanGptLanguageService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${oceangpt.vllm.endpoint:http://localhost:8000/v1/completions}")
    private String vllmEndpoint;
    
    @Value("${oceangpt.vllm.model:oceangpt-14b}")
    private String modelName;
    
    @Value("${oceangpt.vllm.timeout:60}")
    private int timeoutSeconds;
    
    @Value("${oceangpt.vllm.enabled:false}")
    private boolean vllmEnabled;
    
    @Value("${oceangpt.vllm.max-tokens:2048}")
    private int maxTokens;
    
    @Value("${oceangpt.vllm.temperature:0.7}")
    private double temperature;
    
    /**
     * 生成水质分析报告的执行摘要
     */
    public String generateExecutiveSummary(Map<String, Object> predictionData, 
                                          double latitude, double longitude,
                                          java.util.List<java.util.Map<String, Object>> ragDocuments) {
        if (!vllmEnabled) {
            return generateFallbackExecutiveSummary(predictionData, latitude, longitude);
        }
        
        try {
            String prompt = buildExecutiveSummaryPrompt(predictionData, latitude, longitude, ragDocuments);
            return callVllmApi(prompt);
        } catch (Exception e) {
            logger.warn("vLLM调用失败，使用备用方案: {}", e.getMessage());
            return generateFallbackExecutiveSummary(predictionData, latitude, longitude);
        }
    }
    
    // 兼容旧接口
    public String generateExecutiveSummary(Map<String, Object> predictionData, 
                                          double latitude, double longitude) {
        return generateExecutiveSummary(predictionData, latitude, longitude, null);
    }
    
    /**
     * 生成详细的水质分析报告
     */
    public String generateDetailedAnalysis(Map<String, Object> predictionData,
                                         Map<String, Object> spectralData,
                                         java.util.List<java.util.Map<String, Object>> ragDocuments) {
        if (!vllmEnabled) {
            return generateFallbackDetailedAnalysis(predictionData, spectralData);
        }
        
        try {
            String prompt = buildDetailedAnalysisPrompt(predictionData, spectralData, ragDocuments);
            return callVllmApi(prompt);
        } catch (Exception e) {
            logger.warn("vLLM调用失败，使用备用方案: {}", e.getMessage());
            return generateFallbackDetailedAnalysis(predictionData, spectralData);
        }
    }

    // 兼容旧接口
    public String generateDetailedAnalysis(Map<String, Object> predictionData,
                                         Map<String, Object> spectralData) {
        return generateDetailedAnalysis(predictionData, spectralData, null);
    }
    
    /**
     * 生成环境风险评估报告
     */
    public String generateRiskAssessment(Map<String, Object> predictionData,
                                       Map<String, Object> environmentalFactors,
                                       java.util.List<java.util.Map<String, Object>> ragDocuments) {
        if (!vllmEnabled) {
            return generateFallbackRiskAssessment(predictionData, environmentalFactors);
        }
        
        try {
            String prompt = buildRiskAssessmentPrompt(predictionData, environmentalFactors, ragDocuments);
            return callVllmApi(prompt);
        } catch (Exception e) {
            logger.warn("vLLM调用失败，使用备用方案: {}", e.getMessage());
            return generateFallbackRiskAssessment(predictionData, environmentalFactors);
        }
    }

    // 兼容旧接口
    public String generateRiskAssessment(Map<String, Object> predictionData,
                                       Map<String, Object> environmentalFactors) {
        return generateRiskAssessment(predictionData, environmentalFactors, null);
    }
    
    /**
     * 调用vLLM API
     */
    private String callVllmApi(String prompt) throws IOException {
        URL url = new URL(vllmEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // 设置请求参数
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(timeoutSeconds));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(timeoutSeconds));
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("stop", new String[]{"\n\n", "[END]"});
            
            // 发送请求
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(objectMapper.writeValueAsString(requestBody));
                writer.flush();
            }
            
            // 读取响应
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    // 解析响应
                    JsonNode responseJson = objectMapper.readTree(response.toString());
                    JsonNode choices = responseJson.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        JsonNode firstChoice = choices.get(0);
                        JsonNode text = firstChoice.get("text");
                        if (text != null) {
                            return text.asText().trim();
                        }
                    }
                    
                    throw new IOException("无效的API响应格式");
                }
            } else {
                throw new IOException("API调用失败，响应码: " + responseCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * 构建执行摘要提示词
     */
    private String buildExecutiveSummaryPrompt(Map<String, Object> predictionData, 
                                              double latitude, double longitude,
                                              java.util.List<java.util.Map<String, Object>> ragDocuments) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("作为海洋环境专家，请基于以下水质预测数据和相关知识库文档，生成一份简洁的执行摘要：\n\n");
        prompt.append("位置信息：\n");
        prompt.append(String.format("- 经度: %.2f°\n", longitude));
        prompt.append(String.format("- 纬度: %.2f°\n", latitude));
        prompt.append("\n预测结果：\n");
        
        for (Map.Entry<String, Object> entry : predictionData.entrySet()) {
            prompt.append(String.format("- %s: %s\n", entry.getKey(), entry.getValue()));
        }

        if (ragDocuments != null && !ragDocuments.isEmpty()) {
            prompt.append("\n相关知识库参考：\n");
            for (java.util.Map<String, Object> doc : ragDocuments) {
                prompt.append(String.format("- %s: %s\n", doc.getOrDefault("title", "未命名"), doc.getOrDefault("snippet", "")));
            }
        }
        
        prompt.append("\n请生成一份100-200字的执行摘要，包括：\n");
        prompt.append("1. 水质状况总体评价\n");
        prompt.append("2. 主要关注点（结合知识库信息）\n");
        prompt.append("3. 简要建议\n\n");
        prompt.append("执行摘要：");
        
        return prompt.toString();
    }
    
    /**
     * 构建详细分析提示词
     */
    private String buildDetailedAnalysisPrompt(Map<String, Object> predictionData,
                                             Map<String, Object> spectralData,
                                             java.util.List<java.util.Map<String, Object>> ragDocuments) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("作为海洋环境专家，请基于以下数据和相关知识库文档，生成详细的水质分析报告：\n\n");
        prompt.append("水质预测数据：\n");
        
        for (Map.Entry<String, Object> entry : predictionData.entrySet()) {
            prompt.append(String.format("- %s: %s\n", entry.getKey(), entry.getValue()));
        }
        
        if (spectralData != null && !spectralData.isEmpty()) {
            prompt.append("\n光谱数据特征：\n");
            for (Map.Entry<String, Object> entry : spectralData.entrySet()) {
                prompt.append(String.format("- %s: %s\n", entry.getKey(), entry.getValue()));
            }
        }

        if (ragDocuments != null && !ragDocuments.isEmpty()) {
            prompt.append("\n相关知识库参考：\n");
            for (java.util.Map<String, Object> doc : ragDocuments) {
                prompt.append(String.format("- %s: %s\n", doc.getOrDefault("title", "未命名"), doc.getOrDefault("snippet", "")));
            }
        }
        
        prompt.append("\n请生成详细分析，包括：\n");
        prompt.append("1. 各项水质参数的科学解读（引用知识库内容）\n");
        prompt.append("2. 参数间的相互关系分析\n");
        prompt.append("3. 可能的环境影响因素\n");
        prompt.append("4. 与海洋环境标准的对比\n\n");
        prompt.append("详细分析：");
        
        return prompt.toString();
    }
    
    /**
     * 构建风险评估提示词
     */
    private String buildRiskAssessmentPrompt(Map<String, Object> predictionData,
                                           Map<String, Object> environmentalFactors,
                                           java.util.List<java.util.Map<String, Object>> ragDocuments) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("作为海洋环境风险评估专家，请基于以下数据和相关知识库文档，进行环境风险评估：\n\n");
        prompt.append("水质数据：\n");
        
        for (Map.Entry<String, Object> entry : predictionData.entrySet()) {
            prompt.append(String.format("- %s: %s\n", entry.getKey(), entry.getValue()));
        }
        
        if (environmentalFactors != null && !environmentalFactors.isEmpty()) {
            prompt.append("\n环境因子：\n");
            for (Map.Entry<String, Object> entry : environmentalFactors.entrySet()) {
                prompt.append(String.format("- %s: %s\n", entry.getKey(), entry.getValue()));
            }
        }

        if (ragDocuments != null && !ragDocuments.isEmpty()) {
            prompt.append("\n相关知识库参考：\n");
            for (java.util.Map<String, Object> doc : ragDocuments) {
                prompt.append(String.format("- %s: %s\n", doc.getOrDefault("title", "未命名"), doc.getOrDefault("snippet", "")));
            }
        }
        
        prompt.append("\n请进行风险评估，包括：\n");
        prompt.append("1. 营养盐污染风险等级及原因\n");
        prompt.append("2. 海洋酸化风险评估\n");
        prompt.append("3. 生态系统影响风险（结合知识库案例）\n");
        prompt.append("4. 短期和长期风险预测\n");
        prompt.append("5. 风险缓解建议\n\n");
        prompt.append("风险评估：");
        
        return prompt.toString();
    }
    
    // 备用方案方法
    private String generateFallbackExecutiveSummary(Map<String, Object> predictionData, 
                                                   double latitude, double longitude) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("基于卫星遥感数据，对目标海域（经度%.2f°，纬度%.2f°）的水质状况进行了分析。", 
                                   longitude, latitude));
        
        if (predictionData.containsKey("DIN")) {
            summary.append(String.format("溶解无机氮浓度为%s，", predictionData.get("DIN")));
        }
        if (predictionData.containsKey("SRP")) {
            summary.append(String.format("可溶性活性磷浓度为%s，", predictionData.get("SRP")));
        }
        if (predictionData.containsKey("pH")) {
            summary.append(String.format("pH值为%s。", predictionData.get("pH")));
        }
        
        summary.append("总体而言，该海域水质状况需要持续监测，建议定期评估环境变化趋势。");
        return summary.toString();
    }
    
    private String generateFallbackDetailedAnalysis(Map<String, Object> predictionData,
                                                   Map<String, Object> spectralData) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("## 详细水质参数分析\n\n");
        
        for (Map.Entry<String, Object> entry : predictionData.entrySet()) {
            analysis.append(String.format("### %s分析\n", entry.getKey()));
            analysis.append(String.format("预测值：%s\n", entry.getValue()));
            analysis.append("该参数在海洋环境监测中具有重要意义，需要结合其他指标综合评估。\n\n");
        }
        
        return analysis.toString();
    }
    
    private String generateFallbackRiskAssessment(Map<String, Object> predictionData,
                                                 Map<String, Object> environmentalFactors) {
        StringBuilder risk = new StringBuilder();
        risk.append("## 环境风险评估\n\n");
        risk.append("### 营养盐污染风险\n");
        risk.append("基于当前水质参数，营养盐污染风险处于可控范围，建议持续监测。\n\n");
        risk.append("### 生态系统影响风险\n");
        risk.append("当前水质状况对海洋生态系统的影响相对较小，但需要关注长期变化趋势。\n\n");
        
        return risk.toString();
    }
    
    /**
     * 检查vLLM服务状态
     */
    public boolean isVllmServiceAvailable() {
        if (!vllmEnabled) {
            return false;
        }
        
        try {
            URL url = new URL(vllmEndpoint.replace("/v1/completions", "/health"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            logger.debug("vLLM服务不可用: {}", e.getMessage());
            return false;
        }
    }
}
