package com.oceangpt.agent;

import com.oceangpt.dto.ChatRequest;
import com.oceangpt.dto.ChatResponse;
import com.oceangpt.service.CustomModelService;
import com.oceangpt.service.RagService;
import com.oceangpt.service.ReportGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 轻量Agent编排器：Planner + Tools + Memory
 * 将用户请求解析为工具序列并执行，合成ChatResponse
 */
@Service
public class AgentOrchestrator {
    private final AgentMemory memory = new AgentMemory();
    private final RagService ragService;
    private final CustomModelService customModelService;
    private final ReportGenerationService reportService;

    @Autowired
    public AgentOrchestrator(RagService ragService,
                             CustomModelService customModelService,
                             ReportGenerationService reportService) {
        this.ragService = ragService;
        this.customModelService = customModelService;
        this.reportService = reportService;
    }

    public ChatResponse run(ChatRequest request) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(request.getSessionId());
        List<String> steps = new ArrayList<>();

        AgentContext ctx = new AgentContext(request.getSessionId());
        ctx.setLanguage(request.getLanguage());
        if (request.getLocation() != null) {
            ctx.setLatitude(request.getLocation().getLatitude());
            ctx.setLongitude(request.getLocation().getLongitude());
            ctx.setLocationName(request.getLocation().getRegion());
        }

        String msg = Optional.ofNullable(request.getMessage()).orElse("").toLowerCase();
        boolean needPrediction = msg.contains("预测") || msg.contains("predict") || msg.contains("预报");
        boolean needReport = msg.contains("报告") || msg.contains("report");
        boolean needRag = true; // 默认开启RAG增强

        Map<String, Object> baseInput = new HashMap<>();
        if (ctx.getLatitude() != null) baseInput.put("latitude", ctx.getLatitude());
        if (ctx.getLongitude() != null) baseInput.put("longitude", ctx.getLongitude());
        baseInput.put("query", request.getMessage());

        // RAG检索
        if (needRag) {
            steps.add("RAG检索相关文章与资料");
            Map<String, Object> ragOut = safeInvoke(new RagTool(ragService), Map.of("query", request.getMessage()));
            Object docsObj = ragOut.get("docs");
            if (docsObj instanceof List) {
                response.addData("ragDocuments", docsObj);
                // 将RAG文档加入基础输入，供后续步骤（如报告生成）使用
                baseInput.put("ragDocuments", docsObj);
                
                List<String> sources = new ArrayList<>();
                for (Object d : (List<?>) docsObj) {
                    if (d instanceof Map) {
                        Object url = ((Map<?, ?>) d).get("url");
                        if (url != null && !url.toString().isBlank()) {
                            sources.add(url.toString());
                        }
                    }
                }
                if (!sources.isEmpty()) response.setSources(sources);
            }
        }

        // 预测
        if (needPrediction) {
            steps.add("调用水质预测模型");
            Map<String, Object> predOut = safeInvoke(new PredictTool(customModelService), baseInput);
            response.addData("predictions", predOut.get("predictions"));
        }

        // 报告生成
        if (needReport) {
            steps.add("生成水质分析报告");
            Map<String, Object> repOut = safeInvoke(new ReportTool(reportService), baseInput);
            response.addData("report", repOut.get("report"));
        }

        // 合成消息
        StringBuilder sb = new StringBuilder();
        if (needPrediction) sb.append("已完成目标水质参数预测。");
        if (needReport) sb.append("并生成了报告，可进一步查看详情。");
        if (!needPrediction && !needReport) sb.append("已为您检索并整理相关资料。");

        response.setMessage(sb.toString());
        response.setSuccess(true);
        response.setConfidence(0.9);
        response.setSteps(steps);

        // 记忆更新示例
        if (ctx.getLatitude() != null && ctx.getLongitude() != null) {
            memory.set(request.getSessionId(), "last_location",
                Map.of("lat", ctx.getLatitude(), "lng", ctx.getLongitude()));
        }

        return response;
    }

    private Map<String, Object> safeInvoke(AgentTool tool, Map<String, Object> input) {
        try {
            return tool.invoke(input);
        } catch (Exception e) {
            Map<String, Object> out = new HashMap<>();
            out.put("error", e.getMessage());
            return out;
        }
    }
}
