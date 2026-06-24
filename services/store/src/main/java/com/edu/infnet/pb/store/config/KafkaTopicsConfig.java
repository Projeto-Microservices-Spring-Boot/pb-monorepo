package com.edu.infnet.pb.store.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic stockUpdatedTopic(
            @Value("${app.kafka.topics.stock-updated}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentConfirmedTopic(
            @Value("${app.kafka.topics.payment-confirmed}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userEventsTopic(
            @Value("${app.kafka.topics.user-events}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Value("${app.kafka.topics.stock-updated:stock-updated}")
    private String stockUpdatedTopic;

}
