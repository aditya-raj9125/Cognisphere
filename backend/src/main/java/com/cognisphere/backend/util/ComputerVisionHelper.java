package com.cognisphere.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextResponse;
import software.amazon.awssdk.services.textract.model.Document;

import java.util.List;

/**
 * OCR helper using Amazon Textract.
 * Replaces the former Azure Computer Vision OCR integration.
 * Uses the synchronous DetectDocumentText API (supports up to 5MB inline).
 */
@Slf4j
@Component
public class ComputerVisionHelper {

    private final TextractClient textractClient;

    @Autowired
    public ComputerVisionHelper(TextractClient textractClient) {
        this.textractClient = textractClient;
    }

    /**
     * Extract text from an image file using Amazon Textract.
     *
     * @param file the uploaded image file
     * @return extracted text content
     */
    public String readImageFile(MultipartFile file) {
        try {
            log.info("[ComputerVisionHelper] Sending file to Amazon Textract for OCR");

            Document document = Document.builder()
                    .bytes(SdkBytes.fromByteArray(file.getBytes()))
                    .build();

            DetectDocumentTextRequest request = DetectDocumentTextRequest.builder()
                    .document(document)
                    .build();

            DetectDocumentTextResponse response = textractClient.detectDocumentText(request);

            List<Block> blocks = response.blocks();
            StringBuilder sb = new StringBuilder();
            for (Block block : blocks) {
                if (block.blockType() == BlockType.LINE) {
                    sb.append(block.text()).append("\n");
                }
            }

            String extractedText = sb.toString();
            log.info("[ComputerVisionHelper] Textract OCR completed, extracted {} characters", extractedText.length());
            return extractedText;

        } catch (Exception e) {
            log.error("[ComputerVisionHelper] Textract OCR failed: {}", e.getMessage(), e);
            return "";
        }
    }
}
