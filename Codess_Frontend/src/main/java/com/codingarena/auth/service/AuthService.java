package com.codingarena.auth.service;

import com.codingarena.auth.dto.AuthResponse;
import com.codingarena.auth.dto.LoginRequest;
import com.codingarena.auth.dto.RegisterRequest;
import com.codingarena.auth.dto.UserDto;
import com.codingarena.auth.exception.InvalidCredentialsException;
import com.codingarena.auth.exception.UserAlreadyExistsException;
import com.codingarena.auth.model.User;
import com.codingarena.auth.repository.UserRepository;
import com.codingarena.auth.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final CaptchaService captchaService;

    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       CaptchaService captchaService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.captchaService = captchaService;
    }

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this(userRepository, passwordEncoder, tokenProvider, new DefaultCaptchaService("", (org.springframework.web.client.RestOperations) null));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (captchaService != null) {
            captchaService.verifyToken(request.getCaptchaToken());
        }

        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (DisposableEmailBlocklist.isBlocked(normalizedEmail)) {
            throw new IllegalArgumentException("This email provider is not allowed");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getUsername(), normalizedEmail, passwordHash);

        User savedUser = userRepository.save(user);

        String token = tokenProvider.generateToken(savedUser.getId(), savedUser.getEmail());
        return new AuthResponse(token, UserDto.fromEntity(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, UserDto.fromEntity(user));
    }

    public UserDto getCurrentUser(User currentUser) {
        return UserDto.fromEntity(currentUser);
    }
}
