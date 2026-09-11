package com.devmetrics.github;

import com.devmetrics.technology.domain.TechnologyCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Descobre tecnologias a partir do que o GitHub entrega: linguagens do repositorio
 * e arquivos-marcador na raiz (pom.xml, Dockerfile, package.json...).
 *
 * E o que permite ao sistema saber que o usuario "nunca usou Docker" sem que
 * ninguem precise digitar isso.
 */
@Component
public class TechnologyDetector {

    public record DetectedTechnology(String name, TechnologyCategory category) {
    }

    private static final Map<String, DetectedTechnology> FILE_MARKERS = new LinkedHashMap<>();

    static {
        FILE_MARKERS.put("pom.xml", new DetectedTechnology("Maven", TechnologyCategory.TOOL));
        FILE_MARKERS.put("build.gradle", new DetectedTechnology("Gradle", TechnologyCategory.TOOL));
        FILE_MARKERS.put("build.gradle.kts", new DetectedTechnology("Gradle", TechnologyCategory.TOOL));
        FILE_MARKERS.put("dockerfile", new DetectedTechnology("Docker", TechnologyCategory.TOOL));
        FILE_MARKERS.put("docker-compose.yml", new DetectedTechnology("Docker Compose", TechnologyCategory.TOOL));
        FILE_MARKERS.put("docker-compose.yaml", new DetectedTechnology("Docker Compose", TechnologyCategory.TOOL));
        FILE_MARKERS.put("package.json", new DetectedTechnology("Node.js", TechnologyCategory.FRAMEWORK));
        FILE_MARKERS.put("requirements.txt", new DetectedTechnology("pip", TechnologyCategory.TOOL));
        FILE_MARKERS.put("pyproject.toml", new DetectedTechnology("Poetry", TechnologyCategory.TOOL));
        FILE_MARKERS.put("go.mod", new DetectedTechnology("Go Modules", TechnologyCategory.TOOL));
        FILE_MARKERS.put("cargo.toml", new DetectedTechnology("Cargo", TechnologyCategory.TOOL));
        FILE_MARKERS.put("composer.json", new DetectedTechnology("Composer", TechnologyCategory.TOOL));
        FILE_MARKERS.put("gemfile", new DetectedTechnology("Bundler", TechnologyCategory.TOOL));
        FILE_MARKERS.put("makefile", new DetectedTechnology("Make", TechnologyCategory.TOOL));
        FILE_MARKERS.put("vercel.json", new DetectedTechnology("Vercel", TechnologyCategory.CLOUD));
        FILE_MARKERS.put("terraform.tf", new DetectedTechnology("Terraform", TechnologyCategory.CLOUD));
        FILE_MARKERS.put("kubernetes.yml", new DetectedTechnology("Kubernetes", TechnologyCategory.CLOUD));
    }

    private static final Map<String, TechnologyCategory> LANGUAGE_OVERRIDES = Map.of(
            "html", TechnologyCategory.LANGUAGE,
            "css", TechnologyCategory.LANGUAGE,
            "dockerfile", TechnologyCategory.TOOL,
            "shell", TechnologyCategory.TOOL,
            "makefile", TechnologyCategory.TOOL,
            "tsql", TechnologyCategory.DATABASE,
            "plpgsql", TechnologyCategory.DATABASE
    );

    public List<DetectedTechnology> fromLanguages(Map<String, Long> languages) {
        List<DetectedTechnology> detected = new ArrayList<>();
        if (languages == null) {
            return detected;
        }
        languages.forEach((language, bytes) -> {
            TechnologyCategory category = LANGUAGE_OVERRIDES
                    .getOrDefault(language.toLowerCase(Locale.ROOT), TechnologyCategory.LANGUAGE);
            detected.add(new DetectedTechnology(language, category));
        });
        return detected;
    }

    public List<DetectedTechnology> fromRootFiles(List<String> fileNames) {
        List<DetectedTechnology> detected = new ArrayList<>();
        if (fileNames == null) {
            return detected;
        }
        for (String fileName : fileNames) {
            DetectedTechnology technology = FILE_MARKERS.get(fileName.toLowerCase(Locale.ROOT));
            if (technology != null && !detected.contains(technology)) {
                detected.add(technology);
            }
        }
        return detected;
    }
}
