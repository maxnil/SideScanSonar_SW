/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SonarCom;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
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
//public class SonarCom extends Thread {
public class SonarComData {
    int sleepDelay = 1000; // Sleep in ms

    BlockingQueue<GpsData>    guiGPSFifo;         // Reference to the GpsData data FIFO
    BlockingQueue<SensorData> guiSensorFifo;      // Reference to the SensorData data FIFO
    BlockingQueue<SonarData>  guiSonarDataFifo;   // Reference to the SonogramData data FIFO
    
//    private final GetSComDataTask  getSComDataTask; // SCom Data interface task
    
    private static SerialPort serialPort = null;    // SCom Sonar Data serial port
    private int nrPacketsReceived = 0;              // Number of packets received
    
    /** Creates Sonar Communication interface */
    public SonarComData(
            BlockingQueue<GpsData>    guiGPSFifo,
            BlockingQueue<SensorData> guiSensorFifo,
            BlockingQueue<SonarData>  guiSonarDataFifo) {
        this.guiGPSFifo       = guiGPSFifo;       // SFish->GUI GpsData data FIFO
        this.guiSensorFifo    = guiSensorFifo;    // SFish->GUI SensorData data FIFO
        this.guiSonarDataFifo = guiSonarDataFifo; // SFish->GUI SonogramData data FIFO
        
        /* Create and start SCom task */
//        getSComDataTask = new GetSComDataTask();
//        getSComDataTask.execute();
    }

    public enum DataPacketTypes {
        IDLE            (0x00),
        GPS             (0x01),
        SONAR           (0x02),
        SENSOR          (0x03),
        RESPONSE        (0x04),
        COMMAND         (0x05),
        PONG            (0x06),
        LAST_RESPONSE   (0x07),
        UNKNOWN         (0x08),
        NONE            (0x09);
         
        private final int value;

        DataPacketTypes(int value) {
            this.value = value;
        }
    }


    public class DataPacket {
        public int length;
        public DataPacketTypes type;
        public byte data[];
    }

    
    /** This task receives data packets from SonarCom module */
//    class GetSComDataTask extends SwingWorker<Void, Void> {
//
//        @Override
//        protected Void doInBackground() throws Exception {
//            int n = 0;
//            DataPacketTypes packetType;
//            DataPacket dataPacket;
//            
//            System.out.println(">>> GetDataTask started");
//
//            while (true) {
//                System.out.println(">>> GetDataTask tick: " + n);
//                if (isOpened()) {
//                    dataPacket = getDataPacket();
//                    if (dataPacket == null) {
//                        Thread.sleep(2000);
//                    } else {
//                        newPacketsReceived++;
//                    }
//                } else {
//                    Thread.sleep(1000);
//                }
//                n++;
//            }
//        }
//        
//        @Override
//        public void done() {
//            System.out.println("GetDataTask terminated for unknown reason");
//        }
//    }
    
    
    /** PortReader receives bytes from Serial Port and creates strings */
    private class PortReader implements SerialPortEventListener {
        byte[] buffer = new byte[4096];
        int index = -1;
        int dataLen = 0;
        byte currByte = 0x00;
        byte prevByte = 0x00;
        DataPacket dataPacket;
    
        @Override
        public void serialEvent(SerialPortEvent event) {
            try {
                if (event.isCTS()) {
                    System.out.println("Data serialEvent: CTS = " + serialPort.isCTS());
                }
                if (event.isDSR()) {
                    System.out.println("Data serialEvent: DSR = " + serialPort.isDSR());
                }
                if(event.isRXCHAR() && event.getEventValue() > 0) {
                    byte buf[] = serialPort.readBytes();
                    for (byte b: buf) {
                        prevByte = currByte;
                        currByte = b;
                        
                        if (index == -1) {
                            if ((prevByte & 0xFF) == 0xAF && (currByte & 0xFF) == 0xAF) {
                                index = 0;
                                dataPacket = new DataPacket();
                            }
                        } else {
                            buffer[index++] = b;
                        }
                        
                        // Get packet length
                        if (index == 2) {
                            dataPacket.length = (((int)buffer[1] & 0xFF)<<8) + (int)buffer[0] & 0xFF;
                        }
                        
                        // Get packet type
                        if (index == 3) {
//                            System.out.println("type b " + buffer[2]);
                            dataPacket.type = DataPacketTypes.values()[(int)buffer[2]];
//                            System.out.println("type t " + dataPacket.type);
                        }
                        
                        // Copy buffer (excluding first three bytes (length and type)) once we have received all data;
                        if (index > 3 && index > dataPacket.length) {
                            dataPacket.data = Arrays.copyOfRange(buffer, 3, index);
  //                          System.out.println("dataPacket.lengh = " + dataPacket.length);
  //                          StringBuilder sb = new StringBuilder();
  //                          for (int i = 0; i < index; i++) {
  //                              sb.append(String.format("%02X ", buffer[i]));
  //                          }
  //                          System.out.println("Buffer = " + sb);
                            switch (dataPacket.type) {
                                case GPS:
//                                    System.out.println("Data SerialEvent GPS packet");
                                    guiGPSFifo.add(new GpsData(dataPacket));
                                    break;
                                case SONAR:
                                    System.out.println("Data SerialEvent SONAR packet");
                                    guiSonarDataFifo.add(new SonarData(dataPacket));
                                    break;
                                case SENSOR:
//                                    System.out.println("Data SerialEvent SENSOR packet");
                                    guiSensorFifo.add(new SensorData(dataPacket));
                                    break;
                                case PONG:
                                    break;
                                default:
                                    System.out.println("Data SerialEvent default packet (" + dataPacket.type + ")");
                            }
                            nrPacketsReceived++;
                            index = -1;     // Reset index
                        }
                    }                
                }
            }
            catch (SerialPortException ex) {
                System.out.println("### Error in receiving string from serial port: " + ex);
            }
            catch (IllegalStateException ex) {
                System.out.println("### Error packetFifo full: " + ex);
            }
        }

    }


    /** Open serial port */
    public boolean openPort(String portName) {
        /* Check if there is a current port open */
        if (SonarComData.serialPort != null) {
            closePort();
        }
        
        if (portName == null) {
            return false;
        }
        
        try {
            serialPort = new SerialPort(portName);
            System.out.println("Open Data serial port: " + portName);
            if (!serialPort.openPort()) {
                System.out.println(" failed to open Serial Port: " + portName);
                return false;
            }
            serialPort.setParams(SerialPort.BAUDRATE_115200, 8, 1, 0);
            serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
            serialPort.setDTR(true);
            serialPort.setRTS(true);

/*            serialPort.addEventListener(new Reader(), SerialPort.MASK_RXCHAR |
                                                      SerialPort.MASK_RXFLAG |
                                                      SerialPort.MASK_CTS |
                                                      SerialPort.MASK_DSR |
                                                      SerialPort.MASK_RLSD);
*/
            serialPort.addEventListener(new SonarComData.PortReader(), SerialPort.MASK_RXCHAR |
                                                                       SerialPort.MASK_CTS |
                                                                       SerialPort.MASK_DSR);

            return true;
        }
        catch (SerialPortException ex){
            System.out.println("###EXCEPTION; SonarComData.openPort(\"" + ex.getPortName() + "\") " + ex.getExceptionType());
            return false;
        }
    }
    
    
    /** Close Serial Port */
    public boolean closePort() {
        try {
            if (serialPort != null && serialPort.isOpened()) {
                System.out.println("Disconnecting Serial Port: " + serialPort.getPortName());
                if (!serialPort.closePort()) {
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

    
    /** Check if Data port is open */
    public boolean isOpened() {
        return (serialPort != null) && serialPort.isOpened();
    }

    
    /** Get data packet */
//    private DataPacket getDataPacket() {
//        byte byteArray[];
//        int prevByte;
//        int currByte;
//        DataPacket dataPacket;
//        
//        if (this.serialPort == null) {
//            return null;
//        }
//        
//        dataPacket = new DataPacket();
//
//        prevByte = 0x00;
//        currByte = 0x00;
//        
//        try {
//            // Locate start of packet (0xAFAF)
//            do {
//                byteArray = this.serialPort.readBytes(1, 10000);
//                prevByte = currByte;
//                currByte = byteArray[0] & 0xFF;
//                System.out.println("GetDataPacket() header search = " + Integer.toHexString(prevByte) + " " + Integer.toHexString(currByte));
//            } while (!(currByte == 0xAF && prevByte == 0xAF));
//
//            // Get packet length
//            byteArray = this.serialPort.readBytes(2, 100);
////            System.out.println("Length field 0 = " + Integer.toHexString(byteArray[0] & 0xFF));
////            System.out.println("Length field 1 = " + Integer.toHexString(byteArray[1] & 0xFF));
//            dataPacket.length = (((int)byteArray[1] & 0xFF)<<8) + (int)byteArray[0] & 0xFF;
//            
//            // Get packet type
//            byteArray = this.serialPort.readBytes(1, 100);
//            dataPacket.type = DataPacketTypes.values()[(int)byteArray[0]];
//    
//            System.out.println("Packet length: " + dataPacket.length);
//            System.out.println("Packet type: " + dataPacket.type);
//            
//            dataPacket.data = this.serialPort.readBytes(dataPacket.length - 5, 1000);
//            
//        } catch (SerialPortException | SerialPortTimeoutException ex) {
//            System.out.println("###EXCEPTION: getDataPacket() " + ex.getLocalizedMessage());
//            return null;
//        }
//        
//        return dataPacket;
//    }

    
    /** Get Serial Port list */
    public String[] getPortNameList() {
        return SerialPortList.getPortNames(Pattern.compile("cu.(serial|usbserial|usbmodem).*"));
    }

    
    /** Get Data Serial Port name */
//    public String getPortName() {
//        return serialPort.getPortName();
//    }

    
    /** Check if new data has been received since last check */
    public int nrPacketsReceived() {
        return this.nrPacketsReceived;
    }
}