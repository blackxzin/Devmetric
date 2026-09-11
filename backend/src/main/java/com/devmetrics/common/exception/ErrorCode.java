package com.devmetrics.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dados invalidos"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Autenticacao necessaria"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token invalido ou expirado"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Acesso negado"),
    EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "E-mail ja cadastrado"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Usuario nao encontrado"),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "Projeto nao encontrado"),
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "Atividade nao encontrada"),
    TECHNOLOGY_NOT_FOUND(HttpStatus.NOT_FOUND, "Tecnologia nao encontrada"),
    CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Desafio nao encontrado"),
    ACHIEVEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Conquista nao encontrada"),
    READ_ONLY_RESOURCE(HttpStatus.CONFLICT, "Recurso importado do GitHub e somente leitura"),
    GITHUB_NOT_CONNECTED(HttpStatus.CONFLICT, "Conta do GitHub nao conectada"),
    GITHUB_ALREADY_CONNECTED(HttpStatus.CONFLICT, "Conta do GitHub ja conectada"),
    GITHUB_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "Integracao com GitHub nao configurada no servidor"),
    GITHUB_OAUTH_ERROR(HttpStatus.BAD_REQUEST, "Falha na autorizacao do GitHub"),
    GITHUB_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Limite de requisicoes do GitHub atingido"),
    SYNC_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "Aguarde antes de sincronizar novamente"),
    SYNC_NOT_FOUND(HttpStatus.NOT_FOUND, "Sincronizacao nao encontrada"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
