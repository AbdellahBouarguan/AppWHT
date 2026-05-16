package com.messenger.client.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkPreviewService {
    private static final Pattern URL_PATTERN = Pattern.compile(
            "((?:https?://|www\\.)[\\w.-]+(?:\\.[\\w.-]+)+[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=.]+)",
            Pattern.CASE_INSENSITIVE);

    public static String extractUrl(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            String url = matcher.group(1);
            if (!url.toLowerCase().startsWith("http")) {
                url = "http://" + url;
            }
            return url;
        }
        return null;
    }

    public static LinkMetadata fetchMetadata(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .referrer("http://www.google.com")
                    .followRedirects(true)
                    .timeout(10000)
                    .get();

            String title = getMetaTag(doc, "og:title");
            if (title == null || title.isEmpty())
                title = getMetaTag(doc, "twitter:title");
            if (title == null || title.isEmpty())
                title = doc.title();

            String description = getMetaTag(doc, "og:description");
            if (description == null || description.isEmpty())
                description = getMetaTag(doc, "twitter:description");
            if (description == null || description.isEmpty()) {
                Element metaDesc = doc.selectFirst("meta[name=description]");
                if (metaDesc != null)
                    description = metaDesc.attr("content");
            }

            String image = getMetaTag(doc, "og:image");
            if (image == null || image.isEmpty())
                image = getMetaTag(doc, "twitter:image");

            // Handle relative image URLs
            if (image != null && image.startsWith("/")) {
                java.net.URL base = new java.net.URL(url);
                image = base.getProtocol() + "://" + base.getHost() + image;
            }

            return new LinkMetadata(title, description, image);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getMetaTag(Document doc, String property) {
        Element element = doc.selectFirst("meta[property=" + property + "]");
        if (element != null)
            return element.attr("content");
        return null;
    }

    public static class LinkMetadata {
        public final String title;
        public final String description;
        public final String imageUrl;

        public LinkMetadata(String title, String description, String imageUrl) {
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
        }
    }
}
