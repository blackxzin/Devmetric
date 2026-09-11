package com.devmetrics.activity.domain;

/**
 * Tipos de atividade que o DevMetrics reconhece.
 * Persistido como texto (EnumType.STRING): reordenar o enum nunca pode corromper o historico.
 */
public enum ActivityType {

    COMMIT("Commit"),
    PULL_REQUEST("Pull Request"),
    ISSUE("Issue"),
    FEATURE("Feature"),
    BUG_FIX("Bug corrigido"),
    TEST("Teste"),
    DOCUMENTATION("Documentacao"),
    STUDY("Estudo"),
    NEW_PROJECT("Projeto novo"),
    REFACTOR("Refatoracao"),
    CODE_REVIEW("Code review"),
    DEPLOY("Deploy");

    private final String label;

    ActivityType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
