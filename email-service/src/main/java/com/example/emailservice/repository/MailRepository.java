package com.example.emailservice.repository;

import com.example.emailservice.entity.Mail;
import org.springframework.data.repository.CrudRepository;

public interface MailRepository extends CrudRepository<Mail, Long> {

}
