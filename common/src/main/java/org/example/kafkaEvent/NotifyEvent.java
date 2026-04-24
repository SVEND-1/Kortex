package org.example.kafkaEvent;


import java.util.Map;


public record NotifyEvent(
        String email,
        Map<String, Object> data,
        NotifyType type
){
}
