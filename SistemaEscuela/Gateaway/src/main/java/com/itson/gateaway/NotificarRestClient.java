/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.gateaway;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 *
 * @author Usuario
 */
@RegisterRestClient(configKey = "notificar")
@Path("/api/notificar")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public interface NotificarRestClient {

    @GET
    @Path("/buzon/{idTutor}")
    Response buzon(@PathParam("idTutor") String idTutor,
            @QueryParam("matricula") String matricula,
            @QueryParam("soloNoLeidas") Boolean soloNoLeidas);

    @PATCH
    @Path("/buzon/{idAlerta}")
    Response marcarLeida(@PathParam("idAlerta") String idAlerta, String body);

    @GET
    @Path("/buzon/{idTutor}/no-leidas/total")
    Response totalNoLeidas(@PathParam("idTutor") String idTutor);
}
