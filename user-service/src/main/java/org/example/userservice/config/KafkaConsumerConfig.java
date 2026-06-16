package org.example.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.kafkaEvent.RoleUpdateEvent;
import org.example.kafkaEvent.UserRegisterEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String port;

    @Bean
    public ConsumerFactory<String, UserRegisterEvent> userRegisterConsumerFactory(
            ObjectMapper objectMapper
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, port);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "user-service");

        JsonDeserializer<UserRegisterEvent> deserializer =
                new JsonDeserializer<>(UserRegisterEvent.class, objectMapper);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserRegisterEvent>
    userRegisterKafkaListenerContainerFactory(
            ConsumerFactory<String, UserRegisterEvent> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, UserRegisterEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);
        return factory;
    }



    @Bean
    public ConsumerFactory<String, RoleUpdateEvent> roleUpdateConsumerFactory(
            ObjectMapper objectMapper
    ) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, port);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "user-admin-group");

        JsonDeserializer<RoleUpdateEvent> deserializer =
                new JsonDeserializer<>(RoleUpdateEvent.class, objectMapper);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RoleUpdateEvent>
    roleUpdateKafkaListenerContainerFactory(
            ConsumerFactory<String, RoleUpdateEvent> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, RoleUpdateEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);
        return factory;
    }
}