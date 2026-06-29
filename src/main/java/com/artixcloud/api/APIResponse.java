package com.artixcloud.api;

/**
 * Wrapper genérico para respostas da API ArtixCloud.
 *
 * @param <T> tipo do payload de dados
 */
public class APIResponse<T> {

    private final boolean success;
    private final T data;
    private final String errorMessage;
    private final int statusCode;

    private APIResponse(boolean success, T data, String errorMessage, int statusCode) {
        this.success      = success;
        this.data         = data;
        this.errorMessage = errorMessage;
        this.statusCode   = statusCode;
    }

    public static <T> APIResponse<T> success(T data, int statusCode) {
        return new APIResponse<>(true, data, null, statusCode);
    }

    public static <T> APIResponse<T> failure(String errorMessage, int statusCode) {
        return new APIResponse<>(false, null, errorMessage, statusCode);
    }

    public boolean isSuccess()         { return success; }
    public T getData()                 { return data; }
    public String getErrorMessage()    { return errorMessage; }
    public int getStatusCode()         { return statusCode; }

    /** Retorna true se a falha foi por falta de autenticação. */
    public boolean isUnauthorized()    { return statusCode == 401 || statusCode == 403; }

    /** Retorna true se o recurso não foi encontrado. */
    public boolean isNotFound()        { return statusCode == 404; }

    /** Retorna true se houve erro interno na API. */
    public boolean isServerError()     { return statusCode >= 500; }

    @Override
    public String toString() {
        return "APIResponse{success=" + success + ", statusCode=" + statusCode
                + (success ? ", data=" + data : ", error=" + errorMessage) + "}";
    }
}
