package com.codingarena.auth.controller;

import com.codingarena.auth.dto.AuthResponse;
import com.codingarena.auth.dto.LoginRequest;
import com.codingarena.auth.dto.RegisterRequest;
import com.codingarena.auth.dto.UserDto;
import com.codingarena.auth.model.User;
import com.codingarena.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal User currentUser) {
        UserDto userDto = authService.getCurrentUser(currentUser);
        return ResponseEntity.ok(userDto);
    }
}
