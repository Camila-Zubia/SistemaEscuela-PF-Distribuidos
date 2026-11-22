/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.itson.integrationservice;

import java.time.Instant;
import java.util.UUID;

/**
 *
 * @author Usuario
 */
public class IntegrationService {

    private final EventPublisher eventPublisher;

    public IntegrationService(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void sincronizarCalificacionesPeriodicas() {
        String idReporte = "REP-" + UUID.randomUUID().toString().substring(0, 8);
        boolean huboRetardo = false;

        String payload = String.format(
            "{\"idReporte\":\"%s\",\"fechaEnvio\":\"%s\",\"tuvoRetardo\":%s}",
            idReporte, Instant.now(), huboRetardo
        );
        eventPublisher.publicar("calificaciones.sincronizadas", payload);
    }

    public interface EventPublisher {
        void publicar(String topico, String payloadJson);
    }
}
