package it.unipi.iot.coordinator;

import java.util.Map;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import com.google.gson.JsonObject;


public class CoapHandler extends Thread {

    private DBDriver driver;
    private String ipv6;
    private String type;
    private int mode;

    public CoapHandler(String ip, String actuatorType, int newMode) {
        ipv6 = ip;
        type = actuatorType;
        mode = newMode;
    }

    @Override
    public void run() {
    	try {
            CoapClient coapClient = new CoapClient("coap://[" + ipv6 + "]/" + type + "?mode=" + mode);
            CoapResponse response = coapClient.put("", MediaTypeRegistry.TEXT_PLAIN);
            
            if(response == null) System.out.println("Failed to change mode!");
            else {
                CoAP.ResponseCode code = response.getCode();

                switch(code) {
                    case CHANGED:
                        driver.insertActuatorStatus(type, ipv6, mode);
                        break;

                    case BAD_REQUEST:
                        System.err.println("Internal application error!");
                        break;

                    case BAD_OPTION:
                        System.err.println("BAD_OPTION error");
                        break;

                    default:
                        System.err.println("Actuator error, code: " + code);
                        break;

                }
            }
            coapClient.shutdown();
    	}
    	catch(Exception e) {
    		return;
    	}
       
    }
}