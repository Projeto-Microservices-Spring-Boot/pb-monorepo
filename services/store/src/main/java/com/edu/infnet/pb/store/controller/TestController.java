package com.edu.infnet.pb.store.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of(
                "status", "ok",
                "auth", "fake-jwt-enabled"
        );
    }
}
