package com.edu.infnet.pb.payments.controller;

import com.edu.infnet.pb.payments.dto.PaymentRequest;
import com.edu.infnet.pb.payments.dto.PaymentResponse;
import com.edu.infnet.pb.payments.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@RequestBody @Valid PaymentRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return service.create(request, userId);
    }

    @GetMapping("/{id}")
    public PaymentResponse findById(@PathVariable UUID id,
                                    @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return service.findById(id, userId);
    }

    @GetMapping("/my")
    public List<PaymentResponse> findMyPayments(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return service.findByUser(userId);
    }

    @PatchMapping("/{id}/cancel")
    public PaymentResponse cancel(@PathVariable UUID id,
                                  @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return service.cancel(id, userId);
    }

}
