package com.oceangpt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.JsonData;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 简易 RAG 检索服务（最小化集成）
 * - 从 resources/rag/corpus.json 加载语料
 * - 采用朴素的关键词匹配与词频评分进行 TopK 检索
 * - 返回标题、片段、URL 与分数，用于增强 Chat 响应的 supportingEvidence/sources/relatedData
 */
@Service
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<Map<String, Object>> corpus = Collections.emptyList();
    private boolean loaded = false;

    // Lucene components for BM25 lexical retrieval
    private Directory luceneDirectory;
    private StandardAnalyzer analyzer;
    private IndexSearcher indexSearcher;
    private boolean luceneReady = false;

    // Elasticsearch integration (optional, on-demand)
    @Autowired(required = false)
    private ElasticsearchClient esClient;

    @Value("${oceangpt.rag.es.enabled:false}")
    private boolean esEnabled;

    @Value("${oceangpt.rag.es.index:oceangpt_corpus}")
    private String esIndex;

    @Value("${oceangpt.rag.es.bootstrap:true}")
    private boolean esBootstrap;

    @Value("${oceangpt.rag.es.vector.enabled:false}")
    private boolean esVectorEnabled;

    @Value("${oceangpt.rag.es.vector.field:embedding}")
    private String esVectorField;

    @Value("${oceangpt.rag.es.vector.dim:768}")
    private int esVectorDim;

    @Value("${oceangpt.rag.fuse.alpha:0.6}")
    private double fuseAlpha; // BM25 权重，向量融合时使用

    @Value("${oceangpt.rag.mmr.lambda:0.7}")
    private double mmrLambda;

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @PostConstruct
    public void loadCorpus() {
        try {
            ClassPathResource resource = new ClassPathResource("rag/corpus.json");
            if (!resource.exists()) {
                logger.warn("RAG 语料文件未找到: rag/corpus.json");
                return;
            }
            try (InputStream is = resource.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                String json = new String(bytes, StandardCharsets.UTF_8);
                corpus = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
                loaded = corpus != null && !corpus.isEmpty();
                logger.info("RAG 语料加载完成，共 {} 条", corpus.size());
            }
        } catch (IOException e) {
            logger.warn("加载 RAG 语料失败: {}", e.getMessage());
        }

        // build Lucene BM25 index lazily after corpus is loaded
        if (loaded) {
            try {
                buildLuceneIndex();
            } catch (Exception ex) {
                logger.warn("构建 Lucene 索引失败，继续使用朴素检索: {}", ex.getMessage());
            }

            // Initialize Elasticsearch index and bootstrap corpus if enabled
            try {
                initElasticsearchIndexIfEnabled();
            } catch (Exception ex) {
                logger.warn("初始化 Elasticsearch 失败，继续使用 Lucene/关键词降级: {}", ex.getMessage());
            }
        }
    }

    public boolean isAvailable() {
        return loaded;
    }

    /**
     * 根据用户查询进行检索
     * @param query 用户消息
     * @param topK 返回条数
     * @return 文档片段列表（title, snippet, url, score）
     */
    public List<Map<String, Object>> retrieve(String query, int topK) {
        if (!loaded || query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        String q = normalize(query);
        Set<String> qTokens = tokenize(q);

        int k = Math.max(1, topK);
        long t0 = System.currentTimeMillis();

        // 1) Lucene BM25 retrieval
        List<Map<String, Object>> bm25Results = Collections.emptyList();
        if (luceneReady) {
            try {
                bm25Results = searchLucene(query, Math.max(5, k * 2));
            } catch (Exception e) {
                logger.warn("Lucene 检索失败: {}", e.getMessage());
            }
        }

        // 1b) Elasticsearch lexical retrieval (multi_match)
        List<Map<String, Object>> esResults = Collections.emptyList();
        if (esEnabled && esClient != null) {
            try {
                esResults = searchElasticsearchLexical(query, Math.max(5, k * 2));
            } catch (Exception e) {
                logger.warn("Elasticsearch 检索失败: {}", e.getMessage());
            }
        }

        // 1c) Elasticsearch vector retrieval (knn) — when enabled and embedding is available
        List<Map<String, Object>> vecResults = Collections.emptyList();
        if (esEnabled && esVectorEnabled && esClient != null && embeddingService != null) {
            try {
                Optional<double[]> qvecOpt = embeddingService.embed(query, esVectorDim);
                if (qvecOpt.isPresent()) {
                    vecResults = searchElasticsearchVector(qvecOpt.get(), Math.max(5, k * 2));
                }
            } catch (Exception e) {
                logger.warn("Elasticsearch 向量检索失败: {}", e.getMessage());
            }
        }

        // 2) 朴素关键词检索（作为融合与降级路径）
        List<Map<String, Object>> keywordResults = naiveKeywordSearch(qTokens, Math.max(5, k * 2));

        // 3) 融合得分（BM25 + 关键词频次）
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        // 默认权重（仅词法融合）
        double alpha = 0.55; // BM25 权重
        double gamma = 0.30; // ES lexical 权重
        double beta = 0.15;  // 关键词权重
        // 若向量结果存在，则采用 PoC 权重：final = alpha * bm25 + (1-alpha) * vector
        boolean hasVector = vecResults != null && !vecResults.isEmpty();
        double delta = hasVector ? (1.0 - fuseAlpha) : 0.0; // 向量权重
        if (hasVector) {
            alpha = fuseAlpha;
            gamma = 0.0; // PoC 方案中仅使用 BM25+Vector；保留关键词作为兜底
        }

        for (Map<String, Object> r : bm25Results) {
            String key = Objects.toString(r.getOrDefault("url", Objects.toString(r.getOrDefault("title", UUID.randomUUID()))));
            merged.put(key, new HashMap<>(r));
            merged.get(key).put("bm25", r.getOrDefault("score", 0.0));
        }
        for (Map<String, Object> r : esResults) {
            String key = Objects.toString(r.getOrDefault("url", Objects.toString(r.getOrDefault("title", UUID.randomUUID()))));
            Map<String, Object> existing = merged.get(key);
            double esScore = ((Number) r.getOrDefault("score", 0.0)).doubleValue();
            if (existing == null) {
                Map<String, Object> copy = new HashMap<>(r);
                copy.put("es", esScore);
                merged.put(key, copy);
            } else {
                existing.put("es", esScore);
                existing.putIfAbsent("snippet", r.get("snippet"));
                existing.putIfAbsent("title", r.get("title"));
                existing.putIfAbsent("url", r.get("url"));
            }
        }
        for (Map<String, Object> r : vecResults) {
            String key = Objects.toString(r.getOrDefault("url", Objects.toString(r.getOrDefault("title", UUID.randomUUID()))));
            Map<String, Object> existing = merged.get(key);
            double vScore = ((Number) r.getOrDefault("score", 0.0)).doubleValue();
            if (existing == null) {
                Map<String, Object> copy = new HashMap<>(r);
                copy.put("vec", vScore);
                merged.put(key, copy);
            } else {
                existing.put("vec", vScore);
                existing.putIfAbsent("snippet", r.get("snippet"));
                existing.putIfAbsent("title", r.get("title"));
                existing.putIfAbsent("url", r.get("url"));
            }
        }
        for (Map<String, Object> r : keywordResults) {
            String key = Objects.toString(r.getOrDefault("url", Objects.toString(r.getOrDefault("title", UUID.randomUUID()))));
            Map<String, Object> existing = merged.get(key);
            double kw = (double) r.getOrDefault("score", 0.0);
            if (existing == null) {
                Map<String, Object> copy = new HashMap<>(r);
                copy.put("kw", kw);
                merged.put(key, copy);
            } else {
                existing.put("kw", kw);
                // 如果缺少 snippet/title/url，用关键词结果补齐
                existing.putIfAbsent("snippet", r.get("snippet"));
                existing.putIfAbsent("title", r.get("title"));
                existing.putIfAbsent("url", r.get("url"));
            }
        }

        List<Map<String, Object>> fused = new ArrayList<>();
        for (Map<String, Object> m : merged.values()) {
            double bm25 = ((Number) m.getOrDefault("bm25", 0.0)).doubleValue();
            double esLex = ((Number) m.getOrDefault("es", 0.0)).doubleValue();
            double kw = ((Number) m.getOrDefault("kw", 0.0)).doubleValue();
            double vec = ((Number) m.getOrDefault("vec", 0.0)).doubleValue();
            double score = hasVector ? (alpha * bm25 + delta * vec + beta * kw) : (alpha * bm25 + gamma * esLex + beta * kw);
            m.put("score", score);
            fused.add(m);
        }

        // 4) MMR 选择，控制冗余，提升信息密度
        List<Map<String, Object>> mmrSelected = mmrSelect(fused, k, mmrLambda);

        long elapsed = System.currentTimeMillis() - t0;
        double topScore = mmrSelected.isEmpty() ? 0.0 : ((Number) mmrSelected.get(0).getOrDefault("score", 0.0)).doubleValue();
        logger.info("RAG 检索 | query='{}' | hasVec={} | topK={} | topScore={} | elapsedMs={}", query, hasVector, k, topScore, elapsed);
        return mmrSelected;
    }

    private void initElasticsearchIndexIfEnabled() throws Exception {
        if (!esEnabled || esClient == null) return;

        // Check index existence
        var exists = esClient.indices().exists(e -> e.index(esIndex));
        if (!exists.value()) {
            // Create index with basic mappings
            CreateIndexResponse created = esClient.indices().create(c -> c
                .index(esIndex)
                .mappings(m -> {
                    m.properties("title", Property.of(p -> p.text(t -> t)));
                    m.properties("url", Property.of(p -> p.keyword(k -> k)));
                    m.properties("tags", Property.of(p -> p.keyword(k -> k)));
                    m.properties("content", Property.of(p -> p.text(t -> t)));
                    // Optional dense vector mapping (prepared, off by default)
                    if (esVectorEnabled) {
                        m.properties(esVectorField, Property.of(p -> p.denseVector(dv -> dv.dims(esVectorDim).similarity("cosine"))));
                    }
                    return m;
                })
            );
            logger.info("Elasticsearch 索引创建: {} (acknowledged={})", esIndex, created.acknowledged());
        }

        // Bootstrap corpus documents into ES index (optional)
        if (esBootstrap && corpus != null && !corpus.isEmpty()) {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (Map<String, Object> doc : corpus) {
                String id = Objects.toString(doc.getOrDefault("id", UUID.randomUUID().toString()));
                br.operations(op -> op.index(idx -> idx
                    .index(esIndex)
                    .id(id)
                    .document(doc)
                ));
            }
            BulkResponse bulk = esClient.bulk(br.build());
            if (bulk.errors()) {
                logger.warn("Elasticsearch 语料批量写入存在错误，建议检查映射或内容");
            } else {
                logger.info("Elasticsearch 语料批量写入成功: {} 条", corpus.size());
            }
        }
    }

    private List<Map<String, Object>> searchElasticsearchLexical(String query, int topK) throws Exception {
        if (!esEnabled || esClient == null) return Collections.emptyList();

        SearchRequest req = SearchRequest.of(s -> s
            .index(esIndex)
            .query(q -> q.multiMatch(mm -> mm
                .query(query)
                .fields("title^2", "content", "tags")
            ))
            .size(Math.max(1, topK))
        );

        SearchResponse<JsonData> resp = esClient.search(req, JsonData.class);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Hit<JsonData> hit : resp.hits().hits()) {
            Map<String, Object> source = hit.source() != null ? hit.source().to(Map.class) : Collections.emptyMap();
            Map<String, Object> item = new HashMap<>();
            String title = Objects.toString(source.getOrDefault("title", "未知标题"));
            String url = Objects.toString(source.getOrDefault("url", ""));
            String content = Objects.toString(source.getOrDefault("content", ""));
            item.put("title", title);
            item.put("url", url);
            item.put("score", hit.score() != null ? hit.score().doubleValue() : 0.0);
            item.put("snippet", buildSnippet(content, tokenize(normalize(query))));
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> searchElasticsearchVector(double[] queryVector, int topK) throws Exception {
        if (!esEnabled || !esVectorEnabled || esClient == null || queryVector == null) return Collections.emptyList();

        // 构造 knn 查询的原始 JSON，以确保兼容 ES 客户端版本
        int k = Math.max(1, topK);
        int numCandidates = Math.max(50, k * 10);
        String vecJson = Arrays.toString(queryVector);
        String body = "{" +
                "\"size\":" + k + "," +
                "\"knn\":{" +
                    "\"field\":\"" + esVectorField + "\"," +
                    "\"query_vector\":" + vecJson + "," +
                    "\"k\":" + k + "," +
                    "\"num_candidates\":" + numCandidates +
                "}}";

        SearchRequest req = new SearchRequest.Builder()
                .index(esIndex)
                .withJson(new java.io.StringReader(body))
                .build();

        SearchResponse<JsonData> resp = esClient.search(req, JsonData.class);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Hit<JsonData> hit : resp.hits().hits()) {
            Map<String, Object> source = hit.source() != null ? hit.source().to(Map.class) : Collections.emptyMap();
            Map<String, Object> item = new HashMap<>();
            String title = Objects.toString(source.getOrDefault("title", "未知标题"));
            String url = Objects.toString(source.getOrDefault("url", ""));
            String content = Objects.toString(source.getOrDefault("content", ""));
            item.put("title", title);
            item.put("url", url);
            item.put("score", hit.score() != null ? hit.score().doubleValue() : 0.0);
            item.put("snippet", buildSnippet(content, tokenize(normalize(Arrays.toString(queryVector)))));
            results.add(item);
        }
        return results;
    }

    private void buildLuceneIndex() throws Exception {
        analyzer = new StandardAnalyzer();
        luceneDirectory = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(luceneDirectory, config)) {
            for (Map<String, Object> doc : corpus) {
                Document d = new Document();
                String id = Objects.toString(doc.getOrDefault("id", UUID.randomUUID().toString()));
                String title = Objects.toString(doc.getOrDefault("title", "未知标题"));
                String url = Objects.toString(doc.getOrDefault("url", ""));
                String tags = String.join(",", (List<String>) doc.getOrDefault("tags", Collections.emptyList()));
                String content = Objects.toString(doc.getOrDefault("content", ""));

                d.add(new StringField("id", id, Field.Store.YES));
                d.add(new TextField("title", title, Field.Store.YES));
                d.add(new StringField("url", url, Field.Store.YES));
                d.add(new TextField("tags", tags, Field.Store.NO));
                d.add(new TextField("content", content, Field.Store.YES));
                writer.addDocument(d);
            }
        }
        indexSearcher = new IndexSearcher(DirectoryReader.open(luceneDirectory));
        indexSearcher.setSimilarity(new BM25Similarity());
        luceneReady = true;
        logger.info("Lucene BM25 索引构建完成");
    }

    private List<Map<String, Object>> searchLucene(String query, int topK) throws Exception {
        if (!luceneReady) return Collections.emptyList();
        QueryParser parser = new QueryParser("content", analyzer);
        Query q = parser.parse(QueryParser.escape(query));
        TopDocs topDocs = indexSearcher.search(q, Math.max(1, topK));
        List<Map<String, Object>> results = new ArrayList<>();
        for (ScoreDoc sd : topDocs.scoreDocs) {
            Document doc = indexSearcher.doc(sd.doc);
            Map<String, Object> item = new HashMap<>();
            item.put("title", doc.get("title"));
            item.put("url", doc.get("url"));
            item.put("score", (double) sd.score);
            String content = doc.get("content");
            item.put("snippet", buildSnippet(content, tokenize(normalize(query))));
            results.add(item);
        }
        return results;
    }

    private List<Map<String, Object>> naiveKeywordSearch(Set<String> qTokens, int topK) {
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Map<String, Object> doc : corpus) {
            String content = normalize(Objects.toString(doc.getOrDefault("content", "")));
            Set<String> docTokens = tokenize(content);
            if (docTokens.isEmpty()) continue;

            int overlap = 0;
            for (String t : qTokens) {
                if (docTokens.contains(t)) overlap++;
            }
            if (overlap == 0) continue; // 无匹配，不计分

            int freq = frequency(content, qTokens);
            double score = overlap + 0.2 * freq;

            Map<String, Object> item = new HashMap<>();
            item.put("title", doc.get("title"));
            item.put("snippet", buildSnippet(Objects.toString(doc.getOrDefault("content", "")), qTokens));
            item.put("url", doc.get("url"));
            item.put("score", score);
            scored.add(item);
        }
        scored.sort((a, b) -> Double.compare((double) b.get("score"), (double) a.get("score")));
        return scored.subList(0, Math.min(scored.size(), topK));
    }
    
    // MMR (Maximal Marginal Relevance) re-ranking
    private List<Map<String, Object>> mmrSelect(List<Map<String, Object>> items, int topK, double lambda) {
        if (items.isEmpty()) return Collections.emptyList();
        
        List<Map<String, Object>> selected = new ArrayList<>();
        List<Map<String, Object>> candidates = new ArrayList<>(items);
        
        // Pick the best one first
        candidates.sort((a, b) -> Double.compare((double) b.get("score"), (double) a.get("score")));
        selected.add(candidates.remove(0));
        
        while (selected.size() < topK && !candidates.isEmpty()) {
            double bestMMR = -Double.MAX_VALUE;
            int bestIdx = -1;
            
            for (int i = 0; i < candidates.size(); i++) {
                Map<String, Object> cand = candidates.get(i);
                double relevance = (double) cand.get("score");
                double maxSim = 0.0;
                
                String candContent = normalize(Objects.toString(cand.get("snippet"), ""));
                Set<String> candTokens = tokenize(candContent);
                
                for (Map<String, Object> sel : selected) {
                    String selContent = normalize(Objects.toString(sel.get("snippet"), ""));
                    Set<String> selTokens = tokenize(selContent);
                    double sim = jaccardSimilarity(candTokens, selTokens);
                    if (sim > maxSim) maxSim = sim;
                }
                
                double mmr = lambda * relevance - (1 - lambda) * maxSim;
                if (mmr > bestMMR) {
                    bestMMR = mmr;
                    bestIdx = i;
                }
            }
            
            if (bestIdx != -1) {
                selected.add(candidates.remove(bestIdx));
            } else {
                break;
            }
        }
        return selected;
    }
    
    private double jaccardSimilarity(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);
        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);
        return (double) intersection.size() / union.size();
    }

    private String normalize(String text) {
        return text.toLowerCase().replaceAll("[^a-z0-9\u4e00-\u9fa5]+", " ");
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        return Arrays.stream(text.split("\\s+"))
                .filter(s -> s.length() > 1)
                .collect(Collectors.toSet());
    }

    private int frequency(String text, Set<String> tokens) {
        int count = 0;
        String[] words = text.split("\\s+");
        for (String w : words) {
            if (tokens.contains(w)) count++;
        }
        return count;
    }

    private String buildSnippet(String content, Set<String> qTokens) {
        if (content == null) return "";
        if (content.length() < 200) return content;
        
        // Find best window
        String lower = content.toLowerCase();
        int bestIdx = 0;
        int maxCount = 0;
        
        // Scan with sliding window
        for (int i = 0; i < content.length() - 100; i += 50) {
            String window = lower.substring(i, Math.min(i + 150, lower.length()));
            int count = 0;
            for (String t : qTokens) {
                if (window.contains(t)) count++;
            }
            if (count > maxCount) {
                maxCount = count;
                bestIdx = i;
            }
        }
        
        int end = Math.min(bestIdx + 150, content.length());
        return (bestIdx > 0 ? "..." : "") + content.substring(bestIdx, end) + (end < content.length() ? "..." : "");
    }
}
