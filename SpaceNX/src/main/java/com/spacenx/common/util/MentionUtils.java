package com.spacenx.common.util;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for processing @mentions in comment text.
 * Converts raw @username patterns into styled HTML spans.
 */
@Component("mentionUtils")
public class MentionUtils {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    public static String processMentions(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // First, escape HTML to prevent XSS
        String escaped = escapeHtml(content);

        // Then replace @username patterns with styled spans
        Matcher matcher = MENTION_PATTERN.matcher(escaped);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String username = matcher.group(1);
            String replacement = "<span class=\"mention\">@" + username + "</span>";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
