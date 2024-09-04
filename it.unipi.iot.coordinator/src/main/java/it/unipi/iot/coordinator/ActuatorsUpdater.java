package it.unipi.iot.coordinator;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;

public class ActuatorsUpdater extends Thread {
    private DBDriver driver;
    
    private boolean[] conditions = {false, false};
    static final int NOMINAL_TEMPERATURE = 265;
    static final int NOMINAL_PRESSURE = 70;
    static final int NOMINAL_NEUTRON_FLUX = 41000;
    
    public ActuatorsUpdater() {
        driver = DBDriver.getInstance();
    }

    private double calculateAverage(List<Integer> values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return (double) sum / values.size();
    }
    
    private void updateActuatorStatus(String sensorType) {        
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
                    conditions[1] = true;
                    newMode = 2;
                }
                else {
                    content = "DEC";
                    conditions[1] = false;
                    newMode = 1;
                }
                break;

            case "neutron_flux":
                sensorTypeNum = "2";
                int latestMode = 2;
                try {
                	latestMode = Integer.parseInt(driver.getLatestActuatorValue("actuator_control_rods"));
                }
                catch(Exception e) {
                    //
                }
                // increment the number of inserted control rods if the neutron flux is decreasing 
                // meanwhile temperature is increasing and pressure decreasing
                // or if the mode is shutdown
                if(latestMode == 2) {
                    content = "INC";
                    newMode = 2;
                }
                else if(average < 0.95 * NOMINAL_NEUTRON_FLUX && (conditions[0] || conditions[1]) && latestMode == 0) {
                    content = "INC";
                    newMode = 1;
                }
                else {
                    content = "DEC";
                    newMode = latestMode;
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
        		new CoapHandler(ip, actuatorType, newMode).start();
                MQTTPublisher.getInstance().publishValue(actuatorType, content);
                System.out.println("Auto corrected " + actuatorType + " to mode " + newMode);
        	}
        }
        catch(Exception e) { return; }
    }

    @Override
    public void run() {

        while(true) {           
            updateActuatorStatus("temperature");
            updateActuatorStatus("pressure");
            updateActuatorStatus("neutron_flux");
            try {
                Thread.sleep(10000);
            }
            catch (InterruptedException e) {
                //
            }
        }
    }
}
