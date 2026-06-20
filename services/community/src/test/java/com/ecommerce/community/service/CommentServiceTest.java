package com.ecommerce.community.service;

import com.ecommerce.community.dto.CommentResponse;
import com.ecommerce.community.event.EventPublisher;
import com.ecommerce.community.exception.ForbiddenException;
import com.ecommerce.community.exception.NotFoundException;
import com.ecommerce.community.exception.ValidationException;
import com.ecommerce.community.model.Comment;
import com.ecommerce.community.model.Post;
import com.ecommerce.community.repository.CommentRepository;
import com.ecommerce.community.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private EventPublisher eventPublisher;

    private CommentService commentService;

    private static final String USER_ID  = "user-1";
    private static final String POST_ID  = "post-1";
    private static final String NAME     = "Alice";
    private static final String VALID_CONTENT = "This is a valid comment.";

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, postRepository, eventPublisher, new ValidationService());
    }

    @Test
    void createComment_withValidInput_savesCommentAndUpdatesPostBucket() {
        Post post = buildPost(POST_ID, "other-user");
        Comment saved = buildComment("c-1", POST_ID, USER_ID);

        when(postRepository.findByIdAndDeletedAtIsNull(POST_ID)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        CommentResponse response = commentService.createComment(POST_ID, USER_ID, NAME, VALID_CONTENT);

        assertThat(response.getCommentId()).isEqualTo("c-1");
        assertThat(response.getPostId()).isEqualTo(POST_ID);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getRecentComments()).hasSize(1);

        verify(eventPublisher).publishCommentCreated(any());
    }

    @Test
    void createComment_withBlankContent_throwsValidationException() {
        assertThatThrownBy(() -> commentService.createComment(POST_ID, USER_ID, NAME, "  "))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(commentRepository, postRepository, eventPublisher);
    }

    @Test
    void createComment_withContentTooLong_throwsValidationException() {
        String tooLong = "a".repeat(2001);
        assertThatThrownBy(() -> commentService.createComment(POST_ID, USER_ID, NAME, tooLong))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createComment_withMissingPostId_throwsValidationException() {
        assertThatThrownBy(() -> commentService.createComment(null, USER_ID, NAME, VALID_CONTENT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createComment_postNotFound_throwsNotFoundException() {
        when(postRepository.findByIdAndDeletedAtIsNull(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(POST_ID, USER_ID, NAME, VALID_CONTENT))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createComment_dualWriteEmbedsToBucket() {
        Post post = buildPost(POST_ID, "other-user");
        Comment saved = buildComment("c-1", POST_ID, USER_ID);

        when(postRepository.findByIdAndDeletedAtIsNull(POST_ID)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        commentService.createComment(POST_ID, USER_ID, NAME, VALID_CONTENT);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        Post updatedPost = captor.getValue();
        assertThat(updatedPost.getRecentComments()).anyMatch(ec -> ec.getCommentId().equals("c-1"));
    }

   
    @Test
    void deleteComment_byOwner_softDeletesComment() {
        Comment comment = buildComment("c-1", POST_ID, USER_ID);
        Post post = buildPost(POST_ID, "other-user");

        when(commentRepository.findByIdAndDeletedAtIsNull("c-1")).thenReturn(Optional.of(comment));
        when(postRepository.findByIdAndDeletedAtIsNull(POST_ID)).thenReturn(Optional.of(post));

        commentService.deleteComment("c-1", USER_ID, "SELLER");

        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertThat(commentCaptor.getValue().getDeletedAt()).isNotNull();
        verify(eventPublisher).publishCommentDeleted(any());
    }

    @Test
    void deleteComment_byAdmin_allowed() {
        Comment comment = buildComment("c-1", POST_ID, "other-user");
        Post post = buildPost(POST_ID, "post-owner");

        when(commentRepository.findByIdAndDeletedAtIsNull("c-1")).thenReturn(Optional.of(comment));
        when(postRepository.findByIdAndDeletedAtIsNull(POST_ID)).thenReturn(Optional.of(post));

        commentService.deleteComment("c-1", USER_ID, "ADMIN");

        verify(commentRepository).save(any(Comment.class));
        verify(eventPublisher).publishCommentDeleted(any());
    }

    @Test
    void deleteComment_byNonOwner_throwsForbidden() {
        Comment comment = buildComment("c-1", POST_ID, "other-user");
        when(commentRepository.findByIdAndDeletedAtIsNull("c-1")).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment("c-1", USER_ID, "BUYER"))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deleteComment_notFound_throwsNotFoundException() {
        when(commentRepository.findByIdAndDeletedAtIsNull("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment("nonexistent", USER_ID, "SELLER"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteComment_marksEmbeddedCommentAsDeleted() {
        Comment comment = buildComment("c-1", POST_ID, USER_ID);
        Post post = buildPost(POST_ID, "other-user");
        post.addComment(new com.ecommerce.community.model.EmbeddedComment(
                "c-1", USER_ID, NAME, VALID_CONTENT, Instant.now(), null));

        when(commentRepository.findByIdAndDeletedAtIsNull("c-1")).thenReturn(Optional.of(comment));
        when(postRepository.findByIdAndDeletedAtIsNull(POST_ID)).thenReturn(Optional.of(post));

        commentService.deleteComment("c-1", USER_ID, "SELLER");

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getRecentComments())
                .anyMatch(ec -> ec.getCommentId().equals("c-1") && ec.isDeleted());
    }

    private Post buildPost(String postId, String userId) {
        return Post.builder()
                .id(postId)
                .userId(userId)
                .authorName("Owner")
                .title("A valid post title")
                .content("A valid content for post body.")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Comment buildComment(String commentId, String postId, String userId) {
        return Comment.builder()
                .id(commentId)
                .postId(postId)
                .userId(userId)
                .authorName(NAME)
                .content(VALID_CONTENT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
