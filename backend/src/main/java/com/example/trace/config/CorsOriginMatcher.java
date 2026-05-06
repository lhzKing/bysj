package com.example.trace.config;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Matches request origins against exact CORS origins and wildcard patterns.
 */
@Component
public class CorsOriginMatcher {

    public boolean isAllowed(String origin, List<String> allowedOrigins, List<String> allowedOriginPatterns) {
        if (origin == null || origin.isBlank()) {
            return false;
        }

        if (allowedOrigins != null) {
            for (String allowedOrigin : allowedOrigins) {
                if (origin.equals(allowedOrigin)) {
                    return true;
                }
            }
        }

        if (allowedOriginPatterns != null) {
            for (String allowedPattern : allowedOriginPatterns) {
                if (matchesPattern(origin, allowedPattern)) {
                    return true;
                }
            }
        }

        return false;
    }

    boolean matchesPattern(String origin, String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }
        return origin.matches(toRegex(pattern));
    }

    private String toRegex(String wildcardPattern) {
        StringBuilder regex = new StringBuilder("^");
        for (char character : wildcardPattern.toCharArray()) {
            if (character == '*') {
                regex.append(".*");
            } else if ("\\.[]{}()+-^$|?".indexOf(character) >= 0) {
                regex.append('\\').append(character);
            } else {
                regex.append(character);
            }
        }
        regex.append('$');
        return regex.toString();
    }
}
