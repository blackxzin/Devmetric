package com.devmetrics.auth;

import com.devmetrics.auth.dto.AuthResponse;
import com.devmetrics.auth.dto.LoginRequest;
import com.devmetrics.auth.dto.RefreshRequest;
import com.devmetrics.auth.dto.RegisterRequest;
import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.demo.DemoDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacao", description = "Registro, login e ciclo de vida do token")
public class AuthController {

    private final AuthService authService;
    private final DemoDataService demoDataService;

    public AuthController(AuthService authService, DemoDataService demoDataService) {
        this.authService = authService;
        this.demoDataService = demoDataService;
    }

    @PostMapping("/demo")
    @Operation(summary = "Entra na conta demo (so quando DEMO_ENABLED=true)")
    public ApiResponse<AuthResponse> demo() {
        if (!demoDataService.enabled()) {
            throw new BusinessException(ErrorCode.DEMO_DISABLED);
        }
        return ApiResponse.ok(authService.demoLogin(demoDataService.email()));
    }

    @PostMapping("/register")
    @Operation(summary = "Cria uma conta e ja devolve os tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica por e-mail e senha")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotaciona o refresh token e emite um novo access token")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga o refresh token informado")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok();
    }
}
