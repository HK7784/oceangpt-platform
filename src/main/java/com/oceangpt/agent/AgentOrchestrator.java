package com.oceangpt.agent;

import com.oceangpt.dto.ChatRequest;
import com.oceangpt.dto.ChatResponse;
import com.oceangpt.dto.PredictionRequest;
import com.oceangpt.dto.PredictionResponse;
import com.oceangpt.dto.ReportRequest;
import com.oceangpt.dto.ReportResponse;
import com.oceangpt.service.CustomModelService;
import com.oceangpt.service.RagService;
import com.oceangpt.service.ReportGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体编排层 (Orchestrator)
 * 负责分析用户意图，按需调用 RAG、预测模型、报表生成等工具，
 * 并组装最终回复。
 */
@Service
public class AgentOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final RagService ragService;
    private final CustomModelService customModelService;
    private final ReportGenerationService reportService;

    // 简单会话上下文（Demo用途，生产环境建议用 Redis/Database）
    private final Map<String, List<String>> sessionHistory = new ConcurrentHashMap<>();

    @Autowired
    public AgentOrchestrator(RagService ragService,
                             CustomModelService customModelService,
                             ReportGenerationService reportService) {
        this.ragService = ragService;
        this.customModelService = customModelService;
        this.reportService = reportService;
    }

    /**
     * 处理用户请求
     */
    public ChatResponse run(ChatRequest request) {
        String sessionId = request.getSessionId();
        String message = request.getMessage();
        
        // 记录历史
        sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>()).add("User: " + message);

        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        
        List<String> steps = new ArrayList<>();
        response.setSteps(steps);

        try {
            // 1. 意图识别 (简单关键词匹配)
            boolean needRag = checkNeedRag(message);
            boolean needPrediction = checkNeedPrediction(message);
            boolean needReport = checkNeedReport(message);
            
            steps.add("分析用户意图: " + 
                (needRag ? "[RAG]" : "") + 
                (needPrediction ? "[Prediction]" : "") + 
                (needReport ? "[Report]" : ""));

            Map<String, Object> baseInput = new HashMap<>();
            baseInput.put("query", message);
            baseInput.put("sessionId", sessionId);
            
            // 提取地理位置信息
            if (request.getLocation() != null) {
                baseInput.put("latitude", request.getLocation().getLatitude());
                baseInput.put("longitude", request.getLocation().getLongitude());
            }

            // 2. 工具调用链
            
            // RAG检索
            if (needRag) {
                steps.add("RAG检索相关文章与资料");
                Map<String, Object> ragOut = safeInvoke(new RagTool(ragService), Map.of("query", message));
                Object docsObj = ragOut.get("docs");
                if (docsObj instanceof List) {
                   addData(response, "ragDocuments", docsObj);
                }
            }

            // 水质预测
            if (needPrediction) {
                steps.add("调用水质预测模型");
                Map<String, Object> predOut = safeInvoke(new PredictTool(customModelService), baseInput);
                addData(response, "predictions", predOut.get("predictions"));
            }

            // 报表生成
            if (needReport) {
                steps.add("生成水质分析报表");
                Map<String, Object> reportOut = safeInvoke(new ReportTool(reportService), baseInput);
                addData(response, "reportUrl", reportOut.get("reportUrl"));
                addData(response, "reportData", reportOut.get("reportData"));
            }

            // 3. 最终回复生成 (这里简单拼接，实际可用 LLM 润色)
            String finalReply = "收到您的指令：" + message + "\n" +
                    "已执行步骤：" + steps.toString();
            response.setMessage(finalReply);
            
            // 记录回复
            if (sessionHistory.containsKey(sessionId)) {
                sessionHistory.get(sessionId).add("Assistant: " + finalReply);
            }

        } catch (Exception e) {
            logger.error("Agent处理异常", e);
            response.setMessage("抱歉，处理您的请求时发生错误: " + e.getMessage());
        }

        return response;
    }

    private boolean checkNeedRag(String msg) {
        if (msg == null) return false;
        return msg.contains("查询") || msg.contains("资料") || msg.contains("知识") || msg.contains("什么");
    }

    private boolean checkNeedPrediction(String msg) {
        if (msg == null) return false;
        return msg.contains("预测") || msg.contains("趋势") || msg.contains("未来");
    }

    private boolean checkNeedReport(String msg) {
        if (msg == null) return false;
        return msg.contains("报表") || msg.contains("报告") || msg.contains("导出");
    }

    /**
     * 安全调用工具的辅助方法
     */
    private Map<String, Object> safeInvoke(AgentTool tool, Map<String, Object> input) {
        try {
            return tool.execute(input);
        } catch (Exception e) {
            logger.warn("工具调用失败: " + tool.getClass().getSimpleName(), e);
            return Map.of("error", e.getMessage());
        }
    }
    
    private void addData(ChatResponse response, String key, Object value) {
        if (response.getRelatedData() == null) {
            response.setRelatedData(new HashMap<>());
        }
        response.getRelatedData().put(key, value);
    }

    // --- 内部简单工具类定义 (也可以拆分为独立类) ---

    interface AgentTool {
        Map<String, Object> execute(Map<String, Object> input);
    }

    static class RagTool implements AgentTool {
        private final RagService service;
        public RagTool(RagService service) { this.service = service; }
        @Override
        public Map<String, Object> execute(Map<String, Object> input) {
            String q = (String) input.get("query");
            var docs = service.retrieve(q, 3);
            return Map.of("docs", docs);
        }
    }

    static class PredictTool implements AgentTool {
        private final CustomModelService service;
        public PredictTool(CustomModelService service) { this.service = service; }
        @Override
        public Map<String, Object> execute(Map<String, Object> input) {
            // 构造预测请求对象
            PredictionRequest req = new PredictionRequest();
            // 从输入中提取经纬度，若没有则使用默认值或抛出异常
            // 注意：这里需要根据实际业务逻辑决定默认值，这里暂用0.0防止空指针
            Double lat = (Double) input.getOrDefault("latitude", 0.0);
            Double lon = (Double) input.getOrDefault("longitude", 0.0);
            
            req.setLatitude(lat);
            req.setLongitude(lon);
            req.setTimestamp(System.currentTimeMillis());
            
            // 调用新版服务接口
            PredictionResponse result = service.predictWaterQuality(req);
            
            return Map.of("predictions", result);
        }
    }

    static class ReportTool implements AgentTool {
        private final ReportGenerationService service;
        public ReportTool(ReportGenerationService service) { this.service = service; }
        @Override
        public Map<String, Object> execute(Map<String, Object> input) {
            // 构造报告请求对象
            ReportRequest req = new ReportRequest();
            Double lat = (Double) input.getOrDefault("latitude", 0.0);
            Double lon = (Double) input.getOrDefault("longitude", 0.0);
            
            req.setLatitude(lat);
            req.setLongitude(lon);
            req.setReportType("WATER_QUALITY");
            
            // 调用新版服务接口
            ReportResponse report = service.generateWaterQualityReport(req);
            
            return Map.of("reportUrl", "/api/reports/" + report.getReportId(), "reportData", report);
        }
    }
}
