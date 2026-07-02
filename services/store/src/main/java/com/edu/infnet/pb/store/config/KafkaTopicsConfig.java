package com.edu.infnet.pb.store.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Value("${app.kafka.topics.stock-updated}")
    private String stockUpdatedTopic;

    @Value("${app.kafka.topics.payment-confirmed}")
    private String paymentConfirmedTopic;

    @Value("${app.kafka.topics.user-events}")
    private String userEventsTopic;

    @Value("${app.kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Bean
    public NewTopic stockUpdatedTopic() {
        return TopicBuilder.name(stockUpdatedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentConfirmedTopic() {
        return TopicBuilder.name(paymentConfirmedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(userEventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(orderCreatedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
