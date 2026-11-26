package com.itson.notificarservice.eventos;

import com.itson.escuela.common.eventos.DatosEventos.AlertaGenerada;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.mensajeria.PublicadorEventos;
import com.itson.escuela.common.mensajeria.RabbitConfig;
import com.itson.notificarservice.modelo.AlertaRendimiento;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PublicadorAlertas {

    private static final Logger LOG = Logger.getLogger(PublicadorAlertas.class);
    static final String ORIGEN = "notificar-service";

    @ConfigProperty(name = "notificar.publicador.habilitado", defaultValue = "true")
    boolean habilitado;

    private PublicadorEventos publicador;

    public synchronized void publicar(List<AlertaRendimiento> alertas) {
        if (!habilitado || alertas.isEmpty()) {
            return;
        }
        for (AlertaRendimiento alerta : alertas) {
            AlertaGenerada datos = new AlertaGenerada(alerta.idAlerta, alerta.tipo, alerta.matricula,
                    alerta.idTutor, alerta.mensaje, alerta.fechaEmision);
            try {
                conexion().publicar(RoutingKeys.ALERTA_GENERADA, datos);
            } catch (Exception e) {
                LOG.warnf("No se pudo publicar alerta.generada %s para %s: %s",
                        alerta.idAlerta, alerta.idTutor, e.getMessage());
                cerrar();
            }
        }
    }

    private PublicadorEventos conexion() throws IOException, TimeoutException {
        if (publicador == null) {
            publicador = new PublicadorEventos(RabbitConfig.desdeEntorno(), ORIGEN);
        }
        return publicador;
    }

    private void cerrar() {
        if (publicador != null) {
            try {
                publicador.close();
            } catch (Exception e) {
                LOG.debugf("Error al cerrar el publicador: %s", e.getMessage());
            }
            publicador = null;
        }
    }

    synchronized void alApagar(@Observes ShutdownEvent evento) {
        cerrar();
    }
}
