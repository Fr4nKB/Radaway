package it.unipi.iot.coordinator;

import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MQTTCallback implements MqttCallback {

    private final DBDriver driver = DBDriver.getInstance();

    private double calculateAverage(List<Integer> values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return (double) sum / values.size();
    }

    private void updateActuatorStatus(String topic, Double thresholdPerc) {
        List<Integer> values = driver.getValuesFromLastMinute(topic, 1);
        List<Integer> oldValues = driver.getValuesFromLastMinute(topic, 2);

        double newAverage = calculateAverage(values);
        double oldAverage = calculateAverage(oldValues);

        String type = (topic.equals("temperature") || topic.equals("pressure")) ? "actuator_coolant_flow" : "actuator_control_rods";

        int newMode = (newAverage < oldAverage * (1 - thresholdPerc)) ? 
                    (type.equals("actuator_coolant_flow") ? 2 : 0) : 
                    (type.equals("actuator_coolant_flow") ? 0 : 2);

        if(newMode != -1) new CoapHandler(type, newMode).start();
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
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        driver.insertSensorSample(topic, value);
        updateActuatorStatus(topic, 0.01);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("Delivery complete");
    }
}
