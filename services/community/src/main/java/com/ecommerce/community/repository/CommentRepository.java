package com.ecommerce.community.repository;

import com.ecommerce.community.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {

    Page<Comment> findByPostIdAndDeletedAtIsNull(String postId, Pageable pageable);

    List<Comment> findByPostId(String postId);

    List<Comment> findByUserIdAndDeletedAtIsNull(String userId);

    Optional<Comment> findByIdAndDeletedAtIsNull(String id);
}
