package com.ecommerce.community.controller;

import com.ecommerce.community.dto.FeedResponse;
import com.ecommerce.community.dto.PostResponse;
import com.ecommerce.community.dto.PostWithCommentsResponse;
import com.ecommerce.community.exception.ForbiddenException;
import com.ecommerce.community.exception.NotFoundException;
import com.ecommerce.community.security.JwtClaimsExtractor;
import com.ecommerce.community.security.UserContext;
import com.ecommerce.community.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PostService postService;
    @MockBean JwtClaimsExtractor jwtClaimsExtractor;

    @Test
    void createPost_withSellerRole_returns201() throws Exception {
        UserContext seller = new UserContext("user-1", "Alice", "SELLER");
        when(jwtClaimsExtractor.extractUserContext(any(Authentication.class))).thenReturn(seller);
        PostResponse mockResponse = PostResponse.builder()
                .postId("p-1").title("Valid Title").content("Content body here")
                .userId("user-1").authorName("Alice").createdAt(Instant.now()).build();
        when(postService.createPost(any(), any(), any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/posts")
                        .with(jwt().authorities(authorities("ROLE_SELLER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Valid Title", "content": "Content body here" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value("p-1"));
    }

    @Test
    void createPost_withAdminRole_returns201() throws Exception {
        UserContext admin = new UserContext("admin-1", "Admin", "ADMIN");
        when(jwtClaimsExtractor.extractUserContext(any(Authentication.class))).thenReturn(admin);
        PostResponse mockResponse = PostResponse.builder()
                .postId("p-2").title("Valid Title").content("Content body here")
                .userId("admin-1").authorName("Admin").createdAt(Instant.now()).build();
        when(postService.createPost(any(), any(), any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/posts")
                        .with(jwt().authorities(authorities("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Valid Title", "content": "Content body here" }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void createPost_withBuyerRole_returns403() throws Exception {
        mockMvc.perform(post("/posts")
                        .with(jwt().authorities(authorities("ROLE_BUYER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Valid Title", "content": "Content body here" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPost_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Valid Title", "content": "Content body here" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getFeed_withoutAuthentication_returns200() throws Exception {
        FeedResponse feed = FeedResponse.builder()
                .posts(List.of()).totalElements(0).totalPages(0).currentPage(0).build();
        when(postService.getFeed(any())).thenReturn(feed);

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getFeed_withNegativePage_returns400() throws Exception {
        mockMvc.perform(get("/posts?page=-1&size=20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFeed_withZeroSize_returns400() throws Exception {
        mockMvc.perform(get("/posts?page=0&size=0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPost_withoutAuthentication_returns200() throws Exception {
        PostWithCommentsResponse resp = PostWithCommentsResponse.builder()
                .postId("p-1").title("T").content("C").userId("u-1")
                .comments(List.of()).totalElements(0).totalPages(0).currentPage(0).build();
        when(postService.getPostWithComments(eq("p-1"), any())).thenReturn(resp);

        mockMvc.perform(get("/posts/p-1"))
                .andExpect(status().isOk());
    }

    @Test
    void getPost_notFound_returns404() throws Exception {
        when(postService.getPostWithComments(eq("nonexistent"), any()))
                .thenThrow(new NotFoundException("Post not found: nonexistent"));

        mockMvc.perform(get("/posts/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void updatePost_byOwner_returns200() throws Exception {
        UserContext seller = new UserContext("user-1", "Alice", "SELLER");
        when(jwtClaimsExtractor.extractUserContext(any(Authentication.class))).thenReturn(seller);
        PostResponse updated = PostResponse.builder()
                .postId("p-1").title("Updated Title").content("Updated content")
                .userId("user-1").authorName("Alice").createdAt(Instant.now()).build();
        when(postService.updatePost(eq("p-1"), any(), any(), any(), any())).thenReturn(updated);

        mockMvc.perform(put("/posts/p-1")
                        .with(jwt().authorities(authorities("ROLE_SELLER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Updated Title", "content": "Updated content" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void updatePost_forbiddenUser_returns403() throws Exception {
        UserContext seller = new UserContext("other-user", "Bob", "SELLER");
        when(jwtClaimsExtractor.extractUserContext(any(Authentication.class))).thenReturn(seller);
        when(postService.updatePost(any(), any(), any(), any(), any()))
                .thenThrow(new ForbiddenException("Not authorized"));

        mockMvc.perform(put("/posts/p-1")
                        .with(jwt().authorities(authorities("ROLE_SELLER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Updated Title", "content": "Updated content" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePost_byOwner_returns204() throws Exception {
        UserContext seller = new UserContext("user-1", "Alice", "SELLER");
        when(jwtClaimsExtractor.extractUserContext(any(Authentication.class))).thenReturn(seller);

        mockMvc.perform(delete("/posts/p-1")
                        .with(jwt().authorities(authorities("ROLE_SELLER"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePost_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(delete("/posts/p-1"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.security.core.GrantedAuthority[] authorities(String... roles) {
        return java.util.Arrays.stream(roles)
                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                .toArray(org.springframework.security.core.GrantedAuthority[]::new);
    }
}
