package com.ecommerce.community.repository;

import com.ecommerce.community.model.Post;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@DataMongoTest
@Testcontainers
class PostRepositoryIntegrationTest {

    @Container
    static MongoDBContainer MONGODB = new MongoDBContainer(DockerImageName.parse("mongo:7.0.5"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGODB::getReplicaSetUrl);
    }

    @Autowired PostRepository postRepository;

    @AfterEach
    void tearDown() {
        postRepository.deleteAll();
    }

    @Test
    void findByDeletedAtIsNull_excludesDeletedPosts() {
        Instant now = Instant.now();
        Post active = postRepository.save(buildPost(null));
        Post deleted = postRepository.save(buildPost(now));

        Page<Post> page = postRepository.findByDeletedAtIsNull(
                PageRequest.of(0, 10, Sort.by("createdAt").descending()));

        assertThat(page.getContent()).extracting(Post::getId).containsOnly(active.getId());
    }

    @Test
    void findByIdAndDeletedAtIsNull_returnsEmptyForDeletedPost() {
        Instant now = Instant.now();
        Post deleted = postRepository.save(buildPost(now));

        Optional<Post> found = postRepository.findByIdAndDeletedAtIsNull(deleted.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdAndDeletedAtIsNull_returnsActivePost() {
        Post active = postRepository.save(buildPost(null));

        Optional<Post> found = postRepository.findByIdAndDeletedAtIsNull(active.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(active.getId());
    }

    @Test
    void feed_orderedByCreatedAtDescending() {
        Instant base = Instant.now();
        Post older = postRepository.save(buildPostAtTime(base.minusSeconds(100)));
        Post newer = postRepository.save(buildPostAtTime(base));

        Page<Post> page = postRepository.findByDeletedAtIsNull(
                PageRequest.of(0, 10, Sort.by("createdAt").descending()));

        assertThat(page.getContent()).first().extracting(Post::getId).isEqualTo(newer.getId());
    }

    private Post buildPost(Instant deletedAt) {
        return buildPostAtTime(Instant.now(), deletedAt);
    }

    private Post buildPostAtTime(Instant createdAt) {
        return buildPostAtTime(createdAt, null);
    }

    private Post buildPostAtTime(Instant createdAt, Instant deletedAt) {
        return Post.builder()
                .id(UUID.randomUUID().toString())
                .userId("user-1")
                .authorName("Alice")
                .title("A valid title here")
                .content("Some valid post content")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .deletedAt(deletedAt)
                .build();
    }
}
