package com.backend.searchengine.model;

/**
 *  req that arrives from client
 */
public class CrawlerRequest {
    String url;
    Integer maxDistance;
    Integer maxSeconds;
    Integer maxUrls;

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public Integer getMaxUrls() {
        return maxUrls;
    }

    public Integer getMaxDistance() {
        return maxDistance;
    }

    public Integer getMaxSeconds() {
        return maxSeconds;
    }
}

