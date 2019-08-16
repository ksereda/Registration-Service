package com.example.emailservice.kafka.consumer;

import com.example.emailservice.entity.dto.UserDto;
import com.example.emailservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.concurrent.CountDownLatch;

public class Receiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(Receiver.class);

    private CountDownLatch latch = new CountDownLatch(1);

    @Autowired
    private EmailService emailService;

    // In this method, annotated with @KafkaListener, we add the logic we want to be invoked when a message is received.
    @KafkaListener(topics = "${spring.kafka.topic.userCreated}")
    public void receive(UserDto payload) {
        LOGGER.info("received payload='{}'", payload);
        emailService.sendSimpleMessage(payload);
        latch.countDown();
    }
}
