package com.oceangpt.agent;

import com.oceangpt.dto.PredictionRequest;
import com.oceangpt.dto.PredictionResponse;
import com.oceangpt.service.CustomModelService;

import java.util.HashMap;
import java.util.Map;

/**
 * 水质预测工具，封装自定义模型调用
 */
public class PredictTool implements AgentTool {
    private final CustomModelService customModelService;

    public PredictTool(CustomModelService customModelService) {
        this.customModelService = customModelService;
    }

    @Override
    public String getName() { return "水质预测"; }

    @Override
    public String getDescription() { return "调用自定义模型进行DIN/SRP/pH预测"; }

    @Override
    public Map<String, Object> invoke(Map<String, Object> input) throws Exception {
        PredictionRequest req = new PredictionRequest();
        Object lat = input.get("latitude");
        Object lng = input.get("longitude");
        if (lat instanceof Number) req.setLatitude(((Number) lat).doubleValue());
        if (lng instanceof Number) req.setLongitude(((Number) lng).doubleValue());

        if (input.get("chlNN") instanceof Number) req.setChlNN(((Number) input.get("chlNN")).doubleValue());
        if (input.get("tsmNN") instanceof Number) req.setTsmNN(((Number) input.get("tsmNN")).doubleValue());

        Map<String, PredictionResponse> preds = customModelService.predictMultipleTargets(req);
        Map<String, Object> out = new HashMap<>();
        out.put("predictions", preds);
        return out;
    }
}
