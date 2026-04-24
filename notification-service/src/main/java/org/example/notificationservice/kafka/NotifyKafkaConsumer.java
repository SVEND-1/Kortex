package org.example.notificationservice.kafka;

import org.example.kafkaEvent.NotifyEvent;
import org.example.notificationservice.email.EmailSenderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotifyKafkaConsumer {
    private final EmailSenderService emailSenderService;

    private final String topic = "notification-service";

    public NotifyKafkaConsumer(EmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }

    @KafkaListener(topics = topic)
    public void consumeNotify(NotifyEvent event) {
        emailSenderService.sendEmail(event);
    }
}
