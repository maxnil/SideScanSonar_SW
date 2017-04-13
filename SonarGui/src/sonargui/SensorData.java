/*
 * Copyright 2015 Max Nilsson
 * Each line should be prefixed with  * 
 */
package SonarCom;

import SonarCom.SonarComData.DataPacket;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author max
 */
public class SensorData {
    private double depth;                     // Sonar Fish depth in (m)
    public boolean depthValid = false;

    private double heading;                   // Sonar Fish compass heading (deg.)
    public boolean headingValid = false;

    private double pitch;                     // Sonar Fish pitch (deg.)
    public boolean pitchValid = false;

    private double roll;                      // Sonar Fish roll (deg.)
    public boolean rollValid = false;

    private double sensorBoardTemp;           // Sensor board temperature (deg. C)
    private double sensorIrVobj;              // IR voltage value
    private double sensorIrTamb;              // IR sensor die temperature value
    private double sensorIrTemp;              // Sensor IR temperature (deg. C)
    public boolean temperatureValid = false;

    private double voltage;                   // Sonar Fish voltage (V)
    public boolean voltageValid = false;

    private double current;                   // Sonar Fish current (A)
    public boolean currentValid = false;
 
    
    SensorData(DataPacket dataPacket) {
        String dataString;
        try {
            dataString = new String(dataPacket.data, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SensorData.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        try {
            String[] stringArray = dataString.split("(, *)|\r");
//            System.out.print("Sensor data: ");
//            for (int i = 0; i < stringArray.length - 1; i++) {
//                System.out.print("\"" + stringArray[i] + "\", ");
//            }
//            System.out.println("END");
            
            if (stringArray[0].contains("$TEMP")) {
                this.sensorBoardTemp = Double.valueOf(stringArray[1]);
                this.sensorIrTamb = Double.valueOf(stringArray[2]);
                this.sensorIrVobj = Double.valueOf(stringArray[3]);
                this.sensorIrTemp = Double.valueOf(stringArray[4]);
                this.temperatureValid = true;
                return;
            }
            
            if (stringArray[0].contains("$MAGN")) {
                Double.valueOf(stringArray[1]);
                this.heading = Double.valueOf(stringArray[2]) / 10.0;
                Double.valueOf(stringArray[3]);
                Double.valueOf(stringArray[4]);
                Double.valueOf(stringArray[5]);
                this.headingValid = true;
            } 
            
            if (stringArray[0].contains("$ACC")) {
                double xAxis = Double.valueOf(stringArray[1]);
                double zAxis = Double.valueOf(stringArray[2]);
                double yAxis = Double.valueOf(stringArray[3]);
                
                double rollRad = Math.atan(xAxis/Math.sqrt(Math.pow(yAxis,2) + Math.pow(zAxis,2)));
                double pitchRad  = Math.atan(yAxis/Math.sqrt(Math.pow(xAxis,2) + Math.pow(zAxis,2)));
                
                //convert radians into degrees
                this.pitch = pitchRad * (180.0/Math.PI);
                this.roll = rollRad * (180.0/Math.PI);
                this.pitchValid = true;
                this.rollValid = true;
            } 
            
            if (stringArray[0].contains("$COMP")) {
                Double.valueOf(stringArray[1]);
                Double.valueOf(stringArray[2]);
                Double.valueOf(stringArray[3]);
            } 
            
            if (stringArray[0].contains("$GYRO")) {
                Double.valueOf(stringArray[1]);
                Double.valueOf(stringArray[2]);
                Double.valueOf(stringArray[3]);
            }

        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
//            System.out.print("Sensor data: " + dataString+ " END");
//            Logger.getLogger(SensorData.class.getName()).log(Level.SEVERE, null, ex);
        }         
    }

    
    // Rounding function
    private double round(double value, int nrDec) {
        return ((double)((int)((value * Math.pow(10.0, nrDec)) + 0.5))) / Math.pow(10.0, nrDec);
    }
    
    
    // Get Sonar Fish depth (m)
    public String depth() {
        return Double.toString(round(this.depth, 2)) + " m";
    }

    
    // Get Sonar Fish heading (deg.)
    public String heading() {
        return Double.toString(round(this.heading, 1)) + "°";
    }

    
    // Get Sonar Fish pitch angle (deg.)
    public String pitch() {
        return Double.toString(round(this.pitch, 1)) + "°";
    }

    
    // Get Sonar Fish roll angle (deg.)
    public String roll() {
        return Double.toString(round(this.roll, 1)) + "°";
    }


    // Get Sonar Fish sensorBoardTemp (deg. C)
    public String sensorBoardTemp() {
        return Double.toString(round(this.sensorBoardTemp / 256.0, 1)) + "°C";
    }

    
    // Get Sensor Board IR sensorBoardTemp (deg. C)
    public String irTemp() {
        return Double.toString(round(this.sensorIrTamb / 256.0, 1)) + "°C";
    }

    
    // Get Sonar Fish voltage (V)
    public String voltage() {
        return Double.toString(round(this.voltage, 1)) + " V";
    }

    
    // Get Sonar Fish current (A)
    public String current() {
        return Double.toString(round(this.current, 1)) + " A";
    }

    
//    @Override
//    public String toString() {
//        return "Depth = " + this.depth +
//                ", Heading = " + this.heading +
//                ", Pitch = " + this.pitch +
//                ", Roll = " + this.roll +
//                ", Temperature = " + this.sensorBoardTemp +
//                ", Voltage = " + this.voltage;
//    }
}
