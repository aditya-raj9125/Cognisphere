package com.cognisphere.backend.agent.parsers;

import com.cognisphere.backend.service.KnowledgeService;
import com.cognisphere.backend.util.WebPageExtractor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cognisphere.backend.util.Const.URL_PARSER_NAME;

/**
 * URL parser with improved web scraping:
 *  - Realistic browser headers to avoid 403/bot blocks
 *  - Strips nav/footer/ads before extracting
 *  - Prefers <article>/<main>, falls back to longest paragraph block
 *  - Special-cases GitHub blob → raw content
 *  - Map-reduce chunking for large pages (avoids token overflow)
 */
@Slf4j
@Component
class UrlParser implements Parser {

    // Each chunk sent to the LLM (~3000 chars ≈ ~750 tokens, safe for Haiku/Nova)
    private static final int CHUNK_SIZE    = 3000;
    private static final int CHUNK_OVERLAP = 200;

    // Prompt for summarizing a single chunk
    private static final String CHUNK_PROMPT = """
            You are a knowledge extraction assistant.
            Summarize the key facts and concepts from the following web page excerpt in 2-4 sentences.
            Return ONLY plain text, no JSON, no markdown.
            
            Excerpt:
            """;

    // Prompt for final map-reduce step: combine chunk summaries into structured JSON
    private static final String REDUCE_PROMPT = """
            You are a knowledge extraction assistant.
            
            Given the combined summaries of a web page below, extract:
            1. "title" — A concise, specific title (max 5 words) for the main topic. No generic words like "Overview" or "Summary".
            2. "summary" — A structured Markdown summary with headings and bullet points. 3-5 paragraphs.
            
            Return ONLY a valid raw JSON object. No code block markers, no explanation.
            
            Format:
            {
              "title": "...",
              "summary": "..."
            }
            
            Combined summaries:
            """;

    private final ChatLanguageModel chatModel;
    private final KnowledgeService knowledgeService;

    @Autowired
    public UrlParser(ChatLanguageModel chatModelProvider, KnowledgeService knowledgeService) {
        this.chatModel = chatModelProvider;
        this.knowledgeService = knowledgeService;
    }

    @Override
    public String getName() {
        return URL_PARSER_NAME;
    }

    @Override
    public Object run(Map<String, Object> input) {
        String url = (String) input.get("content");

        if (url == null || url.isBlank()) {
            return Map.of("error", "No URL provided.");
        }

        log.info("[UrlParser] Processing URL: {}", url);

        // ── Step 1: Extract page content ─────────────────────────────────────
        WebPageExtractor.PageData page;
        try {
            page = WebPageExtractor.extract(url);
        } catch (Exception e) {
            log.error("[UrlParser] Failed to fetch/scrape URL: {}", e.getMessage());
            return Map.of("error", "Failed to load webpage", "details", e.getMessage());
        }

        if (page.content.isBlank()) {
            log.warn("[UrlParser] No content extracted from URL: {}", url);
            return Map.of("error", "Could not extract any content from this page. It may be JavaScript-rendered or bot-protected.");
        }

        log.info("[UrlParser] Extracted {} chars from '{}' ({})", page.content.length(), page.title, url);

        // ── Step 2: Build full text (title + description + content) ──────────
        StringBuilder fullText = new StringBuilder();
        if (!page.title.isBlank()) {
            fullText.append("Page Title: ").append(page.title).append("\n\n");
        }
        if (!page.description.isBlank()) {
            fullText.append("Page Description: ").append(page.description).append("\n\n");
        }
        fullText.append(page.content);

        // ── Step 3: Chunk and map-summarize ──────────────────────────────────
        List<String> chunks = WebPageExtractor.chunk(fullText.toString(), CHUNK_SIZE, CHUNK_OVERLAP);
        log.info("[UrlParser] Split into {} chunk(s) for summarization", chunks.size());

        String combinedSummary;
        if (chunks.size() == 1) {
            // Short page — skip map step, just reduce directly
            combinedSummary = chunks.get(0);
        } else {
            // Map: summarize each chunk individually
            List<String> chunkSummaries = chunks.stream().map(chunk -> {
                try {
                    return chatModel.chat(CHUNK_PROMPT + chunk);
                } catch (Exception e) {
                    log.warn("[UrlParser] Chunk summarization failed, using raw chunk: {}", e.getMessage());
                    return chunk.substring(0, Math.min(chunk.length(), 500));
                }
            }).collect(Collectors.toList());

            combinedSummary = String.join("\n\n", chunkSummaries);
            log.info("[UrlParser] Map step done, combined {} chunk summaries", chunkSummaries.size());
        }

        // ── Step 4: Reduce — final structured JSON output ────────────────────
        try {
            String prompt = REDUCE_PROMPT + combinedSummary;
            String result = chatModel.chat(prompt);
            log.info("[UrlParser] LLM reduce response: {}", result);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(result, new TypeReference<>() {});
            parsed.put("source", URL_PARSER_NAME);

            Map<String, Object> extra = new HashMap<>();
            extra.put("website_urls", List.of(url));
            if (!page.title.isBlank()) extra.put("page_title", page.title);
            parsed.put("extra", extra);

            knowledgeService.saveFromParsedResult(parsed, true);
            log.info("[UrlParser] Saved to knowledge graph: {}", parsed.get("title"));
            return parsed;

        } catch (Exception e) {
            log.error("[UrlParser] LLM reduce step failed: {}", e.getMessage(), e);
            return Map.of("error", "LLM processing failed", "details", e.getMessage());
        }
    }
}
