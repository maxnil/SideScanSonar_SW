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
public class GpsData {
    private double course = 0.0;                    // Course
    public boolean courseValid = false;

    private double speed = 0.0;                     // Speed in knots
    public boolean speedValid = false;

    private int    latitudeDeg = 0;                 // Latitude (degrees)
    private double latitudeMin = 0.0;               // Latitude (minutes)
    private String latitudeNs = "E";                // Latitude North/South
    public boolean latitudeValid = false;

    private int    longitudeDeg = 0;                // Longitude (degrees)
    private double longitudeMin = 0.0;              // Longitude (minutes)
    private String longitudeEw = "N";               // Longitude East/West
    public boolean longitudeValid  = false;

    private int    nrSat = 0;                       // Number of used satellites
    public boolean nrSatValid = false;

    private double hdop = 0.0;                      // Horizontal accuracy
    public boolean hdopValid = false;

    private String fixStr = "";                     // Fix type
    public boolean fixValid = false;

    private String modeStr = "";                    // 2D/3D mode
    public boolean modeValid = false;
    
    
    GpsData(DataPacket dataPacket) {
        try {
            String dataString = new String(dataPacket.data, "UTF-8");
            String[] stringArray = dataString.split("(, *)|\r");
//            System.out.print("GPS    data: ");
//            for (int i = 0; i < stringArray.length - 1; i++) {
//                System.out.print("\"" + stringArray[i] + "\", ");
//            }
//            System.out.println("");
            if (stringArray[0].contains("$GPGGA")) {
                // Get strings
                if (stringArray[2].length() >= 1) {
                    this.latitudeDeg = Integer.valueOf(stringArray[2].substring(0, 2));
                    this.latitudeMin = Double.valueOf(stringArray[2].substring(2));
                    this.latitudeNs  = stringArray[3];
                }
                if (stringArray[4].length() >= 1) {
                    this.longitudeDeg = Integer.valueOf(stringArray[4].substring(0, 3));
                    this.longitudeMin = Double.valueOf(stringArray[4].substring(3));
                    this.longitudeEw  = stringArray[5];
                }
                this.fixStr         = stringArray[6];
                this.nrSat          = Integer.valueOf(stringArray[7]);
                this.hdop           = Double.valueOf("0" + stringArray[8]);
                // Check if data is valid
                this.latitudeValid  = (stringArray[2].length() >= 1);
                this.longitudeValid = (stringArray[4].length() >= 1);
                this.fixValid       = (stringArray[6].length() >= 1);
                this.nrSatValid     = (stringArray[7].length() >= 1);
                this.hdopValid      = (stringArray[8].length() >= 1);
            } else if (stringArray[0].contains("$GPGLL")) {
                // Get strings
                if (stringArray[1].length() >= 1) {
                    this.latitudeDeg = Integer.valueOf(stringArray[1].substring(0, 2));
                    this.latitudeMin = Double.valueOf(stringArray[1].substring(2));
                    this.latitudeNs  = stringArray[2];
                }
                if (stringArray[3].length() >= 1) {
                    this.longitudeDeg = Integer.valueOf(stringArray[3].substring(0, 3));
                    this.longitudeMin = Double.valueOf(stringArray[3].substring(3));
                    this.longitudeEw  = stringArray[4];
                }
                // Check if data is valid
                this.latitudeValid  = (stringArray[1].length() >= 1);
                this.longitudeValid = (stringArray[3].length() >= 1);
           } else if (stringArray[0].contains("$GPGSA")) {
                // Get strings
                this.modeStr        = stringArray[2];
                this.hdop           = Double.valueOf("0" + stringArray[16]);
                // Check if data is valid
                this.modeValid      = (stringArray[2].length() >= 1);
                this.hdopValid      = (stringArray[16].length() >= 1);
            } else if (stringArray[0].contains("$GPGSV")) {
//                
            } else if (stringArray[0].contains("$GPRMC")) {
                // Get strings
                if (stringArray[3].length() >= 1) {
                    this.latitudeDeg = Integer.valueOf(stringArray[3].substring(0, 2));
                    this.latitudeMin = Double.valueOf(stringArray[3].substring(2));
                    this.latitudeNs  = stringArray[4];
                }
                if (stringArray[5].length() >= 1) {
                    this.longitudeDeg = Integer.valueOf(stringArray[5].substring(0, 3));
                    this.longitudeMin = Double.valueOf(stringArray[5].substring(3));
                    this.longitudeEw  = stringArray[6];
                }           
                this.speed          = Double.valueOf("0" + stringArray[7]);
                this.course         = Double.valueOf("0" + stringArray[8]);
                // Check if data is valid
                this.latitudeValid  = (stringArray[3].length() >= 1);
                this.longitudeValid = (stringArray[5].length() >= 1);
                this.speedValid     = (stringArray[7].length() >= 1);
                this.courseValid    = (stringArray[8].length() >= 1);
            } else if (stringArray[0].contains("$GPVTG")) {
                // Get strings
                this.course      = Double.valueOf("0" + stringArray[1]);
                this.speed       = Double.valueOf("0" + stringArray[5]);
                // Check if data is valid
                this.courseValid = (stringArray[1].length() >= 1);
                this.speedValid  = (stringArray[5].length() >= 1);
            } else {
                System.out.println(">>> Unhandled GPS sentence " + stringArray[0]);
            }
        } catch (NumberFormatException | UnsupportedEncodingException | ArrayIndexOutOfBoundsException ex) {
            Logger.getLogger(SensorData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    // Rounding function
    private double round(double value, int nrDec) {
        return ((double)((int)((value * Math.pow(10.0, nrDec)) + 0.5))) / Math.pow(10.0, nrDec);
    }
    
    /**
     * Get latitude string
     */
    public String latitude() {
        return this.latitudeDeg + "°" + this.latitudeMin + this.latitudeNs;
    }
    
    
    /**
     * Get longitude string
     */
    public String longitude() {
        return this.longitudeDeg + "°" + this.longitudeMin + this.longitudeEw;
    }

    
    /**
     * Get boat course in degrees string
     */
    public String course() {
        double roundCourse = ((double)((int)((this.course * 10.0) + 0.5))) / 10.0;
        return roundCourse + "°";
    }
 
    
    /**
     * Get boat speed in knots
     */
    public String speed() {
        double roundSpeed = ((double)((int)((this.speed * 10.0) + 0.5))) / 10.0;
        return roundSpeed + " kn";
    }
 

    /**
     * Get number of active satellites
     */
    public String nrSat() {
        return String.valueOf(nrSat);
    }
 

    /**
     * Get HDOP
     */
    public String hdop() {
        double roundHdop = ((double)((int)((this.hdop * 10.0) + 0.5))) / 10.0;
        return roundHdop + " m";
    }
 

    /**
     * Get fix quality
     */
    public String fixQuality() {
        if (fixStr.contains("1"))
            return "GPS";
        
        if (fixStr.contains("2"))
            return "DGPS";
        
        return "Invalid";
    }
    
 
    /**
     * Get GpsData mode
     */
    public String mode() {
        if (modeStr.contains("2"))
            return "2D";
        
        if (modeStr.contains("3"))
            return "3D";
        
        return "No fix";
    }
 

//    @Override
//    public String toString() {
//        return "Location: " + latitude() + ", " + longitude();
//    }
}
