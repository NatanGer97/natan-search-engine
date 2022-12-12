package com.backend.searchengine.kafka;

import com.backend.searchengine.crawler.*;
import com.backend.searchengine.model.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Optional;

import static com.backend.searchengine.kafka.Producer.APP_TOPIC;

@Component
public class Consumer {

    @Autowired
    CrawlerAsync crawlerAsync;

    @Autowired
    ObjectMapper objectMapper;
    Logger logger = LoggerFactory.getLogger(Consumer.class);

    /**
     * This method is called when a message is received on the topic
     * the main flow of bfs algorithm is here -> examine the current "node" and make some actions
     * and recurse on the children
     * @param record
     * @throws JsonProcessingException
     */
    @KafkaListener(topics = {APP_TOPIC})
    public void listen(ConsumerRecord<?, ?> record) throws JsonProcessingException {

        Optional<?> kafkaMessage = Optional.ofNullable(record.value());
        logger.info("kafka:" + String.valueOf((kafkaMessage.isPresent())));

        if (kafkaMessage.isPresent()) {
            logger.info("Received from kafka <" + kafkaMessage.get() + ">");
            Object message = kafkaMessage.get();
            try {
                crawlerAsync.crawlOneRecorde(objectMapper.readValue(message.toString(), CrawlerRecord.class));

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }


        }
    }
}
