package com.cognisphere.backend.controller;

import com.cognisphere.backend.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class GraphController {

    private final Neo4jClient neo4jClient;
    private final KnowledgeService knowledgeService;

    @Autowired
    public GraphController(Neo4jClient neo4jClient, KnowledgeService knowledgeService) {
        this.neo4jClient = neo4jClient;
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/api/graph")
    public Map<String, Object> getGraph() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        // Query all nodes
        neo4jClient.query("""
                MATCH (n)
                RETURN n.uuid AS uuid, n.title AS title,
                       coalesce(n.category, 'general') AS category,
                       coalesce(n.subcategory, 'knowledge') AS subcategory
                """).fetch().all().forEach(row -> {
            nodes.add(Map.of(
                    "uuid", row.get("uuid"),
                    "title", row.get("title"),
                    "category", row.getOrDefault("category", "general"),
                    "subcategory", row.getOrDefault("subcategory", "knowledge")
            ));
        });

        // Query all edges with labels
        neo4jClient.query("""
                OPTIONAL MATCH (a)-[r:SIMILAR]->(b)
                WHERE a.uuid <> b.uuid
                RETURN a.uuid AS source, b.uuid AS target,
                       coalesce(r.score, 0.0) AS score,
                       coalesce(r.label, '') AS label
                """).fetch().all().forEach(row -> {
            Object src = row.get("source");
            if (src == null) return;

            Map<String, Object> edge = new HashMap<>();
            edge.put("source", src);
            edge.put("target", row.get("target"));
            edge.put("score", row.get("score"));
            edge.put("label", row.getOrDefault("label", ""));
            edges.add(edge);
        });

        return Map.of("nodes", nodes, "edges", edges);
    }

    /**
     * Trigger a full graph relink — scans all nodes, uses AI to discover missing connections.
     * POST /api/graph/relink
     */
    @PostMapping("/api/graph/relink")
    public Map<String, Object> relinkGraph() {
        int newEdges = knowledgeService.relinkAllNodes();
        return Map.of("status", "ok", "newEdgesCreated", newEdges);
    }
}
