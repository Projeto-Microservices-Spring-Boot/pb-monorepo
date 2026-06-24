package com.edu.infnet.pb.store.controller;

import com.edu.infnet.pb.store.dto.request.CheckoutRequest;
import com.edu.infnet.pb.store.dto.response.CheckoutResponse;
import com.edu.infnet.pb.store.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = checkoutService.finalizarCompra(request.userId());
        return ResponseEntity.ok(response);
    }
}
