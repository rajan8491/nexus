package com.nexus.gateway.route;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRouteMatcherTest {

    private final RouteMatcher matcher =
            new DefaultRouteMatcher();

    @Test
    void shouldMatchExactPath() {

        assertTrue(
                matcher.matches(
                        "/api/users",
                        "/api/users"
                )
        );
    }

    @Test
    void shouldMatchWildcardPath() {

        assertTrue(
                matcher.matches(
                        "/api/users/**",
                        "/api/users/123"
                )
        );
    }

    @Test
    void shouldNotMatchDifferentPath() {

        assertFalse(
                matcher.matches(
                        "/api/users/**",
                        "/api/orders/123"
                )
        );
    }

    @Test
    void shouldCalculateSpecificity() {

        assertTrue(
                matcher.specificity(
                        "/api/users/**"
                ) >
                        matcher.specificity(
                                "/api/**"
                        )
        );
    }
}