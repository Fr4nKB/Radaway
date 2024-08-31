package it.unipi.iot.coordinator;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CLI extends Thread {

    private MqttClient client;
    private DBDriver driver;
    String broker = "tcp://[::1]:1883";
    String clientId = "RemoteApp";
    
    public CLI() {
        driver = DBDriver.getInstance();

        try {
            client = new MqttClient(broker, clientId);
            client.setCallback(new MQTTCallback());
            client.connect();
            
            client.subscribe("temperature");
            client.subscribe("pressure");
            client.subscribe("neutron_flux");
            
        	System.out.println("MQTT: DONE");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    
    private int getActuatorCurrentStatus(String type) {
    	try {
    		String ip = driver.getIpFromActuatorType(type);
	        CoapClient coapClient = new CoapClient("coap://[" + ip + "]/" + type);
	        CoapResponse response = coapClient.get();
	        coapClient.shutdown();
	        
	        if(response == null) return -1;
	        String content = response.getResponseText();
        	JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();

        	return jsonObject.get("status").getAsInt();
    	}
        catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while(true) {
            System.out.println("----- Main menu -----");
            System.out.println("Type \"!help\" to show all the available commands");
            String command;
            try{
                command = reader.readLine();
                executeCommand(command);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void executeCommand(String command) {
        String type = "";
        int mode = -1;
        
        System.out.print("\033[H\033[2J");
        System.out.flush();

        switch (command) {
            case "!help":
                System.out.println("\nSTATUS:");
                System.out.println("!status --> prints the status of all actuators");
                System.out.println("\nACTIONS:");
                System.out.println("!shutdown --> to fully insert all control rods");
                System.out.println("!startup --> start fission reaction");
                System.out.println("!regime --> set fully inserted control rods to minimum");
                System.out.println("!coolant_off --> stop coolant flow");
                System.out.println("!coolant_on --> set coolant flow to minimum");
                System.out.println("!cooldown --> set coolant flow to max");
                break;

            case "!status":
                int controlRodsStatus = getActuatorCurrentStatus("actuator_control_rods");
                int coolantFlowStatus = getActuatorCurrentStatus("actuator_coolant_flow");
                System.out.println("Percentage of fully inserted control rods: " + controlRodsStatus + "%");
                System.out.println("Coolant flow percentage: " + coolantFlowStatus + "%");
                break;

            case "!shutdown":
                type = "actuator_control_rods";
                mode = 2;
                break;

            case "!startup":
                type = "actuator_control_rods";
                mode = 1;
                break;

            case "!regime":
                if(getActuatorCurrentStatus("actuator_coolant_flow") == 0) {
                    System.out.println("ERROR: cannot extract more control rods when coolant flow is at minimum");
                    return;
                }
                type = "actuator_control_rods";
                mode = 0;
                break;

            case "!coolant_off":
                if(getActuatorCurrentStatus("actuator_control_rods") == 0) {
                    System.out.println("ERROR: cannot turn off coolant when control rods at minimum");
                    return;
                }
                type = "actuator_coolant_flow";
                mode = 0;
                break;

            case "!coolant_on":
                type = "actuator_coolant_flow";
                mode = 1;
                break;

            case "!cooldown":
                type = "actuator_coolant_flow";
                mode = 2;
                break;
        }

        if(mode != -1) {
        	String ip = driver.getIpFromActuatorType(type);
            new CoapHandler(ip, type, mode).start();
            MQTTPublisher.getInstance().updateSensorValues(type, mode);
        }
    }
}