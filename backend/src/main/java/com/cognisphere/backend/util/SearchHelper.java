package com.cognisphere.backend.util;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SearchHelper {

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.customsearch.cx}")
    private String cx;

    @Value("${google.customsearch.search-url:https://www.googleapis.com/customsearch/v1}")
    private String searchUrl;

    /**
     * Search YouTube for a given query. Returns the first result URL.
     */
    public String searchYouTube(String query) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(searchUrl)
                    .queryParam("key", apiKey)
                    .queryParam("cx", cx)
                    .queryParam("q", query)
                    .queryParam("siteSearch", "youtube.com")
                    .queryParam("num", 1)
                    .build().toUri();

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items != null && !items.isEmpty()) {
                Map<String, Object> firstItem = items.get(0);
                return (String) firstItem.get("link");
            }
        } catch (Exception e) {
            System.err.println("[VideoSearchHelper] Search YouTube failed: " + e.getMessage());
        }
        return "";
    }

    /**
     * General web search using Google Custom Search API.
     * Returns a list of {title, link, snippet} maps for the top results.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> searchWeb(String query, int maxResults) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(searchUrl)
                    .queryParam("key", apiKey)
                    .queryParam("cx", cx)
                    .queryParam("q", query)
                    .queryParam("num", Math.min(maxResults, 5))
                    .build().toUri();

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null || items.isEmpty()) return List.of();

            return items.stream()
                    .map(item -> Map.of(
                            "title", String.valueOf(item.getOrDefault("title", "")),
                            "link", String.valueOf(item.getOrDefault("link", "")),
                            "snippet", String.valueOf(item.getOrDefault("snippet", ""))
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[SearchHelper] webSearch failed: " + e.getMessage());
            return List.of();
        }
    }
}

