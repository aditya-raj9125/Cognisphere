package com.cognisphere.backend.util;

public class Const {
    // ── Linking thresholds ────────────────────────────────────────────────
    // Embedding similarity floor for *candidate* retrieval (wide net — AI filters later)
    public static final double LINK_SIMILARITY_THRESHOLD = 0.58f;
    // Max candidates to pull from vector search for AI relationship evaluation
    public static final int LINK_MAX_CANDIDATES = 8;
    // Minimum AI-judged strength to actually create an edge (0.0 – 1.0)
    public static final double LINK_MIN_AI_STRENGTH = 0.3;

    // ── Query thresholds ──────────────────────────────────────────────────
    public static final double QUERY_SIMILARITY_THRESHOLD = 0.8f;
    public static final int MAX_RESULTS = 5;
    public static final int MAX_CHAR_LENGTH = 2000;
    public static final int MAX_TOKENS = 500;
    public static final int MAX_MERGE_COUNT = 4;

    public static final String URL_PARSER_NAME = "url_parser";
    public static final String TEXT_PARSER_NAME = "text_parser";
    public static final String VIDEO_PARSER_NAME = "video_parser";
    public static final String YOUTUBE_PARSER_NAME = "youtubeVideoParser";
    public static final String IMAGE_PARSER_NAME = "image_parser";
    public static final String DOCUMENT_PARSER_NAME = "document_parser";
}
