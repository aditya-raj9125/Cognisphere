package com.cognisphere.backend.agent.models;

import dev.langchain4j.model.bedrock.BedrockTitanEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;

/**
 * Provides Amazon Titan Embedding Model via Bedrock.
 * Replaces the former Azure OpenAI embedding configuration.
 */
@Slf4j
@Component
public class BedrockEmbeddingModelConfig {

    @Value("${aws.bedrock.embeddingModelId}")
    private String embeddingModelId;

    @Value("${aws.region}")
    private String region;

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Initializing Amazon Bedrock Titan Embedding Model: {}", embeddingModelId);
        return BedrockTitanEmbeddingModel.builder()
                .model(embeddingModelId)
                .region(Region.of(region))
                .build();
    }
}
