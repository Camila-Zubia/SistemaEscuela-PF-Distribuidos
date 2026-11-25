package com.itson.servicioaval;

import com.itson.escuela.common.eventos.DatosEventos;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.json.Json;
import com.itson.escuela.common.mensajeria.ConsumidorEventos;
import com.itson.escuela.common.mensajeria.PublicadorEventos;
import com.itson.escuela.common.mensajeria.RabbitConfig;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.jboss.logging.Logger;

@Startup
@ApplicationScoped
public class AvalMensajeriaService {
    private static final Logger LOG = Logger.getLogger(AvalMensajeriaService.class);

    @Inject
    RegistroEventosPanache registro;
    private PublicadorEventos publicador;
    private ConsumidorEventos consumidor;

    @PostConstruct
    void iniciar() {
        try {
            var config = RabbitConfig.desdeEntorno();
            publicador = new PublicadorEventos(config, "aval-service");
            consumidor = new ConsumidorEventos(config, "aval.eventos",
                    List.of(RoutingKeys.ENTREGA_REQUIERE_AVAL), registro);
            consumidor.iniciar((clave, json) -> registrarSolicitudYNotificar(
                    Json.leerEvento(json, DatosEventos.EntregaRequiereAval.class).datos()));
        } catch (Exception e) {
            cerrar();
            throw new IllegalStateException("No se pudo conectar AvalService a RabbitMQ", e);
        }
    }

    @Transactional(rollbackOn = IOException.class)
    void registrarSolicitudYNotificar(DatosEventos.EntregaRequiereAval datos) throws IOException {
        if (SolicitudAval.findById(datos.idEntrega()) != null) {
            return;
        }
        new SolicitudAval(datos.idEntrega(), datos.matricula(), datos.idTarea(), datos.idCurso()).persistAndFlush();
        publicador.publicar(RoutingKeys.AVAL_SOLICITADO,
                new DatosEventos.AvalSolicitado(datos.idEntrega(), datos.idTarea(), datos.matricula()));
    }

    public void publicarAvalResuelto(SolicitudAval solicitud) {
        try {
            publicador.publicar(RoutingKeys.AVAL_RESUELTO, new DatosEventos.AvalResuelto(
                    solicitud.idEntrega, solicitud.matriculaAlumno, solicitud.idTutor,
                    solicitud.estado, solicitud.fechaResolucion));
        } catch (IOException e) {
            throw new UncheckedIOException("Error al publicar aval.resuelto", e);
        }
    }

    @PreDestroy
    void cerrar() {
        cerrar(consumidor);
        cerrar(publicador);
    }

    private static void cerrar(AutoCloseable recurso) {
        if (recurso != null) {
            try {
                recurso.close();
            } catch (Exception e) {
                LOG.warn("Error al cerrar la conexión con RabbitMQ", e);
            }
        }
    }
}
