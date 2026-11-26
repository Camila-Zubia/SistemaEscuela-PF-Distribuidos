package com.itson.integrationservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itson.escuela.common.eventos.DatosEventos;
import com.itson.escuela.common.eventos.Evento;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.json.Json;
import com.itson.escuela.common.modelo.EstadoEntrega;
import com.itson.integrationservice.servicio.GestorEventosRabbit;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias para el manejo de eventos entrantes en GestorEventosRabbit.
 */
class GestorEventosRabbitTest {

    private GestorEventosRabbit gestor;

    @BeforeEach
    void setUp() {
        gestor = new GestorEventosRabbit();
    }

    @Test
    void procesaEventoAvalResueltoAprobadoYRechazado() {
        Evento<DatosEventos.AvalResuelto> ev1 = Evento.nuevo(
                RoutingKeys.AVAL_RESUELTO,
                "aval-service",
                new DatosEventos.AvalResuelto("ENT-101", "A001", "T001", EstadoEntrega.AVALADA, Instant.now())
        );
        Evento<DatosEventos.AvalResuelto> ev2 = Evento.nuevo(
                RoutingKeys.AVAL_RESUELTO,
                "aval-service",
                new DatosEventos.AvalResuelto("ENT-103", "A003", "T001", EstadoEntrega.RECHAZADA_POR_TUTOR, Instant.now())
        );

        gestor.procesarEventoRecibido(RoutingKeys.AVAL_RESUELTO, Json.escribir(ev1));
        gestor.procesarEventoRecibido(RoutingKeys.AVAL_RESUELTO, Json.escribir(ev2));

        assertEquals(2, gestor.getAvalesResueltos().size());
        assertEquals(EstadoEntrega.AVALADA, gestor.getAvalesResueltos().get(0).estado());
        assertEquals(EstadoEntrega.RECHAZADA_POR_TUTOR, gestor.getAvalesResueltos().get(1).estado());
    }

    @Test
    void ignoraEventosConRoutingKeyDesconocidaOJsonInvalido() {
        gestor.procesarEventoRecibido("otro.evento", "{}");
        gestor.procesarEventoRecibido(RoutingKeys.AVAL_RESUELTO, "{json-roto");

        assertTrue(gestor.getAvalesResueltos().isEmpty());
    }
}
