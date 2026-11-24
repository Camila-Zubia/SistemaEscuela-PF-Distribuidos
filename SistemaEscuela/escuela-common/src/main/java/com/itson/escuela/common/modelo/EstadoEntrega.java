package com.itson.escuela.common.modelo;

/** Estados de una entrega de tarea (diagrama de clases: EstadoEntrega). */
public enum EstadoEntrega {
    PENDIENTE,
    ENTREGA_EN_TIEMPO,
    ENTREGA_TARDIA,
    PENDIENTE_AVAL,
    AVALADA,
    RECHAZADA_POR_TUTOR,
    CALIFICADA
}
