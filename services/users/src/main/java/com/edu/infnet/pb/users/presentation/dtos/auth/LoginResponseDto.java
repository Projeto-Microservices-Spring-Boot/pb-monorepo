package com.edu.infnet.pb.users.presentation.dtos.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponseDto(
    @Schema(example = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliL...") String accessToken,
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000") String refreshToken,
    @Schema(example = "604800") Long refreshTokenExpiresIn,
    @Schema(example = "300") Long accessTokenExpiresIn) {

}
