package com.cognisphere.backend.config;

import dev.langchain4j.community.store.embedding.neo4j.Neo4jEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.neo4j.authentication.username}")
    private String username;

    @Value("${spring.neo4j.authentication.password}")
    private String password;

    @Value("${spring.neo4j.uri}")
    private String uri;

    @Value("${spring.neo4j.database:neo4j}")
    private String database;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel, Driver neo4jDriver) {
        return Neo4jEmbeddingStore.builder()
                .driver(neo4jDriver)
                .databaseName(database)
                .dimension(1024)
                .build();
    }
}
