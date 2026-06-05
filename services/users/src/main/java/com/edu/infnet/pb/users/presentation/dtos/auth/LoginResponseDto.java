package com.edu.infnet.pb.users.presentation.dtos.auth;

public record LoginResponseDto(String accessToken, String refreshToken, Long expiresIn) {

}
