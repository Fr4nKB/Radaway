package it.unipi.iot.coordinator;

import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MQTTCallback implements MqttCallback {

    private final DBDriver driver = DBDriver.getInstance();
    private long lastUpdate = 0;

    private double calculateAverage(List<Integer> values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return (double) sum / values.size();
    }

    private void updateActuatorStatus(String sensorType, Double thresholdPerc) {
        long currentTime = System.currentTimeMillis();
        if(currentTime <= lastUpdate + 10 * 1000) return;
        else lastUpdate = currentTime;
        
    	List<Integer> values = driver.getValuesFromLastSeconds(sensorType, 0, 5);
        List<Integer> oldValues = driver.getValuesFromLastSeconds(sensorType, 5, 10);

        double newAverage = calculateAverage(values);
        double oldAverage = calculateAverage(oldValues);

        String sensorTypeNum = "";
        if(sensorType == "temperature") sensorTypeNum = "0";
        else if(sensorType == "pressure") sensorTypeNum = "1";
        else sensorTypeNum = "0";
        
        
		Map<String, String> ipAndType = driver.getActuatorInfoFromSensorType(sensorTypeNum);
		String ip = ipAndType.get("ipv6");
		String actuatorType = ipAndType.get("type");

        int newMode = -1;
        if(Math.abs(newAverage - oldAverage) > oldAverage * thresholdPerc) {
        	if(actuatorType.equals("actuator_control_rods")) newMode = 2;
        	else newMode = 0;
        }
        else {
        	int rndVal = (int) (Math.random() * 2);
        	if(actuatorType.equals("actuator_coolant_flow")) newMode = 1 - rndVal;
        	else newMode = rndVal;
        }

        try {
        	if(newMode != -1) {
                System.out.println("Auto correcting " + actuatorType + " to mode " + newMode);
                System.out.println("Old average: " + oldAverage + "; New average: " + newAverage);
        		new CoapHandler(ip, actuatorType, newMode).start();
                MQTTPublisher.getInstance().updateSensorValues(actuatorType, newMode);
        	}
        }
        catch(Exception e) { return; }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connection lost: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        JsonObject jsonObject = null;
        int value = -1;
        
        try {
            jsonObject = JsonParser.parseString(payload).getAsJsonObject();
            value = jsonObject.get("value").getAsInt();
            
            driver.insertSensorSample(topic, value);
            updateActuatorStatus(topic, 0.05);
        }
        catch (Exception e) {
            return;
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("Delivery complete");
    }
}
