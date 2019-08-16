package com.example.emailservice.service;

import com.example.emailservice.entity.dto.UserDto;

public interface EmailService {

    void sendSimpleMessage(UserDto input);

}
