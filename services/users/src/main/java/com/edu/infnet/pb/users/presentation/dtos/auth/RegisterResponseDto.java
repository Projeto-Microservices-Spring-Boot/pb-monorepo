package com.edu.infnet.pb.users.presentation.dtos.auth;

import com.edu.infnet.pb.users.domain.enums.Roles;

public record RegisterResponseDto(String name, String email, Roles role) {

}
