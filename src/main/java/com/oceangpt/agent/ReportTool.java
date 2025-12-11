package com.oceangpt.agent;

import com.oceangpt.dto.ReportRequest;
import com.oceangpt.dto.ReportResponse;
import com.oceangpt.service.ReportGenerationService;

import java.util.HashMap;
import java.util.Map;

/**
 * 报告生成工具，封装报告生成服务
 */
public class ReportTool implements AgentTool {
    private final ReportGenerationService reportService;

    public ReportTool(ReportGenerationService reportService) {
        this.reportService = reportService;
    }

    @Override
    public String getName() { return "报告生成"; }

    @Override
    public String getDescription() { return "基于预测生成水质分析报告"; }

    @Override
    public Map<String, Object> invoke(Map<String, Object> input) throws Exception {
        ReportRequest rr = new ReportRequest();
        if (input.get("latitude") instanceof Number) rr.setLatitude(((Number) input.get("latitude")).doubleValue());
        if (input.get("longitude") instanceof Number) rr.setLongitude(((Number) input.get("longitude")).doubleValue());
        
        // 传递RAG文档
        if (input.containsKey("ragDocuments") && input.get("ragDocuments") instanceof java.util.List) {
            rr.setRagDocuments((java.util.List<java.util.Map<String, Object>>) input.get("ragDocuments"));
        }
        
        ReportResponse resp = reportService.generateWaterQualityReport(rr);
        Map<String, Object> out = new HashMap<>();
        out.put("report", resp);
        return out;
    }
}
