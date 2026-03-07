package com.cognisphere.backend.agent.tools;

import com.cognisphere.backend.service.KnowledgeService;
import com.cognisphere.backend.util.SearchHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.cognisphere.backend.util.Const.QUERY_SIMILARITY_THRESHOLD;
import static com.cognisphere.backend.util.Prompts.RECOMMEND_CONFIRM;
import static com.cognisphere.backend.util.Prompts.CHAT_NODE_CREATE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatTools {

    private final KnowledgeService knowledgeService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatLanguageModel chatModel;
    private final SearchHelper searchHelper;

    @Tool("Quick search: finds the single most relevant knowledge node for a topic")
    public Map<String, Object> findRelevantNode(@P("user input topic") String topic) {
        log.info("[QueryVectorTool] Searching for relevant knowledge node for topic: {}", topic);
        try {
            Embedding queryEmbedding = embeddingModel.embed(topic).content();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .minScore(QUERY_SIMILARITY_THRESHOLD)
                    .maxResults(1)
                    .build();

            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();
            if (matches.isEmpty()) {
                return Map.of("matched", false, "message", "No relevant knowledge node found.");
            }

            EmbeddingMatch<TextSegment> match = matches.get(0);
            TextSegment segment = match.embedded();

            Metadata metadata = segment.metadata();
            log.info("[QueryVectorTool] Found relevant node title: {}", metadata.getString("title"));

            return Map.of(
                    "matched", true,
                    "uuid", Objects.requireNonNull(metadata.getString("uuid")),
                    "title", Objects.requireNonNull(metadata.getString("title")),
                    "summary", segment.text(),
                    "score", match.score()
            );
        } catch (Exception e) {
            log.error("[QueryVectorTool] Failed to query embedding store", e);
            return Map.of("matched", false, "error", e.getMessage());
        }
    }

    /**
     * Deep search across the entire knowledge graph — returns multiple matching nodes.
     * Preferred tool for KNOWLEDGE_ONLY_MODE to gather comprehensive context.
     */
    @Tool("Deep search: searches the entire knowledge graph and returns ALL relevant nodes (up to 5) for a topic. Use this for thorough knowledge-graph queries.")
    public Map<String, Object> searchKnowledgeGraph(@P("the topic or question to search for") String query) {
        log.info("[DeepSearch] Searching knowledge graph for: {}", query);
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // Use a lower threshold to cast a wider net and return up to 5 nodes
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .minScore(0.55)
                    .maxResults(5)
                    .build();

            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();
            if (matches.isEmpty()) {
                log.info("[DeepSearch] No matches found for: {}", query);
                return Map.of("found", false, "count", 0, "message", "No relevant nodes found in your knowledge graph.");
            }

            List<Map<String, Object>> results = matches.stream().map(match -> {
                TextSegment segment = match.embedded();
                Metadata metadata = segment.metadata();
                Map<String, Object> node = new HashMap<>();
                node.put("uuid", metadata.getString("uuid"));
                node.put("title", metadata.getString("title"));
                node.put("summary", segment.text());
                node.put("score", match.score());
                return node;
            }).toList();

            log.info("[DeepSearch] Found {} matching nodes for: {}", results.size(), query);
            return Map.of("found", true, "count", results.size(), "nodes", results);
        } catch (Exception e) {
            log.error("[DeepSearch] Failed to search knowledge graph", e);
            return Map.of("found", false, "error", e.getMessage());
        }
    }

    @Tool("Given a node uuid, query for details of the node.")
    public Map<String, Object> queryNodeByUuid(@P("uuid") String uuid) {
        log.info("[QueryNodeByUuidTool] Querying node with UUID: {}", uuid);
        try {
            Map<String, Object> node = knowledgeService.getNodeByUuid(uuid);
            if (node == null || node.isEmpty()) {
                log.warn("[QueryNodeByUuidTool] Node not found for UUID: {}", uuid);
                return Map.of("error", "Node not found for UUID: " + uuid);
            }

            Map<String, Object> retNode = new HashMap<>(knowledgeService.getNodeByUuid(uuid));

            retNode.remove("createdAt");
            retNode.remove("type");
            log.info("[QueryNodeByUuidTool] Node found, title:{}", retNode.getOrDefault("title", "N/A"));
            return retNode;
        } catch (Exception e) {
            log.warn("[QueryNodeByUuidTool] error for UUID: {}", uuid, e);
            return Map.of("error", "Node not found for UUID: " + uuid);
        }
    }

    @Tool("Given a topic the user is interested in, recommends 3 related knowledge nodes with title and summary.")
    public List<Map<String, String>> recommendNodesTool(String topic) {
        log.info("[RecommendNodesTool] Recommending nodes for topic: {}", topic);
        String prompt = String.format("""
                    You are a smart assistant helping to expand a knowledge graph.
                
                    The user wants to learn about: "%s"
                
                    Please recommend 3 distinct knowledge nodes that are relevant to this topic.
                    For each recommended node, include:
                    - A concise title (5 words max)
                    - A short summary (1-2 sentences)
                
                    Return ONLY a valid **raw JSON array** object. DO NOT include ```json or ``` or any explanation text. Format:
                    [
                      {"title": "Node Title A", "summary": "A short summary of A..."},
                      {"title": "Node Title B", "summary": "A short summary of B..."},
                      {"title": "Node Title C", "summary": "A short summary of C..."}
                    ]
                """, topic);

        try {
            String result = chatModel.chat(prompt);
            log.info("[RecommendNodesTool] Raw result:\n{}", result);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(result, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("[RecommendNodesTool] Failed to parse recommendation result", e);
            return List.of(Map.of("error", "Failed to parse result", "raw", e.getMessage()));
        }
    }

    @Tool("Confirm and save a newly recommended node into the knowledge graph.")
    public Map<String, Object> confirmRecommendationTool(@P("title") String title, @P("summary") String summary) {
        log.info("[ConfirmRecommendationTool] Confirming recommendation for title: {}", title);
        try {
            String prompt = String.format(RECOMMEND_CONFIRM, title, summary);

            String fullText = chatModel.chat(prompt);

            Map<String, Object> parsedResult = Map.of(
                    "title", title,
                    "summary", fullText,
                    "type", "generated"
            );
            knowledgeService.saveFromParsedResult(parsedResult, true);
            log.info("[ConfirmRecommendationTool] Node saved successfully for title: {}", title);
            return Map.of("status", "success", "message", "Node saved successfully");
        } catch (Exception e) {
            log.error("[ConfirmRecommendationTool] Failed to save node", e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @Tool("When the user wants to learn more about a given node title, find and suggest a relevant YouTube video as an additional resource.")
    public Map<String, Object> recommendVideoTool(@P("title") String title) {
        log.info("[RecommendVideoTool] Searching video for title: {}", title);
        try {
            String youtubeLink = searchHelper.searchYouTube(title);
            if (youtubeLink == null || youtubeLink.isBlank()) {
                return Map.of("status", "error", "message", "No video found");
            }
            return Map.of("status", "success", "youtubeLink", youtubeLink);
        } catch (Exception e) {
            log.error("[RecommendVideoTool] Failed to search video", e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @Tool("Attach/save a given YouTube video link to an existing knowledge node by its UUID, enhancing the node with additional multimedia content.")
    public Map<String, Object> attachVideoToNodeTool(@P("uuid") String uuid, @P("youtubeLink") String youtubeLink) {
        log.info("[attachVideoToNodeTool] Processing node UUID: {}, youtubeLink: {}", uuid, youtubeLink);
        try {
            knowledgeService.addYoutubeUrlToNode(uuid, youtubeLink);

            log.info("[attachVideoToNodeTool] Successfully added video {} to node {}", youtubeLink, uuid);
            return Map.of("status", "success", "youtubeLink", youtubeLink);

        } catch (Exception e) {
            log.error("[attachVideoToNodeTool] Failed to process", e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    /**
     * Creates a new knowledge node directly from a chat message exchange.
     * Call this after every meaningful AI response to capture the knowledge.
     */
    @Tool("Create a new knowledge node in the graph from a chat conversation. Call this after answering a meaningful knowledge question to preserve the insight.")
    public Map<String, Object> createNodeFromChatTool(
            @P("The user's question or message") String userMessage,
            @P("The AI's complete response / answer") String aiResponse) {
        log.info("[GRAPH] Creating chat knowledge node for topic derived from: '{}'", userMessage);
        try {
            String prompt = String.format(CHAT_NODE_CREATE, userMessage, aiResponse);
            String result = chatModel.chat(prompt);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(result, new TypeReference<>() {});

            String title = (String) parsed.getOrDefault("title", "Chat Knowledge");
            String summary = (String) parsed.getOrDefault("summary", aiResponse);

            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("title", title);
            nodeData.put("summary", summary);
            nodeData.put("source", "chat");

            knowledgeService.saveFromParsedResult(nodeData, true);
            log.info("[GRAPH] Node created from chat: '{}'", title);
            return Map.of("status", "success", "title", title, "message", "Node '" + title + "' added to your knowledge graph.");
        } catch (Exception e) {
            log.error("[GRAPH] Failed to create chat node", e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    /**
     * Performs a general web search and returns summarized results.
     * Use when the user's question cannot be answered from the knowledge graph.
     */
    @Tool("Search the web for general knowledge questions not found in the knowledge graph. Returns top search results with titles, links, and snippets.")
    public Map<String, Object> webSearchTool(@P("search query") String query) {
        log.info("[AI] Web search invoked for: '{}'", query);
        try {
            List<Map<String, String>> results = searchHelper.searchWeb(query, 3);
            if (results.isEmpty()) {
                return Map.of("status", "no_results", "message", "No web results found for: " + query);
            }
            log.info("[AI] Web search returned {} results for '{}'", results.size(), query);
            return Map.of("status", "success", "query", query, "results", results);
        } catch (Exception e) {
            log.error("[AI] Web search failed for '{}': {}", query, e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
