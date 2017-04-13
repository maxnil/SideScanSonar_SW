/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sonargui;


import java.util.concurrent.*;


/**
 *
 * @author max
 */
public class SonarGui {

    /** Main program body */
    public static void main(String[] args) throws InterruptedException {
        // Create Sonar Configuration (read from file)
        SonarComConfig  sonarComConfig  = new SonarComConfig("sonarCom.conf");
        SonarFishConfig sonarFishConfig = new SonarFishConfig(sonarComConfig.sonarFishConfigFileName);

        // Create FIFOs for GPS, Sensor and Sonar data
        BlockingQueue<GpsData>    guiGPSFifo       = new ArrayBlockingQueue<>(10);
        BlockingQueue<SensorData> guiSensorFifo    = new ArrayBlockingQueue<>(10);
        BlockingQueue<SonarData>  guiSonarDataFifo = new ArrayBlockingQueue<>(10);

        // Create connections to the SonarCom module
        SonarComCli  sonarComCLI  = new SonarComCli();
        SonarComData sonarComData = new SonarComData(guiGPSFifo, guiSensorFifo , guiSonarDataFifo);

        sonarComConfig.sonarComCLI = sonarComCLI;
        sonarFishConfig.sonarComCLI = sonarComCLI;
        
//        if (isDispatchThread()) System.out.println("WARNING main in DT");
        
        // Create the main window
        ComInterfaceJFrame mainFrame = new ComInterfaceJFrame(sonarComConfig, sonarFishConfig, sonarComCLI, sonarComData, guiGPSFifo, guiSensorFifo, guiSonarDataFifo);
//        mainFrame.setLocationRelativeTo(null);
//        mainFrame.setVisible(true);
        
//        System.out.println("Reached the end");
//        System.exit(0);
    }
}