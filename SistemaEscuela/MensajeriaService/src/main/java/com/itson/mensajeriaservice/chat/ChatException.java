package com.itson.mensajeriaservice.chat;

public class ChatException extends RuntimeException {

    private final String codigo;
    private final int http;

    private ChatException(String codigo, int http, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.http = http;
    }

    public static ChatException datosInvalidos(String mensaje) {
        return new ChatException("DATOS_INVALIDOS", 400, mensaje);
    }

    public static ChatException prohibido(String mensaje) {
        return new ChatException("PROHIBIDO", 403, mensaje);
    }

    public static ChatException noEncontrado(String mensaje) {
        return new ChatException("NO_ENCONTRADO", 404, mensaje);
    }

    public String codigo() {
        return codigo;
    }

    public int http() {
        return http;
    }
}
