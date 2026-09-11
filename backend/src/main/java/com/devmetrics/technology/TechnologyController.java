package com.devmetrics.technology;

import com.devmetrics.auth.AuthenticatedUser;
import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.technology.domain.TechnologyCategory;
import com.devmetrics.technology.dto.TechnologyResponse;
import com.devmetrics.technology.dto.UserTechnologyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Tecnologias", description = "Catalogo global e tecnologias do usuario")
public class TechnologyController {

    private final TechnologyService technologyService;

    public TechnologyController(TechnologyService technologyService) {
        this.technologyService = technologyService;
    }

    @GetMapping("/technologies")
    @Operation(summary = "Catalogo de tecnologias, com busca por nome ou categoria")
    public ApiResponse<List<TechnologyResponse>> catalog(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TechnologyCategory category) {
        return ApiResponse.ok(technologyService.search(q, category));
    }

    @GetMapping("/users/me/technologies")
    @Operation(summary = "Tecnologias ja usadas pelo usuario, com data do primeiro uso")
    public ApiResponse<List<UserTechnologyResponse>> mine(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(technologyService.listUserTechnologies(principal.id()));
    }
}
