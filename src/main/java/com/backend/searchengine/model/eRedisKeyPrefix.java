package com.backend.searchengine.model;

public enum eRedisKeyPrefix {
    STATUS(".status"),
    URLS(".urls."),
    URLS_COUNT(".urls.count"),
    ;

    private final String keyPrefix;

    eRedisKeyPrefix(String keyPrefix)
    {
        this.keyPrefix = keyPrefix;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }
}
