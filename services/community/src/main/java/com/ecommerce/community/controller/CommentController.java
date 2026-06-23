package com.ecommerce.community.controller;

import com.ecommerce.community.dto.CommentResponse;
import com.ecommerce.community.dto.CreateCommentRequest;
import com.ecommerce.community.security.JwtClaimsExtractor;
import com.ecommerce.community.security.UserContext;
import com.ecommerce.community.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final JwtClaimsExtractor jwtClaimsExtractor;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> createComment(@RequestBody CreateCommentRequest request,
                                                           Authentication authentication) {
        UserContext user = jwtClaimsExtractor.extractUserContext(authentication);
        CommentResponse response = commentService.createComment(
                request.getPostId(), user.getUserId(), user.getName(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(@PathVariable String commentId, Authentication authentication) {
        UserContext user = jwtClaimsExtractor.extractUserContext(authentication);
        commentService.deleteComment(commentId, user.getUserId(), user.getRole());
        return ResponseEntity.noContent().build();
    }
}
