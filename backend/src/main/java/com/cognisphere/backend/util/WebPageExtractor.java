package com.cognisphere.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Robust web page text extractor using Jsoup.
 *
 * Strategy:
 *  1. Fetch with realistic headers + timeout + redirect follow
 *  2. Strip nav/footer/ads/scripts/styles
 *  3. Prefer <article> or <main> for main content
 *  4. Fallback: longest contiguous paragraph block
 *  5. Special-case GitHub blob URLs -> fetch raw content directly
 *  6. Final fallback: full body text
 */
@Slf4j
public class WebPageExtractor {

    // Blocked hostnames and IP prefixes to prevent SSRF
    private static final String[] BLOCKED_HOSTS = {
            "localhost", "127.0.0.1", "0.0.0.0", "[::1]",
            "169.254.", "10.", "192.168.", "172.16.", "172.17.",
            "172.18.", "172.19.", "172.20.", "172.21.", "172.22.",
            "172.23.", "172.24.", "172.25.", "172.26.", "172.27.",
            "172.28.", "172.29.", "172.30.", "172.31.",
            "metadata.google.internal"
    };

    /**
     * Validates that a URL does not point to internal/private resources (SSRF protection).
     */
    private static void validateUrlSafety(String url) throws SecurityException {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new SecurityException("Only http/https URLs are allowed");
            }
            String host = uri.getHost();
            if (host == null) {
                throw new SecurityException("Invalid URL: no host");
            }
            // Check hostname against blocked list
            String hostLower = host.toLowerCase();
            for (String blocked : BLOCKED_HOSTS) {
                if (hostLower.equals(blocked) || hostLower.startsWith(blocked)) {
                    throw new SecurityException("Access to internal/private addresses is not allowed");
                }
            }
            // Resolve DNS and check if IP is private
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                throw new SecurityException("Access to internal/private addresses is not allowed");
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("URL validation failed: " + e.getMessage());
        }
    }

    // Selectors to remove before extracting text (boilerplate)
    private static final String[] NOISE_SELECTORS = {
            "nav", "header", "footer", "aside", "script", "style",
            ".nav", ".navbar", ".header", ".footer", ".sidebar",
            ".menu", ".cookie", ".banner", ".ad", ".ads", ".advertisement",
            "#nav", "#header", "#footer", "#sidebar", "#menu",
            "[role=navigation]", "[role=banner]", "[role=complementary]",
            "noscript", "iframe", "form", ".social", ".share", ".comment",
            ".comments", "#comments", ".related", ".breadcrumb", ".pagination"
    };

    public static class PageData {
        public final String title;
        public final String description;
        public final String content;

        public PageData(String title, String description, String content) {
            this.title = title;
            this.description = description;
            this.content = content;
        }
    }

    /**
     * Extracts structured text content from a URL.
     */
    public static PageData extract(String url) throws Exception {
        validateUrlSafety(url);

        // Special-case GitHub blob -> raw content
        if (isGithubBlob(url)) {
            String rawContent = fetchGithubRaw(url);
            if (rawContent != null && !rawContent.isBlank()) {
                log.info("[WebPageExtractor] Fetched GitHub raw content ({} chars)", rawContent.length());
                return new PageData(githubTitle(url), "", rawContent);
            }
        }

        log.info("[WebPageExtractor] Fetching URL: {}", url);
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .referrer("https://www.google.com")
                .timeout(15_000)
                .followRedirects(true)
                .maxBodySize(5 * 1024 * 1024) // 5MB max
                .get();

        // Extract metadata
        String title = firstNonEmpty(
                doc.title(),
                metaTag(doc, "og:title"),
                metaTag(doc, "twitter:title")
        );
        String description = firstNonEmpty(
                metaTag(doc, "description"),
                metaTag(doc, "og:description"),
                metaTag(doc, "twitter:description")
        );

        // Remove boilerplate elements before extracting text
        for (String selector : NOISE_SELECTORS) {
            doc.select(selector).remove();
        }

        String mainText = "";

        // 1. Try <article>
        Element article = doc.selectFirst("article");
        if (article != null) {
            mainText = cleanText(article.text());
            log.info("[WebPageExtractor] Extracted from <article> ({} chars)", mainText.length());
        }

        // 2. Try <main>
        if (isEmpty(mainText)) {
            Element main = doc.selectFirst("main, [role=main], .main-content, #main-content, .post-content, .entry-content, .article-body, .content");
            if (main != null) {
                mainText = cleanText(main.text());
                log.info("[WebPageExtractor] Extracted from <main> ({} chars)", mainText.length());
            }
        }

        // 3. Longest contiguous paragraph block
        if (isEmpty(mainText)) {
            mainText = longestParagraphBlock(doc.select("p"));
            log.info("[WebPageExtractor] Extracted from paragraph blocks ({} chars)", mainText.length());
        }

        // 4. Full body fallback
        if (isEmpty(mainText)) {
            mainText = cleanText(doc.body().text());
            log.info("[WebPageExtractor] Fallback: full body text ({} chars)", mainText.length());
        }

        return new PageData(
                title == null ? "" : title,
                description == null ? "" : description,
                mainText == null ? "" : mainText
        );
    }

    /**
     * Splits text into overlapping chunks for map-reduce summarization.
     * Each chunk is at most maxChars characters, with 'overlap' chars carried over.
     */
    public static List<String> chunk(String text, int maxChars, int overlap) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + maxChars);
            // Try to break at sentence boundary
            if (end < text.length()) {
                int dot = text.lastIndexOf(". ", end);
                if (dot > start + maxChars / 2) end = dot + 2;
            }
            out.add(text.substring(start, end).trim());
            if (end >= text.length()) break;
            start = Math.max(0, end - overlap);
        }
        return out;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String metaTag(Document doc, String key) {
        // Try <meta name="..."> and <meta property="og:...">
        Element e = doc.selectFirst("meta[name=" + key + "]");
        if (e != null && e.hasAttr("content")) return e.attr("content");
        e = doc.selectFirst("meta[property=" + key + "]");
        if (e != null && e.hasAttr("content")) return e.attr("content");
        e = doc.selectFirst("meta[name=twitter:" + key + "]");
        if (e != null && e.hasAttr("content")) return e.attr("content");
        return "";
    }

    private static String longestParagraphBlock(Elements paragraphs) {
        StringBuilder best = new StringBuilder();
        StringBuilder current = new StringBuilder();

        for (Element p : paragraphs) {
            String text = cleanText(p.text());
            if (text.length() < 80) {
                // Short paragraph — possible section break
                if (current.length() > best.length()) {
                    best.setLength(0);
                    best.append(current);
                }
                current.setLength(0);
            } else {
                if (current.length() > 0) current.append("\n\n");
                current.append(text);
            }
        }
        // Check last block
        if (current.length() > best.length()) {
            best.setLength(0);
            best.append(current);
        }
        return best.toString();
    }

    private static boolean isGithubBlob(String url) {
        try {
            URI u = new URI(url);
            return "github.com".equalsIgnoreCase(u.getHost()) && url.contains("/blob/");
        } catch (Exception e) {
            return false;
        }
    }

    private static String fetchGithubRaw(String url) {
        try {
            // https://github.com/user/repo/blob/branch/path
            // -> https://raw.githubusercontent.com/user/repo/branch/path
            String raw = url
                    .replace("https://github.com/", "https://raw.githubusercontent.com/")
                    .replace("/blob/", "/");
            Document d = Jsoup.connect(raw)
                    .ignoreContentType(true)
                    .timeout(10_000)
                    .get();
            return d.body().text();
        } catch (Exception e) {
            log.warn("[WebPageExtractor] GitHub raw fetch failed: {}", e.getMessage());
            return null;
        }
    }

    private static String githubTitle(String url) {
        // Extract "user/repo/path" from URL for a meaningful title
        try {
            String path = new URI(url).getPath();
            String[] parts = path.replaceAll("^/", "").split("/blob/");
            return parts.length > 0 ? parts[0] : url;
        } catch (Exception e) {
            return url;
        }
    }

    private static String cleanText(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\t ]{2,}", " ")
                .replaceAll("(\\r?\\n){3,}", "\n\n")
                .trim();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().length() < 100; // treat < 100 chars as empty
    }

    private static String firstNonEmpty(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return "";
    }
}
