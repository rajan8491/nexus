package com.nexus.gateway.route;

import org.springframework.stereotype.Component;

@Component
public class DefaultRouteMatcher implements RouteMatcher {

    private static final String WILDCARD = "/**";

    @Override
    public boolean matches(
            String pattern,
            String path
    ) {

        if (pattern.equals(path)) {
            return true;
        }

        if (pattern.endsWith(WILDCARD)) {

            String prefix = pattern.substring(
                    0,
                    pattern.length() - WILDCARD.length()
            );

            return path.startsWith(prefix);
        }

        return false;
    }
}
