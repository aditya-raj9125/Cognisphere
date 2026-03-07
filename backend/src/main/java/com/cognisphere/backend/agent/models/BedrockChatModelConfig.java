package com.cognisphere.backend.agent.models;

import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

/**
 * Provides access to Amazon Bedrock-hosted ChatLanguageModel via Converse API.
 * Supports Amazon Nova, Meta Llama, Cohere and other Bedrock models.
 */
@Configuration
public class BedrockChatModelConfig {

    private static final Logger logger = LoggerFactory.getLogger(BedrockChatModelConfig.class);

    @Value("${aws.bedrock.modelId}")
    private String modelId;

    @Value("${aws.region}")
    private String region;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        try {
            logger.info("Initializing Amazon Bedrock Chat Model with model: {}", modelId);

            BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                    .region(Region.of(region))
                    .build();

            ChatLanguageModel chatModel = BedrockChatModel.builder()
                    .client(client)
                    .modelId(modelId)
                    .build();

            logger.info("Amazon Bedrock Chat Model initialized successfully");
            return chatModel;
        } catch (Exception e) {
            logger.error("Failed to initialize Amazon Bedrock Chat Model: {}", e.getMessage(), e);
            throw new RuntimeException("Bedrock Chat Model initialization failed", e);
        }
    }
}
