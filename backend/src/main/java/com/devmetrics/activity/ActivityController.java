package com.devmetrics.activity;

import com.devmetrics.activity.domain.ActivitySource;
import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.activity.dto.ActivityResponse;
import com.devmetrics.activity.dto.ActivityTypeInfo;
import com.devmetrics.activity.dto.CreateActivityRequest;
import com.devmetrics.activity.dto.UpdateActivityRequest;
import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.common.response.PageMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/v1/activities")
@Tag(name = "Atividades", description = "Registro e consulta das atividades de desenvolvimento")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    @Operation(summary = "Lista atividades do usuario com filtros e paginacao")
    public ApiResponse<List<ActivityResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) ActivitySource source,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ActivityResponse> page = activityService.search(principal.id(), type, source,
                projectId, from, to, pageable);
        return ApiResponse.ok(page.getContent(), PageMeta.from(page));
    }

    @GetMapping("/types")
    @Operation(summary = "Tipos de atividade disponiveis")
    public ApiResponse<List<ActivityTypeInfo>> types() {
        return ApiResponse.ok(ActivityTypeInfo.all());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma atividade")
    public ApiResponse<ActivityResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @PathVariable Long id) {
        return ApiResponse.ok(activityService.get(principal.id(), id));
    }

    @PostMapping
    @Operation(summary = "Registra uma atividade manual e ja calcula os pontos")
    public ResponseEntity<ApiResponse<ActivityResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(activityService.create(principal.id(), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita uma atividade e reprocessa os pontos do periodo afetado")
    public ApiResponse<ActivityResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @PathVariable Long id,
                                                @Valid @RequestBody UpdateActivityRequest request) {
        return ApiResponse.ok(activityService.update(principal.id(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma atividade manual")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @PathVariable Long id) {
        activityService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
