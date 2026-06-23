package com.edu.infnet.pb.users.presentation.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequestDto(
    @Schema(example = "joao@email.com") String email,
    @Schema(example = "123456") String password) {

}
