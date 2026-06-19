package com.edu.infnet.pb.stickers.Filters;

import com.edu.infnet.pb.stickers.Exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RequestUserContext {

    public UUID getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new ForbiddenException("Usuário não autenticado");
        }
        return UUID.fromString(userId.toString());
    }
}
