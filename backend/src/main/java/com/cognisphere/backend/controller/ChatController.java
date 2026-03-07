package com.cognisphere.backend.controller;

import com.cognisphere.backend.agent.AgentProvider.AssistantAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final int MAX_MESSAGE_LENGTH = 5000;
    private static final int MAX_SESSION_ID_LENGTH = 128;
    private static final java.util.regex.Pattern UUID_PATTERN =
            java.util.regex.Pattern.compile("^[a-fA-F0-9\\-]{1,64}$");

    private final AssistantAgent assistantAgent;

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        String uuid = request.get("uuid");
        String sessionId = request.get("sessionId");
        String knowledgeOnly = request.get("knowledgeOnly");

        if (userMessage == null || userMessage.trim().isBlank()) {
            return Map.of("error", "Message is required");
        }
        if (userMessage.length() > MAX_MESSAGE_LENGTH) {
            return Map.of("error", "Message exceeds maximum length");
        }

        if (sessionId == null || sessionId.isBlank()) {
            return Map.of("error", "Session ID is required");
        }
        if (sessionId.length() > MAX_SESSION_ID_LENGTH) {
            return Map.of("error", "Invalid session ID");
        }

        if (uuid != null && !uuid.isBlank() && !UUID_PATTERN.matcher(uuid).matches()) {
            return Map.of("error", "Invalid UUID format");
        }

        // Build contextual message with mode instructions
        StringBuilder ctx = new StringBuilder();

        if ("true".equalsIgnoreCase(knowledgeOnly)) {
            ctx.append("[KNOWLEDGE_ONLY_MODE] ");
            ctx.append("Use searchKnowledgeGraph to find relevant nodes. Answer the question directly using only what's in the knowledge graph. If nothing is found, say 'I don't have information about that in your knowledge graph.' Keep response to 2-3 paragraphs max. No tables, no suggestions, no offers. ");
        } else {
            ctx.append("[ALL_SOURCES_MODE] ");
            ctx.append("Answer like ChatGPT: direct, concise, 2-3 paragraphs. Use your knowledge and web search if needed. After answering, silently save the insight with createNodeFromChatTool. No tables, no action menus, no suggestions. ");
        }

        if (uuid != null && !uuid.isBlank()) {
            ctx.append("[User is viewing node: ").append(uuid).append("] ");
        }

        ctx.append(userMessage);

        String response = assistantAgent.chat(sessionId, ctx.toString());
        return Map.of("response", response);
    }
}
