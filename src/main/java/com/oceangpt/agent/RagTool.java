package com.oceangpt.agent;

import com.oceangpt.service.RagService;

import java.util.*;

/**
 * RAG检索工具
 */
public class RagTool implements AgentTool {
    private final RagService ragService;

    public RagTool(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public String getName() { return "RAG检索"; }

    @Override
    public String getDescription() { return "检索相关知识文档与来源"; }

    @Override
    public Map<String, Object> invoke(Map<String, Object> input) throws Exception {
        String query = input.getOrDefault("query", "").toString();
        List<Map<String, Object>> docs = Collections.emptyList();
        if (ragService != null && ragService.isAvailable()) {
            docs = ragService.retrieve(query, 5);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("docs", docs);
        return out;
    }
}
