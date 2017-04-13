/*
 * Copyright 2015 Max Nilsson
 * Each line should be prefixed with  * 
 */
package SonarCom;

import SonarCom.SonarComData.DataPacket;



/**
 *
 * @author max
 */
public class SonarData {
    
    // SonarData constants
    public static final double SPEED_OF_SOUND = 1500.0;    // Speed of sound in water in m/s
    
    // SonarData sample data
    public double[] leftData;                       // Left ping data
    public double[] rightData;                      // Right ping data
    
    // SonarData information
    private final int sampleRate;                   // Sample rate (Hz)
    private final double pingRate;                  // SonarData rate (Hz)
    private final double boatSpeed;                 // Boat speed (m/s)
        
    // SonarData constructor
    SonarData(DataPacket dataPacket) {
        this.sampleRate = 220000;
        this.pingRate = 10.0;
    
        this.leftData  = leftData.clone();
        this.rightData = rightData.clone();
    
        this.boatSpeed = 12.3;
    }
    
//    SonarData(int sampleRate, double pingRate, double[] leftData, double[] rightData, double boatSpeed) {
//        this.sampleRate = sampleRate;
//        this.pingRate = pingRate;
//    
//        this.leftData  = leftData.clone();
//        this.rightData = rightData.clone();
//    
//        this.boatSpeed = boatSpeed;
//    }
//    
    // SonarData constructor
//    SonarData(int sampleRate, double pingRate, int sampleLength, byte[] byteData, double boatSpeed) {
//        this.sampleRate = sampleRate;
//        this.pingRate = pingRate;
//    
//        this.leftData = new double[sampleLength];
//        this.rightData = new double[sampleLength];
//                
//        int j = 0;
//        for (int i = 0; i < 4*sampleLength; i += 4) {
//            this.leftData[j]  = ((((int)byteData[i+1]) & 0xFF)<<8 | ((int)byteData[i+0]) & 0xFF) / 65536.0;
//            this.rightData[j] = ((((int)byteData[i+3]) & 0xFF)<<8 | ((int)byteData[i+2]) & 0xFF) / 65536.0;
//            j++;
//        }
//        
//        this.boatSpeed = boatSpeed;
//    }
    
    // Get Sample Rate (Hz)
    double sampleRate() {
        return this.sampleRate;
    }

    // Get Boat Speed (m/s)
    double boatSpeed() {
        return this.boatSpeed;
    }
    
    // Get left/right range (m)
    double range() {
        return (double)leftData.length * this.xResolution();
    }
    
    // Get X resolution (m)
    double xResolution() {
        return 0.5 * (SonarData.SPEED_OF_SOUND / this.sampleRate);
    }

    // Get Y resolution (m)
    double yResolution() {
        return (this.boatSpeed / this.pingRate);
    }
    
    @Override
    public String toString() {
        return "SampleRate = " + this.sampleRate +
                ", PingRate = " + this.pingRate +
                ", LeftSamples = " + this.leftData.length +
                ", RightSamples = " + this.rightData.length +
                ", Range = " + range() +
                ", XResolution = " + xResolution() +
                ", YResolution = " + yResolution();
    }
}
