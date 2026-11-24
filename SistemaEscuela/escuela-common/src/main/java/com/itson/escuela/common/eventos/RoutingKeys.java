package com.itson.escuela.common.eventos;

/**
 * Nombres del exchange y de las routing keys del broker.
 * Usa siempre estas constantes en lugar de escribir el texto a mano.
 * Detalle de cada evento en docs/contrato-eventos.md.
 */
public final class RoutingKeys {

    /** Exchange de tipo topic donde se publican todos los eventos. */
    public static final String EXCHANGE = "escuela.eventos";

    /** Integration: llego una calificacion nueva desde Moodle. */
    public static final String CALIFICACION_REGISTRADA = "calificacion.registrada";
    /** Integration: paso la fecha limite y el alumno no entrego. */
    public static final String TAREA_VENCIDA = "tarea.vencida";
    /** Integration: una entrega necesita el aval del padre. */
    public static final String ENTREGA_REQUIERE_AVAL = "entrega.requiereAval";
    /** Aval: la solicitud quedo en PENDIENTE_AVAL. */
    public static final String AVAL_SOLICITADO = "aval.solicitado";
    /** Aval: el tutor aprobo o rechazo. */
    public static final String AVAL_RESUELTO = "aval.resuelto";
    /** Notificar: se creo una AlertaRendimiento. */
    public static final String ALERTA_GENERADA = "alerta.generada";

    private RoutingKeys() {
    }
}
