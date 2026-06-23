package com.ecommerce.community.controller;

import com.ecommerce.community.dto.CreatePostRequest;
import com.ecommerce.community.dto.FeedResponse;
import com.ecommerce.community.dto.PostResponse;
import com.ecommerce.community.dto.PostWithCommentsResponse;
import com.ecommerce.community.dto.UpdatePostRequest;
import com.ecommerce.community.security.JwtClaimsExtractor;
import com.ecommerce.community.security.UserContext;
import com.ecommerce.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<PostResponse> createPost(@RequestBody CreatePostRequest request,
                                                    Authentication authentication) {
        UserContext user = jwtClaimsExtractor.extractUserContext(authentication);
        PostResponse response = postService.createPost(
                user.getUserId(), user.getName(), request.getTitle(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostResponse> updatePost(@PathVariable String postId,
                                                    @RequestBody UpdatePostRequest request,
                                                    Authentication authentication) {
        UserContext user = jwtClaimsExtractor.extractUserContext(authentication);
        PostResponse response = postService.updatePost(
                postId, user.getUserId(), user.getRole(), request.getTitle(), request.getContent());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePost(@PathVariable String postId, Authentication authentication) {
        UserContext user = jwtClaimsExtractor.extractUserContext(authentication);
        postService.deletePost(postId, user.getUserId(), user.getRole());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<FeedResponse> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(postService.getFeed(pageable));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostWithCommentsResponse> getPost(
            @PathVariable String postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        validatePagination(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return ResponseEntity.ok(postService.getPostWithComments(postId, pageable));
    }

   
    private void validatePagination(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new com.ecommerce.community.exception.ValidationException(
                    "page and size must be non-negative and size must be greater than zero");
        }
    }
}
