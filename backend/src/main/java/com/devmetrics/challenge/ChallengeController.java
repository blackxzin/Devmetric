package com.devmetrics.challenge;

import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.challenge.dto.ChallengeResponse;
import com.devmetrics.challenge.dto.CompleteChallengeRequest;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.common.response.PageMeta;
import com.devmetrics.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/challenges")
@Tag(name = "Desafios", description = "Desafio de Hoje: anti-procrastinacao")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final UserService userService;

    public ChallengeController(ChallengeService challengeService, UserService userService) {
        this.challengeService = challengeService;
        this.userService = userService;
    }

    @GetMapping("/today")
    @Operation(summary = "Desafio de hoje, gerado a partir da atividade recente")
    public ApiResponse<ChallengeResponse> today(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(challengeService.today(userService.requireUser(principal.id())));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Marca o desafio como concluido")
    public ApiResponse<ChallengeResponse> complete(@AuthenticationPrincipal AuthenticatedUser principal,
                                                   @PathVariable Long id,
                                                   @RequestBody(required = false)
                                                   CompleteChallengeRequest request) {
        Long activityId = request == null ? null : request.activityId();
        return ApiResponse.ok(challengeService.complete(
                userService.requireUser(principal.id()), id, activityId));
    }

    @PostMapping("/{id}/skip")
    @Operation(summary = "Pula o desafio e sorteia outro para o mesmo dia")
    public ApiResponse<ChallengeResponse> skip(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable Long id) {
        return ApiResponse.ok(challengeService.skip(userService.requireUser(principal.id()), id));
    }

    @GetMapping("/history")
    @Operation(summary = "Desafios anteriores")
    public ApiResponse<List<ChallengeResponse>> history(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ChallengeResponse> page = challengeService.history(principal.id(), pageable);
        return ApiResponse.ok(page.getContent(), PageMeta.from(page));
    }
}
