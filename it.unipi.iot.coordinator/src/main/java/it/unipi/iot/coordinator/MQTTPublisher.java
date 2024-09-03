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
    
    private MQTTPublisher() {
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
    
    public void publishValue(String actuatorType, String content) {
        String topic = ""
        if(actuatorType.equals("actuator_control_rods")) topic = "control_rods";
        else if(actuatorType.equals("actuator_coolant_flow")) topic = "coolant";
        else return;
        try {
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            client.publish(topic, message);
        }
        catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
