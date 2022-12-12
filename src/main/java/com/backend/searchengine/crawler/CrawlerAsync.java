package com.backend.searchengine.crawler;

import com.backend.searchengine.Exception.*;
import com.backend.searchengine.StrategyPattern.*;
import com.backend.searchengine.aws.*;
import com.backend.searchengine.kafka.*;
import com.backend.searchengine.model.*;
import com.backend.searchengine.util.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;

/**
 * CrawlerAsync is a class that repository for the asynchronous crawler
 * uses message queue to crawl the web, redis to store the data
 *  and elastic to save the crawled data
 */
@Service
public class CrawlerAsync implements ICrawl {

    private final Logger logger = Logger.getLogger(CrawlerAsync.class.getName());

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ElasticSearch elasticSearch;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    Producer producer;

    @Autowired
    private ProducerSQS producerSQS;

    @Override
    public void crawl(String crawlId, CrawlerRequest request) throws IOException, InterruptedException {
        initNewCrawlerInRedis(crawlId);
        /* with kafka */
        producer.send(CrawlerRecord.of(crawlId, request));

        /* with sqs */
        /*producerSQS.send(CrawlerRecord.of(crawlId, request));*/


    }


    public void crawlOneRecorde(CrawlerRecord crawlerRecord) throws IOException, InterruptedException {
        logger.info("crawling url:" + crawlerRecord.getUrl());
        StopReason stopReason = getStopReason(crawlerRecord);

        // TODO: 04/12/2022 num of pages crawled may be deleted in the set method
        // update each "node" with the distance from the root and the start time of the crawl
        setCrawlStatus(crawlerRecord.getCrawlId(), CrawlStatus.of(crawlerRecord.getDistance(), crawlerRecord.getStartTime(), 0, stopReason));

        if (stopReason == null) { // crawler still running
            Document webPageContent = Jsoup.connect(crawlerRecord.getUrl()).get();
            // write to elastic search the content of the page
            indexElasticSearch(crawlerRecord, webPageContent);
            List<String> innerUrls = extractWebPageUrls(crawlerRecord.getBaseUrl(), webPageContent);

            // add each url to the queue (each url is a "neighbor" of the current url)
            addUrlsToQueue(crawlerRecord, innerUrls, crawlerRecord.getDistance() + 1);

        }

    }

    private void indexElasticSearch(CrawlerRecord rec, Document webPageContent) {
        logger.info(">> adding elastic search for webPage: " + rec.getUrl());
        String text = String.join(" ", webPageContent.select("a[href]").eachText());
        UrlSearchDoc searchDoc = UrlSearchDoc.of(rec.getCrawlId(), text, rec.getUrl(), rec.getBaseUrl(), rec.getDistance());
        elasticSearch.addData(searchDoc);
    }

    /**
     * determine if the crawler should stop, and if so, return the reason
     * @param crawlerRecord
     * @return stop reason
     */
    private StopReason getStopReason(CrawlerRecord crawlerRecord) {
        if (crawlerRecord.getDistance() == crawlerRecord.getMaxDistance() + 1) return StopReason.maxDistance;
        if (getVisitedUrlsCount(crawlerRecord.getCrawlId()) >= crawlerRecord.getMaxUrls()) return StopReason.maxUrls;
        if (System.currentTimeMillis() >= crawlerRecord.getMaxTime()) return StopReason.timeout;

        // if  still running
        return null;
    }

    /**
     * add the urls to the queue -> each url is a like a neighbor node of parent node, in a graph
     *
     * @param crawlerRecord parent node
     * @param urls          urls to be added
     * @param distance      current distance from source
     * @throws InterruptedException thrown when a thread is interrupted while it's waiting, sleeping
     */
    private void addUrlsToQueue(CrawlerRecord crawlerRecord, List<String> urls, int distance) throws InterruptedException, JsonProcessingException {
        logger.info(">> adding urls to queue: distance->" + distance + " amount->" + urls.size());
        for (String url : urls) {

            if (!crawlHasVisited(crawlerRecord, url)) {
                //simulate insert to queue as in original bfs algorithm

                /** add to kafka queue*/
                  producer.send(CrawlerRecord.of(crawlerRecord).withUrl(url).withIncDistance());

                /*with aws sqs*/
                /*producerSQS.send(CrawlerRecord.of(crawlerRecord).withUrl(url).withIncDistance());*/
            }
        }
    }

    /**
     * extract the urls from the web page
     *
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

    private void initNewCrawlerInRedis(String crawlId) {
        setCrawlStatus(crawlId, CrawlStatus.of(0, System.currentTimeMillis(), 0, null));
        redisTemplate.opsForValue().set(crawlId + eRedisKeyPrefix.URLS_COUNT.getKeyPrefix(), "1");
    }

    private void setCrawlStatus(String crawlId, CrawlStatus crawlStatus) {
        try {
            String statusAsString = objectMapper.writeValueAsString(crawlStatus);
            String redisKey = crawlId + eRedisKeyPrefix.STATUS.getKeyPrefix();

            redisTemplate.opsForValue().set(redisKey, statusAsString);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    /**
     * check if the given url is already crawled
     * setIfUpset will return true if the key is not exist -> if the url is not crawled yet
     * if url not crawled yet, add the url to the redis list and increment his count by 1
     * url count is used to know the  amount of urls that was crawled (visited) by the corresponding crawler (crawlId)
     *
     * @param crawlerRecord
     * @param url
     * @return
     */
    private boolean crawlHasVisited(CrawlerRecord crawlerRecord, String url) {
        if (redisTemplate.opsForValue().setIfAbsent(crawlerRecord.getCrawlId() + eRedisKeyPrefix.URLS.getKeyPrefix() + url, "1")) {
            redisTemplate.opsForValue().increment(crawlerRecord.getCrawlId() + eRedisKeyPrefix.URLS_COUNT.getKeyPrefix(), 1L);
            return false;
        } else {
            return true;
        }
    }

    private int getVisitedUrlsCount(String crawlId) {
        Object countValue = redisTemplate.opsForValue()
                .get(crawlId + eRedisKeyPrefix.URLS_COUNT.getKeyPrefix());
        if (countValue == null) return 0;
        else return Integer.parseInt(countValue.toString());

    }

    public CrawlStatusOut getCrawlInfo(String crawlId) {
        Object crawlInfoById = redisTemplate.opsForValue().get(crawlId + eRedisKeyPrefix.STATUS.getKeyPrefix());
        // get method will return null if the key is not exist
        if (crawlInfoById != null) {
            try {
                CrawlStatus crawlStatus = objectMapper.readValue(crawlInfoById.toString(), CrawlStatus.class);
                crawlStatus.setNumPages(getVisitedUrlsCount(crawlId));
                return CrawlStatusOut.of(crawlStatus);

            } catch (JsonProcessingException e) {
                logger.warning("failed to parse crawl status from redis");
                e.printStackTrace();

            }
        }

        throw new NotFoundException(crawlId);
    }
}

