package com.edu.infnet.pb.payments.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic paymentInitiated() {
        return new NewTopic("payment.initiated", 1, (short) 1);
    }

    @Bean
    public NewTopic paymentApproved() {
        return new NewTopic("payment.approved", 1, (short) 1);
    }

    @Bean
    public NewTopic paymentFailed() {
        return new NewTopic("payment.failed", 1, (short) 1);
    }

}
