package org.example.notificationservice.email;

import lombok.extern.slf4j.Slf4j;
import org.example.kafkaEvent.NotifyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
public class EmailSenderService {
    private final JavaMailSender javaMailSender;
    private final EmailTemplateService templateService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailSenderService(JavaMailSender javaMailSender, EmailTemplateService templateService) {
        this.javaMailSender = javaMailSender;
        this.templateService = templateService;
    }

    public void sendEmail(NotifyEvent event) {
        if (event == null || event.email() == null) {
            log.error("Cannot send email: event or email is null");
            return;
        }

        try {
            String subject = templateService.getSubject(event.type(), event.data());
            String content = templateService.getContent(event.type(), event.data());

            sendMessage(event.email(), subject, content);
            log.info("Сообщение успешно отправилось");

        } catch (Exception e) {
            log.error("Ошибка при отправки сообщение, ex={}", e.getMessage());
        }
    }

    private void sendMessage(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        javaMailSender.send(message);
    }

}
