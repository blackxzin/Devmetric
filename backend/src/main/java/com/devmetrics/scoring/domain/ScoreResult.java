package com.devmetrics.scoring.domain;

import java.util.List;

public record ScoreResult(int devScore, String level, List<ScoreComponent> components) {

    public static String levelFor(int devScore) {
        if (devScore >= 800) {
            return "Em evolucao continua";
        }
        if (devScore >= 600) {
            return "Consistente";
        }
        if (devScore >= 400) {
            return "Ativo";
        }
        if (devScore >= 200) {
            return "Em construcao";
        }
        return "Iniciando";
    }
}
