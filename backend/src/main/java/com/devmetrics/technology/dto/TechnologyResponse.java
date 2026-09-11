package com.devmetrics.technology.dto;

import com.devmetrics.technology.domain.Technology;

public record TechnologyResponse(Long id, String name, String slug, String category) {

    public static TechnologyResponse from(Technology technology) {
        return new TechnologyResponse(technology.getId(), technology.getName(),
                technology.getSlug(), technology.getCategory().name());
    }
}
