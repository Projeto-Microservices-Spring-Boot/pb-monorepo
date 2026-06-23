package com.ecommerce.community.service;

import com.ecommerce.community.dto.CommentResponse;
import com.ecommerce.community.dto.CreatePostRequest;
import com.ecommerce.community.dto.FeedResponse;
import com.ecommerce.community.dto.PostResponse;
import com.ecommerce.community.dto.PostWithCommentsResponse;
import com.ecommerce.community.dto.UpdatePostRequest;
import com.ecommerce.community.event.EventPublisher;
import com.ecommerce.community.event.PostCreatedEvent;
import com.ecommerce.community.event.PostDeletedEvent;
import com.ecommerce.community.event.PostUpdatedEvent;
import com.ecommerce.community.exception.ForbiddenException;
import com.ecommerce.community.exception.InternalServerException;
import com.ecommerce.community.exception.NotFoundException;
import com.ecommerce.community.model.Comment;
import com.ecommerce.community.model.Post;
import com.ecommerce.community.repository.CommentRepository;
import com.ecommerce.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PostService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final EventPublisher eventPublisher;
    private final ValidationService validationService;

  
    public PostResponse createPost(String userId, String name, String title, String content) {
        validationService.validateTitle(title);
        validationService.validatePostContent(content);

        Instant now = Instant.now();
        Post post = Post.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .authorName(name)
                .title(title)
                .content(content)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Post saved;
        try {
            saved = postRepository.save(post);
        } catch (DataAccessException e) {
            throw new InternalServerException("Failed to persist Post", e);
        }

        eventPublisher.publishPostCreated(PostCreatedEvent.builder()
                .payload(PostCreatedEvent.Payload.builder()
                        .postId(saved.getId())
                        .userId(saved.getUserId())
                        .authorName(saved.getAuthorName())
                        .title(saved.getTitle())
                        .content(saved.getContent())
                        .createdAt(saved.getCreatedAt())
                        .build())
                .build());

        return toPostResponse(saved);
    }

   
    public PostResponse updatePost(String postId, String userId, String role, String title, String content) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));

        checkOwnershipOrAdmin(post.getUserId(), userId, role);

        validationService.validateTitle(title);
        validationService.validatePostContent(content);

        post.setTitle(title);
        post.setContent(content);
        post.setUpdatedAt(Instant.now());

        Post saved;
        try {
            saved = postRepository.save(post);
        } catch (DataAccessException e) {
            throw new InternalServerException("Failed to persist Post", e);
        }

        eventPublisher.publishPostUpdated(PostUpdatedEvent.builder()
                .payload(PostUpdatedEvent.Payload.builder()
                        .postId(saved.getId())
                        .userId(saved.getUserId())
                        .title(saved.getTitle())
                        .content(saved.getContent())
                        .updatedAt(saved.getUpdatedAt())
                        .build())
                .build());

        return toPostResponse(saved);
    }

    
    public void deletePost(String postId, String userId, String role) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));

        checkOwnershipOrAdmin(post.getUserId(), userId, role);

        Instant now = Instant.now();
        post.setDeletedAt(now);

        try {
            postRepository.save(post);

            List<Comment> comments = commentRepository.findByPostId(postId);
            for (Comment comment : comments) {
                if (comment.getDeletedAt() == null) {
                    comment.setDeletedAt(now);
                }
            }
            if (!comments.isEmpty()) {
                commentRepository.saveAll(comments);
            }
        } catch (DataAccessException e) {
            throw new InternalServerException("Failed to delete Post", e);
        }

        eventPublisher.publishPostDeleted(PostDeletedEvent.builder()
                .payload(PostDeletedEvent.Payload.builder()
                        .postId(post.getId())
                        .userId(post.getUserId())
                        .deletedAt(now)
                        .build())
                .build());
    }

  
    public FeedResponse getFeed(Pageable pageable) {
        Page<Post> page = postRepository.findByDeletedAtIsNull(pageable);

        List<PostResponse> posts = page.getContent().stream()
                .map(this::toPostResponse)
                .collect(Collectors.toList());

        return FeedResponse.builder()
                .posts(posts)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    
    public PostWithCommentsResponse getPostWithComments(String postId, Pageable pageable) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));

        Page<Comment> commentsPage = commentRepository.findByPostIdAndDeletedAtIsNull(postId, pageable);

        List<CommentResponse> comments = commentsPage.getContent().stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());

        return PostWithCommentsResponse.builder()
                .postId(post.getId())
                .userId(post.getUserId())
                .authorName(post.getAuthorName())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .comments(comments)
                .totalElements(commentsPage.getTotalElements())
                .totalPages(commentsPage.getTotalPages())
                .currentPage(commentsPage.getNumber())
                .build();
    }

    private void checkOwnershipOrAdmin(String resourceOwnerId, String userId, String role) {
        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(role);
        boolean isOwner = resourceOwnerId != null && resourceOwnerId.equals(userId);
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("User is not authorized to modify this resource");
        }
    }

    private PostResponse toPostResponse(Post post) {
        return PostResponse.builder()
                .postId(post.getId())
                .userId(post.getUserId())
                .authorName(post.getAuthorName())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .recentComments(post.getRecentComments())
                .build();
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
