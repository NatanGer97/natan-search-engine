package com.backend.searchengine.aws;

import com.backend.searchengine.crawler.*;
import com.backend.searchengine.model.*;
import com.fasterxml.jackson.databind.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.cloud.aws.messaging.listener.annotation.*;
import org.springframework.stereotype.*;

import java.io.*;

/**
 * ConsumerSQS is a class that implements the SQS consumer
 */
@Service
public class ConsumerSQS {
    Logger logger = LoggerFactory.getLogger(ConsumerSQS.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CrawlerAsync crawlerAsync;

    @SqsListener(value = "natan-queue")
    public void handlerInComingMessage(Object message) throws IOException, InterruptedException {
        if (message != null)
        {
            logger.info("Received <" + message + ">");
            CrawlerRecord crawlerRecord = objectMapper.readValue(message.toString(), CrawlerRecord.class);
            crawlerAsync.crawlOneRecorde(crawlerRecord);
        }


    }




}
