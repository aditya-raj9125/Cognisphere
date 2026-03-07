package com.cognisphere.backend.agent.parsers;

import com.cognisphere.backend.service.KnowledgeService;
import com.cognisphere.backend.util.TextTruncator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

import static com.cognisphere.backend.util.Const.DOCUMENT_PARSER_NAME;
import static com.cognisphere.backend.util.Prompts.TEXT_EXTRACT;

/**
 * Handles document uploads: PDF, DOCX, PPTX, XLSX, TXT, CSV, etc.
 * Uses Apache Tika for content extraction — no AWS service needed.
 * Extracted text is then processed by the LLM exactly like TextParser.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParser implements Parser {

    private final ChatLanguageModel chatModel;
    private final KnowledgeService knowledgeService;

    @Override
    public String getName() {
        return DOCUMENT_PARSER_NAME;
    }

    @Override
    public Object run(Map<String, Object> input) {
        MultipartFile file = (MultipartFile) input.get("file");

        if (file == null || file.isEmpty()) {
            return Map.of("error", "No document file provided.");
        }

        log.info("[DocumentParser] Extracting text from '{}' ({})", file.getOriginalFilename(), file.getContentType());

        String extractedText;
        try (InputStream is = file.getInputStream()) {
            // -1 = no character limit on handler buffer
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(is, handler, metadata);
            extractedText = handler.toString();
        } catch (Exception e) {
            log.error("[DocumentParser] Tika extraction failed: {}", e.getMessage(), e);
            return Map.of("error", "Failed to extract text from document: " + e.getMessage());
        }

        if (extractedText == null || extractedText.isBlank()) {
            log.warn("[DocumentParser] No text extracted from '{}'", file.getOriginalFilename());
            return Map.of("error", "No readable text found in this document.");
        }

        log.info("[DocumentParser] Extracted {} characters from '{}'", extractedText.length(), file.getOriginalFilename());

        String truncated = TextTruncator.truncate(extractedText);
        String prompt = TEXT_EXTRACT + truncated;

        try {
            String result = chatModel.chat(prompt);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(result, new TypeReference<>() {});
            parsed.put("source", DOCUMENT_PARSER_NAME);

            // Store original filename as extra metadata
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
            parsed.put("extra", Map.of("filename", originalName, "content_type", file.getContentType() != null ? file.getContentType() : "unknown"));

            log.info("[DocumentParser] Parsed result: {}", parsed);
            knowledgeService.saveFromParsedResult(parsed, true);
            return parsed;
        } catch (Exception e) {
            log.error("[DocumentParser] LLM processing failed: {}", e.getMessage(), e);
            return Map.of("error", "LLM processing failed: " + e.getMessage());
        }
    }
}
