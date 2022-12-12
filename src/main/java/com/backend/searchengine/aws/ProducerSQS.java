package com.backend.searchengine.aws;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.cloud.aws.messaging.core.*;
import org.springframework.messaging.*;
import org.springframework.messaging.support.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Component
public class ProducerSQS {

    @Autowired
    private QueueMessagingTemplate queueMessagingTemplate;

    @Value("${cloud.aws.end-point.uri}")
    private String endpoint;

    @Autowired
    private  ObjectMapper objectMapper;
    /**
     * send a message to the queue
     * @param message the message to send
     */
    public void send(Object message) throws JsonProcessingException {
        String messageAsString = objectMapper.writeValueAsString(message);
        Message<String> payload = MessageBuilder.withPayload(messageAsString)
                .setHeader("sender", "natan")
                .build();

        queueMessagingTemplate.send(endpoint, payload);
    }


}
