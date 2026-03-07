package com.cognisphere.backend.service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.cognisphere.backend.mapper.NodeMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.document.Metadata;


import static com.cognisphere.backend.util.Const.LINK_SIMILARITY_THRESHOLD;
import static com.cognisphere.backend.util.Const.LINK_MAX_CANDIDATES;
import static com.cognisphere.backend.util.Const.LINK_MIN_AI_STRENGTH;
import static com.cognisphere.backend.util.Const.MAX_MERGE_COUNT;
import static com.cognisphere.backend.util.Prompts.MERGE_KNOWLEDGE;
import static com.cognisphere.backend.util.Prompts.RECOMMEND_CONFIRM;
import static com.cognisphere.backend.util.Prompts.CATEGORY_CLASSIFY;
import static com.cognisphere.backend.util.Prompts.RELATIONSHIP_JUDGE;

@Slf4j
@Service
public class KnowledgeService {
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatLanguageModel;
    private final NodeMapper nodeMapper;

    @Autowired
    public KnowledgeService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> vectorStore,
                            NodeMapper nodeMapper, ChatLanguageModel chatModelProvider) {
        this.embeddingStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.chatLanguageModel = chatModelProvider;
        this.nodeMapper = nodeMapper;
    }

    public void saveFromParsedResult(Map<String, Object> parsed, boolean autoLink) {
        String title = (String) parsed.getOrDefault("title", "Untitled");
        String summary = (String) parsed.getOrDefault("summary", "");
        String source = (String) parsed.getOrDefault("source", "unknown");
        String createdAt = Instant.now().toString();
        String uuid = (String) parsed.getOrDefault("uuid", UUID.randomUUID().toString());
        if (uuid.isBlank()) {
            uuid = UUID.randomUUID().toString();
        }

        // ── AI-based category classification ──────────────────────────────────────
        String category = "general";
        String subcategory = "knowledge";
        try {
            String snippet = summary.length() > 300 ? summary.substring(0, 300) : summary;
            String classifyPrompt = String.format(CATEGORY_CLASSIFY, title, snippet);
            String classifyResult = chatLanguageModel.chat(classifyPrompt);
            ObjectMapper classifyMapper = new ObjectMapper();
            Map<String, String> classification = classifyMapper.readValue(classifyResult, new TypeReference<>() {});
            category = classification.getOrDefault("category", "general");
            subcategory = classification.getOrDefault("subcategory", "knowledge");
            log.info("[AI] Node '{}' classified as category='{}', subcategory='{}'", title, category, subcategory);
        } catch (Exception e) {
            log.warn("[AI] Category classification failed for '{}', defaulting to 'general/knowledge': {}", title, e.getMessage());
        }
        // ──────────────────────────────────────────────────────────────────────────

        Metadata metadata = new Metadata();
        metadata.put("uuid", uuid);
        metadata.put("title", title);
        metadata.put("source", source);
        metadata.put("createdAt", createdAt);
        metadata.put("category", category);
        metadata.put("subcategory", subcategory);
        try {
            if (parsed.containsKey("extra")) {
                ObjectMapper mapper = new ObjectMapper();
                String extraJson = mapper.writeValueAsString(parsed.get("extra"));
                metadata.put("extra", extraJson);
            }
        } catch (Exception e) {
            log.error("[KnowledgeService] Failed to parse extra metadata", e);
        }

        TextSegment textSegment = new TextSegment(summary, metadata);
        log.info("[KnowledgeService] Saving text segment: {}, Metadata: {}", textSegment, metadata);
        Embedding embedding = embeddingModel.embed(textSegment).content();

        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .minScore(LINK_SIMILARITY_THRESHOLD)
                .maxResults(LINK_MAX_CANDIDATES)
                .build();

        // save node
        this.embeddingStore.add(embedding, textSegment);
        log.info("[GRAPH] Node created: '{}' [category={}, subcategory={}]", title, category, subcategory);

        if (!autoLink) {
            log.info("[KnowledgeService] Link creation disabled.");
            return;
        }

        // ── Phase 1: Embedding similarity — wide net of candidates ────────────
        List<EmbeddingMatch<TextSegment>> relevant = embeddingStore.search(embeddingSearchRequest).matches();
        List<EmbeddingMatch<TextSegment>> candidates = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> r : relevant) {
            String matchUuid = r.embedded().metadata().getString("uuid");
            if (uuid.equals(matchUuid) || r.score() >= 0.999) continue;
            candidates.add(r);
        }

        if (candidates.isEmpty()) {
            log.info("[LINK] No embedding candidates found for '{}'", title);
            return;
        }

        log.info("[LINK] Found {} embedding candidates for '{}', invoking AI relationship judge...", candidates.size(), title);

        // ── Phase 2: AI Relationship Judge — single batched LLM call ──────────
        StringBuilder candidateBlock = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            EmbeddingMatch<TextSegment> c = candidates.get(i);
            String cTitle = c.embedded().metadata().getString("title");
            String cText = c.embedded().text();
            String cSnippet = cText.length() > 250 ? cText.substring(0, 250) + "..." : cText;
            candidateBlock.append(String.format("%d. \"%s\" (similarity=%.2f) - %s\n",
                    i + 1, cTitle, c.score(), cSnippet));
        }

        String snippet = summary.length() > 300 ? summary.substring(0, 300) : summary;
        String judgePrompt = String.format(RELATIONSHIP_JUDGE, title, snippet, candidateBlock.toString());

        try {
            String judgeResult = chatLanguageModel.chat(judgePrompt);
            log.info("[LINK] AI judge response for '{}': {}", title, judgeResult);

            ObjectMapper judgeMapper = new ObjectMapper();
            List<Map<String, Object>> judgments = judgeMapper.readValue(
                    judgeResult, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});

            int linksCreated = 0;
            for (Map<String, Object> judgment : judgments) {
                int index = ((Number) judgment.getOrDefault("index", 0)).intValue() - 1; // 0-based
                boolean connected = Boolean.TRUE.equals(judgment.get("connected"));
                if (!connected || index < 0 || index >= candidates.size()) continue;

                double strength = ((Number) judgment.getOrDefault("strength", 0.0)).doubleValue();
                if (strength < LINK_MIN_AI_STRENGTH) continue;

                String label = (String) judgment.getOrDefault("label", "related");
                String toId = candidates.get(index).embedded().metadata().getString("uuid");

                // Combine embedding score and AI strength: weighted blend
                double embScore = candidates.get(index).score();
                double finalScore = 0.4 * embScore + 0.6 * strength;

                nodeMapper.createRelationship(uuid, toId, finalScore, label);
                log.info("[LINK] AI-approved edge: '{}' --[{}]--> '{}' (strength={}, embSim={}, final={})",
                        title, label, candidates.get(index).embedded().metadata().getString("title"),
                        strength, String.format("%.3f", embScore), String.format("%.3f", finalScore));
                linksCreated++;
            }
            log.info("[LINK] Created {} AI-validated links for '{}'", linksCreated, title);

        } catch (Exception e) {
            // Fallback: if AI judge fails, create links for any candidate with embedding score >= 0.70
            log.warn("[LINK] AI judge failed for '{}', falling back to embedding-only linking: {}", title, e.getMessage());
            for (EmbeddingMatch<TextSegment> c : candidates) {
                if (c.score() >= 0.70) {
                    String toId = c.embedded().metadata().getString("uuid");
                    nodeMapper.createRelationship(uuid, toId, c.score());
                    log.info("[LINK] Fallback link: {} -> {} (score={})", uuid, toId, c.score());
                }
            }
        }
    }


    /**
     * Retroactive relink — scans ALL nodes and discovers missing connections.
     * For each node, embeds its summary, searches for candidates, and runs the AI judge
     * to create edges that were missed at insertion time.
     *
     * @return number of new edges created
     */
    public int relinkAllNodes() {
        log.info("[RELINK] Starting full graph relink...");
        List<Map<String, String>> allNodes = nodeMapper.getAllNodeSummaries();
        Set<String> existingPairs = nodeMapper.getExistingEdgePairs();

        int totalNew = 0;

        for (Map<String, String> node : allNodes) {
            String uuid = node.get("uuid");
            String title = node.get("title");
            String snippet = node.get("snippet");

            // Embed this node's text and search for candidates
            Embedding embedding = embeddingModel.embed(snippet).content();
            EmbeddingSearchRequest req = EmbeddingSearchRequest.builder()
                    .queryEmbedding(embedding)
                    .minScore(LINK_SIMILARITY_THRESHOLD)
                    .maxResults(LINK_MAX_CANDIDATES)
                    .build();

            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(req).matches();

            // Filter: remove self, near-perfect, and already-connected pairs
            List<EmbeddingMatch<TextSegment>> candidates = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> m : matches) {
                String mUuid = m.embedded().metadata().getString("uuid");
                if (uuid.equals(mUuid) || m.score() >= 0.999) continue;

                String pairKey = uuid.compareTo(mUuid) < 0 ? uuid + "|" + mUuid : mUuid + "|" + uuid;
                if (existingPairs.contains(pairKey)) continue;

                candidates.add(m);
            }

            if (candidates.isEmpty()) continue;

            // Build candidate block for AI judge
            StringBuilder candidateBlock = new StringBuilder();
            for (int i = 0; i < candidates.size(); i++) {
                EmbeddingMatch<TextSegment> c = candidates.get(i);
                String cTitle = c.embedded().metadata().getString("title");
                String cSnippet = c.embedded().text();
                if (cSnippet.length() > 250) cSnippet = cSnippet.substring(0, 250) + "...";
                candidateBlock.append(String.format("%d. \"%s\" (similarity=%.2f) - %s\n",
                        i + 1, cTitle, c.score(), cSnippet));
            }

            String judgePrompt = String.format(RELATIONSHIP_JUDGE, title, snippet, candidateBlock.toString());

            try {
                String result = chatLanguageModel.chat(judgePrompt);
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> judgments = mapper.readValue(
                        result, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});

                for (Map<String, Object> j : judgments) {
                    int idx = ((Number) j.getOrDefault("index", 0)).intValue() - 1;
                    boolean connected = Boolean.TRUE.equals(j.get("connected"));
                    if (!connected || idx < 0 || idx >= candidates.size()) continue;

                    double strength = ((Number) j.getOrDefault("strength", 0.0)).doubleValue();
                    if (strength < LINK_MIN_AI_STRENGTH) continue;

                    String label = (String) j.getOrDefault("label", "related");
                    String toId = candidates.get(idx).embedded().metadata().getString("uuid");
                    double embScore = candidates.get(idx).score();
                    double finalScore = 0.4 * embScore + 0.6 * strength;

                    nodeMapper.createRelationship(uuid, toId, finalScore, label);
                    // Mark as existing so we don't re-evaluate the reverse direction
                    String pairKey = uuid.compareTo(toId) < 0 ? uuid + "|" + toId : toId + "|" + uuid;
                    existingPairs.add(pairKey);
                    totalNew++;

                    log.info("[RELINK] New edge: '{}' --[{}]--> '{}' (score={})",
                            title, label, candidates.get(idx).embedded().metadata().getString("title"),
                            String.format("%.3f", finalScore));
                }
            } catch (Exception e) {
                log.warn("[RELINK] AI judge failed for node '{}': {}", title, e.getMessage());
            }
        }

        log.info("[RELINK] Complete. {} new edges created across {} nodes.", totalNew, allNodes.size());
        return totalNew;
    }

    public List<Map<String, Object>> recommendByUuid(String fromId) {
        Map<String, Object> node = nodeMapper.getNodeByUuid(fromId);
        return recommendFromNode(node, fromId);
    }

    public List<Map<String, Object>> recommendByTitle(String title) {
        Map<String, Object> node = nodeMapper.getNodeByTitle(title);
        if (node == null) {
            log.error("[KnowledgeService] No node found for title: {}", title);
            return List.of();
        }
        String uuid = (String) node.get("uuid");
        if (uuid == null || uuid.isEmpty()) {
            log.error("[KnowledgeService] No UUID found for title: {}", title);
            return List.of();
        }
        return recommendFromNode(node, (String) node.get("uuid"));
    }

    private List<Map<String, Object>> recommendFromNode(Map<String, Object> node, String fromId) {
        String title = (String) node.get("title");
        String summary = (String) node.get("text");

        if (summary == null || summary.isEmpty()) {
            log.error("[KnowledgeService] No summary found for title: {}", title);
            return List.of();
        }

        String prompt = String.format("""
                You are a smart knowledge assistant helping to expand a knowledge graph.
                
                Given the following node:
                Title: "%s"
                Content: "%s"
                
                Please recommend 3 new, distinct knowledge nodes that are related to the original content.
                For each recommended node, include:
                - A concise and meaningful title (5 words max)
                - A one or two sentence summary explaining the topic
                
                Return ONLY a valid **raw JSON array** object. DO NOT include ```json or ``` or any explanation text. Format:
                [
                  {"title": "Title A", "summary": "Summary of A..."},
                  {"title": "Title B", "summary": "Summary of B..."},
                  {"title": "Title C", "summary": "Summary of C..."}
                ]
                """, title, summary);

        String result = chatLanguageModel.chat(prompt);
        log.info("[KnowledgeService] Recommendation result for '{}':\n{}", title, result);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, String>> parsed = objectMapper.readValue(result, new TypeReference<>() {
            });
            return parsed.stream()
                    .map(entry -> Map.<String, Object>of(
                            "fromId", fromId,
                            "title", entry.get("title"),
                            "summary", entry.get("summary")
                    ))
                    .toList();
        } catch (Exception e) {
            log.warn("[KnowledgeService] Failed to parse recommendation result, returning raw result", e);
            return List.of(Map.of(
                    "fromId", fromId,
                    "raw", result
            ));
        }
    }


    public void confirmAndSave(String title, String summary, String fromId, String newId) {
        String prompt = String.format(RECOMMEND_CONFIRM, title, summary);

        String fullText = chatLanguageModel.chat(prompt);

        Map<String, Object> parsedResult = Map.of(
                "title", title,
                "summary", fullText,
                "source", "generated",
                "uuid", newId
        );

        saveFromParsedResult(parsedResult, true);
        nodeMapper.createRelationship(fromId, newId, 0);
        log.info("[KnowledgeService] Created relationship from {} to {}", fromId, newId);
    }

    public Map<String, Object> mergeNodes(List<String> uuids) throws JsonProcessingException {
        if (uuids == null || uuids.isEmpty()) {
            log.error("[mergeNodes] No UUIDs provided");
            throw new IllegalArgumentException("No UUIDs provided");
        }
        if (uuids.size() > MAX_MERGE_COUNT) {
            log.error("[mergeNodes] Cannot merge more than {} nodes. Provided: {}", MAX_MERGE_COUNT, uuids.size());
            throw new IllegalArgumentException("Cannot merge more than " + MAX_MERGE_COUNT + " nodes.");
        }

        // 1. Get all nodes by UUIDs
        log.info("[mergeNodes] Fetching nodes for UUIDs: {}", uuids);
        List<Map<String, Object>> nodes = uuids.stream()
                .map(nodeMapper::getNodeByUuid)
                .toList();

        // 2. Combine all titles and texts
        String combinedTitle = nodes.stream().map(n -> (String) n.get("title")).collect(Collectors.joining(" | "));
        String combinedText = IntStream.range(0, nodes.size())
                .mapToObj(i -> "Topic " + (i + 1) + ": " + nodes.get(i).get("title") + "\n" + nodes.get(i).get("text"))
                .collect(Collectors.joining("\n\n"));

        log.info("[mergeNodes] Combined title: {}", combinedTitle);

        // 3. Merge extra fields
        log.info("[mergeNodes] Merging extra fields from nodes");
        Set<String> websiteUrls = new HashSet<>();
        Set<String> youtubeUrls = new HashSet<>();
        Set<String> videoIds = new HashSet<>();

        for (Map<String, Object> node : nodes) {
            Map<String, Object> extra = (Map<String, Object>) node.getOrDefault("extra", Map.of());
            log.info("[mergeNodes] Extra fields: {}", extra);
            websiteUrls.addAll((List<String>) extra.getOrDefault("website_urls", List.of()));
            youtubeUrls.addAll((List<String>) extra.getOrDefault("youtube_urls", List.of()));
            videoIds.addAll((List<String>) extra.getOrDefault("video_ids", List.of()));
        }
        log.info("[mergeNodes] Merged extra fields: website_urls: {}, youtube_urls: {}, video_ids: {}",
                websiteUrls, youtubeUrls, videoIds);

        // 4. Generate a new title and summary using the LLM
        log.info("[mergeNodes] Generating merged title and summary using LLM");
        String prompt = MERGE_KNOWLEDGE + combinedText;

        log.info("[mergeNodes] Merging nodes with prompt: {}", prompt);

        String result = chatLanguageModel.chat(prompt);
        log.info("[mergeNodes] LLM response: {}", result);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> resultMap = mapper.readValue(result, new TypeReference<>() {
        });
        String newUuid = UUID.randomUUID().toString();
        String newTitle = (String) resultMap.getOrDefault("title", "");
        String mergedText = (String) resultMap.getOrDefault("summary", "");

        if (newTitle.isBlank() || mergedText.isBlank()) {
            log.error("[mergeNodes] Failed to generate new title or summary from LLM");
            throw new IllegalArgumentException("Failed to generate new title or summary");
        }
        log.info("[mergeNodes] Generated new node with UUID: {}, Title: {}", newUuid, newTitle);

        // 5. Save the new node
        Map<String, Object> newNodeInfo = Map.of(
                "uuid", newUuid,
                "title", newTitle,
                "summary", mergedText,
                "source", "merged",
                "createdAt", Instant.now().toString(),
                "extra", Map.of(
                        "website_urls", websiteUrls,
                        "youtube_urls", youtubeUrls,
                        "video_ids", videoIds
                )
        );
        log.info("[mergeNodes] Saving new merged node");
        saveFromParsedResult(newNodeInfo, false);

        // 6. find all neighbors of the original nodes and connect them to the new node
        log.info("[mergeNodes] Reconnecting neighbors to new node");
        nodeMapper.reconnectNeighbors(newUuid, uuids);

        // 7. delete the original nodes and their relationships
        log.info("[mergeNodes] Deleting original nodes: {}", uuids);
        nodeMapper.deleteNodesByUuids(uuids);

        log.info("[mergeNodes] Merge completed successfully. New node UUID: {}", newUuid);
        return Map.of("uuid", newUuid, "title", newTitle, "text", mergedText, "extra", Map.of(
                "website_urls", websiteUrls,
                "youtube_urls", youtubeUrls,
                "video_ids", videoIds
        ));
    }

    public Map<String, Object> getNodeByUuid(String uuid) {
        Map<String, Object> node = nodeMapper.getNodeByUuid(uuid);
        if (node == null) {
            log.error("[KnowledgeService] No node found for UUID: {}", uuid);
            return Map.of("error", "Node not found for UUID: " + uuid);
        }
        return node;
    }

    public void addYoutubeUrlToNode(String uuid, String youtubeLink) {
        nodeMapper.addYoutubeUrlToNode(uuid, youtubeLink);
    }

    public void deleteNode(String uuid) {
        log.info("[KnowledgeService] Deleting node with UUID: {}", uuid);
        nodeMapper.deleteNodesByUuids(List.of(uuid));
        log.info("[KnowledgeService] Node deleted: {}", uuid);
    }
}
