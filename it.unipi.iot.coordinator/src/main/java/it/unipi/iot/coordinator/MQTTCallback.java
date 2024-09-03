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
    private boolean[] conditions = {false, false, false};

    static final int NOMINAL_TEMPERATURE = 265;
    static final int NOMINAL_PRESSURE = 70;
    static final int NOMINAL_NEUTRON_FLUX = 41;

    private double calculateAverage(List<Integer> values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return (double) sum / values.size();
    }

    private void updateActuatorStatus(String sensorType, Double thresholdPerc) {
        long currentTime = System.currentTimeMillis();
        if(currentTime <= lastUpdate + 5 * 1000) return;
        else lastUpdate = currentTime;
        
    	List<Integer> values = driver.getValuesFromLastSeconds(sensorType, 0, 10);
        double average = calculateAverage(values);

        String sensorTypeNum = "";
        String content = "";
        int newMode = -1;
        switch(sensorType) {
            case "temperature":
                sensorTypeNum = "0";

                if(average > 1.05 * NOMINAL_TEMPERATURE) {
                    content = "INC";
                    conditions[0] = true;
                    newMode = 2;
                }
                else {
                    content = "DEC";
                    conditions[0] = false;
                    newMode = 1;
                }
                break;

            case "pressure":
                sensorTypeNum = "1";

                if(average < 0.95 * NOMINAL_PRESSURE) {
                    content = "INC";
                    conditions[0] = true;
                    newMode = 2;
                }
                else {
                    content = "DEC";
                    conditions[0] = false;
                    newMode = 1;
                }
                break;

            case "neutron_flux":
                sensorTypeNum = "2";
                String latestMode = getLatestActuatorMode("actuator_control_rods")

                // increment the number of inserted control rods if the neutron flux is decreasing 
                // meanwhile temperature is increasing and pressure decreasing
                // or if the mode is shutdown
                if(latestMode.equals("2")) {
                    content = "INC";
                    newMode = 2;
                }
                else if(average < 0.95 * NOMINAL_NEUTRON_FLUX && conditions[0] && conditions[1] && latestMode.equals("0")) {
                    content = "INC";
                    newMode = 1;
                }
                else {
                    content = "DEC";
                    newMode = latestMode.toInt();
                }

                break;
            default:
                return;
        }
        
		Map<String, String> ipAndType = driver.getActuatorInfoFromSensorType(sensorTypeNum);
		String ip = ipAndType.get("ipv6");
		String actuatorType = ipAndType.get("type");
        try {
        	if(newMode != -1) {
                System.out.println("Auto correcting " + actuatorType + " to mode " + newMode);
        		new CoapHandler(ip, actuatorType, newMode).start();
                MQTTPublisher.getInstance().publishValue(actuatorType, content);
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
            updateActuatorStatus(topic);
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
