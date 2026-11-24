package com.itson.escuela.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itson.escuela.common.eventos.Evento;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Conversion a JSON y desde JSON con la misma configuracion en todos los servicios:
 * fechas en formato ISO-8601 y campos desconocidos ignorados.
 */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private Json() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String escribir(Object valor) {
        try {
            return MAPPER.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo convertir a JSON: " + valor, e);
        }
    }

    public static <T> T leer(String json, Class<T> clase) {
        try {
            return MAPPER.readValue(json, clase);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON invalido para " + clase.getSimpleName() + ": " + json, e);
        }
    }

    public static <T> T leer(InputStream json, Class<T> clase) {
        try {
            return MAPPER.readValue(json, clase);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer JSON para " + clase.getSimpleName(), e);
        }
    }

    /**
     * Lee un evento completo indicando la clase de sus datos.
     * Ejemplo: {@code Json.leerEvento(json, DatosEventos.TareaVencida.class)}
     */
    public static <T> Evento<T> leerEvento(String json, Class<T> claseDatos) {
        JavaType tipo = MAPPER.getTypeFactory().constructParametricType(Evento.class, claseDatos);
        try {
            return MAPPER.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Evento invalido para " + claseDatos.getSimpleName() + ": " + json, e);
        }
    }
}
