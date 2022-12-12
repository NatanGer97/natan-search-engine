package com.backend.searchengine.crawler;

import com.backend.searchengine.model.CrawlStatus;
import com.backend.searchengine.model.CrawlerRecord;
import com.backend.searchengine.model.CrawlerRequest;
import com.backend.searchengine.model.StopReason;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

/**
 * crawler service that crawls the base url and returns the crawl status
 * the crawler is a simple BFS crawler that uses a blocking queue to store the urls to be crawled
 * the crawler stops when the queue is empty or when the max distance is reached
 * the crawler also stops if the max number of pages is reached
 * the crawler also stops if the max time is reached
 * the crawler use blocking queue because it is thread safe and all methods are atomic
 */
@Service
public class Crawler {

    protected final Log logger = LogFactory.getLog(getClass());

    public static final int MAX_CAPACITY = 100000;
    private Set<String> visitedUrls = new HashSet<>();
    private BlockingQueue<CrawlerRecord> queue = new ArrayBlockingQueue<CrawlerRecord>(MAX_CAPACITY);
    private int curDistance = 0;
    private long startTime = 0;
    private StopReason stopReason;

    /**
     * Crawl the given url and return the crawl status
     * bfs approach
     * @param crawlId
     * @param crawlerRequest
     * @return crawl status with information about the crawl
     * @throws InterruptedException
     * @throws IOException
     */
    public CrawlStatus crawl(String crawlId, CrawlerRequest crawlerRequest) throws InterruptedException, IOException {
        visitedUrls.clear();
        queue.clear();
        curDistance = 0;
        startTime = System.currentTimeMillis();
        stopReason = null;

        /*source node */
        CrawlerRecord startRecord = CrawlerRecord.of(crawlId, crawlerRequest);
        queue.put(startRecord);

        while (!queue.isEmpty() && getStopReason(queue.peek()) == null) {
            CrawlerRecord crawlRecord = queue.poll();
            if (crawlRecord == null) {
                throw new RuntimeException("Crawl record is null");
            }

            logger.info("crawling url:" + crawlRecord.getUrl());

            Document webPageContent = Jsoup.connect(crawlRecord.getUrl()).get();
            /*each link is like a neighbor if look on it as  a graph*/
            List<String> innerUrls = extractWebPageUrls(crawlRecord.getBaseUrl(), webPageContent);

            addUrlsToQueue(crawlRecord, innerUrls, crawlRecord.getDistance() + 1);
        }
        stopReason = queue.isEmpty() ? null : getStopReason(queue.peek());
        return CrawlStatus.of(curDistance, startTime, visitedUrls.size(), stopReason);

    }

    /**
     * get the stop reason for the given crawl record
     * @param crawlerRecord
     * @return
     */
    private StopReason getStopReason(CrawlerRecord crawlerRecord) {
        if (crawlerRecord.getDistance() == crawlerRecord.getMaxDistance() +1) return StopReason.maxDistance;
        if (visitedUrls.size() >= crawlerRecord.getMaxUrls()) return StopReason.maxUrls;
        if (System.currentTimeMillis() >= crawlerRecord.getMaxTime()) return StopReason.timeout;

        return null; // if not stop yet
    }

    /**
     * add the urls to the queue -> each url is a like a neighbor node of parent node, in a graph
     * @param crawlerRecord parent node
     * @param urls urls to be added
     * @param distance current distance from source
     * @throws InterruptedException thrown when a thread is interrupted while it's waiting, sleeping
     */
    private void addUrlsToQueue(CrawlerRecord crawlerRecord, List<String> urls, int distance) throws InterruptedException {
        logger.info(">> adding urls to queue: distance->" + distance + " amount->" + urls.size());

        curDistance = distance;
        for (String url : urls) {

            if (!visitedUrls.contains(url)) {
                visitedUrls.add(url);
                queue.put(CrawlerRecord.of(crawlerRecord).withUrl(url).withIncDistance()) ;
            }
        }
    }
/**
     * extract the urls from the web page
     * @param baseUrl
     * @param webPageContent
     * @return
     */
    private List<String> extractWebPageUrls(String baseUrl, Document webPageContent) {
        List<String> links = webPageContent.select("a[href]")
                .eachAttr("abs:href")
                .stream()
                .filter(url -> url.startsWith(baseUrl)) //extract only fitting url -> www.google.com/xxx
                .collect(Collectors.toList());

        logger.info(">> extracted->" + links.size() + " links");

        return links;
    }


}
