package com.backend.searchengine.StrategyPattern;

import com.backend.searchengine.model.*;


import java.io.*;

/**
 * interface for the strategy pattern of the crawler
 */
public interface ICrawl {
    void crawl(String id, CrawlerRequest request) throws IOException, InterruptedException;

}
