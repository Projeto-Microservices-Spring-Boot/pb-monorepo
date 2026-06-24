package com.edu.infnet.pb.store.dto.request;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull Long userId
) {
}
