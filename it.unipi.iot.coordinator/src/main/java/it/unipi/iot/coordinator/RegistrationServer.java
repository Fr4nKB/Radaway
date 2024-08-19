package it.unipi.iot.coordinator;

import org.eclipse.californium.core.CoapServer;

public class RegistrationServer extends CoapServer {

    public static void main(String args[]) {
        RegistrationServer server = new RegistrationServer();
        server.add(new CoapRegistrationResource());
        server.start();
        System.out.println("\nSERVER STARTED\n");
    }

}