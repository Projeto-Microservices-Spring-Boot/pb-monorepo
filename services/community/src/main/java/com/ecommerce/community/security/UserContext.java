package com.ecommerce.community.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {

    private String userId;
    private String name;
    private String role;
}
