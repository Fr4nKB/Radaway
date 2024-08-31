package it.unipi.iot.coordinator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTPublisher {
    private static MQTTPublisher instance = null;
	private MqttClient client;
    String broker = "tcp://[::1]:1883";
    String clientId = "RemoteAppPublisher";
    int qos = 2;    //ensure highest reliability at a speed cost
    private List<Integer> lastModes;
    
    private MQTTPublisher() {
        lastModes = new ArrayList<>();
        lastModes.add(2);
        lastModes.add(0);
        
        try {
        	client = new MqttClient(broker, clientId);
            client.connect();
        }
        catch (MqttException e) {
            e.printStackTrace();
        }
        
    }
    
    public static MQTTPublisher getInstance() {
        if(instance == null)
            instance = new MQTTPublisher();

        return instance;
    }
    
    private void publishValue(String topic, String content) {
        try {
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            client.publish(topic, message);
        }
        catch (MqttException e) {
            e.printStackTrace();
        }
    }
    
    public void updateSensorValues(String type, int mode) {
        String content = "";

        if(type.equals("actuator_control_rods")) {
            if(mode > lastModes.get(0)) content = "INC";
            else if(mode < lastModes.get(0)) content = "DEC";
            lastModes.set(0, mode);
            
            if(content != "") publishValue("control_rods", content);
        }
        else if(type.equals("actuator_coolant_flow")) {
            if(mode < lastModes.get(1)) content = "INC";
            else if(mode > lastModes.get(0)) content = "DEC";
            lastModes.set(1, mode);
            
            if(content != "") publishValue("coolant", content);
        }
    }
}
