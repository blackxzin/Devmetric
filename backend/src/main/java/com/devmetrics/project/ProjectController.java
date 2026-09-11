package com.devmetrics.project;

import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.common.response.PageMeta;
import com.devmetrics.project.dto.CreateProjectRequest;
import com.devmetrics.project.dto.ProjectResponse;
import com.devmetrics.project.dto.SetTechnologiesRequest;
import com.devmetrics.project.dto.UpdateProjectRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projetos", description = "Projetos do usuario, manuais ou importados do GitHub")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "Lista os projetos do usuario")
    public ApiResponse<List<ProjectResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "active") String status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProjectResponse> page = projectService.list(principal.id(),
                "active".equalsIgnoreCase(status), pageable);
        return ApiResponse.ok(page.getContent(), PageMeta.from(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do projeto com totais de atividade")
    public ApiResponse<ProjectResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @PathVariable Long id) {
        return ApiResponse.ok(projectService.get(principal.id(), id));
    }

    @PostMapping
    @Operation(summary = "Cria um projeto e registra a atividade de projeto novo")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(projectService.create(principal.id(), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um projeto manual")
    public ApiResponse<ProjectResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable Long id,
                                               @Valid @RequestBody UpdateProjectRequest request) {
        return ApiResponse.ok(projectService.update(principal.id(), id, request));
    }

    @PutMapping("/{id}/technologies")
    @Operation(summary = "Define as tecnologias do projeto")
    public ApiResponse<ProjectResponse> setTechnologies(@AuthenticationPrincipal AuthenticatedUser principal,
                                                        @PathVariable Long id,
                                                        @Valid @RequestBody SetTechnologiesRequest request) {
        return ApiResponse.ok(projectService.setTechnologies(principal.id(), id, request));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Arquiva o projeto")
    public ApiResponse<ProjectResponse> archive(@AuthenticationPrincipal AuthenticatedUser principal,
                                                @PathVariable Long id) {
        return ApiResponse.ok(projectService.archive(principal.id(), id));
    }

    @PostMapping("/{id}/unarchive")
    @Operation(summary = "Reativa o projeto")
    public ApiResponse<ProjectResponse> unarchive(@AuthenticationPrincipal AuthenticatedUser principal,
                                                  @PathVariable Long id) {
        return ApiResponse.ok(projectService.unarchive(principal.id(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um projeto manual")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal,
                                       @PathVariable Long id) {
        projectService.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
