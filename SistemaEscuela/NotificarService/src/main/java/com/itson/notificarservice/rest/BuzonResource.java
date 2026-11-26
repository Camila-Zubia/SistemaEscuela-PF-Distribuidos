package com.itson.notificarservice.rest;

import com.itson.escuela.common.modelo.TipoAlerta;
import com.itson.notificarservice.modelo.AlertaRendimiento;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;

@Path("/api/notificar/buzon")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BuzonResource {

    @GET
    @Path("/{idTutor}")
    public List<AlertaDto> buzon(@PathParam("idTutor") String idTutor,
            @QueryParam("matricula") String matricula,
            @QueryParam("soloNoLeidas") Boolean soloNoLeidas) {
        return AlertaRendimiento.buzon(idTutor, matricula, Boolean.TRUE.equals(soloNoLeidas)).stream()
                .map(AlertaDto::de)
                .toList();
    }

    @PATCH
    @Path("/{idAlerta}")
    @Transactional
    public Response marcarLeida(@PathParam("idAlerta") String idAlerta, CambioLeida cambio) {
        if (cambio == null || cambio.leida() == null) {
            return error(Response.Status.BAD_REQUEST, "CAMPOS_INVALIDOS", "Falta el campo leida");
        }
        AlertaRendimiento alerta = AlertaRendimiento.findById(idAlerta);
        if (alerta == null) {
            return error(Response.Status.NOT_FOUND, "ALERTA_NO_ENCONTRADA", "No existe la alerta " + idAlerta);
        }
        alerta.leida = cambio.leida();
        return Response.ok(AlertaDto.de(alerta)).build();
    }

    @GET
    @Path("/{idTutor}/no-leidas/total")
    public TotalNoLeidas totalNoLeidas(@PathParam("idTutor") String idTutor) {
        return new TotalNoLeidas(idTutor, AlertaRendimiento.contarNoLeidas(idTutor));
    }

    private static Response error(Response.Status estado, String codigo, String mensaje) {
        return Response.status(estado).entity(new ErrorApi(codigo, mensaje, Instant.now())).build();
    }

    public record AlertaDto(String idAlerta, TipoAlerta tipo, String matricula, String mensaje,
            Instant fechaEmision, boolean leida) {
        static AlertaDto de(AlertaRendimiento a) {
            return new AlertaDto(a.idAlerta, a.tipo, a.matricula, a.mensaje, a.fechaEmision, a.leida);
        }
    }

    public record CambioLeida(Boolean leida) {
    }

    public record TotalNoLeidas(String idTutor, long noLeidas) {
    }

    public record ErrorApi(String error, String mensaje, Instant fecha) {
    }
}
