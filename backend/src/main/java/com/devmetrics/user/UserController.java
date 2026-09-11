package com.devmetrics.user;

import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.user.dto.ChangePasswordRequest;
import com.devmetrics.user.dto.UpdateUserRequest;
import com.devmetrics.user.dto.UserProfileResponse;
import com.devmetrics.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "Usuario", description = "Perfil do usuario autenticado")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Perfil do usuario autenticado")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(userService.profile(principal.id()));
    }

    @PatchMapping
    @Operation(summary = "Atualiza nome, fuso horario e meta semanal")
    public ApiResponse<UserResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userService.update(principal.id(), request));
    }

    @PutMapping("/password")
    @Operation(summary = "Troca a senha")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.id(), request);
        return ApiResponse.ok();
    }
}
