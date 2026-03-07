package com.cognisphere.backend.util;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

public class FileUtils {
    private static final String HASH_STORAGE_FILE = "backend/data/uploaded_videos_hash.txt";

    public static String calculateHash(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static boolean isDuplicate(String hash) {
        try {
            File file = new File(HASH_STORAGE_FILE);
            if (!file.exists()) {
                return false;
            }
            List<String> lines = Files.readAllLines(file.toPath());
            return lines.contains(hash);
        } catch (IOException e) {
            return false;
        }
    }

    public static void saveMD5(String hash) {
        try (FileWriter fw = new FileWriter(HASH_STORAGE_FILE, true)) {
            fw.write(hash + "\n");
        } catch (IOException e) {
            // Ignore
        }
    }
}
