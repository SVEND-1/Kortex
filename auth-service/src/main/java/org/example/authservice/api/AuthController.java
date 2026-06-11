package org.example.authservice.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.authservice.api.dto.request.LoginRequest;
import org.example.authservice.api.dto.request.RegisterCodeRequest;
import org.example.authservice.api.dto.request.ResetPasswordRequest;
import org.example.authservice.api.dto.request.VerifyRegisterRequest;
import org.example.authservice.api.dto.response.LoginResponse;
import org.example.authservice.api.dto.response.PasswordResetResponse;
import org.example.authservice.api.dto.response.RegistrationResponse;
import org.example.authservice.api.dto.response.SimpleResponse;
import org.example.authservice.domain.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Управление авторизацией")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @Operation(summary = "Вход в систему существуещего пользователя")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        return ResponseEntity.ok(authService.logout(email, accessToken));
    }
    @Operation(summary = "Заполения полей для регистации и отправка кода")
    @PostMapping("/register/send-code")
    public ResponseEntity<RegistrationResponse> sendRegistrationCode(@RequestBody @Valid RegisterCodeRequest request) {
        return ResponseEntity.ok(authService.sendRegistrationCode(request));
    }

    @Operation(summary = "Подтверждение регистрации и создания пользователя")
    @PostMapping("/register/verify")
    public ResponseEntity<LoginResponse> verifyRegistration(
            @RequestBody @Valid VerifyRegisterRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.verifyRegistration(request, response));
    }

    @Operation(summary = "Повторная отправка кода")
    @PostMapping("/register/resend-code")
    public ResponseEntity<SimpleResponse> resendVerificationCode(@RequestParam String registrationId) {
        return ResponseEntity.ok(authService.resendVerificationCode(registrationId));
    }

    @Operation(summary = "Заполнения email пользователя который забыл пароль и отправка кода")
    @PostMapping("/password/forgot")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@RequestParam String email) {
        return ResponseEntity.ok(authService.forgotPassword(email));
    }

    @Operation(summary = "Подтверждение кода")
    @PostMapping("/password/verify")
    public ResponseEntity<PasswordResetResponse> verifyResetCode(
            @RequestParam String resetId,
            @RequestParam String code) {
        return ResponseEntity.ok(authService.verifyResetCode(resetId, code));
    }

    @Operation(summary = "Смена пароля пользователя")
    @PostMapping("/password/reset")
    public ResponseEntity<LoginResponse> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(authService.resetPassword(request, response));
    }
}
