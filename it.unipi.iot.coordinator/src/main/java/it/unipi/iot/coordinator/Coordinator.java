package it.unipi.iot.coordinator;

public class Coordinator {

    public static void main(String[] args) {
        // Start the CLIThread
    	CLI cliThread = new CLI();
        cliThread.start();

        // Start the RegistrationServer
        RegistrationServer server = new RegistrationServer();
        server.add(new CoapRegistrationResource());
        try {
            server.start();
            System.out.println("\nSERVER STARTED\n");
        }
        catch(Exception e) {
        	e.printStackTrace();
        }
        
        // Start actuator updater
        ActuatorsUpdater updater = new ActuatorsUpdater();
        updater.run();
    }
}
