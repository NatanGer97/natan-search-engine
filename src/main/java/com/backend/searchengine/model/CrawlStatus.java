package com.backend.searchengine.model;

/**
 * response to client
 */
public class CrawlStatus {
    int distance; // distance from source
    long startTime; // when crawl started
    StopReason stopReason; // why crawl stopped
    long lastModified;
    long numPages = 0; // number of pages crawled

    public static CrawlStatus of(int distance, long startTime, int numPages, StopReason stopReason) {
        CrawlStatus res = new CrawlStatus();
        res.distance = distance;
        res.startTime =  startTime;
        res.lastModified = System.currentTimeMillis();
        res.stopReason = stopReason;
        res.numPages = numPages;
        return res;
    }

    public int getDistance() {
        return distance;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getStartTime() {
        return startTime;
    }

    public StopReason getStopReason() {
        return stopReason;
    }

    public long getNumPages() {
        return numPages;
    }

    public void setNumPages(long numPages) {
        this.numPages = numPages;
    }
}
