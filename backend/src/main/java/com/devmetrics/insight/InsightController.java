package com.devmetrics.insight;

import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.insight.dto.NextStepResponse;
import com.devmetrics.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
@Tag(name = "Insights", description = "Proximo Passo: descoberta de novas tecnologias")
public class InsightController {

    private final NextStepService nextStepService;
    private final UserService userService;

    public InsightController(NextStepService nextStepService, UserService userService) {
        this.nextStepService = nextStepService;
        this.userService = userService;
    }

    @GetMapping("/next-step")
    @Operation(summary = "Sugestao de proxima tecnologia com justificativa e primeiro passo")
    public ApiResponse<NextStepResponse> nextStep(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(nextStepService.nextStep(userService.requireUser(principal.id())));
    }
}
