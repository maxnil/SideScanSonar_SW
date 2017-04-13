/*
 * Copyright 2015 Max Nilsson
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
 * @author emaxnil
 */
public final class SonarFishConfig {
    /* SonarFish configuration settings */
    public boolean sensorAccEn = false;
    public boolean sensorCVEn = false;
    public boolean sensorCompEn = false;
    public boolean sensorPressEn = false;
    public boolean sensorRecEn = false;
    public boolean sensorTempEn = false;
    public boolean sonarRxRecEn = false;
    public boolean sensorGreenLedEn = false;
    public boolean sensorYellowLedEn = false;
    public boolean sonarBlueLedEn = false;
    public boolean sonarRedLedEn = false;
    public int sonarRxOn = 0;
    public int sonarRxGainType = 0;
    public int sonarRxRange = 0;
    public int sonarRxDeadZone = 0;
    public int sonarRxGainSlope = 0;
    public int sonarRxGainOffset = 0;
    public int sonarTxOn = 0;
    public int sonarTxMod = 0;
    public int sonarTxPwr = 0;
    public int sonarTxPulseLen = 0;
    public int sonarFpgaLeds = 0;

    private String fileName;
    
    public SonarComCli sonarComCLI;             // Control interface to SonarCom CLI
    
    /** Constructor */
    public SonarFishConfig(String fileName) {
        this.fileName = fileName;
        
        File configFile = new File(fileName);
        if (configFile.exists()) {
            // Import configuration file
            load(fileName);
        } else {
            // If configuration file did not exist, creat a new empty one
            System.out.println("SonarFish Configuration file \"" + fileName + "\" did not exist. Creating a new one");

            try {
                configFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(SonarFishConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            // load the new empty file, use default settings
            load(fileName);
            
            // save default settings
            save(fileName);
        }
    }
    

    /** load SonarCom settings from file */
    public void load() {
        load(this.fileName);
    }

    
    /** load SonarCom settings from file */
    public void load(String fileName) {
        
        try {
            Properties props = new Properties();
            props.load(new FileReader(fileName));

            if (!Boolean.valueOf(props.getProperty("sonarFishConfig", "false"))) {
                JOptionPane.showMessageDialog(null, fileName + " is not a valid SonarFish configuration file");
                return;
            }
            
            this.sensorGreenLedEn        = Boolean.valueOf(props.getProperty("sensorGreenLedEn",        String.valueOf(this.sensorGreenLedEn)));
            this.sensorYellowLedEn       = Boolean.valueOf(props.getProperty("sensorYellowLedEn",       String.valueOf(this.sensorYellowLedEn)));
            this.sonarBlueLedEn          = Boolean.valueOf(props.getProperty("sonarBlueLedEn",          String.valueOf(this.sonarBlueLedEn)));
            this.sonarRedLedEn           = Boolean.valueOf(props.getProperty("sonarRedLedEn",           String.valueOf(this.sonarRedLedEn)));
            this.sensorAccEn             = Boolean.valueOf(props.getProperty("sensorAccEn",             String.valueOf(this.sensorAccEn)));
            this.sensorCVEn              = Boolean.valueOf(props.getProperty("sensorCVEn",              String.valueOf(this.sensorCVEn)));
            this.sensorCompEn            = Boolean.valueOf(props.getProperty("sensorCompEn",            String.valueOf(this.sensorCompEn)));
            this.sensorPressEn           = Boolean.valueOf(props.getProperty("sensorPressEn",           String.valueOf(this.sensorPressEn)));
            this.sensorRecEn             = Boolean.valueOf(props.getProperty("sensorRecEn",             String.valueOf(this.sensorRecEn)));
            this.sensorTempEn            = Boolean.valueOf(props.getProperty("sensorTempEn",            String.valueOf(this.sensorTempEn)));
            this.sonarRxDeadZone         = Integer.valueOf(props.getProperty("sonarRxDeadZone",         String.valueOf(this.sonarRxDeadZone)));
            this.sonarRxOn               = Integer.valueOf(props.getProperty("sonarRxOn",               String.valueOf(this.sonarRxOn)));
            this.sonarRxGainType         = Integer.valueOf(props.getProperty("sonarRxGain",             String.valueOf(this.sonarRxGainType)));
            this.sonarRxGainOffset       = Integer.valueOf(props.getProperty("sonarRxGainOffset",       String.valueOf(this.sonarRxGainOffset)));
            this.sonarRxGainSlope        = Integer.valueOf(props.getProperty("sonarRxGainSlope",        String.valueOf(this.sonarRxGainSlope)));
            this.sonarRxRange            = Integer.valueOf(props.getProperty("sonarRxRange",            String.valueOf(this.sonarRxRange)));
            this.sonarRxRecEn            = Boolean.valueOf(props.getProperty("sonarRxRecEn",            String.valueOf(this.sonarRxRecEn)));
            this.sonarTxOn               = Integer.valueOf(props.getProperty("sonarTxOn",               String.valueOf(this.sonarTxOn)));
            this.sonarTxMod              = Integer.valueOf(props.getProperty("sonarTxMod",              String.valueOf(this.sonarTxMod)));
            this.sonarTxPwr              = Integer.valueOf(props.getProperty("sonarTxPwr",              String.valueOf(this.sonarTxPwr)));
            this.sonarTxPulseLen         = Integer.valueOf(props.getProperty("sonarTxPulseLen",         String.valueOf(this.sonarTxPulseLen)));
            this.sonarFpgaLeds           = Integer.valueOf(props.getProperty("sonarFpgaLeds",           String.valueOf(this.sonarFpgaLeds)));
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
        save(this.fileName);
    }
    
    
    /** save settings to file */
    public void save(String fileName) {
        this.fileName = fileName;
 
        Properties props = new Properties() {
            // Make all properties come out in alphabetical order
            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<Object>(super.keySet()));
            }
        };

        props.setProperty("sensorGreenLedEn",        String.valueOf(this.sensorGreenLedEn));
        props.setProperty("sensorYellowLedEn",       String.valueOf(this.sensorYellowLedEn));
        props.setProperty("sonarBlueLedEn",          String.valueOf(this.sonarBlueLedEn));
        props.setProperty("sonarRedLedEn",           String.valueOf(this.sonarRedLedEn));
        props.setProperty("sensorAccEn",             String.valueOf(this.sensorAccEn));
        props.setProperty("sensorCVEn",              String.valueOf(this.sensorCVEn));
        props.setProperty("sensorCompEn",            String.valueOf(this.sensorCompEn));
        props.setProperty("sensorPressEn",           String.valueOf(this.sensorPressEn));
        props.setProperty("sensorRecEn",             String.valueOf(this.sensorRecEn));
        props.setProperty("sensorTempEn",            String.valueOf(this.sensorTempEn));
        props.setProperty("sonarRxDeadZone",         String.valueOf(this.sonarRxDeadZone));
        props.setProperty("sonarRxOn",               String.valueOf(this.sonarRxOn));
        props.setProperty("sonarRxGain",             String.valueOf(this.sonarRxGainType));
        props.setProperty("sonarRxGainOffset",       String.valueOf(this.sonarRxGainOffset));
        props.setProperty("sonarRxGainSlope",        String.valueOf(this.sonarRxGainSlope));
        props.setProperty("sonarRxRange",            String.valueOf(this.sonarRxRange));
        props.setProperty("sonarRxRecEn",            String.valueOf(this.sonarRxRecEn));
        props.setProperty("sonarTxOn",               String.valueOf(this.sonarTxOn));
        props.setProperty("sonarTxMod",              String.valueOf(this.sonarTxMod));
        props.setProperty("sonarTxPwr",              String.valueOf(this.sonarTxPwr));
        props.setProperty("sonarTxPulseLen",         String.valueOf(this.sonarTxPulseLen));
        props.setProperty("sonarFpgaLeds",           String.valueOf(this.sonarFpgaLeds));

        // Tag it as a valid SonarFish config file
        props.setProperty("sonarFishConfig",          "true");

        try {
            props.store(new FileWriter(fileName), "SonarFish configuration");
        } catch (IOException ex) {
            Logger.getLogger(SonarFishConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
   
    
    /** Set FPGA LEDs preference */
    public int sonarFpgaLeds(int value) {
        try {
            this.sonarFpgaLeds = sonarComCLI.sendCommand("sonar set_fpga_leds", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarFpgaLeds;
    }

    
    /** Set Blue LED Enable preference */
    public boolean sonarBlueLedEn(boolean value) {
        try {
            this.sonarBlueLedEn = sonarComCLI.sendCommand("sonar set_blue_led_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarBlueLedEn;
    }

    
    /** Set Red LED Enable preference */
    public boolean sonarRedLedEn(boolean value) {
        try {
            this.sonarRedLedEn = sonarComCLI.sendCommand("sonar set_red_led_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarRedLedEn;
    }

    
    /** Set Green LED Enable preference */
    public boolean sensorGreenLedEn(boolean value) {
        try {
            this.sensorGreenLedEn = sonarComCLI.sendCommand("sonar set_sens_green_led_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sensorGreenLedEn;
    }

    
    /** Set Yellow LED Enable preference */
    public boolean sensorYellowLedEn(boolean value) {
        try {
            this.sensorYellowLedEn = sonarComCLI.sendCommand("sonar set_sens_yellow_led_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sensorYellowLedEn;
    }

    
    /** Set Accelerometer Enable preference */
    public boolean sensorAccEn(boolean value) {
        try {
            this.sensorAccEn = sonarComCLI.sendCommand("sonar set_sens_acc_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sensorAccEn;
    }

    
    /** Set Current/Voltage Enable preference */
    public boolean sensorCVEn(boolean value) {
        try {
            this.sensorCVEn = sonarComCLI.sendCommand("sonar set_sens_cv_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sensorCVEn;
    }

    
    /** Set Compass Enable preference */
    public boolean sensorCompEn(boolean value) {
        try {
            this.sensorCompEn = sonarComCLI.sendCommand("sonar set_sens_comp_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sensorCompEn;
    }

    
    /** Set Pressure Enable preference */
    public boolean sensorPressEn(boolean value) {
        try {
            this.sensorPressEn = sonarComCLI.sendCommand("sonar set_sens_press_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sensorPressEn;
    }

    
    /** Set Sensor Record Enable preference */
    public boolean sensorRecEn(boolean value) {
        try {
            this.sensorRecEn = sonarComCLI.sendCommand("sonar set_sens_rec_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sensorRecEn;
    }

    
    /** Set Temperature Enable preference */
    public boolean sensorTempEn(boolean value) {
        try {
            this.sensorTempEn = sonarComCLI.sendCommand("sonar set_sens_temp_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarCom\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sensorTempEn;
    }


    /** Set Rx Dead Zone preference */
    public int sonarRxDeadZone(int value) {
        try {
            this.sonarRxDeadZone = sonarComCLI.sendCommand("sonar set_rx_dead_zone", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarRxDeadZone;
    }

    
    /** Set Rx On preference */
    public int sonarRxOn(int value) {
        try {
            this.sonarRxOn = sonarComCLI.sendCommand("sonar set_rx_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarRxOn;
    }

    
    /** Set Rx Gain type preference */
    public int sonarRxGainType(int value) {
        try {
            this.sonarRxGainType = sonarComCLI.sendCommand("sonar set_gain_type", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarRxGainType;
    }

    
    /** Set Rx Gain Offset preference */
    public int sonarRxGainOffset(int value) {
        try {
            this.sonarRxGainOffset = sonarComCLI.sendCommand("sonar set_gain_offset", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarRxGainOffset;
    }

    
    /** Set Rx Gain Slope preference */
    public int sonarRxGainSlope(int value) {
        try {
            this.sonarRxGainSlope = sonarComCLI.sendCommand("sonar set_gain_slope", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarRxGainSlope;
    }

    
    /** Set Rx Range preference */
    public int sonarRxRange(int value) {
        try {
            this.sonarRxRange = sonarComCLI.sendCommand("sonar set_range", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarRxRange;
    }

    
    /** Set Rx Record preference */
    public boolean sonarRxRecEn(boolean value) {
        try {
            this.sonarRxRecEn = sonarComCLI.sendCommand("sonar set_rx_rec_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarRxRecEn;
    }

    
    /** Set Tx On preference */
    public int sonarTxOn(int value) {
        try {
            this.sonarTxOn = sonarComCLI.sendCommand("sonar set_tx_en", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarTxOn;
    }

    
    /** Set Tx Modulation preference */
    public int sonarTxMod(int value) {
        try {
            this.sonarTxMod = sonarComCLI.sendCommand("sonar set_tx_mod", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarTxMod;
    }

    
    /** Set Tx Pwrer preference */
    public int sonarTxPwr(int value) {
        try {
            this.sonarTxPwr = sonarComCLI.sendCommand("sonar set_tx_pow", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarTxPwr;
    }

    
    /** Set Tx Pulse Length preference */
    public int sonarTxPulseLen(int value) {
        try {
            this.sonarTxPulseLen = sonarComCLI.sendCommand("sonar set_tx_pulse_len", value);
        } catch (TimeoutException ex) {
            JOptionPane.showMessageDialog(null, "Could not send command to SonarFish\n(" + ex.getMessage() + ")", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return this.sonarTxPulseLen;
    }

    /** Send all configuration settings to SonarFish */
    public boolean sendSettings() {
        // Check that port is open
        if (!this.sonarComCLI.isOpened()) {
            System.out.println("## Could not send config settings to SonarFish, CLI port is not open");
            return false;
        }
        
        sonarFpgaLeds(this.sonarFpgaLeds);
        sonarBlueLedEn(this.sonarBlueLedEn);    
        sonarRedLedEn(this.sonarRedLedEn);    
        sensorGreenLedEn(this.sensorGreenLedEn);    
        sensorYellowLedEn(this.sensorYellowLedEn);    
        sensorAccEn(this.sensorAccEn);    
        sensorCVEn(this.sensorCVEn);    
        sensorCompEn(this.sensorCompEn);    
        sensorPressEn(this.sensorPressEn);    
        sensorRecEn(this.sensorRecEn);    
        sensorTempEn(this.sensorTempEn);
        sonarRxDeadZone(this.sonarRxDeadZone);    
        sonarRxOn(this.sonarRxOn);    
        sonarRxGainType(this.sonarRxGainType);    
        sonarRxGainOffset(this.sonarRxGainOffset);    
        sonarRxGainSlope(this.sonarRxGainSlope);    
        sonarRxRange(this.sonarRxRange);    
        sonarRxRecEn(this.sonarRxRecEn);    
        sonarTxOn(this.sonarTxOn);    
        sonarTxMod(this.sonarTxMod);    
        sonarTxPwr(this.sonarTxPwr);    
        sonarTxPulseLen(this.sonarTxPulseLen);
        return true;
    }

}
