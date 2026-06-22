package com.edu.infnet.pb.stickers.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic CollectionTransfer(){
        return new NewTopic("collection-transfer", 1, (short) 1);
    }
}
