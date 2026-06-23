package com.ecommerce.community.controller;

import com.ecommerce.community.dto.CommentResponse;
import com.ecommerce.community.exception.ForbiddenException;
import com.ecommerce.community.security.JwtClaimsExtractor;
import com.ecommerce.community.security.UserContext;
import com.ecommerce.community.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CommentService commentService;
    @MockBean JwtClaimsExtractor jwtClaimsExtractor;

    @Test
    void createComment_authenticated_returns201() throws Exception {
        UserContext buyer = new UserContext("user-1", "Alice", "BUYER");
        when(jwtClaimsExtractor.extractUserContext(any(Authentication.class))).thenReturn(buyer);

        CommentResponse response = CommentResponse.builder()
                .commentId("c-1").postId("p-1").userId("user-1")
                .authorName("Alice").content("This is a valid comment.")
                .createdAt(Instant.now()).build();
        when(commentService.createComment(eq("p-1"), eq("user-1"), eq("Alice"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/comments")
                        .with(jwt().authorities(authority("ROLE_BUYER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "postId": "p-1", "content": "This is a valid comment." }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value("c-1"));
    }

    @Test
    void createComment_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "postId": "p-1", "content": "Hello" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteComment_byOwner_returns204() throws Exception {
        UserContext buyer = new UserContext("user-1", "Alice", "BUYER");
        when(jwtClaimsExtractor.extractUserContext(any(Authentication.class))).thenReturn(buyer);

        mockMvc.perform(delete("/comments/c-1")
                        .with(jwt().authorities(authority("ROLE_BUYER"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_forbidden_returns403() throws Exception {
        UserContext buyer = new UserContext("other-user", "Bob", "BUYER");
        when(jwtClaimsExtractor.extractUserContext(any(Authentication.class))).thenReturn(buyer);
        when(commentService.deleteComment(eq("c-1"), eq("other-user"), eq("BUYER")))
                .thenThrow(new ForbiddenException("Not the owner"));

        mockMvc.perform(delete("/comments/c-1")
                        .with(jwt().authorities(authority("ROLE_BUYER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteComment_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(delete("/comments/c-1"))
                .andExpect(status().isUnauthorized());
    }

    private GrantedAuthority authority(String role) {
        return new SimpleGrantedAuthority(role);
    }
}
