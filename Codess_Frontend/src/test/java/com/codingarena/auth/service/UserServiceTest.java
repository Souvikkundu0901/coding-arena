package com.codingarena.auth.service;

import com.codingarena.auth.dto.*;
import com.codingarena.auth.exception.UserAlreadyExistsException;
import com.codingarena.auth.model.User;
import com.codingarena.auth.model.UserPreferences;
import com.codingarena.auth.repository.UserPreferencesRepository;
import com.codingarena.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    private User currentUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userPreferencesRepository, passwordEncoder);

        userId = UUID.randomUUID();
        currentUser = new User("original_user", "orig@example.com", "hashed_pwd");
        currentUser.setId(userId);
    }

    @Test
    void updateUsername_Success() {
        UpdateUsernameRequest request = new UpdateUsernameRequest("new_username");
        when(userRepository.findByUsername("new_username")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(currentUser);

        UserDto result = userService.updateUsername(currentUser, request);

        assertEquals("new_username", currentUser.getUsername());
        verify(userRepository).save(currentUser);
    }

    @Test
    void updateUsername_Taken_ThrowsUserAlreadyExistsException() {
        UpdateUsernameRequest request = new UpdateUsernameRequest("taken_user");
        User otherUser = new User("taken_user", "other@example.com", "pwd");
        otherUser.setId(UUID.randomUUID());

        when(userRepository.findByUsername("taken_user")).thenReturn(Optional.of(otherUser));

        assertThrows(UserAlreadyExistsException.class, () -> userService.updateUsername(currentUser, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateEmail_Success() {
        UpdateEmailRequest request = new UpdateEmailRequest("new_email@example.com", "correct_pwd");
        when(passwordEncoder.matches("correct_pwd", "hashed_pwd")).thenReturn(true);
        when(userRepository.findByEmail("new_email@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(currentUser);

        UserDto result = userService.updateEmail(currentUser, request);

        assertEquals("new_email@example.com", currentUser.getEmail());
        verify(userRepository).save(currentUser);
    }

    @Test
    void updateEmail_InvalidPassword_ThrowsBadCredentialsException() {
        UpdateEmailRequest request = new UpdateEmailRequest("new_email@example.com", "wrong_pwd");
        when(passwordEncoder.matches("wrong_pwd", "hashed_pwd")).thenReturn(false);

        assertThrows(com.codingarena.auth.exception.InvalidCredentialsException.class, () -> userService.updateEmail(currentUser, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateEmail_Taken_ThrowsUserAlreadyExistsException() {
        UpdateEmailRequest request = new UpdateEmailRequest("taken@example.com", "correct_pwd");
        User otherUser = new User("other", "taken@example.com", "pwd");
        otherUser.setId(UUID.randomUUID());

        when(passwordEncoder.matches("correct_pwd", "hashed_pwd")).thenReturn(true);
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UserAlreadyExistsException.class, () -> userService.updateEmail(currentUser, request));
    }

    @Test
    void updatePassword_Success() {
        UpdatePasswordRequest request = new UpdatePasswordRequest("current_pwd", "new_secret_pwd");
        when(passwordEncoder.matches("current_pwd", "hashed_pwd")).thenReturn(true);
        when(passwordEncoder.encode("new_secret_pwd")).thenReturn("new_hash");
        when(userRepository.save(any(User.class))).thenReturn(currentUser);

        UserDto result = userService.updatePassword(currentUser, request);

        assertEquals("new_hash", currentUser.getPasswordHash());
        verify(userRepository).save(currentUser);
    }

    @Test
    void updatePassword_InvalidCurrentPassword_ThrowsBadCredentialsException() {
        UpdatePasswordRequest request = new UpdatePasswordRequest("wrong_current_pwd", "new_secret_pwd");
        when(passwordEncoder.matches("wrong_current_pwd", "hashed_pwd")).thenReturn(false);

        assertThrows(com.codingarena.auth.exception.InvalidCredentialsException.class, () -> userService.updatePassword(currentUser, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getPreferences_AutoCreatesDefaultIfMissing() {
        when(userPreferencesRepository.findById(userId)).thenReturn(Optional.empty());
        UserPreferences defaultPref = new UserPreferences(userId, "dark", true);
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(defaultPref);

        UserPreferencesDto result = userService.getPreferences(currentUser);

        assertNotNull(result);
        assertEquals("dark", result.getTheme());
        assertTrue(result.getEmailNotifications());
        verify(userPreferencesRepository).save(any(UserPreferences.class));
    }

    @Test
    void updatePreferences_UpdatesThemeAndNotifications() {
        UserPreferences existing = new UserPreferences(userId, "dark", true);
        when(userPreferencesRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenReturn(existing);

        UpdatePreferencesRequest request = new UpdatePreferencesRequest("light", false);
        UserPreferencesDto result = userService.updatePreferences(currentUser, request);

        assertEquals("light", existing.getTheme());
        assertFalse(existing.getEmailNotifications());
        verify(userPreferencesRepository).save(existing);
    }

    @Test
    void deleteAccount_Success() {
        DeleteAccountRequest request = new DeleteAccountRequest("correct_pwd");
        when(passwordEncoder.matches("correct_pwd", "hashed_pwd")).thenReturn(true);

        userService.deleteAccount(currentUser, request);

        assertTrue(currentUser.getIsDeleted());
        verify(userRepository).save(currentUser);
    }

    @Test
    void deleteAccount_InvalidPassword_ThrowsBadCredentialsException() {
        DeleteAccountRequest request = new DeleteAccountRequest("wrong_pwd");
        when(passwordEncoder.matches("wrong_pwd", "hashed_pwd")).thenReturn(false);

        assertThrows(com.codingarena.auth.exception.InvalidCredentialsException.class, () -> userService.deleteAccount(currentUser, request));
        verify(userRepository, never()).save(any(User.class));
    }
}
