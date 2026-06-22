package com.edu.infnet.pb.stickers.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic CollectionTransfer(){
        return new NewTopic("collection-transfer", 1, (short) 1);



    }
    /**
     * Factory responsável por criar os containers que executam os @KafkaListener.
     * Tudo que configuramos aqui se aplica a TODOS os consumers que usarem
     * containerFactory = "kafkaListenerContainerFactory".
     *
     * O Spring injeta automaticamente:
     * - consumerFactory: criado pelo auto-configure do Spring Boot com as props do application.yml
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler errorHandler) { // <- injeta o bean já criado

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        // MANUAL: o offset só é commitado quando ack.acknowledge() for chamado explicitamente.
        // Sem isso, o Kafka marcaria a mensagem como processada antes de saber se deu certo.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        // Registra handler de erros — ele intercepta qualquer exceção
        // lançada dentro de um @KafkaListener e decide se faz retry ou manda para DLT.
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
    /**
     * Define O QUE FAZER quando uma mensagem falha.
     *
     * Fluxo:
     *   Exceção lançada no consumer
     *       → DefaultErrorHandler intercepta
     *       → aguarda backoff
     *       → tenta de novo (mesma mensagem em memória)
     *       → esgotou tentativas? → chama o recoverer (DLT)
     *
     * O Spring injeta o recoverer definido no bean abaixo.
     */
    @Bean
    public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer recoverer) { // <- injeta o recoverer
        // Backoff exponencial:
        // 1ª tentativa: aguarda 1s
        // 2ª tentativa: aguarda 2s  (1s * 2.0)
        // 3ª tentativa: aguarda 4s  (2s * 2.0)
        // 4ª tentativa: aguarda 8s  (4s * 2.0)
        // Para de tentar quando o tempo total acumulado ultrapassar 30s
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(30000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // DeserializationException significa que a mensagem é inválida/corrompida.
        // Não adianta tentar de novo — vai falhar sempre. Manda direto para DLT.
        handler.addNotRetryableExceptions(DeserializationException.class);

        return handler;
    }

    /**
     * Define PARA ONDE a mensagem vai após esgotar todos os retries.
     *
     * DeadLetterPublishingRecoverer publica a mensagem em um tópico de "quarentena" (DLT)
     * para que possa ser analisada e reprocessada manualmente depois.
     *
     * O Spring injeta o KafkaTemplate automaticamente (configurado pelo auto-configure
     * com as props de producer do application.yml).
     *
     */
    @Bean
    public DeadLetterPublishingRecoverer dlqPublisher(KafkaTemplate<String, Object> template) { // <- Spring injeta o template
        return new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition("collection-transfer.DLT", record.partition())
        );
    }
}
