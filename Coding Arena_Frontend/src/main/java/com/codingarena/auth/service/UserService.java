package com.codingarena.auth.service;

import com.codingarena.auth.dto.*;
import com.codingarena.auth.exception.InvalidCredentialsException;
import com.codingarena.auth.exception.UserAlreadyExistsException;
import com.codingarena.auth.model.User;
import com.codingarena.auth.model.UserPreferences;
import com.codingarena.auth.repository.UserPreferencesRepository;
import com.codingarena.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserPreferencesRepository userPreferencesRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto updateUsername(User currentUser, UpdateUsernameRequest request) {
        boolean changed = false;

        if (request.getAvatarSeed() != null && !request.getAvatarSeed().isBlank()) {
            currentUser.setAvatarSeed(request.getAvatarSeed().trim());
            changed = true;
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equalsIgnoreCase(currentUser.getUsername())) {
                userRepository.findByUsername(newUsername).ifPresent(existing -> {
                    if (!existing.getId().equals(currentUser.getId())) {
                        throw new UserAlreadyExistsException("Username is already taken");
                    }
                });
                currentUser.setUsername(newUsername);
                log.info("Updated username for user {} to {}", currentUser.getId(), newUsername);
                changed = true;
            }
        }

        if (changed) {
            User saved = userRepository.save(currentUser);
            return UserDto.fromEntity(saved);
        }
        return UserDto.fromEntity(currentUser);
    }

    @Transactional
    public UserDto updateEmail(User currentUser, UpdateEmailRequest request) {
        if (!passwordEncoder.matches(request.getPassword(), currentUser.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password confirmation");
        }

        String normalizedEmail = request.getNewEmail().toLowerCase().trim();
        if (!normalizedEmail.equalsIgnoreCase(currentUser.getEmail())) {
            userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
                if (!existing.getId().equals(currentUser.getId())) {
                    throw new UserAlreadyExistsException("Email already exists");
                }
            });
            currentUser.setEmail(normalizedEmail);
            User saved = userRepository.save(currentUser);
            log.info("Updated email for user {} to {}", currentUser.getId(), normalizedEmail);
            return UserDto.fromEntity(saved);
        }
        return UserDto.fromEntity(currentUser);
    }

    @Transactional
    public UserDto updatePassword(User currentUser, UpdatePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid current password");
        }

        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        User saved = userRepository.save(currentUser);
        log.info("Updated password for user {}", currentUser.getId());
        return UserDto.fromEntity(saved);
    }

    @Transactional
    public UserPreferencesDto getPreferences(User currentUser) {
        UserPreferences preferences = userPreferencesRepository.findById(currentUser.getId())
                .orElseGet(() -> userPreferencesRepository.save(new UserPreferences(currentUser.getId())));
        return UserPreferencesDto.fromEntity(preferences);
    }

    @Transactional
    public UserPreferencesDto updatePreferences(User currentUser, UpdatePreferencesRequest request) {
        UserPreferences preferences = userPreferencesRepository.findById(currentUser.getId())
                .orElseGet(() -> new UserPreferences(currentUser.getId()));

        if (request.getTheme() != null) {
            preferences.setTheme(request.getTheme());
        }
        if (request.getEmailNotifications() != null) {
            preferences.setEmailNotifications(request.getEmailNotifications());
        }

        UserPreferences saved = userPreferencesRepository.save(preferences);
        return UserPreferencesDto.fromEntity(saved);
    }

    @Transactional
    public void deleteAccount(User currentUser, DeleteAccountRequest request) {
        if (!passwordEncoder.matches(request.getPassword(), currentUser.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password confirmation");
        }

        currentUser.setIsDeleted(true);
        userRepository.save(currentUser);
        log.info("Soft deleted user account {}", currentUser.getId());
    }
}
