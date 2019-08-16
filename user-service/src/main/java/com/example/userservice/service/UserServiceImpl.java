package com.example.userservice.service;

import com.example.userservice.entity.User;
import com.example.userservice.kafka.producer.Sender;
import com.example.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The business logic to send message to Kafka when a new user is saved in the DB
 */

@Component
public class UserServiceImpl implements UserService {

    @Value("${spring.kafka.topic.userCreated}")
    private String USER_CREATED_TOPIC;

    private UserRepository userRepository;
    private Sender sender;

    @Autowired
    UserServiceImpl(UserRepository userRepository, Sender sender) {
        this.userRepository = userRepository;
        this.sender = sender;
    }

    @Override
    public User registerUser(User input) {
        User createdUser = userRepository.save(input);
        sender.send(USER_CREATED_TOPIC, createdUser);
        return createdUser;
    }

    @Override
    public Iterable<User> findAll() {
        return userRepository.findAll();
    }
}
