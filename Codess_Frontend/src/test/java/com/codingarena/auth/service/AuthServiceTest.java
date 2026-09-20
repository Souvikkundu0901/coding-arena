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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider tokenProvider;

    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;
    private UUID mockUserId;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "c3VwZXItc2VjcmV0LWtleS1mb3ItY29kaW5nLWFyZW5hLWp3dC1hdXRoZW50aWNhdGlvbi0yNTYtYml0cw==",
                86400000L
        );
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider);

        mockUserId = UUID.randomUUID();
        registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");
        loginRequest = new LoginRequest("test@example.com", "password123");
        mockUser = new User("testuser", "test@example.com", "encodedPassword");
        mockUser.setId(mockUserId);
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertTrue(tokenProvider.validateToken(response.getToken()));
        assertEquals(mockUserId, tokenProvider.getUserIdFromToken(response.getToken()));
        assertEquals("testuser", response.getUser().getUsername());
        assertEquals("test@example.com", response.getUser().getEmail());
        assertEquals(0, response.getUser().getRating());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_EmailNormalization_NormalizesToLowercaseAndTrims() {
        RegisterRequest unnormalizedReq = new RegisterRequest("testuser2", "  TestUser@Example.COM  ", "password123");
        User savedMockUser = new User("testuser2", "testuser@example.com", "encodedPassword");
        savedMockUser.setId(mockUserId);

        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser2")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedMockUser);

        AuthResponse response = authService.register(unnormalizedReq);

        assertNotNull(response);
        assertEquals("testuser@example.com", response.getUser().getEmail());
        verify(userRepository).existsByEmail("testuser@example.com");
    }

    @Test
    void register_DuplicateEmail_ThrowsUserAlreadyExistsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername_ThrowsUserAlreadyExistsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertTrue(tokenProvider.validateToken(response.getToken()));
        assertEquals(mockUserId, tokenProvider.getUserIdFromToken(response.getToken()));
        assertEquals("testuser", response.getUser().getUsername());
    }

    @Test
    void login_InvalidEmail_ThrowsInvalidCredentialsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void login_InvalidPassword_ThrowsInvalidCredentialsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void getCurrentUser_Success() {
        UserDto userDto = authService.getCurrentUser(mockUser);

        assertNotNull(userDto);
        assertEquals(mockUserId, userDto.getId());
        assertEquals("testuser", userDto.getUsername());
        assertEquals("test@example.com", userDto.getEmail());
    }
}
