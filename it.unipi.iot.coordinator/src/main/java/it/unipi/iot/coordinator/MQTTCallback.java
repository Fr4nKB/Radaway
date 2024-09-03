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
