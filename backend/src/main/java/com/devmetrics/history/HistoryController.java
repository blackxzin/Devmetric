package com.devmetrics.history;

import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.history.dto.MonthlyHistoryResponse;
import com.devmetrics.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
@Tag(name = "Historico", description = "Evolucao mes a mes")
public class HistoryController {

    private final HistoryService historyService;
    private final UserService userService;

    public HistoryController(HistoryService historyService, UserService userService) {
        this.historyService = historyService;
        this.userService = userService;
    }

    @GetMapping("/monthly")
    @Operation(summary = "Serie mensal de atividade, pontos, score, tecnologias e desafios")
    public ApiResponse<MonthlyHistoryResponse> monthly(@AuthenticationPrincipal AuthenticatedUser principal,
                                                       @RequestParam(required = false) Integer year) {
        return ApiResponse.ok(historyService.monthly(userService.requireUser(principal.id()), year));
    }
}
