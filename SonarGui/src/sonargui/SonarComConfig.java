/*
 * Copyright 2016 Max Nilsson
 * Each line should be prefixed with  * 
 */
package SonarCom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author max
 */
public final class SonarComConfig {
    /* SonarCom configuration settings */
    public String  sonarComConfigFileName = "sonarCom.conf";
    public String  sonarFishConfigFileName = "sonarFish.conf";
    public boolean sonarComAutoConnect = false;
    public String  sonarComCLIPortName = "<none1>";
    public String  sonarComDataPortName = "<none2>";
    public boolean sonarFishPwrEn = false;
    public boolean sonarComGreenLed0 = false;
    public boolean sonarComGreenLed1 = false;
    public boolean sonarComRedLed0 = false;
    public boolean sonarComRedLed1 = false;

    public SonarComCli sonarComCLI;             // Control interface to SonarCom CLI
    
    /** Constructor */
    public SonarComConfig(String fileName) {
        this.sonarComConfigFileName = fileName;
        
        File configFile = new File(this.sonarComConfigFileName);
        if (configFile.exists()) {
            // Import configuration file
            load();
        } else {
            // If configuration file did not exist, creat a new empty one
            System.out.println("SonarCom Configuration file \"" + this.sonarComConfigFileName + "\" did not exist. Creating a new one");

            try {
                configFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(SonarFishConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            // load the new empty file, use default settings
            load();
            
            // save default settings
            save();
        }
    }
    
    
    /** load SonarCom settings from file */
    public void load() {
        try {
            Properties props = new Properties();
            props.load(new FileReader(this.sonarComConfigFileName));
    
            if (!Boolean.valueOf(props.getProperty("sonarComConfig", "false"))) {
                JOptionPane.showMessageDialog(null, this.sonarComConfigFileName + " is not a valid SonarCom configuration file");
                return;
            }

            this.sonarFishConfigFileName = String.valueOf (props.getProperty("sonarFishConfigFileName", String.valueOf(this.sonarFishConfigFileName)));
            this.sonarComCLIPortName     = String.valueOf (props.getProperty("sonarComCLIPortName",     String.valueOf(this.sonarComCLIPortName)));
            this.sonarComAutoConnect     = Boolean.valueOf(props.getProperty("sonarComAutoConnect",     String.valueOf(this.sonarComAutoConnect)));
            this.sonarComDataPortName    = String.valueOf (props.getProperty("sonarComDataPortName",    String.valueOf(this.sonarComDataPortName)));
            this.sonarFishPwrEn          = Boolean.valueOf(props.getProperty("sonarComFishPwrEn",       String.valueOf(this.sonarFishPwrEn)));
            this.sonarComGreenLed0       = Boolean.valueOf(props.getProperty("sonarComGreenLed0",       String.valueOf(this.sonarComGreenLed0)));
            this.sonarComGreenLed1       = Boolean.valueOf(props.getProperty("sonarComGreenLed1",       String.valueOf(this.sonarComGreenLed1)));
            this.sonarComRedLed0         = Boolean.valueOf(props.getProperty("sonarComRedLed0",         String.valueOf(this.sonarComRedLed0)));
            this.sonarComRedLed1         = Boolean.valueOf(props.getProperty("sonarComRedLed1",         String.valueOf(this.sonarComRedLed1)));
        } catch (FileNotFoundException ex) {
            // file does not exist
            Logger.getLogger(SonarFishConfig.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            // I/O error
            Logger.getLogger(SonarFishConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


        /** save settings to file */
    public void save() {
 
        Properties props = new Properties() {
            // Make all properties come out in alphabetical order
            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<Object>(super.keySet()));
            }
        };

        props.setProperty("sonarFishConfigFileName", String.valueOf(this.sonarFishConfigFileName));
        props.setProperty("sonarComCLIPortName",     String.valueOf(this.sonarComCLIPortName));
        props.setProperty("sonarComAutoConnect",     String.valueOf(this.sonarComAutoConnect));
        props.setProperty("sonarComDataPortName",    String.valueOf(this.sonarComDataPortName));
        props.setProperty("sonarComFishPwrEn",       String.valueOf(this.sonarFishPwrEn));
        props.setProperty("sonarComGreenLed0",       String.valueOf(this.sonarComGreenLed0));
        props.setProperty("sonarComGreenLed1",       String.valueOf(this.sonarComGreenLed1));
        props.setProperty("sonarComRedLed0",         String.valueOf(this.sonarComRedLed0));
        props.setProperty("sonarComRedLed1",         String.valueOf(this.sonarComRedLed1));
        props.setProperty("sonarComConfig",          "true");
        
        try {
            props.store(new FileWriter(this.sonarComConfigFileName), "SonarCom configuration");
        } catch (IOException ex) {
            Logger.getLogger(SonarFishConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    /** Set SonarCom connection preference */
    public boolean sonarComAutoConnect(boolean value) {
        this.sonarComAutoConnect = value;
        return value;
    }

    
    /** Set SonarCom CLI Port preference */
    public String sonarComCLIPortName(String value) {
        this.sonarComCLIPortName = value;
        return value;
    }

    
    /** Set SonarCom Data Port preference */
    public String sonarComDataPortName(String value) {
        this.sonarComDataPortName = value;
        return value;
    }
    
    
    /** Set SonarFish Power Enable preference */
    public boolean sonarFishPwrEn(boolean value) {
        try {
            this.sonarFishPwrEn = sonarComCLI.sendCommand("set_sonar_pwr", value);
        } catch (TimeoutException | NullPointerException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarFishPwrEn;
    }

    
    /** Set SonarCom Green LED #0 preference */
    public boolean sonarComGreenLed0(boolean value) {
        try {
            this.sonarComGreenLed0 = sonarComCLI.sendCommand("set_green_led_0", value);
        } catch (TimeoutException | NullPointerException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarComGreenLed0;
    }

    
    /** Set SonarCom Green LED #1 preference */
    public boolean sonarComGreenLed1(boolean value) {
        try {
            this.sonarComGreenLed1 = sonarComCLI.sendCommand("set_green_led_1", value);
        } catch (TimeoutException | NullPointerException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarComGreenLed1;
    }

    
    /** Set SonarCom Red LED #0 preference */
    public boolean sonarComRedLed0(boolean value) {
        try {
            this.sonarComRedLed0 = sonarComCLI.sendCommand("set_green_led_0", value);
        } catch (TimeoutException | NullPointerException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarComRedLed0;
    }

    
    /** Set SonarCom Red LED #1 preference */
    public boolean sonarComRedLed1(boolean value) {
        try {
            this.sonarComRedLed1 = sonarComCLI.sendCommand("set_green_led_1", value);
        } catch (TimeoutException | NullPointerException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarComRedLed1;
    }

    
    /** Send all configuration settings to SonarCom */
    public boolean sendSettings() {
        // Check that port is open
        if (!this.sonarComCLI.isOpened()) {
            System.out.println("## Could not send config settings to SonarCom, CLI port is not open");
            return false;
        }
        
        sonarFishPwrEn(this.sonarFishPwrEn);
        sonarComGreenLed0(this.sonarComGreenLed0);
        sonarComGreenLed1(this.sonarComGreenLed1);
        sonarComRedLed0(this.sonarComRedLed0);
        sonarComRedLed1(this.sonarComRedLed1);
        return true;
    }

}
