package it.unipi.iot.coordinator;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;

class CoapRegistrationResource extends CoapResource {
    public CoapRegistrationResource() {
        super("registration");
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        Response response;
        JsonObject json = null;
        CoAP.ResponseCode responseCode = CoAP.ResponseCode.BAD_REQUEST;

        try {
            json = JsonParser.parseString(exchange.getRequestText()).getAsJsonObject();
        }
        catch (Exception err) {
            System.err.println("Json format not valid!");
            System.err.println(exchange.getRequestText());
        }

        if(json != null) {
            try {
            	String sourceAddress = exchange.getSourceAddress().toString().replace("/", "");
                String name = json.get("name").getAsString();
                String sensorTypes = json.getAsJsonArray("sensor_types").toString();

                DBDriver.getInstance().registerActuator(
                    sourceAddress,
                    name,
                    sensorTypes
                );

                responseCode = CoAP.ResponseCode.CREATED;
            }
            catch (Exception e) {
                System.out.println("Unable to register coap actuator!");
            }
        }

        response = new Response(responseCode);
        exchange.respond(response);
    }
}
