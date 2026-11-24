package com.itson.mensajeriaservice.modelo;

public enum EstadoMensaje {
    EN_COLA,
    ENVIADO,
    ENTREGADO,
    LEIDO,
    FALLIDO;

    /** Los estados solo avanzan: un mensaje LEIDO no puede regresar a ENTREGADO. */
    public boolean esAnteriorA(EstadoMensaje otro) {
        return this.ordinal() < otro.ordinal();
    }
}
