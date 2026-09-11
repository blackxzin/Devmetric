package com.devmetrics.achievement;

import com.devmetrics.achievement.dto.AchievementResponse;
import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Conquistas", description = "Catalogo de conquistas e progresso do usuario")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping("/achievements")
    @Operation(summary = "Catalogo completo com o progresso do usuario em cada conquista")
    public ApiResponse<List<AchievementResponse>> catalog(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(achievementService.catalog(principal.id()));
    }

    @GetMapping("/users/me/achievements")
    @Operation(summary = "Conquistas ja desbloqueadas")
    public ApiResponse<List<AchievementResponse>> mine(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(achievementService.unlocked(principal.id()));
    }
}
