package com.cognisphere.backend.agent.parsers;

import com.cognisphere.backend.service.KnowledgeService;
import com.cognisphere.backend.util.TextTruncator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static com.cognisphere.backend.util.Const.YOUTUBE_PARSER_NAME;
import static com.cognisphere.backend.util.Prompts.SUBTITLE_EXTRACT;

/**
 * Lightweight YouTube parser — no yt-dlp, no S3, no Transcribe.
 *
 * Architecture:
 *  1. Extract video ID from URL
 *  2. Fetch title + description via YouTube Data API v3 (instant, just an HTTP call)
 *  3. Try to fetch auto-generated subtitles via YouTube's timedtext API (best-effort)
 *  4. Combine title + description + subtitles and feed to LLM
 *  5. Save result to Neo4j knowledge graph
 *
 * Speed: ~3-10 seconds total (vs 2-10 minutes with the old yt-dlp + Transcribe approach)
 * AWS console changes needed: NONE — uses the existing Google API key from application.properties
 */
@Slf4j
@Component
public class YoutubeVideoParser implements Parser {

    private final ChatLanguageModel chatModel;
    private final KnowledgeService knowledgeService;
    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String YT_API_URL =
            "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=%s&key=%s";
    private static final String TIMEDTEXT_URL =
            "https://www.youtube.com/api/timedtext?v=%s&lang=en&fmt=vtt";

    @Autowired
    public YoutubeVideoParser(ChatLanguageModel chatModel,
                               KnowledgeService knowledgeService,
                               @Value("${google.api.key}") String apiKey) {
        this.chatModel = chatModel;
        this.knowledgeService = knowledgeService;
        this.apiKey = apiKey;
    }

    @Override
    public String getName() {
        return YOUTUBE_PARSER_NAME;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object run(Map<String, Object> input) {
        String url = (String) input.get("content");

        if (url == null || url.isBlank()) {
            return Map.of("error", "No YouTube URL provided.");
        }

        String videoId = extractVideoId(url);
        if (videoId == null) {
            return Map.of("error", "Could not extract video ID from URL: " + url);
        }

        log.info("[YoutubeVideoParser] Processing video ID: {}", videoId);

        try {
            // ── Step 1: Title & Description via YouTube Data API v3 ──────────────
            String apiUrl = String.format(YT_API_URL, videoId, apiKey);
            Map<String, Object> ytResponse = restTemplate.getForObject(apiUrl, Map.class);

            if (ytResponse == null) {
                return Map.of("error", "YouTube Data API returned null for video: " + videoId);
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) ytResponse.get("items");
            if (items == null || items.isEmpty()) {
                return Map.of("error", "Video not found or is private: " + videoId);
            }

            Map<String, Object> snippet    = (Map<String, Object>) items.get(0).get("snippet");
            String videoTitle              = (String) snippet.getOrDefault("title", "Unknown Title");
            String description             = (String) snippet.getOrDefault("description", "");
            String channelTitle            = (String) snippet.getOrDefault("channelTitle", "");

            log.info("[YoutubeVideoParser] Title='{}', Channel='{}'", videoTitle, channelTitle);

            // ── Step 2: Auto-generated subtitles (best-effort) ───────────────────
            String subtitles = fetchSubtitles(videoId);

            // ── Step 3: Combine & truncate for LLM ───────────────────────────────
            StringBuilder content = new StringBuilder();
            content.append("Video Title: ").append(videoTitle).append("\n");
            content.append("Channel: ").append(channelTitle).append("\n\n");
            if (!description.isBlank()) {
                content.append("Description:\n").append(description).append("\n");
            }
            if (subtitles != null && !subtitles.isBlank()) {
                log.info("[YoutubeVideoParser] Subtitles fetched successfully, including in prompt.");
                content.append("\nTranscript/Subtitles:\n").append(subtitles);
            } else {
                log.info("[YoutubeVideoParser] No subtitles available, using title + description only.");
            }

            String truncated = TextTruncator.truncate(content.toString());
            String prompt    = SUBTITLE_EXTRACT + truncated;

            // ── Step 4: LLM summarization ─────────────────────────────────────────
            log.info("[YoutubeVideoParser] Sending to LLM...");
            String result = chatModel.chat(prompt);
            log.info("[YoutubeVideoParser] LLM response: {}", result);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(result, new TypeReference<>() {});
            parsed.put("source", YOUTUBE_PARSER_NAME);
            parsed.put("extra", Map.of(
                    "youtube_urls", List.of(url),
                    "video_id", videoId,
                    "channel", channelTitle
            ));

            // ── Step 5: Save to Neo4j ─────────────────────────────────────────────
            knowledgeService.saveFromParsedResult(parsed, true);
            log.info("[YoutubeVideoParser] Saved to knowledge graph successfully.");
            return parsed;

        } catch (Exception e) {
            log.error("[YoutubeVideoParser] Error processing {}: {}", url, e.getMessage(), e);
            return Map.of("error", "Processing failed", "details", e.getMessage());
        }
    }

    /**
     * Extracts the YouTube video ID from common URL formats:
     *   - youtube.com/watch?v=ID
     *   - youtu.be/ID
     *   - youtube.com/shorts/ID
     *   - youtube.com/embed/ID
     */
    private String extractVideoId(String url) {
        if (url.contains("youtu.be/")) {
            return url.split("youtu.be/")[1].split("[?&]")[0].trim();
        }
        if (url.contains("v=")) {
            return url.split("v=")[1].split("[?&]")[0].trim();
        }
        if (url.contains("/shorts/")) {
            return url.split("/shorts/")[1].split("[?&]")[0].trim();
        }
        if (url.contains("/embed/")) {
            return url.split("/embed/")[1].split("[?&]")[0].trim();
        }
        return null;
    }

    /**
     * Attempts to fetch auto-generated English captions via the YouTube timedtext API.
     * This is a best-effort approach — returns null if no captions are available.
     * Strips WEBVTT header, timestamps, and HTML tags from the VTT output.
     */
    private String fetchSubtitles(String videoId) {
        try {
            String timedtextUrl = String.format(TIMEDTEXT_URL, videoId);
            String vtt = restTemplate.getForObject(timedtextUrl, String.class);
            if (vtt == null || vtt.isBlank()) return null;

            StringBuilder sb = new StringBuilder();
            for (String line : vtt.split("\n")) {
                line = line.trim();
                // Skip WEBVTT header, timestamp lines, and empty lines
                if (line.startsWith("WEBVTT") || line.isEmpty()
                        || line.matches("^\\d{2}:\\d{2}:\\d{2}\\..*")) {
                    continue;
                }
                // Strip VTT inline tags like <00:00:01.000><c>text</c>
                line = line.replaceAll("<[^>]+>", "").trim();
                if (!line.isEmpty()) {
                    sb.append(line).append(" ");
                }
            }
            String text = sb.toString().trim();
            return text.isBlank() ? null : text;
        } catch (Exception e) {
            log.warn("[YoutubeVideoParser] Could not fetch subtitles for {}: {}", videoId, e.getMessage());
            return null;
        }
    }
}
