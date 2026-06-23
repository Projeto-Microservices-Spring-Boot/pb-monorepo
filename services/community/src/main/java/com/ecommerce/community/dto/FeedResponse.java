package com.ecommerce.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedResponse {
    private List<PostResponse> posts;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
