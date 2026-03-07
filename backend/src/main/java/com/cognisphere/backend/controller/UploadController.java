package com.cognisphere.backend.controller;

import com.cognisphere.backend.agent.ParserManager;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.cognisphere.backend.util.Const.*;

/**
 * Controller for handling file uploads and text input.
 * It processes the input and returns the result from the appropriate parser.
 */
@RestController
@RequestMapping("/api/agent")
public class UploadController {
    @Resource
    private ParserManager parserManager;

    @PostMapping("/upload")
    public ResponseEntity<?> handleUpload(
            @RequestPart(value = "text", required = false) String text,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws IOException {
        Map<String, Object> inputMap = new HashMap<>();
        String inputContent = null;
        String parserName = null;

        // MIME types that should be treated as documents (Tika will extract text)
        Set<String> documentMimeTypes = Set.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain",
                "text/csv",
                "application/rtf",
                "text/rtf",
                "application/vnd.oasis.opendocument.text",
                "application/vnd.oasis.opendocument.presentation",
                "application/vnd.oasis.opendocument.spreadsheet"
        );

        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null) {
                return ResponseEntity.badRequest().body("Unsupported file type.");
            }
            if (contentType.startsWith("image/")) {
                inputMap.put("file", file);
                parserName = IMAGE_PARSER_NAME;
            } else if (contentType.startsWith("video/") || contentType.startsWith("audio/")) {
                inputMap.put("file", file);
                parserName = VIDEO_PARSER_NAME;
            } else if (documentMimeTypes.contains(contentType)) {
                inputMap.put("file", file);
                parserName = DOCUMENT_PARSER_NAME;
            } else {
                // try Tika as a fallback for any other file type
                inputMap.put("file", file);
                parserName = DOCUMENT_PARSER_NAME;
            }
        } else if (text != null && !text.isEmpty()) {
            if (text.startsWith("http://") || text.startsWith("https://")) {
                if (text.contains("youtube.com")) {
                    parserName = YOUTUBE_PARSER_NAME;
                    inputContent = text;
                } else {
                    parserName = URL_PARSER_NAME;
                    inputContent = text;
                }
            } else {
                parserName = TEXT_PARSER_NAME;
                inputContent = text;
            }
        } else {
            return ResponseEntity.badRequest().body("No valid input provided.");
        }


        inputMap.put("content", inputContent);

        Object result = parserManager.call(parserName, inputMap);
        return ResponseEntity.ok(result);
    }
}
