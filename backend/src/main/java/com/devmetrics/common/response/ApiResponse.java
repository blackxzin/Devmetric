package com.devmetrics.common.response;

/**
 * Envelope unico de resposta da API. Toda rota devolve este formato.
 */
public record ApiResponse<T>(boolean success, T data, ApiError error, PageMeta meta) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, PageMeta meta) {
        return new ApiResponse<>(true, data, null, meta);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(false, null, error, null);
    }
}
