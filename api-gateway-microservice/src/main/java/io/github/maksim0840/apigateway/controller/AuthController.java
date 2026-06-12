package io.github.maksim0840.apigateway.controller;

import io.github.maksim0840.apigateway.dto.api.AuthApiRequest;
import io.github.maksim0840.apigateway.dto.api.LoginResponse;
import io.github.maksim0840.apigateway.dto.api.RegisterResponse;
import io.github.maksim0840.apigateway.exception.DataUnavailableException;
import io.github.maksim0840.apigateway.security.JwtService;
import io.github.maksim0840.apigateway.service.UserRemoteService;
import io.github.maksim0840.internalapi.user.v1.dto.UserDTO;
import io.github.maksim0840.internalapi.user.v1.enums.UserRole;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRemoteService userRemoteService;
    private final JwtService jwtService;

    public AuthController(UserRemoteService userRemoteService, JwtService jwtService) {
        this.userRemoteService = userRemoteService;
        this.jwtService = jwtService;
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
    public LoginResponse login(@RequestBody AuthApiRequest request) {
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

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, "Bearer");
    }
}
