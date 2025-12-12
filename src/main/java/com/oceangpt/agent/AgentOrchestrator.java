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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);
    private final RagService ragService;
    private final CustomModelService customModelService;
    private final ReportGenerationService reportService;
    private final Map<String, List<String>> sessionHistory = new ConcurrentHashMap<>();

    @Autowired
    public AgentOrchestrator(RagService ragService, CustomModelService customModelService, ReportGenerationService reportService) {
        this.ragService = ragService;
        this.customModelService = customModelService;
        this.reportService = reportService;
    }

    public ChatResponse run(ChatRequest request) {
        String sessionId = request.getSessionId();
        String message = request.getMessage();
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        List<String> steps = new ArrayList<>();
        response.setSteps(steps);

        try {
            boolean needRag = message.contains("查询") || message.contains("资料");
            boolean needPrediction = message.contains("预测") || message.contains("趋势");
            boolean needReport = message.contains("报表") || message.contains("报告");

            Map<String, Object> baseInput = new HashMap<>();
            baseInput.put("query", message);
            if (request.getLocation() != null) {
                baseInput.put("latitude", request.getLocation().getLatitude());
                baseInput.put("longitude", request.getLocation().getLongitude());
            }

            if (needRag) {
                steps.add("RAG检索资料");
                Map<String, Object> ragOut = new RagTool(ragService).execute(baseInput);
                addData(response, "ragDocuments", ragOut.get("docs"));
            }
            if (needPrediction) {
                steps.add("调用水质预测");
                Map<String, Object> predOut = new PredictTool(customModelService).execute(baseInput);
                addData(response, "predictions", predOut.get("predictions"));
            }
            if (needReport) {
                steps.add("生成报表");
                Map<String, Object> reportOut = new ReportTool(reportService).execute(baseInput);
                addData(response, "reportUrl", reportOut.get("reportUrl"));
                addData(response, "reportData", reportOut.get("reportData"));
            }

            String finalReply = "收到指令：" + message + "\n执行步骤：" + steps;
            response.setMessage(finalReply);

        } catch (Exception e) {
            logger.error("Agent Error", e);
            response.setMessage("处理出错: " + e.getMessage());
        }
        return response;
    }

    private void addData(ChatResponse response, String key, Object value) {
        if (response.getRelatedData() == null) response.setRelatedData(new HashMap<>());
        response.getRelatedData().put(key, value);
    }

    interface AgentTool { Map<String, Object> execute(Map<String, Object> input); }

    static class RagTool implements AgentTool {
        private final RagService service;
        public RagTool(RagService service) { this.service = service; }
        public Map<String, Object> execute(Map<String, Object> input) {
            return Map.of("docs", service.retrieve((String)input.get("query"), 3));
        }
    }

    static class PredictTool implements AgentTool {
        private final CustomModelService service;
        public PredictTool(CustomModelService service) { this.service = service; }
        public Map<String, Object> execute(Map<String, Object> input) {
            PredictionRequest req = new PredictionRequest();
            req.setLatitude((Double) input.getOrDefault("latitude", 0.0));
            req.setLongitude((Double) input.getOrDefault("longitude", 0.0));
            return Map.of("predictions", service.predictWaterQuality(req));
        }
    }

    static class ReportTool implements AgentTool {
        private final ReportGenerationService service;
        public ReportTool(ReportGenerationService service) { this.service = service; }
        public Map<String, Object> execute(Map<String, Object> input) {
            ReportRequest req = new ReportRequest();
            req.setLatitude((Double) input.getOrDefault("latitude", 0.0));
            req.setLongitude((Double) input.getOrDefault("longitude", 0.0));
            ReportResponse res = service.generateWaterQualityReport(req);
            return Map.of("reportUrl", "/api/reports/" + res.getReportId(), "reportData", res);
        }
    }
}
