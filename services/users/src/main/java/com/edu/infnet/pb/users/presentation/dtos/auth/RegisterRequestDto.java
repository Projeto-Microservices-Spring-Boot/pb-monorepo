package com.edu.infnet.pb.users.presentation.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterRequestDto(
    @Schema(example = "João Silva") String name,
    @Schema(example = "joao@email.com") String email,
    @Schema(example = "123456") String password) {

}
