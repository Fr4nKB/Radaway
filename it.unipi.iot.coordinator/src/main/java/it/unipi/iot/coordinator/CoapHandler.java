package it.unipi.iot.coordinator;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import com.google.gson.JsonObject;


public class CoapHandler extends Thread {

    private DBDriver driver;
    private String ip;
    private String type;
    private int mode;

    public CoapHandler(String type, int mode) {
        this.type = type;
        this.mode = mode;

        driver = DBDriver.getInstance();
        this.ip = driver.getIp(this.type);
    }

    @Override
    public void run() {
        CoapClient coapClient = new CoapClient("coap://[" + ip + "]/" + type);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mode", mode);
        
        CoapResponse response = coapClient.put(jsonObject.toString(), MediaTypeRegistry.APPLICATION_JSON);
        coapClient.shutdown();

        if(response != null) {

            CoAP.ResponseCode code = response.getCode();

            switch(code) {
                case CHANGED:
                    driver.insertActuatorStatus(type, ip, mode);
                    break;

                case BAD_REQUEST:
                    System.err.println("Internal application error!");
                    break;

                case BAD_OPTION:
                    System.err.println("BAD_OPTION error");
                    break;

                default:
                    System.err.println("Actuator error!");
                    break;

            }

        }
    }
}