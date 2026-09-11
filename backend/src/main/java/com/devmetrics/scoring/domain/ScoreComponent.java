package com.devmetrics.scoring.domain;

/**
 * Um componente do Dev Score, sempre exposto na API.
 * O usuario precisa conseguir responder "por que meu score caiu?" sem ler o codigo.
 */
public record ScoreComponent(
        String component,
        String label,
        double weight,
        double raw,
        double normalized,
        int points,
        int maxPoints,
        String explanation
) {
}
