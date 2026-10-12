package com.nexus.gateway.ratelimit;

public enum Algorithm {
    TOKEN_BUCKET,
    FIXED_WINDOW,
    SLIDING_WINDOW,
}
