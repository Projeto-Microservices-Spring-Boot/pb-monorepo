package com.ecommerce.community.service;

import com.ecommerce.community.dto.FeedResponse;
import com.ecommerce.community.dto.PostResponse;
import com.ecommerce.community.dto.PostWithCommentsResponse;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks private PostService postService;
    private ValidationService validationService;

    private static final String USER_ID = "user-1";
    private static final String NAME = "Alice";
    private static final String VALID_TITLE = "A valid post title";
    private static final String VALID_CONTENT = "A valid content for the post body.";

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
        postService = new PostService(postRepository, commentRepository, eventPublisher, validationService);
    }

    
    @Test
    void createPost_withValidInput_savesAndReturnsPost() {
        Post saved = buildPost(USER_ID, VALID_TITLE, VALID_CONTENT);
        when(postRepository.save(any(Post.class))).thenReturn(saved);

        PostResponse response = postService.createPost(USER_ID, NAME, VALID_TITLE, VALID_CONTENT);

        assertThat(response.getPostId()).isEqualTo(saved.getId());
        assertThat(response.getTitle()).isEqualTo(VALID_TITLE);
        verify(postRepository).save(any(Post.class));
        verify(eventPublisher).publishPostCreated(any());
    }

    @Test
    void createPost_withBlankTitle_throwsValidationException() {
        assertThatThrownBy(() -> postService.createPost(USER_ID, NAME, "   ", VALID_CONTENT))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(postRepository, eventPublisher);
    }

    @Test
    void createPost_withTitleTooShort_throwsValidationException() {
        assertThatThrownBy(() -> postService.createPost(USER_ID, NAME, "ab", VALID_CONTENT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createPost_withContentTooShort_throwsValidationException() {
        assertThatThrownBy(() -> postService.createPost(USER_ID, NAME, VALID_TITLE, "short"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createPost_withTitleTooLong_throwsValidationException() {
        String tooLong = "a".repeat(201);
        assertThatThrownBy(() -> postService.createPost(USER_ID, NAME, tooLong, VALID_CONTENT))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createPost_withContentTooLong_throwsValidationException() {
        String tooLong = "a".repeat(10001);
        assertThatThrownBy(() -> postService.createPost(USER_ID, NAME, VALID_TITLE, tooLong))
                .isInstanceOf(ValidationException.class);
    }

   
    @Test
    void updatePost_byOwner_updatesAndPublishesEvent() {
        Post existing = buildPost(USER_ID, VALID_TITLE, VALID_CONTENT);
        when(postRepository.findByIdAndDeletedAtIsNull(existing.getId())).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenReturn(existing);

        String newTitle = "Updated post title";
        String newContent = "Updated content body for the post to meet minimums.";
        PostResponse response = postService.updatePost(existing.getId(), USER_ID, "SELLER", newTitle, newContent);

        assertThat(response.getTitle()).isEqualTo(newTitle);
        verify(eventPublisher).publishPostUpdated(any());
    }

    @Test
    void updatePost_byAdmin_allowed() {
        Post existing = buildPost("other-user", VALID_TITLE, VALID_CONTENT);
        when(postRepository.findByIdAndDeletedAtIsNull(existing.getId())).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenReturn(existing);

        assertThat(postService.updatePost(existing.getId(), USER_ID, "ADMIN", VALID_TITLE, VALID_CONTENT))
                .isNotNull();
    }

    @Test
    void updatePost_byNonOwner_throwsForbidden() {
        Post existing = buildPost("other-user", VALID_TITLE, VALID_CONTENT);
        when(postRepository.findByIdAndDeletedAtIsNull(existing.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> postService.updatePost(existing.getId(), USER_ID, "SELLER", VALID_TITLE, VALID_CONTENT))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updatePost_postNotFound_throwsNotFoundException() {
        when(postRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost("nonexistent", USER_ID, "SELLER", VALID_TITLE, VALID_CONTENT))
                .isInstanceOf(NotFoundException.class);
    }

    
    @Test
    void deletePost_byOwner_softDeletesPostAndComments() {
        Post post = buildPost(USER_ID, VALID_TITLE, VALID_CONTENT);
        Comment comment = buildComment(post.getId(), "comment-user");
        when(postRepository.findByIdAndDeletedAtIsNull(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.findByPostId(post.getId())).thenReturn(List.of(comment));

        postService.deletePost(post.getId(), USER_ID, "SELLER");

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getDeletedAt()).isNotNull();

        verify(commentRepository).saveAll(argThat(comments -> {
            List<Comment> list = (List<Comment>) comments;
            return list.stream().allMatch(c -> c.getDeletedAt() != null);
        }));
        verify(eventPublisher).publishPostDeleted(any());
    }

    @Test
    void deletePost_byNonOwner_throwsForbidden() {
        Post post = buildPost("other-user", VALID_TITLE, VALID_CONTENT);
        when(postRepository.findByIdAndDeletedAtIsNull(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(post.getId(), USER_ID, "SELLER"))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(eventPublisher);
    }

    
    @Test
    void getFeed_returnsPagedPosts() {
        Post p1 = buildPost(USER_ID, VALID_TITLE, VALID_CONTENT);
        Post p2 = buildPost(USER_ID, "Another title here", VALID_CONTENT);
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        Page<Post> page = new PageImpl<>(List.of(p1, p2), pageable, 2);
        when(postRepository.findByDeletedAtIsNull(pageable)).thenReturn(page);

        FeedResponse feed = postService.getFeed(pageable);

        assertThat(feed.getPosts()).hasSize(2);
        assertThat(feed.getTotalElements()).isEqualTo(2);
    }

   
    @Test
    void getPostWithComments_returnsPostAndPaginatedComments() {
        Post post = buildPost(USER_ID, VALID_TITLE, VALID_CONTENT);
        Comment comment = buildComment(post.getId(), USER_ID);
        PageRequest pageable = PageRequest.of(0, 50, Sort.by("createdAt").ascending());
        Page<Comment> commentsPage = new PageImpl<>(List.of(comment), pageable, 1);

        when(postRepository.findByIdAndDeletedAtIsNull(post.getId())).thenReturn(Optional.of(post));
        when(commentRepository.findByPostIdAndDeletedAtIsNull(eq(post.getId()), eq(pageable))).thenReturn(commentsPage);

        PostWithCommentsResponse response = postService.getPostWithComments(post.getId(), pageable);

        assertThat(response.getPostId()).isEqualTo(post.getId());
        assertThat(response.getComments()).hasSize(1);
    }

    @Test
    void getPostWithComments_postNotFound_throwsNotFoundException() {
        when(postRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostWithComments("nonexistent", PageRequest.of(0, 10)))
                .isInstanceOf(NotFoundException.class);
    }

    
    private Post buildPost(String userId, String title, String content) {
        Instant now = Instant.now();
        return Post.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .authorName(NAME)
                .title(title)
                .content(content)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Comment buildComment(String postId, String userId) {
        Instant now = Instant.now();
        return Comment.builder()
                .id(UUID.randomUUID().toString())
                .postId(postId)
                .userId(userId)
                .authorName(NAME)
                .content("A valid comment content")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
