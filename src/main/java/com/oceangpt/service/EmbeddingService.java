package com.oceangpt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${oceangpt.rag.embedding.endpoint:}")
    private String endpoint;

    @Value("${oceangpt.rag.embedding.timeoutMs:4000}")
    private int timeoutMs;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(2000))
            .build();

    public Optional<double[]> embed(String text, int dim) {
        if (endpoint == null || endpoint.isBlank() || text == null || text.isBlank()) return Optional.empty();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", text);
            String body = objectMapper.writeValueAsString(payload);

            HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                logger.warn("Embedding endpoint 非 2xx: status={}", resp.statusCode());
                return Optional.empty();
            }
            Map<String, Object> map = objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
            Object vecObj = map.getOrDefault("vector", map.get("embedding"));
            if (!(vecObj instanceof List<?>)) {
                return Optional.empty();
            }
            List<?> list = (List<?>) vecObj;
            double[] v = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object o = list.get(i);
                v[i] = o instanceof Number ? ((Number) o).doubleValue() : Double.parseDouble(String.valueOf(o));
            }
            return Optional.of(v);
        } catch (Exception e) {
            logger.warn("生成 Embedding 失败: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
