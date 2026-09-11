package com.devmetrics.scoring;

import com.devmetrics.activity.ActivityService;
import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.scoring.dto.ScoreHistoryPoint;
import com.devmetrics.scoring.dto.ScoreResponse;
import com.devmetrics.scoring.dto.ScoringRuleResponse;
import com.devmetrics.scoring.dto.UpdateScoringRuleRequest;
import com.devmetrics.user.UserService;
import com.devmetrics.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Pontuacao", description = "Regras de pontuacao e Dev Score")
public class ScoringController {

    private final ScoringRuleService scoringRuleService;
    private final ScoreService scoreService;
    private final ActivityService activityService;
    private final UserService userService;

    public ScoringController(ScoringRuleService scoringRuleService,
                             ScoreService scoreService,
                             ActivityService activityService,
                             UserService userService) {
        this.scoringRuleService = scoringRuleService;
        this.scoreService = scoreService;
        this.activityService = activityService;
        this.userService = userService;
    }

    @GetMapping("/scoring/rules")
    @Operation(summary = "Regras efetivas de pontuacao (padrao global + override do usuario)")
    public ApiResponse<List<ScoringRuleResponse>> rules(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(scoringRuleService.list(principal.id()));
    }

    @PutMapping("/scoring/rules/{activityType}")
    @Operation(summary = "Cria ou atualiza o override de pontuacao do usuario")
    public ApiResponse<ScoringRuleResponse> updateRule(@AuthenticationPrincipal AuthenticatedUser principal,
                                                       @PathVariable ActivityType activityType,
                                                       @Valid @RequestBody UpdateScoringRuleRequest request) {
        return ApiResponse.ok(scoringRuleService.update(principal.id(), activityType, request));
    }

    @DeleteMapping("/scoring/rules/{activityType}")
    @Operation(summary = "Remove o override e volta para a regra padrao")
    public ApiResponse<ScoringRuleResponse> resetRule(@AuthenticationPrincipal AuthenticatedUser principal,
                                                      @PathVariable ActivityType activityType) {
        return ApiResponse.ok(scoringRuleService.reset(principal.id(), activityType));
    }

    @PostMapping("/scoring/recalculate")
    @Operation(summary = "Reprocessa os pontos das atividades do periodo com as regras atuais")
    public ApiResponse<Map<String, Object>> recalculate(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        User user = userService.requireUser(principal.id());
        LocalDate today = LocalDate.now(user.zoneId());
        LocalDate start = from == null ? today.minusDays(365) : from;
        LocalDate end = to == null ? today : to;
        int processed = activityService.recalculate(user, start, end);
        scoreService.snapshot(user, today);
        return ApiResponse.ok(Map.of("processed", processed, "from", start, "to", end));
    }

    @GetMapping("/score")
    @Operation(summary = "Dev Score atual com o breakdown de cada componente")
    public ApiResponse<ScoreResponse> score(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(scoreService.current(userService.requireUser(principal.id())));
    }

    @GetMapping("/score/history")
    @Operation(summary = "Serie historica do Dev Score")
    public ApiResponse<List<ScoreHistoryPoint>> history(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        User user = userService.requireUser(principal.id());
        LocalDate today = LocalDate.now(user.zoneId());
        return ApiResponse.ok(scoreService.history(principal.id(),
                from == null ? today.minusDays(180) : from,
                to == null ? today : to));
    }
}
