package com.example.userservice.service;

import com.example.userservice.entity.User;

public interface UserService {

    User registerUser(User input);

    Iterable<User> findAll();
}
