package com.cognisphere.backend.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class NodeMapper {
    private final Neo4jClient neo4jClient;

    @Autowired
    public NodeMapper(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @PostConstruct
    public void verifyNeo4jConnection() {
        neo4jClient.query("RETURN 1").run();
        log.info("Connected to Neo4j");
    }
    public void createRelationship(String fromId, String toId, double score) {
        createRelationship(fromId, toId, score, null);
    }

    /**
     * Create or update a :SIMILAR relationship with an optional label.
     * Canonical UUID ordering prevents duplicate edges for the same pair.
     */
    public void createRelationship(String fromId, String toId, double score, String label) {
        if (fromId.compareTo(toId) > 0) {
            String temp = fromId;
            fromId = toId;
            toId = temp;
        }

        String cypher;
        Map<String, Object> params;

        if (label != null && !label.isBlank()) {
            cypher = """
                MATCH (a {uuid: $fromId}), (b {uuid: $toId})
                MERGE (a)-[r:SIMILAR]->(b)
                SET r.score = $score, r.label = $label
                """;
            params = Map.of("fromId", fromId, "toId", toId, "score", score, "label", label);
        } else {
            cypher = """
                MATCH (a {uuid: $fromId}), (b {uuid: $toId})
                MERGE (a)-[r:SIMILAR]->(b)
                ON CREATE SET r.score = $score
                """;
            params = Map.of("fromId", fromId, "toId", toId, "score", score);
        }

        neo4jClient.query(cypher).bindAll(params).run();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getNodeByUuid(String uuid) {
         ObjectMapper mapper = new ObjectMapper();
        return (Map<String, Object>) neo4jClient.query("""
                        MATCH (n {uuid: $uuid})
                        RETURN n.title AS title, n.text AS text, n.createdAt AS createdAt,
                               n.source AS source, coalesce(n.extra, '{}') AS extra
                    """)
                .bind(uuid).to("uuid")
                .fetchAs(Map.class)
                .mappedBy((typeSystem, record) -> {
                    String extraStr = record.get("extra").isNull() ? "{}" : record.get("extra").asString();
                    Map<String, Object> extraMap;
                    try {
                        extraMap = mapper.readValue(extraStr, Map.class);
                    } catch (Exception e) {
                        throw new RuntimeException("[getNodeByUuid] Failed to parse extra JSON", e);
                    }

                    return Map.of(
                            "title", record.get("title").asString(),
                            "text", record.get("text").asString(),
                            "createdAt", record.get("createdAt").asString(),
                            "source", record.get("source").asString(),
                            "extra", extraMap,
                            "uuid", uuid
                    );
                })
                .one()
                .orElseThrow(() -> new RuntimeException("Node not found for uuid: " + uuid));
    }



    @SuppressWarnings("unchecked")
    public Map<String, Object> getNodeByTitle(String title) {
        return (Map<String, Object>) neo4jClient.query("""
                            MATCH (n {title: $title})
                            RETURN n.title AS title, n.text AS text, n.createdAt AS createdAt, n.type AS type
                            LIMIT 1
                        """)
                .bind(title).to("title")
                .fetchAs(Map.class)
                .mappedBy((typeSystem, record) -> Map.of(
                        "title", record.get("title").asString(),
                        "text", record.get("text").asString(),
                        "createdAt", record.get("createdAt").asString(),
                        "type", record.get("type").asString()
                ))
                .one()
                .orElseThrow(() -> new RuntimeException("Node not found for title: " + title));
    }

    /**
     * Delete nodes and their relationships by a list of UUIDs.
     *
     * @param uuids List of UUIDs to be deleted.
     */
    public void deleteNodesByUuids(List<String> uuids) {
        neo4jClient.query("""
            MATCH (n)
            WHERE n.uuid IN $uuids
            DETACH DELETE n
            """)
                .bind(uuids).to("uuids")
                .run();
    }

    /**
     * Reconnects the neighbors of old nodes to the new merged node.
     *
     * @param newUuid The UUID of the newly created merged node.
     * @param uuids   List of UUIDs of the original nodes to be merged and deleted.
     */
    public void reconnectNeighbors(String newUuid, List<String> uuids) {
        // 1. Find all neighbors connected to any of the original nodes
        Collection<Map> neighbors = neo4jClient.query("""
            MATCH (n)-[r]->(m)
            WHERE n.uuid IN $uuids
            RETURN m.uuid AS neighborUuid
            UNION
            MATCH (n)-[r]->(m)
            WHERE m.uuid IN $uuids
            RETURN n.uuid AS neighborUuid
            """)
                .bind(uuids).to("uuids")
                .fetchAs(Map.class)
                .mappedBy((typeSystem, record) -> Map.of(
                        "neighborUuid", record.get("neighborUuid").asString()
                ))
                .all();

        // 2. Deduplicate neighbor UUIDs
        Set<String> neighborUuids = neighbors.stream()
                .map(neighbor -> (String) neighbor.get("neighborUuid"))
                .filter(uuid -> uuid != null && !uuids.contains(uuid) && !uuid.equals(newUuid))
                .collect(Collectors.toSet());

        // 3. Create new relationships from the new node to each neighbor
        for (String neighborUuid : neighborUuids) {
            createRelationship(newUuid, neighborUuid, 0.0);
        }
    }

    public void addYoutubeUrlToNode(String uuid, String youtubeLink) {
        neo4jClient.query("""
        MATCH (n {uuid: $uuid})
        WITH n, 
             CASE WHEN n.extra IS NULL THEN { youtube_urls: [] } ELSE apoc.convert.fromJsonMap(n.extra) END AS extraMap
        WITH n, apoc.map.setKey(extraMap, 'youtube_urls', coalesce(extraMap.youtube_urls, []) + $youtubeLink) AS updatedExtra
        SET n.extra = apoc.convert.toJson(updatedExtra)
    """)
                .bind(uuid).to("uuid")
                .bind(youtubeLink).to("youtubeLink")
                .run();
    }

    /**
     * Fetch lightweight summaries of all nodes — used for batch relink operations.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getAllNodeSummaries() {
        return (List<Map<String, String>>) (List<?>) neo4jClient.query("""
                MATCH (n)
                RETURN n.uuid AS uuid, n.title AS title,
                       left(n.text, 300) AS snippet
                ORDER BY n.createdAt DESC
                """)
                .fetchAs(Map.class)
                .mappedBy((typeSystem, record) -> Map.of(
                        "uuid", record.get("uuid").asString(),
                        "title", record.get("title").asString(),
                        "snippet", record.get("snippet").asString()
                ))
                .all()
                .stream().toList();
    }

    /**
     * Get the set of existing edge pairs so we don't re-evaluate them.
     */
    public Set<String> getExistingEdgePairs() {
        Collection<Map> pairs = neo4jClient.query("""
                MATCH (a)-[r:SIMILAR]->(b)
                RETURN a.uuid AS source, b.uuid AS target
                """)
                .fetchAs(Map.class)
                .mappedBy((typeSystem, record) -> Map.of(
                        "source", record.get("source").asString(),
                        "target", record.get("target").asString()
                ))
                .all();

        Set<String> result = new HashSet<>();
        for (Map pair : pairs) {
            String s = (String) pair.get("source");
            String t = (String) pair.get("target");
            // canonical order
            if (s.compareTo(t) > 0) { String tmp = s; s = t; t = tmp; }
            result.add(s + "|" + t);
        }
        return result;
    }
}
