package com.cognisphere.backend.util;

public class Prompts {
    public static final String TEXT_EXTRACT = """
            You are a knowledge extraction assistant.
            
            Given the content below, extract the following structured information:
            
            1. "title" — A concise, **specific** title (within 5 words) that clearly identifies the **main topic or entity**, suitable as a knowledge graph node. Avoid generic words like "Overview", "Summary", "Understanding", "Introduction", etc.
            2. "summary" — A **structured summary using Markdown**, including headings and bullet points where appropriate. About 3–5 paragraphs.
            
            Return ONLY a valid raw **JSON** object. DO NOT include any code block markers (like ```json) or additional explanations.
            
            Format:
            {
              "title": "Concise and meaningful title",
              "summary": "## Key Concepts\\n\\n- ...\\n- ...\\n\\n### Details\\n\\n- ..."
            }
            
            Content:
            
            """;

    public static final String SUBTITLE_EXTRACT = """
            You are a knowledge extraction assistant.
            
            Given the transcript text from a spoken video (e.g., a lecture, podcast, or talk), extract the following structured information:
            
            1. "title" — A concise and specific title (maximum 5 words) that captures the **core subject or entity** discussed in the video. Avoid vague terms like "Overview", "Summary", "Understanding" or "Discussion".
            
            2. "summary" — A well-organized **Markdown summary** of the key points discussed in the video. Use appropriate section headings and bullet points. Try to convert informal spoken language into **clear, structured insights**. The summary should be 3–5 paragraphs long.
            
            Assume the transcript may contain some repetition or disfluency. Focus on **meaningful content**, not filler words.
            
            Return ONLY a valid raw **JSON** object. DO NOT include any code block markers (like ```json) or any explanations.
            
            Format:
            {
              "title": "Concise and meaningful title",
              "summary": "## Key Concepts\\n\\n- ...\\n- ...\\n\\n### Details\\n\\n- ..."
            }
            
            Transcript:
            
            """;

    public static final String RECOMMEND_CONFIRM = """
            You are a knowledge assistant.
            
            Given the following knowledge title and its short summary, write A **structured explanation using Markdown**, including headings and bullet points where appropriate. About 3–5 paragraphs.
            
            Title: %s
            Summary: %s
            """;

    public static final String MERGE_KNOWLEDGE = """
            You are a knowledge consolidation assistant.
            
            You will receive several **knowledge points**. Your task is to **synthesize all of them** into a **single structured representation** that fully **integrates key ideas from each topic**. But keeps the overall length similar to a single knowledge point (about the same length as previous topics).
            
            Your output must:
            
            1. Create a "title" — A concise, **specific** title (within 5 words) that clearly captures the **combined essence** of all topics. Avoid generic words like "Overview", "Summary", "Understanding", "Introduction", etc.
            2. Write a "summary" — A **structured summary using Markdown**, organized into headings and bullet points. The summary must **explicitly cover major points from every input topic**. Write about 3–5 paragraphs.
            
            Return ONLY a valid raw **JSON** object. DO NOT include any code block markers (like ```json) or any extra explanations.
            
            Format:
            {
              "title": "Concise and meaningful title",
              "summary": "## Key Concepts\\n\\n- ...\\n- ...\\n\\n### Details\\n\\n- ..."
            }
            
            Content:
            
            """;

    /**
     * Extract a knowledge node title + summary from a chat conversation snippet.
     * Usage: String.format(CHAT_NODE_CREATE, userQuestion, aiAnswer)
     */
    public static final String CHAT_NODE_CREATE = """
            You are a knowledge extraction assistant.
            
            A user asked the following question:
            "%s"
            
            The AI responded:
            "%s"
            
            Your task: extract the **core knowledge concept** from this exchange and produce a knowledge node.
            
            Return ONLY a valid raw JSON object (no code block markers). Format:
            {
              "title": "Concise topic title (max 5 words)",
              "summary": "## Summary\\n\\nMarkdown-formatted explanation of the key concept discussed."
            }
            """;

    /**
     * Classify a knowledge node into a high-level category and subcategory.
     * Usage: String.format(CATEGORY_CLASSIFY, title, summarySnippet)
     */
    public static final String CATEGORY_CLASSIFY = """
            You are a knowledge classification assistant.
            
            Given this knowledge node:
            Title: "%s"
            Summary snippet: "%s"
            
            Classify it into ONE of these high-level categories:
            - science
            - technology
            - news
            - academics
            - arts
            - health
            - business
            - history
            - general
            
            Also choose a specific subcategory (1-3 words, lowercase, e.g. "machine learning", "finance", "biology").
            
            Return ONLY a valid raw JSON object (no code block markers). Format:
            {
              "category": "technology",
              "subcategory": "machine learning"
            }
            """;

    /**
     * AI Relationship Judge — evaluate candidate connections in ONE batched call.
     * Usage: String.format(RELATIONSHIP_JUDGE, newNodeTitle, newNodeSnippet, candidateListBlock)
     *
     * candidateListBlock is built dynamically, e.g.:
     *   1. "Atom" — Atom is the basic unit of matter...
     *   2. "Electron: Definition" — An electron is a subatomic particle...
     */
    public static final String RELATIONSHIP_JUDGE = """
            You are a knowledge-graph relationship expert.
            
            A NEW node has just been added to a knowledge graph:
            Title: "%s"
            Summary: "%s"
            
            Below are CANDIDATE nodes that might be related. For EACH candidate, decide:
            1. Should it be connected to the new node? (true/false)
            2. If yes, provide a short relationship label (2-5 words, e.g. "subtopic of", "historical context for", "related concept", "application of", "part of", "contrasts with").
            3. If yes, rate the connection strength from 0.0 (weak) to 1.0 (strong).
            
            CANDIDATES:
            %s
            
            Think carefully about domain relationships:
            - Hierarchical (parent-child, part-whole)
            - Associative (same field, shared concepts)
            - Causal (causes, effects, prerequisites)
            - Temporal (historical context, evolution of ideas)
            
            Return ONLY a valid raw JSON array (no code block markers). One object per candidate, in order:
            [
              {"index": 1, "connected": true, "label": "subtopic of", "strength": 0.85},
              {"index": 2, "connected": false},
              ...
            ]
            """;
}
