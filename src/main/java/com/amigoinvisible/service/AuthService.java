package com.amigoinvisible.service;

import com.amigoinvisible.dto.AuthDtos.AuthResponse;
import com.amigoinvisible.dto.AuthDtos.LoginRequest;
import com.amigoinvisible.dto.AuthDtos.RegisterRequest;
import com.amigoinvisible.dto.AuthDtos.UserResponse;
import com.amigoinvisible.entity.AuthToken;
import com.amigoinvisible.entity.User;
import com.amigoinvisible.exception.BadRequestException;
import com.amigoinvisible.exception.ConflictException;
import com.amigoinvisible.repository.AuthTokenRepository;
import com.amigoinvisible.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.token-expiration-days}")
    private long tokenExpirationDays;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Ese nombre de usuario ya esta en uso");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Ese email ya esta registrado");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadRequestException("Usuario o contrasena incorrectos"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Usuario o contrasena incorrectos");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String rawToken) {
        authTokenRepository.deleteByToken(rawToken);
    }

    private AuthResponse buildAuthResponse(User user) {
        AuthToken authToken = AuthToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString().replace("-", ""))
                .expiresAt(Instant.now().plus(tokenExpirationDays, ChronoUnit.DAYS))
                .build();
        authTokenRepository.save(authToken);

        return new AuthResponse(
                authToken.getToken(),
                new UserResponse(user.getId(), user.getUsername(), user.getEmail())
        );
    }
}
