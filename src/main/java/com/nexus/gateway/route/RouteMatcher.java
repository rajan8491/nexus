package com.nexus.gateway.route;

public interface RouteMatcher {
    boolean matches(String pattern, String path);

    default int specificity(String pattern) {
        return pattern
                .replace("/**", "")
                .length();
    }
}
