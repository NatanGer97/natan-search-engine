package com.backend.searchengine.controller;


import com.backend.searchengine.crawler.*;
import com.backend.searchengine.kafka.*;
import com.backend.searchengine.model.*;
import com.backend.searchengine.util.*;
import com.fasterxml.jackson.core.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.cloud.aws.messaging.core.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;

@RestController
@RequestMapping("searchEngine/api")
public class AppController {


    @Autowired
    Crawler crawler;

    @Autowired
    Producer producer;

    @Autowired
    CrawlerAsync crawlerAsync;

    @Autowired QueueMessagingTemplate queueMessagingTemplate;
    @Value("${cloud.aws.end-point.uri}")
    private String endpoint;

    /**
     * This method is responsible for crawling the web with bfs algorithm
     * @param request
     * @return the result of the crawling
     * @throws IOException
     * @throws InterruptedException
     */
    @PostMapping(value = "/crawlBFS")
    public CrawlStatusOut crawl(@RequestBody CrawlerRequest request) throws IOException, InterruptedException {
        String crawlId = IdGenerator.generateCrawlId();

        if (!request.getUrl().startsWith("http")) {
            request.setUrl("https://" + request.getUrl());
        }
        // start the crawl -> start bfs with the given url in the request
        CrawlStatus res = crawler.crawl(crawlId, request);
        return CrawlStatusOut.of(res);
    }


    /**
     * This method is responsible for crawling the web with redis and kafka (or sqs) queues
     * uses asynchronous approach
     * @param request
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @PostMapping(value = "/crawl_async")
    public String crawlAsync(@RequestBody CrawlerRequest request) {
        String crawlId = IdGenerator.generateCrawlId();

        if (!request.getUrl().startsWith("http")) {
            request.setUrl("https://" + request.getUrl());
        }

        // start the crawl -> the thread simulates async work
        new Thread(() -> {
            try {
                crawlerAsync.crawl(crawlId, request);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        return crawlId;
    }

    /**
     * this method is responsible for getting the crawl status with info from redis
     * @param crawlId
     * @return
     */
    @GetMapping(value = "/crawl/{crawlId}")
    public CrawlStatusOut getCrawlStatus(@PathVariable String crawlId) {
        return crawlerAsync.getCrawlInfo(crawlId);
    }

   /* *//**
     * for testing purposes of kafka queue
     * @param request
     * @return
     * @throws JsonProcessingException
     *//*
    @PostMapping(value = "/sendKafka")
    public String sendKafka(String request) throws JsonProcessingException {
        producer.send(request);
        return "OK - sent to kafka";
    }

    *//**
     * for testing purposes of sqs queue
     * @param message
     *//*
   @GetMapping("/send/{message}")
    public String send(@PathVariable(value = "message") String message) {
        Message<String> payload = MessageBuilder.withPayload(message)
                .setHeader("sender", "natan")
                .build();

        queueMessagingTemplate.send(endpoint, payload);

        return "OK - sent to sqs";


    }*/

}
