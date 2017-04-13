/*
 * Copyright 2015 Max Nilsson
 * Each line should be prefixed with  * 
 */
package SonarCom;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;



/**
 *
 * @author max
 */
public class SonarComCli {
    private final Object portLock = new Object();           // Lock for CLI Port
    private static SerialPort serialPort  = null;           // SCom CLI serial port
    private static BlockingQueue<String> receiveFifo;       // Local receive FIFO

    
    /** Creates Sonar Communication Command Line Interface (CLI) */
    public SonarComCli() {
        SonarComCli.receiveFifo = new LinkedBlockingQueue<>(3);
    }

    
    /** PortReader receives bytes from Serial Port and creates strings */
    private static class PortReader implements SerialPortEventListener {
        StringBuilder receivedString = new StringBuilder();
        
        @Override
        public void serialEvent(SerialPortEvent event) {
            try {
//                System.out.println("SerialPortEvent " + event.getEventType());
                if (event.isCTS()) {
                    System.out.println("CLI serialEvent: CTS = " + serialPort.isCTS());
                }
                if (event.isDSR()) {
                    System.out.println("CLI serialEvent: DSR = " + serialPort.isDSR());
                }
                if (event.isRXCHAR() && event.getEventValue() > 0) {
                    byte buf[] = serialPort.readBytes();
//                    System.out.println("CLI serialEvent got " + buf.length + " bytes");
                    for (byte b: buf) {
                        if (b == 0x00) {        // Check for end of string
                            String str = receivedString.toString();
//                            System.out.print("CLI serialEvent got string: " + str);
                            receiveFifo.add(str);          // Put string on response FIFO
                            receivedString.setLength(0);    // 'reset' reseiveString
                        } else {
                            receivedString.append((char)b);
                        }
                    }                
                }
            }
            catch (SerialPortException ex) {
                System.out.println("### Error in receiving string from serial port: " + ex);
            }
            catch (IllegalStateException ex) {
                System.out.println("### Error responseFifo full: " + ex);
            }
        }

    }


    /** Open CLI Serial port */
    public boolean openPort(String portName) {
        /* Check if there is a current port open */
        if (SonarComCli.serialPort != null) {
            System.out.println("Closing port \"" + SonarComCli.serialPort.getPortName() + "\" before opening \"" + portName + "\"");
            closePort();
        }
        
        if (portName == null) {
            return false;
        }
        
        try {
            SonarComCli.serialPort = new SerialPort(portName);
            System.out.println("Open CLI serial port: " + portName);
            if (!SonarComCli.serialPort.openPort()) {
                System.out.println(" failed to open Serial Port: " + portName);
                return false;
            }
            SonarComCli.serialPort.setParams(SerialPort.BAUDRATE_115200, 8, 1, 0);
            SonarComCli.serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
            SonarComCli.serialPort.setDTR(true);
            SonarComCli.serialPort.setRTS(true);
            
            SonarComCli.serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR |
                                                                      SerialPort.MASK_CTS |
                                                                      SerialPort.MASK_DSR | 0xFFFFFFFF);

/*            serialPort.addEventListener(new Reader(), SerialPort.MASK_RXCHAR |
                                                      SerialPort.MASK_RXFLAG |
                                                      SerialPort.MASK_CTS |
                                                      SerialPort.MASK_DSR |
                                                      SerialPort.MASK_RLSD);
*/
            return true;
        }
        catch (SerialPortException ex){
            System.out.println("### EXCEPTION; SonarComCLI.openPort(\"" + ex.getPortName() + "\") " + ex.getExceptionType());
            return false;
        }
    }

    
    /** Close CLI Serial Port */
    public boolean closePort() {
        try {
            if (this.serialPort != null && this.serialPort.isOpened()) {
                System.out.println("Disconnecting Serial Port: " + this.serialPort.getPortName());
                if (!this.serialPort.closePort()) {
                    System.out.println(" closePort() failed");
                    return false;
                }
            }
        }
        catch (SerialPortException ex){
            System.out.println("Serial port " + ex.getPortName() + " " + ex.getExceptionType());
            return false;
        }
        return true;
    }

    
    /** Check if CLI Serial port is open */
    public boolean isOpened() {
        return (this.serialPort != null) && this.serialPort.isOpened();
    }
    

    /** Send Command string with boolean parameter */
    public boolean sendCommand(String command, boolean value) throws TimeoutException {
        String stringValue = value ? "1" : "0";
//        if (this.debug)
//            System.out.println(command + " " + stringValue);
        String response = sendCommand(command + " " + stringValue);
//        return value;
        return response.contains("1");
    }

    
    /** Send Command string with integer parameter */
    public int sendCommand(String command, int value) throws TimeoutException {
        String stringValue = String.valueOf(value);
//        if (this.debug)
//            System.out.println(command + " " + stringValue);
        String response = sendCommand(command + " " + stringValue);
//        return value;
        return Integer.valueOf(response);
    }

    
    /** Send Command string */
    public String sendCommand(String command) throws TimeoutException {
        // Make sure response is received before next command is sent
        synchronized(portLock) {
            System.out.println("SendCommandString(\"" + command + "\")");
            try {
                /* Check if port is available, if not exit */
                if (serialPort == null) {
                    throw new TimeoutException("SonarCom CLI port not open");
                }
                
                /* Send command */
                serialPort.writeString(command + "\n");
               
                /* Get response string */
                String str = receiveFifo.poll(2000, TimeUnit.MILLISECONDS);
                if (str == null) {
                    throw new TimeoutException("SonarCom CLI: empty command response received");
                }

                System.out.println("SendCommandString response = \"" + str.trim() + "\"");
                return str;
            } catch (SerialPortException ex) {
                System.out.println("###EXCEPTION: SendString() " + ex.getLocalizedMessage());
                throw new TimeoutException("SonarCom CLI failed");
            } catch (InterruptedException ex) {
                Logger.getLogger(SonarComCli.class.getName()).log(Level.SEVERE, null, ex);
                throw new TimeoutException("SonarCom CLI failed");
            }
        }
    }


    /** Get Serial Port list */
    public String[] getPortNameList() {
        return SerialPortList.getPortNames(Pattern.compile("cu.(serial|usbserial|usbmodem).*"));
    }

    
    /** Get CLI Serial Port name */
    public String getPortName() {
        return serialPort.getPortName();
    }   
}
