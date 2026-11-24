package org.yasha.auth.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.yasha.auth.service.AuthService;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody @Valid CredentialsRequest req) {
        return AuthResponse.from(authService.register(req.email(), req.password()));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid CredentialsRequest req) {
        return AuthResponse.from(authService.login(req.email(), req.password()));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody @Valid RefreshRequest req) {
        return AuthResponse.from(authService.refresh(req.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody @Valid RefreshRequest req) {
        authService.logout(req.refreshToken());
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        authService.logoutAll(userId);
    }

    @GetMapping("/me")
    public AuthService.MeResponse me(Authentication auth) {
        return authService.me(UUID.fromString(auth.getName()));
    }

    public record CredentialsRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record AuthResponse(String accessToken, String refreshToken, String tokenType) {
        static AuthResponse from(AuthService.AuthTokens t) {
            return new AuthResponse(t.accessToken(), t.refreshToken(), "Bearer");
        }
    }
}