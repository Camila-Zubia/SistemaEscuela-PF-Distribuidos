/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.gateaway;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
@RegisterRestClient(configKey = "mensajeria")
@Path("/api/mensajeria")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public interface MensajeriaRestClient {

    @GET
    @Path("/conversaciones/{idUsuario}")
    Response conversaciones(@PathParam("idUsuario") String idUsuario);

    @POST
    @Path("/conversaciones")
    Response crearConversacion(String body);

    @GET
    @Path("/conversaciones/{idConversacion}/mensajes")
    Response mensajes(@PathParam("idConversacion") String idConversacion,
                      @QueryParam("desde") String desde);
}
