package com.ecommerce.community;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    @Container
    static final MongoDBContainer MONGODB = new MongoDBContainer(
            DockerImageName.parse("mongo:7.0.5"));

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.2"))
            .withServices(SNS, SQS);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGODB::getReplicaSetUrl);

        registry.add("spring.cloud.aws.region.static", () -> LOCALSTACK.getRegion());
        registry.add("spring.cloud.aws.credentials.access-key", () -> LOCALSTACK.getAccessKey());
        registry.add("spring.cloud.aws.credentials.secret-key", () -> LOCALSTACK.getSecretKey());
        registry.add("spring.cloud.aws.sns.endpoint",
                () -> LOCALSTACK.getEndpointOverride(SNS).toString());
        registry.add("spring.cloud.aws.sqs.endpoint",
                () -> LOCALSTACK.getEndpointOverride(SQS).toString());
    }
}
