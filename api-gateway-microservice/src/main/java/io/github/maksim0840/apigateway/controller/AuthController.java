package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.dto.api.*;
import io.github.maksim0840.apigateway.exception.DataUnavailableException;
import io.github.maksim0840.apigateway.exception.RefreshTokenException;
import io.github.maksim0840.apigateway.security.JwtService;
import io.github.maksim0840.apigateway.security.refresh.service.RefreshTokenService;
import io.github.maksim0840.apigateway.service.UserRemoteService;
import io.github.maksim0840.internalapi.user.v1.dto.UserDTO;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRemoteService userRemoteService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserRemoteService userRemoteService,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService) {
        this.userRemoteService = userRemoteService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody AuthApiRequest request) {
        UserDTO user;
        try {
            user = userRemoteService.createUser(request.username(), request.password(), UserRole.ROLE_USER);
        } catch (DataUnavailableException e) {
            throw new BadCredentialsException("User already exists");
        }
        return new RegisterResponse(user.id(), user.name(), "User registered successfully");
    }

    @PostMapping("/login")
    public AuthApiLoginResponse login(@RequestBody AuthApiRequest request) {
        UserDTO user;
        try {
            user = userRemoteService.getUserByName(request.username());
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid username or password");
        }

        boolean correct = userRemoteService.checkUserPasswordById(user.id(), request.password());
        if (!correct) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return issueTokens(user);
    }

    @PostMapping("/refresh")
    public AuthApiLoginResponse refresh(@RequestBody AuthApiRefreshRequest request) {
        String refreshToken = request.refreshToken();

        // подпись и срок действия
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new RefreshTokenException("Refresh token is invalid or expired");
        }

        // защита от подстановки access-токена вместо refresh
        if (!"refresh".equals(jwtService.extractType(refreshToken))) {
            throw new RefreshTokenException("Token type is not refresh");
        }

        String oldJti = jwtService.extractJti(refreshToken);

        if (!refreshTokenService.isActive(oldJti, refreshToken)
                && !refreshTokenService.isWithinGraceWindow(oldJti)) {
            throw new RefreshTokenException("Refresh token revoked or expired");
        }

        Long userId = jwtService.extractUserId(refreshToken);
        UserDTO user = userRemoteService.getUserById(userId);

        AuthApiLoginResponse response = issueTokens(user);
        refreshTokenService.markReplaced(oldJti, jwtService.extractJti(response.refreshToken()));
        return response;
    }

    @PostMapping("/logout")
    public void logout(@RequestBody AuthApiRefreshRequest request) {
        String refreshToken = request.refreshToken();

        if (jwtService.isTokenValid(refreshToken)) {
            refreshTokenService.revoke(jwtService.extractJti(refreshToken));
        }
    }

    // Выпускает пару токенов и сохраняет refresh в хранилище
    private AuthApiLoginResponse issueTokens(UserDTO user) {
        String jti = UUID.randomUUID().toString();

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, jti);

        refreshTokenService.save(jti, user.id(), refreshToken, jwtService.getRefreshLifetimeSec());

        return new AuthApiLoginResponse(accessToken, refreshToken, "Bearer");
    }
}