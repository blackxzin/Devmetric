package com.devmetrics.profile;

import com.devmetrics.common.response.ApiResponse;
import com.devmetrics.profile.dto.PublicProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/public/u/{username}")
@Tag(name = "Perfil publico", description = "Perfil e badge sem autenticacao (so para quem ativou)")
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    public PublicProfileController(PublicProfileService publicProfileService) {
        this.publicProfileService = publicProfileService;
    }

    @GetMapping
    @Operation(summary = "Perfil publico: score, streak, tecnologias, conquistas e calendario")
    public ApiResponse<PublicProfileResponse> profile(@PathVariable String username) {
        return ApiResponse.ok(publicProfileService.profile(username));
    }

    @GetMapping("/badge.svg")
    @Operation(summary = "Card SVG para README do GitHub")
    public ResponseEntity<String> badge(@PathVariable String username) {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(publicProfileService.badge(username));
    }
}
