package com.ecommerce.community.config;

import io.awspring.cloud.sns.core.SnsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AWS SNS/SQS configuration beans (Requirements 9.1, 12.1-12.5, 15.1).
 *
 * Actual queue/topic provisioning (topic, queues, DLQs, subscriptions with
 * filter policies) is documented in docs/aws-setup.md and provisioned via
 * infrastructure-as-code or LocalStack init scripts for local development.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public SnsTemplate snsTemplate(SnsClient snsClient) {
        return new SnsTemplate(snsClient);
    }
}
