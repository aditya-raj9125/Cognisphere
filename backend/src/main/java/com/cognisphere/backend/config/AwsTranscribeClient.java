package com.cognisphere.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.transcribe.TranscribeClient;
import software.amazon.awssdk.services.transcribe.model.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * AWS Transcribe + S3 client for video processing.
 * Replaces the former Azure Video Indexer client.
 *
 * Flow:
 * 1. Upload video to S3
 * 2. Start Amazon Transcribe job
 * 3. Poll for completion
 * 4. Retrieve transcript from result
 */
@Slf4j
@Component
public class AwsTranscribeClient {

    private final S3Client s3Client;
    private final TranscribeClient transcribeClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Autowired
    public AwsTranscribeClient(S3Client s3Client, TranscribeClient transcribeClient) {
        this.s3Client = s3Client;
        this.transcribeClient = transcribeClient;
    }

    /**
     * Upload a video file to S3 and return the S3 key.
     */
    public String uploadVideo(String name, File videoFile) throws IOException {
        String s3Key = "videos/" + name + ".mp4";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build(),
                RequestBody.fromFile(videoFile)
        );
        log.info("[AwsTranscribeClient] Video uploaded to S3: s3://{}/{}", bucketName, s3Key);
        return s3Key;
    }

    /**
     * Start an Amazon Transcribe job for the given S3 key.
     * Returns the transcription job name.
     */
    public String startTranscriptionJob(String s3Key) {
        String jobName = "transcribe-" + UUID.randomUUID();
        String s3Uri = "s3://" + bucketName + "/" + s3Key;

        Media media = Media.builder()
                .mediaFileUri(s3Uri)
                .build();

        StartTranscriptionJobRequest request = StartTranscriptionJobRequest.builder()
                .transcriptionJobName(jobName)
                .media(media)
                .mediaFormat(MediaFormat.MP4)
                .languageCode(LanguageCode.EN_US)
                .build();

        transcribeClient.startTranscriptionJob(request);
        log.info("[AwsTranscribeClient] Started transcription job: {}", jobName);
        return jobName;
    }

    /**
     * Poll until the transcription job completes or fails.
     */
    public void waitForTranscription(String jobName) throws InterruptedException {
        while (true) {
            GetTranscriptionJobRequest request = GetTranscriptionJobRequest.builder()
                    .transcriptionJobName(jobName)
                    .build();

            GetTranscriptionJobResponse response = transcribeClient.getTranscriptionJob(request);
            TranscriptionJobStatus status = response.transcriptionJob().transcriptionJobStatus();

            log.info("[AwsTranscribeClient] Job {} status: {}", jobName, status);

            if (status == TranscriptionJobStatus.COMPLETED) return;
            if (status == TranscriptionJobStatus.FAILED) {
                throw new RuntimeException("Transcription job failed: " +
                        response.transcriptionJob().failureReason());
            }

            TimeUnit.SECONDS.sleep(10);
        }
    }

    /**
     * Retrieve the transcript text from a completed transcription job.
     */
    public String getTranscript(String jobName) throws IOException {
        GetTranscriptionJobRequest request = GetTranscriptionJobRequest.builder()
                .transcriptionJobName(jobName)
                .build();

        GetTranscriptionJobResponse response = transcribeClient.getTranscriptionJob(request);
        String transcriptUri = response.transcriptionJob().transcript().transcriptFileUri();

        // Download and parse the transcript JSON
        String transcriptJson = restTemplate.getForObject(transcriptUri, String.class);
        JsonNode root = objectMapper.readTree(transcriptJson);
        JsonNode results = root.get("results");
        JsonNode transcripts = results.get("transcripts");

        StringBuilder sb = new StringBuilder();
        for (JsonNode transcript : transcripts) {
            sb.append(transcript.get("transcript").asText()).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Return insights-like map from a completed transcription job,
     * compatible with the existing VideoParser processing flow.
     */
    public Map<String, Object> getInsights(String jobName) throws IOException {
        String transcript = getTranscript(jobName);
        return Map.of("transcript", transcript);
    }

    /**
     * Delete a video object from S3.
     */
    public void deleteS3Object(String s3Key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build());
        log.info("[AwsTranscribeClient] Deleted S3 object: {}", s3Key);
    }
}
