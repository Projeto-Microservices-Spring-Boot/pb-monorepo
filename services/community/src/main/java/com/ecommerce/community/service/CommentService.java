package com.ecommerce.community.service;

import com.ecommerce.community.dto.CommentResponse;
import com.ecommerce.community.event.CommentCreatedEvent;
import com.ecommerce.community.event.CommentDeletedEvent;
import com.ecommerce.community.event.EventPublisher;
import com.ecommerce.community.exception.ForbiddenException;
import com.ecommerce.community.exception.InternalServerException;
import com.ecommerce.community.exception.NotFoundException;
import com.ecommerce.community.model.Comment;
import com.ecommerce.community.model.EmbeddedComment;
import com.ecommerce.community.model.Post;
import com.ecommerce.community.repository.CommentRepository;
import com.ecommerce.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final EventPublisher eventPublisher;
    private final ValidationService validationService;

   
    public CommentResponse createComment(String postId, String userId, String name, String content) {
        validationService.validatePostId(postId);
        validationService.validateCommentContent(content);

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));

        Instant now = Instant.now();
        Comment comment = Comment.builder()
                .id(UUID.randomUUID().toString())
                .postId(postId)
                .userId(userId)
                .authorName(name)
                .content(content)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Comment savedComment;
        try {
            savedComment = commentRepository.save(comment);

            EmbeddedComment embedded = new EmbeddedComment(
                    savedComment.getId(),
                    savedComment.getUserId(),
                    savedComment.getAuthorName(),
                    savedComment.getContent(),
                    savedComment.getCreatedAt(),
                    null);
            post.addComment(embedded);
            postRepository.save(post);
        } catch (DataAccessException e) {
            throw new InternalServerException("Failed to persist Comment", e);
        }

        eventPublisher.publishCommentCreated(CommentCreatedEvent.builder()
                .payload(CommentCreatedEvent.Payload.builder()
                        .commentId(savedComment.getId())
                        .postId(savedComment.getPostId())
                        .userId(savedComment.getUserId())
                        .authorName(savedComment.getAuthorName())
                        .content(savedComment.getContent())
                        .createdAt(savedComment.getCreatedAt())
                        .build())
                .build());

        return toCommentResponse(savedComment);
    }

   
    public void deleteComment(String commentId, String userId, String role) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found: " + commentId));

        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(role);
        boolean isOwner = comment.getUserId() != null && comment.getUserId().equals(userId);
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("User is not authorized to delete this comment");
        }

        Instant now = Instant.now();
        comment.setDeletedAt(now);

        try {
            commentRepository.save(comment);

            postRepository.findByIdAndDeletedAtIsNull(comment.getPostId())
                    .ifPresent(post -> {
                        post.markCommentAsDeleted(commentId);
                        postRepository.save(post);
                    });
        } catch (DataAccessException e) {
            throw new InternalServerException("Failed to delete Comment", e);
        }

        eventPublisher.publishCommentDeleted(CommentDeletedEvent.builder()
                .payload(CommentDeletedEvent.Payload.builder()
                        .commentId(comment.getId())
                        .postId(comment.getPostId())
                        .userId(comment.getUserId())
                        .deletedAt(now)
                        .build())
                .build());
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .authorName(comment.getAuthorName())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
