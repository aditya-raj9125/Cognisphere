package com.cognisphere.backend.agent.parsers;

import com.cognisphere.backend.config.AwsTranscribeClient;
import com.cognisphere.backend.service.KnowledgeService;
import com.cognisphere.backend.util.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.cognisphere.backend.util.Const.VIDEO_PARSER_NAME;
import static com.cognisphere.backend.util.Prompts.SUBTITLE_EXTRACT;

/**
 * Video parser using Amazon Transcribe + S3 for speech-to-text.
 * Replaces the former Azure Video Indexer integration.
 *
 * Flow:
 * 1. Upload video to S3
 * 2. Start Amazon Transcribe job
 * 3. Poll for completion
 * 4. Extract transcript
 * 5. Process with LLM
 */
@Slf4j
@Component
public class VideoParser implements Parser {

    private final ChatLanguageModel chatModel;
    private final KnowledgeService knowledgeService;
    private final AwsTranscribeClient awsTranscribeClient;

    @Autowired
    public VideoParser(ChatLanguageModel chatModel, KnowledgeService knowledgeService, AwsTranscribeClient awsTranscribeClient) {
        this.chatModel = chatModel;
        this.knowledgeService = knowledgeService;
        this.awsTranscribeClient = awsTranscribeClient;
    }

    @Override
    public String getName() {
        return VIDEO_PARSER_NAME;
    }

    @Override
    public Object run(Map<String, Object> input) {
        MultipartFile file = (MultipartFile) input.get("file");
        if (file == null || file.isEmpty()) {
            return Map.of("error", "No video file provided.");
        }

        try {
            String name = "upload_" + UUID.randomUUID();
            File localFile = File.createTempFile(name, ".mp4");
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                fos.write(file.getBytes());
            }

            // 1. Calculate hash to deduplicate
            String hash = FileUtils.calculateHash(localFile);
            if (FileUtils.isDuplicate(hash)) {
                log.warn("[VideoParser] Duplicate video detected, skipping processing.");
                return Map.of("message", "Duplicate video, skipping processing.");
            }
            FileUtils.saveMD5(hash);

            log.info("[VideoParser] Uploading video to S3: {}", name);
            String s3Key = awsTranscribeClient.uploadVideo(name, localFile);

            // 2. Start transcription job
            String jobName = awsTranscribeClient.startTranscriptionJob(s3Key);

            // 3. Return immediately, and asynchronously process the rest
            CompletableFuture.runAsync(() -> {
                try {
                    Instant t1 = Instant.now();

                    // Wait for Amazon Transcribe to complete
                    awsTranscribeClient.waitForTranscription(jobName);

                    // Get transcript text
                    String transcript = awsTranscribeClient.getTranscript(jobName);
                    if (transcript == null || transcript.isBlank()) {
                        log.error("[VideoParser] No transcript returned from Transcribe");
                        return;
                    }

                    log.info("[VideoParser] Extracted transcript: {}", transcript);

                    String prompt = SUBTITLE_EXTRACT + transcript;
                    String result = chatModel.chat(prompt);

                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> parsed = mapper.readValue(result, Map.class);
                    parsed.put("source", VIDEO_PARSER_NAME);
                    parsed.put("extra", Map.of("s3_keys", List.of(s3Key)));

                    log.info("[VideoParser] Processing time: {} ms", Duration.between(t1, Instant.now()).toMillis());

                    knowledgeService.saveFromParsedResult(parsed, true);
                    log.info("[VideoParser] Parsed result saved successfully");

                } catch (Exception e) {
                    log.error("[VideoParser] Async processing error: {}", e.getMessage(), e);
                }
            });

            return Map.of("jobName", jobName, "s3Key", s3Key, "message", "Video uploaded. Processing asynchronously.");

        } catch (Exception e) {
            log.error("[VideoParser] Error: {}", e.getMessage(), e);
            return Map.of("error", "Processing failed", "details", e.getMessage());
        }
    }
}