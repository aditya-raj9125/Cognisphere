package com.cognisphere.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.transcribe.TranscribeClient;

/**
 * Centralized AWS client configuration.
 * Provides S3Client, TextractClient, and TranscribeClient beans.
 * Uses DefaultCredentialsProvider which supports:
 *   - Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 *   - AWS credentials file (~/.aws/credentials)
 *   - IAM roles (EC2/ECS task roles)
 */
@Configuration
public class AwsClientConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public TextractClient textractClient() {
        return TextractClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public TranscribeClient transcribeClient() {
        return TranscribeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
