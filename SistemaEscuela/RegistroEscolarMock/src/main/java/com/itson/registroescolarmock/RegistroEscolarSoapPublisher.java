/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.itson.registroescolarmock;
import jakarta.xml.ws.Endpoint;

/**
 *
 * @author Usuario
 */
public class RegistroEscolarSoapPublisher {
    public static void main(String[] args) {
        String url = "http://localhost:8092/ws/registro-escolar";
        Endpoint.publish(url, new RegistroEscolarEndpoint());
        System.out.println("WSDL disponible en: " + url + "?wsdl");
    }
}
