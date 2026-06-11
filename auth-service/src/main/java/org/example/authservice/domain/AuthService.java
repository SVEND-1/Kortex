package org.example.authservice.domain;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.api.dto.request.LoginRequest;
import org.example.authservice.api.dto.request.RegisterCodeRequest;
import org.example.authservice.api.dto.request.ResetPasswordRequest;
import org.example.authservice.api.dto.request.VerifyRegisterRequest;
import org.example.authservice.api.dto.response.LoginResponse;
import org.example.authservice.api.dto.response.PasswordResetResponse;
import org.example.authservice.api.dto.response.RegistrationResponse;
import org.example.authservice.api.dto.response.SimpleResponse;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final RegistrationManager registrationManager;
    private final AuthenticationsManager authenticationsManager;
    private final PasswordResetManager passwordResetManager;

    public LoginResponse login(LoginRequest loginRequest, HttpServletResponse response) {
        return authenticationsManager.login(loginRequest,response);
    }

    public SimpleResponse logout(HttpServletResponse response) {
        return authenticationsManager.logout(response);
    }

    public RegistrationResponse sendRegistrationCode(RegisterCodeRequest request) {
        return registrationManager.sendRegistrationCode(request);
    }

    public LoginResponse verifyRegistration(VerifyRegisterRequest request, HttpServletResponse response) {
       return registrationManager.verifyRegistration(request,response);
    }

    public SimpleResponse resendVerificationCode(String registrationId) {
        return registrationManager.resendVerificationCode(registrationId);
    }

    public PasswordResetResponse forgotPassword(String email) {
        return passwordResetManager.forgotPassword(email);
    }

    public PasswordResetResponse verifyResetCode(String resetId, String code) {
        return passwordResetManager.verifyResetCode(resetId,code);
    }

    public LoginResponse resetPassword(ResetPasswordRequest request, HttpServletResponse response) {
        return passwordResetManager.resetPassword(request,response);
    }
}