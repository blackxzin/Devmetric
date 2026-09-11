package com.devmetrics.dashboard;

import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.dashboard.dto.ActivityBreakdownItem;
import com.devmetrics.dashboard.dto.CalendarResponse;
import com.devmetrics.dashboard.dto.DashboardSummary;
import com.devmetrics.dashboard.dto.StreakInfo;
import com.devmetrics.user.UserService;
import com.devmetrics.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Agregacoes de leitura para a tela principal")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumo completo do dashboard")
    public ApiResponse<DashboardSummary> summary(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(dashboardService.summary(userService.requireUser(principal.id())));
    }

    @GetMapping("/calendar")
    @Operation(summary = "Calendario anual de atividade com nivel de intensidade por dia")
    public ApiResponse<CalendarResponse> calendar(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @RequestParam(required = false) Integer year) {
        return ApiResponse.ok(dashboardService.calendar(userService.requireUser(principal.id()), year));
    }

    @GetMapping("/streak")
    @Operation(summary = "Sequencia de dias ativos")
    public ApiResponse<StreakInfo> streak(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(dashboardService.streak(userService.requireUser(principal.id())));
    }

    @GetMapping("/activity-breakdown")
    @Operation(summary = "Distribuicao de atividades por tipo no periodo")
    public ApiResponse<List<ActivityBreakdownItem>> breakdown(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "30") int days) {
        User user = userService.requireUser(principal.id());
        LocalDate today = LocalDate.now(user.zoneId());
        int window = Math.max(1, Math.min(365, days));
        return ApiResponse.ok(dashboardService.breakdown(user.getId(), today.minusDays(window - 1L), today));
    }
}
