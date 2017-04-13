/*
 * Copyright Max Nilsson 2015
 */
package SonarCom;

import SonarCom.Dialogs.SensorJDialog;
import SonarCom.Dialogs.PrefsJDialog;
import SonarCom.Dialogs.InfoJDialog;
import SonarCom.Dialogs.GpsJDialog;
import SonarCom.SonarComData.DataPacket;
import java.awt.Color;
import static java.awt.EventQueue.isDispatchThread;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.MouseInputAdapter;


/**
 *
 * @author max
 */
public class ComInterfaceJFrame extends javax.swing.JFrame {
    private static final int SIDE_MARGIN = 20;      // Left margin
    
    private final HistogramJPanel histogramPanel;
    private final SonogramJPanel  sonogramPanel;
    
    private final HMarkerImage hMarkerImage;
  
    private final SonarComConfig  sonarComConfig;
    private final SonarFishConfig sonarFishConfig;
    
    private final BlockingQueue<SonarData> guiSonarDataFifo;

    private final SonarDataReceiveTask      sonarDataReceiveTask;
    private final ConnectionCheckTask  connectionCheckTask;
    
    private final SonarComCli  sonarComCLI;
    private final SonarComData sonarComData;
//    private final ConnectionStatus connectionStatus; 
    
    private SensorJDialog sensorJDialog = null;
    private GpsJDialog gpsJDialog = null;
    private InfoJDialog infoDialog = null;
    
    private SonogramGraphics.ColorMaps colorMap = SonogramGraphics.ColorMaps.BONE;

    /**
     * Creates new form NewJFrame
     * @param sonarCom
     */
    public ComInterfaceJFrame(SonarComConfig sonarComConfig,
            SonarFishConfig sonarFishConfig,
            SonarComCli sonarComCLI,
            SonarComData sonarComData,
            BlockingQueue<GpsData>    gpsFifo,
            BlockingQueue<SensorData> sensorFifo,
            BlockingQueue<SonarData>  guiSonarDataFifo) throws InterruptedException {
        this.sonarComConfig   = sonarComConfig;
        this.sonarFishConfig  = sonarFishConfig;
        this.sonarComCLI      = sonarComCLI;
        this.sonarComData     = sonarComData;
        this.guiSonarDataFifo = guiSonarDataFifo;
        
        this.hMarkerImage = new HMarkerImage();
        
        if (isDispatchThread()) System.out.println("WARNING ComInterfaceJFrame in DT");
                
                
        histogramPanel = new HistogramJPanel(this.hMarkerImage);
        sonogramPanel  = new SonogramJPanel(this.hMarkerImage);
        
//        this.connectionStatus = new ConnectionStatus();
        
        // Initialize Swing stuff in the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            if (!isDispatchThread()) System.out.println("WARNING ComInterfaceJFrame.initComponents not in DT");
//            System.out.println("%% Before initcomponents");
            initComponents();

//            System.out.println("%%  Before javax.swing.GroupLayout(jPanelHistogram)");
            javax.swing.GroupLayout jPanelDisplayLayoutHistogram = new javax.swing.GroupLayout(jPanelHistogram);

//            System.out.println("%%  Before jPanelHistogram.setLayout(jPanelDisplayLayoutHistogram)");
            jPanelHistogram.setLayout(jPanelDisplayLayoutHistogram);

//            System.out.println("%%  Before jPanelDisplayLayoutHistogram.setHorizontalGroup");
            jPanelDisplayLayoutHistogram.setHorizontalGroup(jPanelDisplayLayoutHistogram.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(histogramPanel)
            );

        //%  Before jPanelDisplayLayoutHistogram.setHorizontalGroup
        //Exception in thread "AWT-EventQueue-0" java.lang.IllegalStateException: sonar1.ComInterfaceJFrame$HistogramJPanel[,0,0,0x0,invalid,layout=java.awt.FlowLayout,alignmentX=0.0,alignmentY=0.0,border=,flags=9,maximumSize=,minimumSize=,preferredSize=] is not attached to a horizontal group
        //%  Before jPanelDisplayLayoutHistogram.setVerticalGroup

//            System.out.println("%%  Before jPanelDisplayLayoutHistogram.setVerticalGroup");
            jPanelDisplayLayoutHistogram.setVerticalGroup(jPanelDisplayLayoutHistogram.createBaselineGroup(true, true)
                    .addComponent(histogramPanel)
            );
//            System.out.println("%%  After jPanelDisplayLayoutHistogram.setVerticalGroup");

            javax.swing.GroupLayout jPanelDisplayLayoutSonogram = new javax.swing.GroupLayout(jPanelSonogram);
            jPanelSonogram.setLayout(jPanelDisplayLayoutSonogram);
            jPanelDisplayLayoutSonogram.setHorizontalGroup(jPanelDisplayLayoutSonogram.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sonogramPanel)
            );
            jPanelDisplayLayoutSonogram.setVerticalGroup(jPanelDisplayLayoutSonogram.createBaselineGroup(true, true)
                    .addComponent(sonogramPanel)
            );

        //    pack();

        //        System.out.println("Starting ConnectionKeeperTask");
            repaint();
            setLocationRelativeTo(null);
            setVisible(true);
        });
        
        // Create and start tasks
        connectionCheckTask = new ConnectionCheckTask();
        connectionCheckTask.execute();
        sonarDataReceiveTask = new SonarDataReceiveTask();
        sonarDataReceiveTask.execute();
        
        // Create GpsData and SensorData status windows
        this.gpsJDialog    = new GpsJDialog(gpsFifo);
        this.sensorJDialog = new SensorJDialog(sensorFifo);
        
        // Send default settings to SonarCom
        sonarComConfig.sendSettings();
        sonarFishConfig.sendSettings();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        hMarkerImage.paint(histogramPanel.getWidth());
//        System.out.println("ComInterfaceJFrame.paint");
    }

    // HMarkerImage contains a horizontal marker image
    private static class HMarkerImage {
        static final int MARKER_HEIGHT = 20;    // Marker height
        private BufferedImage image;
        private int leftRange = 0;
        private int rightRange = 0;
        private int range = 0;
        private double leftRangeScale = 1.0;
        private double rightRangeScale = 1.0;
        
        public HMarkerImage() {
            // Create a minimal image to start with
            this.image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        public void setRange(int range) {
            if (range != this.range) {
                this.range = range;
                paint(image.getWidth());
            }
        }
        
        public void setRangeScale(double leftRangeScale, double rightRangeScale) {
            this.leftRangeScale = leftRangeScale;
            this.rightRangeScale = rightRangeScale;
        }
        
        private void drawMarker(Graphics2D g2d, int xPos, int yPos, int value, boolean printValue) {
            //int[] x = {-5, 5, 0, -5};
            //int[] y = {10, 10, 0, 10};
            int[] x = {0, -4, 0, 4, 0};
            int[] y = {10, 5, 0, 5, 10};
            Polygon polygon = new Polygon(x, y, x.length);
            polygon.translate(xPos, yPos);
            g2d.fillPolygon(polygon);
            if (printValue) {
                String str = value + " m";
                g2d.drawString(str, (int)(xPos - (double)str.length()*3.6), yPos + HMarkerImage.MARKER_HEIGHT);
            }
        }

        private void drawHMarkers(Graphics2D g2d, int width, int leftRange, int rightRange) {            
            int majorInterval;
            int minorInterval;
            int xPos;            
            int gfxWidth;
            double xStep;           
            boolean drawValue;

            gfxWidth = width - 2 * ComInterfaceJFrame.SIDE_MARGIN;
            xStep = (double)gfxWidth / (double)(leftRange + rightRange);
            
            if (Math.max(leftRange, rightRange) > 200) {
                majorInterval = 50;
                minorInterval = 10;
            } else if (Math.max(leftRange, rightRange) > 100) {
                majorInterval = 25;
                minorInterval = 5;
            } else if (Math.max(leftRange, rightRange) > 50) {
                majorInterval = 10;
                minorInterval = 2;
            } else {
                majorInterval = 5;
                minorInterval = 1;
            }
            
            g2d.setColor(Color.GRAY);

            // Draw left markers
            for (int i = 0; i < leftRange; i += majorInterval) {
                drawValue = xStep * (leftRange - i) > 35;
                drawMarker(g2d, (int)(xStep * (leftRange - i)) + ComInterfaceJFrame.SIDE_MARGIN, 0, i, drawValue);
            }

            // Draw right markers
            for (int i = 0; i < rightRange; i += majorInterval) {
                drawValue = xStep * (leftRange + i) < (gfxWidth - 35);
                drawMarker(g2d, (int)(xStep * (leftRange + i)) + ComInterfaceJFrame.SIDE_MARGIN, 0, i, drawValue);
            }
      
            // Left and right endpoints
            drawMarker(g2d, ComInterfaceJFrame.SIDE_MARGIN           , 0, leftRange, true);
            drawMarker(g2d, ComInterfaceJFrame.SIDE_MARGIN + gfxWidth, 0, rightRange, true);

            // Left Minor markers
            for (int i = 0; i < leftRange - 1; i += minorInterval) {
                xPos = (int)(xStep * (leftRange - i)) + ComInterfaceJFrame.SIDE_MARGIN;
                g2d.drawLine(xPos, 0, xPos, 5);
            }
            
            // Right Minor markers
            for (int i = 0; i < rightRange - 1; i += minorInterval) {
                xPos = (int)(xStep * (leftRange + i)) + ComInterfaceJFrame.SIDE_MARGIN;
                g2d.drawLine(xPos, 0, xPos, 5);
            }
            
            g2d.dispose();            
        }

        public void paint(int width) {
            int tempLeftRange  = (int)(this.range * this.leftRangeScale);
            int tempRightRange = (int)(this.range * this.rightRangeScale);
            
            // Check if range or Width has changed
            if (this.image.getWidth() != width || this.leftRange != tempLeftRange || this.rightRange != tempRightRange) {
                // Draw markers
                this.image = new BufferedImage(width, HMarkerImage.MARKER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                drawHMarkers(this.image.createGraphics(), this.image.getWidth(), tempLeftRange, tempRightRange);
                this.leftRange = tempLeftRange;
                this.rightRange = tempRightRange;
              
//                System.out.println("HMarkerImage.paint: drawMarkers leftRange = " + tempLeftRange + ", rightRange = " + tempRightRange);
            }
        }

        public int getHeight() {
            return this.image.getHeight();
        }
        
        @Override
        public String toString() {
            return "HMarkerImage:";
        }
    }
 
    private static class HistogramJPanel extends JPanel {
        private final HistogramImage histogramImage;           // GFX image container
        private final HMarkerImage hMarkerImage;
        
        private double leftRangeScale = 1.0;            // Left range (%)
        private double rightRangeScale = 1.0;           // Right range (%)
        
        public HistogramJPanel(HMarkerImage hMarkerImage) {
            this.hMarkerImage = hMarkerImage;
            this.histogramImage = new HistogramImage(this.getBackground());
        }

        // HistogramImage contains the graphical Histogram image
        private class HistogramImage {
            private static final int BUFFER_DEPTH = 256;
            private final Color backgroundColor;
            private BufferedImage image;
            private int totalDataLength;

            public HistogramImage(Color backgroundColor) {
                this.totalDataLength = 0;
                this.image = null;
                this.backgroundColor = backgroundColor;
            }

            public void paint(BufferedImage bufferImage, double leftRangeScale, double rightRangeScale) {
                if (this.image != null) {
                    // Draw the image onto the Graphics reference
                    int dx1 = 0;
                    int dy1 = 0;
                    int dx2 = bufferImage.getWidth();
                    int dy2 = bufferImage.getHeight();
                    int sx1 = this.image.getWidth() / 2 - (int)(this.totalDataLength / 2 * leftRangeScale);
                    int sy1 = 0;
                    int sx2 = this.image.getWidth() / 2 + (int)(this.totalDataLength / 2 * rightRangeScale);
                    int sy2 = this.image.getHeight();

                    Graphics g = bufferImage.createGraphics();
                    g.drawImage(this.image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
                }
            }

            public void drawHistogram(SonarData ping) {
                int y;
                int imageWidth;
                int imageHeight;
                Graphics2D g2d;

                int dataLength = ping.leftData.length + ping.rightData.length;

                // If dimensions has changed, create a new image
                if (this.image == null || this.totalDataLength != dataLength) {
                    this.image = new BufferedImage(dataLength, HistogramImage.BUFFER_DEPTH, BufferedImage.TYPE_INT_ARGB);
                    this.totalDataLength = dataLength;
//                    System.out.println("drawHistogram: new image totalDataLength = " + dataLength);
                }

                g2d = this.image.createGraphics();

                g2d.setBackground(this.backgroundColor);
                g2d.setColor(Color.BLACK);

                imageWidth  = this.image.getWidth();
                imageHeight = this.image.getHeight();

                g2d.clearRect(0, 0, imageWidth, imageHeight);       // Clear image

                // Draw left data
                for (int i = 0; i < ping.leftData.length; i++) {
                    y = (int)(ping.leftData[ping.leftData.length - i - 1] * imageHeight);
                    g2d.drawLine(i, imageHeight - y, i, imageHeight);
                }

                // Draw right data
                for (int i = 0; i < ping.rightData.length; i++) {
                    y = (int)(ping.rightData[ping.rightData.length - i - 1] * imageHeight);
                    g2d.drawLine(imageWidth - i - 1, imageHeight - y, imageWidth - i - 1, imageHeight);
                }
                g2d.dispose();
                
                repaint();
            }

            @Override
            public String toString() {
                return "HistogramImage: totalDataLength = " + this.totalDataLength;
            }
        }

        public void setRangeScale(double leftRangeScale, double rightRangeScale) {
            this.leftRangeScale = leftRangeScale;
            this.rightRangeScale = rightRangeScale;
        }
        
//        @Override
//        public Dimension getPreferredSize() {
//            System.out.println("getPreferredSize 2" +
//                    super.getPreferredSize());
//            return isPreferredSizeSet() ?
//                    super.getPreferredSize() : new Dimension(MAX_WIDTH, MAX_HEIGHT);
//        }
        
        @Override
        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);
            BufferedImage panelImage;
            
//            System.out.println("HistogramJPanel().paintComponent");
            
            // Get an Image pice if the JPanel that we can paint the SonoGram on
            panelImage = (BufferedImage)(this.createImage(this.getWidth() - 2 * ComInterfaceJFrame.SIDE_MARGIN, this.getHeight()- HMarkerImage.MARKER_HEIGHT));            
            // Paint the Sonogram images
            histogramImage.paint(panelImage, this.leftRangeScale, this.rightRangeScale);
            g.drawImage(panelImage, ComInterfaceJFrame.SIDE_MARGIN, 0, null);
            
            // Paint the Marker image
            g.drawImage(hMarkerImage.image, 0, this.getHeight() - hMarkerImage.getHeight() - 1, null); 
            g.dispose();
        }
    }
    
    private static class SonogramJPanel extends JPanel {
        public static final int MAX_BUFFER_DEPTH = 1000;
        public static final int MARKER_WIDTH = 10;    // Marker width
        
        private final SonogramImage sonogramImage;
        private final HMarkerImage hMarkerImage;
        
        private double leftRangeScale = 1.0;            // Left range (%)
        private double rightRangeScale = 1.0;           // Right range (%)
    
        private BufferedImage image;
        private Rectangle shape;
       
        // Constructor for SonogramJPanel
        public SonogramJPanel(HMarkerImage hMarkerImage) {
            // Create the SonogramImage buffer
            this.sonogramImage = new SonogramImage();
            
            // Create the Horizontal Marker image
            this.hMarkerImage = hMarkerImage;
            
            MyMouseListener ml = new MyMouseListener();
            addMouseListener(ml);
            addMouseMotionListener(ml);
//            System.out.println("DrawingArea");
        }
    
        public void setRangeScale(double leftRangeScale, double rightRangeScale) {
            this.leftRangeScale = leftRangeScale;
            this.rightRangeScale = rightRangeScale;
        }
        
        // SonogramImage contains the graphical Sonogram image
        private class SonogramImage {
            private static final int SUBIMAGE_BUFFER_DEPTH = 100;
            private double yMinorDistance = 0.0;
            private int minorCount = 0;
            private SonogramSubImage firstSubImage;

            private SonogramImage() {
                // Create a first minimal sub-image (1x1)
                firstSubImage = new SonogramSubImage(1, 1, null, 1, 0);
            }

            private class SonogramSubImage {
                private final BufferedImage sonoImage;          // This Sonogram sub-image
                private final BufferedImage vMarkerImage;       // This vertical marker sub-image
                private final int totalDataWidth;               // Left + Right data width
                private final int nr;                           // Sub-image number
                private int yPos;                               // Current Y-position
                private SonogramSubImage nextSubImage;          // Next sub-image in the list

                // Create sub-image
                SonogramSubImage(int height, int vMarkerWidth, SonogramSubImage nextSubImage, int dataLength, int nr) {
                    this.nextSubImage = nextSubImage;
                    this.yPos = 0 - height;
                    this.nr = nr;
                    this.totalDataWidth = dataLength;
                    this.sonoImage = new BufferedImage(dataLength, height, BufferedImage.TYPE_INT_ARGB);    
                    this.vMarkerImage = new BufferedImage(vMarkerWidth * 2, height, BufferedImage.TYPE_INT_ARGB);
//                    System.out.println("SonogramSubImage[" + this.nr + "] new image totalDataLength = " + dataLength);
                }

    //            protected void finalize() {
    //                System.out.println("SonogramSubImage[" + this.nr + "].finalize()");
    //            }

                // Paint sub-image
                public void paint(BufferedImage bufferImage, double leftRangeScale, double rightRangeScale) {
                    // Draw the image onto the Graphics reference
                    Graphics g = bufferImage.createGraphics();
                    int dx1, dy1, dx2, dy2;
                    int sx1, sy1, sx2, sy2;

                    // Paint left vertical marker image
                    dx1 = 0;
                    dy1 = 0;
                    dx2 = this.vMarkerImage.getWidth();
                    dy2 = (int)(bufferImage.getHeight() / ((leftRangeScale + rightRangeScale)/2));
                    sx1 = 0;
                    sy1 = 0 - this.yPos;
                    sx2 = this.vMarkerImage.getWidth();
                    sy2 = bufferImage.getHeight() - this.yPos;
                    g.drawImage(this.vMarkerImage, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);

                    // Paint right vertical marker image
                    dx1 = bufferImage.getWidth() - this.vMarkerImage.getWidth();
                    dy1 = 0;
                    dx2 = bufferImage.getWidth();
                    dy2 = (int)(bufferImage.getHeight() / ((leftRangeScale + rightRangeScale)/2));
                    sx1 = 0;
                    sy1 = 0 - this.yPos;
                    sx2 = this.vMarkerImage.getWidth();
                    sy2 = bufferImage.getHeight() - this.yPos;
                    g.drawImage(this.vMarkerImage, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);

                    // Paint sonogram image
                    dx1 = this.vMarkerImage.getWidth()/2;
                    dy1 = 0;
                    dx2 = bufferImage.getWidth() - this.vMarkerImage.getWidth()/2;
                    dy2 = (int)(bufferImage.getHeight() / ((leftRangeScale + rightRangeScale)/2));
                    sx1 = this.sonoImage.getWidth() / 2 - (int)(this.totalDataWidth / 2 * leftRangeScale);
                    sy1 = 0 - this.yPos;
                    sx2 = this.sonoImage.getWidth() / 2 + (int)(this.totalDataWidth / 2 * rightRangeScale);
                    sy2 = bufferImage.getHeight() - this.yPos;
    //                System.out.println("SubImage[" + this.nr + "].paint");
                    g.drawImage(this.sonoImage, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
                    g.dispose();

                    if (this.nextSubImage != null && this.nextSubImage.getYPos() < bufferImage.getHeight()) {
                        this.nextSubImage.paint(bufferImage, leftRangeScale, rightRangeScale);
                    }
                }

                // Get current Y-position for sub-image
                public int getYPos() {
                    return this.yPos;
                }

                // Increase the Y position
                public void stepYPos() {
                    this.yPos++;
     //               System.out.println("SonogramSubImage[" + this.nr + "].increaseY() = " + this.yPos);
                    // Move next SubImages (if any), and check if it is off screen
                    if (this.nextSubImage != null && this.nextSubImage.getYPos() < SonogramJPanel.MAX_BUFFER_DEPTH) {
                        this.nextSubImage.stepYPos();
                    } else {
                        if (this.nextSubImage != null) {
     //                       System.out.println("SonogramSubImage[" + this.nextSubImage.nr + "] is off screen, killing it!");
                        }
                        this.nextSubImage = null;   // Kill the image below
                    }
                }

                // Draw vertical marker
                public void drawVMarker(int width, Color color) {
                    Graphics2D g2d;
//                    System.out.println("SonogramSubImage[" + this.nr + "].drawVMarker()");

                    int yOffset = 0 - this.yPos - 1;

                    g2d = this.vMarkerImage.createGraphics();
                    g2d.setColor(color);
                    g2d.drawLine(vMarkerImage.getWidth()/2 - width, yOffset, vMarkerImage.getWidth()/2 + width, yOffset);
                    g2d.dispose();
                }

                // Draw one line of Sonogram on sub-image
                public void drawSonogramLine(SonarData ping, SonogramGraphics.ColorMaps cm) {
                    double val;
                    int yOffset = 0 - this.yPos - 1;

                    for (int i = 0; i < ping.leftData.length; i++) {
                        val = ping.leftData[i];
                        this.sonoImage.setRGB(this.sonoImage.getWidth()/2 - i - 1, yOffset, SonogramGraphics.rgbColor(val, cm));
                    }

                    // Draw right data
                    for (int i = 0; i < ping.rightData.length; i++) {
                        val = ping.rightData[i];
                        this.sonoImage.setRGB(this.sonoImage.getWidth()/2 + i, yOffset, SonogramGraphics.rgbColor(val, cm));
                    }
                }

                @Override
                public String toString() {
                    return "SonogramSubmage: nr = " + this.nr + ", totalDataLength = " + totalDataWidth;
                }       
            }

            private void drawSonogram(SonarData ping, SonogramGraphics.ColorMaps cm) {
                int majorInterval;
                int minorInterval;

                int dataLength = ping.leftData.length + ping.rightData.length;

                if (dataLength != this.firstSubImage.totalDataWidth || this.firstSubImage.getYPos() >= 0) {
                    this.firstSubImage = new SonogramSubImage(SonogramImage.SUBIMAGE_BUFFER_DEPTH, ComInterfaceJFrame.SIDE_MARGIN, this.firstSubImage, dataLength, this.firstSubImage.nr + 1);            
                }

                if (ping.range() > 200) {
                    majorInterval = 5;
                    minorInterval = 10;
                } else if (ping.range() > 100) {
                    majorInterval = 5;
                    minorInterval = 5;
                } else if (ping.range() > 50) {
                    majorInterval = 5;
                    minorInterval = 2;
                } else {
                    majorInterval = 5;
                    minorInterval = 1;
                }

                this.yMinorDistance += ping.yResolution();

                if (this.yMinorDistance > (double)minorInterval) {
                    this.yMinorDistance -= (double)minorInterval;
                    this.minorCount++;
                    if (this.minorCount < majorInterval) {
                        this.firstSubImage.drawVMarker(5, Color.GRAY);      // Minor marker
                    } else {
                        this.firstSubImage.drawVMarker(10, Color.BLACK);    // Major marker
                        this.minorCount = 0;
                    }

                }

                // Draw one line of Sonogram data on the first sub-image
                this.firstSubImage.drawSonogramLine(ping, cm);

                // Recursevly step the Y-position on all sub-images
                this.firstSubImage.stepYPos();
                
                repaint(); // Kanske ska denna ligga 'process' tasken?
            }

            private void paint(BufferedImage bufferImage, double leftRangeScale, double rightRangeScale) {
                this.firstSubImage.paint(bufferImage, leftRangeScale, rightRangeScale);
            }

            @Override
            public String toString() {
                return "SonogramImage: ";
            }
        }

//      @Override
//        public Dimension getPreferredSize() {
//            System.out.println("getPreferredSize" + (isPreferredSizeSet() ?
//                    super.getPreferredSize() : new Dimension(Max_WIDTH, MAX_HEIGHT)));
//            return isPreferredSizeSet() ?
//                    super.getPreferredSize() : new Dimension(Max_WIDTH, MAX_HEIGHT);
//        }
        
        @Override
        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);
            BufferedImage panelImage;
           
            // Get an Image pice if the JPanel that we can paint the SonoGram on
            panelImage = (BufferedImage)(this.createImage(this.getWidth(), this.getHeight() - this.hMarkerImage.getHeight()));            
            // Paint the Sonogram image
            sonogramImage.paint(panelImage, this.leftRangeScale, this.rightRangeScale);
            g.drawImage(panelImage, 0, 0, null);

            g.drawImage(this.hMarkerImage.image, 0, this.getHeight() - this.hMarkerImage.getHeight() - 1, null); 
            g.dispose();
        }

        public void addRectangle(Rectangle rectangle, Color color) {
            //  Draw the Rectangle onto the BufferedImage
            Graphics2D g2d = (Graphics2D)image.getGraphics();
            g2d.setColor(color);
            g2d.draw(rectangle);
            repaint();
//            System.out.println("addRectangle");
        }
                
        class MyMouseListener extends MouseInputAdapter {
            private Point startPoint;

            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                shape = new Rectangle();
                System.out.println("mousedPressed");
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int x = Math.min(startPoint.x, e.getX());
                int y = Math.min(startPoint.y, e.getY());
                int width = Math.abs(startPoint.x - e.getX());
                int height = Math.abs(startPoint.y - e.getY());

                shape.setBounds(x, y, width, height);
                repaint();
                System.out.println("mouseDragged");
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (shape.width != 0 || shape.height != 0) {
                    addRectangle(shape, Color.RED);
                }

                shape = null;
                System.out.println("DrawingArea mouseReleased");
            }
        }
    }
 
    class ConnectionStatus {
        boolean sonarComCliConnected  = false;
        boolean sonarComDataConnected = false;
        boolean sonarFishConnected    = false;
    }

    
    /**
     * This class handles the reception of Sonar Data from the Sonar Fish
     */
    class SonarDataReceiveTask extends SwingWorker<Void, SonarData> {
        /*
         * Main DataFetchTask. Executed in background thread.
         */
        @Override
        public Void doInBackground() throws InterruptedException {
            SonarData ping;
            
            while (true) {
//                System.out.println("DataFetch tick");
//                System.out.println("pingFifo size " + pingFifo.size());
                
                // Get ping from FIFO (wait if empty)

                ping = null; //guiSonarDataFifo.take();
                
                // Update horizontal marker range
                hMarkerImage.setRange((int)ping.range());
                
                // Draw histogram
                histogramPanel.histogramImage.drawHistogram(ping);
                
                // Draw sonogram
                sonogramPanel.sonogramImage.drawSonogram(ping, colorMap);
                
                publish(ping);
            }
        }

        @Override
        protected void process(List<SonarData> chunks) {
            SonarData ping = chunks.get(chunks.size()-1);

            // Update status fields
            updatePingStatusFields(ping);            
        }
        
        @Override
        public void done() {
            System.out.println("DataFetcherTask done(?)");
        }
    }

    
    /**
     * This task checks the connection to the SonarCom and SonarFish
     */
    class ConnectionCheckTask extends SwingWorker<Void, ConnectionStatus> {
        private int nrPacketsReceived = 0;
        
        /*
         * Main DataFetchTask. Executed in background thread.
         */
        @Override
        protected Void doInBackground() throws InterruptedException {
            int count = 0;
            System.out.println(">>> ConnectionCheckTask started");
            String response;
            ConnectionStatus connectionStatus = new ConnectionStatus();
            int oldNrDataPacketsReceived = 0;
            int newNrDataPacketsReceived = 0;
            
            while (true) {
                if (sonarComConfig.sonarComAutoConnect) {
                    connectionStatus.sonarFishConnected = false;
                    connectionStatus.sonarComCliConnected = false;
                    connectionStatus.sonarComDataConnected = false;
                    
//                    System.out.println(">>> ConnectionCheckTask tick: " + count);
  
                    try {
                        /* Check if there is a connection to the SonarCom CLI interface */
                        response = sonarComCLI.sendCommand("ping");
//                        System.out.println("ConnectionCheckTask1 got response: " + response);
                        connectionStatus.sonarComCliConnected = response.contains("scom_pong");
                       
                        if (connectionStatus.sonarComCliConnected) {
                            response = sonarComCLI.sendCommand("sonar ping");
//                            System.out.println("ConnectionCheckTask2 got response: " + response);
                            connectionStatus.sonarFishConnected = response.contains("sonar_pong1");
                        }

                        newNrDataPacketsReceived = sonarComData.nrPacketsReceived();
                        connectionStatus.sonarComDataConnected = (newNrDataPacketsReceived > oldNrDataPacketsReceived);
                        oldNrDataPacketsReceived = newNrDataPacketsReceived;
                      
                    } catch (TimeoutException ex) {
                        System.out.println("ConnectionCheckTask timeout");
                    }

                    if (!connectionStatus.sonarComCliConnected) {
                        System.out.println("Reopen port");
                        sonarComCLI.openPort(sonarComConfig.sonarComCLIPortName);
                        sonarComData.openPort(sonarComConfig.sonarComDataPortName);
                    }
                        
                    publish(connectionStatus);  // Update GUI
                    count++;
                    Thread.sleep(500);
                }
            }
        }

        @Override
        protected void done() {
            System.out.println("ConnectionCheckTask terminated(?)");
        }
        
        @Override
        protected void process(List<ConnectionStatus> chunks) {
            ConnectionStatus connectionStatus = chunks.get(chunks.size() - 1);
            
            if (connectionStatus.sonarComCliConnected && connectionStatus.sonarComDataConnected) {
                jLabelSComStatus.setBackground(Color.GREEN);
                jLabelSComStatus.setText("connected");
            } else if (connectionStatus.sonarComCliConnected || connectionStatus.sonarComDataConnected) {
                jLabelSComStatus.setBackground(Color.ORANGE);
                jLabelSComStatus.setText("partial");
            } else {
                jLabelSComStatus.setBackground(Color.RED);
                jLabelSComStatus.setText("disconnected");
            }

            if (connectionStatus.sonarFishConnected) {
                jLabelSFishStatus.setBackground(Color.GREEN);
                jLabelSFishStatus.setText("connected");
            } else {
                jLabelSFishStatus.setBackground(Color.RED);
                jLabelSFishStatus.setText("disconnected");
            }
        }
    }
    
    
    private void updatePingStatusFields(SonarData ping) {
        jLabelRange.setText(Integer.toString((int)ping.range()) + " m");
        jLabelZoom.setText("100%");
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jSplitPaneGraphics = new javax.swing.JSplitPane();
        jPanelHistogram = new javax.swing.JPanel();
        jPanelSonogram = new javax.swing.JPanel();
        jScrollBarGFXCenter = new javax.swing.JScrollBar();
        jPanelInfo = new javax.swing.JPanel();
        jLabelRange = new javax.swing.JLabel();
        jLabelZoom = new javax.swing.JLabel();
        jComboBoxColorMap = new javax.swing.JComboBox();
        jButton1 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jPanelSonarComStatus = new javax.swing.JPanel();
        jLabelSComStatus = new javax.swing.JLabel();
        jPanelSonarFishStatus = new javax.swing.JPanel();
        jLabelSFishStatus = new javax.swing.JLabel();
        jMenuBar = new javax.swing.JMenuBar();
        jMenuSonarCom = new javax.swing.JMenu();
        jMenuAbout = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuPrefs = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuQuit = new javax.swing.JMenuItem();
        jMenuFile = new javax.swing.JMenu();
        jMenuView = new javax.swing.JMenu();
        jMenuStatus = new javax.swing.JMenuItem();
        jMenuGpsWindow = new javax.swing.JMenuItem();
        jMenuSensorWindow = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1044, 480));

        jSplitPaneGraphics.setBorder(null);
        jSplitPaneGraphics.setDividerLocation(100);
        jSplitPaneGraphics.setDividerSize(4);
        jSplitPaneGraphics.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPaneGraphics.setMinimumSize(new java.awt.Dimension(0, 0));
        jSplitPaneGraphics.setPreferredSize(new java.awt.Dimension(0, 0));

        jPanelHistogram.setBorder(javax.swing.BorderFactory.createTitledBorder("Sonar Histogram"));
        jPanelHistogram.setMinimumSize(new java.awt.Dimension(0, 50));

        javax.swing.GroupLayout jPanelHistogramLayout = new javax.swing.GroupLayout(jPanelHistogram);
        jPanelHistogram.setLayout(jPanelHistogramLayout);
        jPanelHistogramLayout.setHorizontalGroup(
            jPanelHistogramLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1045, Short.MAX_VALUE)
        );
        jPanelHistogramLayout.setVerticalGroup(
            jPanelHistogramLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 76, Short.MAX_VALUE)
        );

        jSplitPaneGraphics.setTopComponent(jPanelHistogram);

        jPanelSonogram.setBorder(javax.swing.BorderFactory.createTitledBorder("Sonogram"));
        jPanelSonogram.setMinimumSize(new java.awt.Dimension(0, 50));

        javax.swing.GroupLayout jPanelSonogramLayout = new javax.swing.GroupLayout(jPanelSonogram);
        jPanelSonogram.setLayout(jPanelSonogramLayout);
        jPanelSonogramLayout.setHorizontalGroup(
            jPanelSonogramLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1045, Short.MAX_VALUE)
        );
        jPanelSonogramLayout.setVerticalGroup(
            jPanelSonogramLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 157, Short.MAX_VALUE)
        );

        jSplitPaneGraphics.setBottomComponent(jPanelSonogram);

        jScrollBarGFXCenter.setBlockIncrement(25);
        jScrollBarGFXCenter.setMaximum(1000);
        jScrollBarGFXCenter.setMinimum(-1000);
        jScrollBarGFXCenter.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        jScrollBarGFXCenter.setVisibleAmount(1);
        jScrollBarGFXCenter.setFocusTraversalKeysEnabled(false);
        jScrollBarGFXCenter.setFocusable(false);
        jScrollBarGFXCenter.setMaximumSize(new java.awt.Dimension(3276, 15));
        jScrollBarGFXCenter.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jScrollBarGFXCenterMouseClicked(evt);
            }
        });
        jScrollBarGFXCenter.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
            }
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
                jScrollBarGFXCenterCaretPositionChanged(evt);
            }
        });
        jScrollBarGFXCenter.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                jScrollBarGFXCenterAdjustmentValueChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPaneGraphics, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollBarGFXCenter, javax.swing.GroupLayout.DEFAULT_SIZE, 1045, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jSplitPaneGraphics, javax.swing.GroupLayout.DEFAULT_SIZE, 285, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollBarGFXCenter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jLabelRange.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabelRange.setText("-");
        jLabelRange.setBorder(javax.swing.BorderFactory.createTitledBorder("Range"));

        jLabelZoom.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabelZoom.setText("-");
        jLabelZoom.setBorder(javax.swing.BorderFactory.createTitledBorder("Zoom"));

        jComboBoxColorMap.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "BONE", "NISSE", "COPPER", "AFM_HOT", "HOT", "JET", "GRAY" }));
        jComboBoxColorMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxColorMapActionPerformed(evt);
            }
        });

        jButton1.setText("jButton1");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton6.setText("jButton1");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jPanelSonarComStatus.setBorder(javax.swing.BorderFactory.createTitledBorder("SonarCom"));
        jPanelSonarComStatus.setPreferredSize(new java.awt.Dimension(100, 40));

        jLabelSComStatus.setBackground(new java.awt.Color(255, 120, 120));
        jLabelSComStatus.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabelSComStatus.setText("disconnected");
        jLabelSComStatus.setOpaque(true);

        javax.swing.GroupLayout jPanelSonarComStatusLayout = new javax.swing.GroupLayout(jPanelSonarComStatus);
        jPanelSonarComStatus.setLayout(jPanelSonarComStatusLayout);
        jPanelSonarComStatusLayout.setHorizontalGroup(
            jPanelSonarComStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelSComStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanelSonarComStatusLayout.setVerticalGroup(
            jPanelSonarComStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelSonarComStatusLayout.createSequentialGroup()
                .addComponent(jLabelSComStatus)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jPanelSonarFishStatus.setBorder(javax.swing.BorderFactory.createTitledBorder("SonarFish"));
        jPanelSonarFishStatus.setPreferredSize(new java.awt.Dimension(100, 40));

        jLabelSFishStatus.setBackground(new java.awt.Color(255, 120, 120));
        jLabelSFishStatus.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabelSFishStatus.setText("diconnected");
        jLabelSFishStatus.setOpaque(true);

        javax.swing.GroupLayout jPanelSonarFishStatusLayout = new javax.swing.GroupLayout(jPanelSonarFishStatus);
        jPanelSonarFishStatus.setLayout(jPanelSonarFishStatusLayout);
        jPanelSonarFishStatusLayout.setHorizontalGroup(
            jPanelSonarFishStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelSFishStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)
        );
        jPanelSonarFishStatusLayout.setVerticalGroup(
            jPanelSonarFishStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelSonarFishStatusLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabelSFishStatus))
        );

        javax.swing.GroupLayout jPanelInfoLayout = new javax.swing.GroupLayout(jPanelInfo);
        jPanelInfo.setLayout(jPanelInfoLayout);
        jPanelInfoLayout.setHorizontalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addComponent(jLabelRange, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelZoom, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxColorMap, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 273, Short.MAX_VALUE)
                .addComponent(jButton6)
                .addGap(108, 108, 108)
                .addComponent(jPanelSonarComStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelSonarFishStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanelInfoLayout.setVerticalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton6)
                    .addComponent(jButton1)
                    .addComponent(jComboBoxColorMap, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelSonarComStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanelSonarFishStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelZoom)
                    .addComponent(jLabelRange))
                .addGap(2, 2, 2))
        );

        jMenuSonarCom.setText("SonarCom");
        jMenuSonarCom.setFont(jMenuSonarCom.getFont().deriveFont(jMenuSonarCom.getFont().getStyle() | java.awt.Font.BOLD));

        jMenuAbout.setText("About SonarCom");
        jMenuSonarCom.add(jMenuAbout);
        jMenuSonarCom.add(jSeparator2);

        jMenuPrefs.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_COMMA, java.awt.event.InputEvent.META_MASK));
        jMenuPrefs.setText("Preferences...");
        jMenuPrefs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuPrefsActionPerformed(evt);
            }
        });
        jMenuSonarCom.add(jMenuPrefs);
        jMenuSonarCom.add(jSeparator1);

        jMenuQuit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.META_MASK));
        jMenuQuit.setText("Quit");
        jMenuQuit.setToolTipText("");
        jMenuQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuQuitActionPerformed(evt);
            }
        });
        jMenuSonarCom.add(jMenuQuit);

        jMenuBar.add(jMenuSonarCom);

        jMenuFile.setText("File");
        jMenuBar.add(jMenuFile);

        jMenuView.setText("View");

        jMenuStatus.setText("Information window");
        jMenuStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuStatusActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuStatus);

        jMenuGpsWindow.setText("GPS window");
        jMenuGpsWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuGpsWindowActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuGpsWindow);

        jMenuSensorWindow.setText("Sensor window");
        jMenuSensorWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSensorWindowActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuSensorWindow);

        jMenuBar.add(jMenuView);

        setJMenuBar(jMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelInfo, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelInfo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void jMenuQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuQuitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jMenuQuitActionPerformed

    private void jMenuPrefsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuPrefsActionPerformed
//        PrefsJDialog prefDiag = new PrefsJDialog(ComInterfaceJFrame.this, false, this.sonarConf);
        PrefsJDialog prefDialog = new PrefsJDialog(this.sonarComConfig, this.sonarFishConfig, this.sonarComCLI);
//        prefDialog.initializeDialog();
    System.out.println("Hej" + sonarComConfig.sonarComDataPortName);
            prefDialog.setLocationRelativeTo(null);
        prefDialog.setVisible(true);
    }//GEN-LAST:event_jMenuPrefsActionPerformed
     
    private void jMenuStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuStatusActionPerformed
        if (this.infoDialog == null || !this.infoDialog.isShowing()) {
            this.infoDialog = new InfoJDialog(this.sonarComCLI);
        }
    }//GEN-LAST:event_jMenuStatusActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        repaint();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jScrollBarGFXCenterAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_jScrollBarGFXCenterAdjustmentValueChanged
        double center;
        double leftRangeScale;
        double rightRangeScale;
        
        center = (double)jScrollBarGFXCenter.getValue() / (double)jScrollBarGFXCenter.getMaximum();
        if (center > 0) {
            leftRangeScale  = 1.0;                                  // 100% left range
            rightRangeScale = 1.0 - 2*(center / (center + 1.0));
        } else {
            leftRangeScale  = 1.0 - 2*(center / (center - 1.0));
            rightRangeScale = 1.0;                                  // 100% right range            
        }
        
//        System.out.println("scrollBar() center " + center + ", leftRangeScale " + leftRangeScale + ", rightRangeScale " + rightRangeScale);
        histogramPanel.setRangeScale(leftRangeScale, rightRangeScale);
        sonogramPanel.setRangeScale(leftRangeScale, rightRangeScale);
        hMarkerImage.setRangeScale(leftRangeScale, rightRangeScale);
        repaint();
    }//GEN-LAST:event_jScrollBarGFXCenterAdjustmentValueChanged

    private void jScrollBarGFXCenterCaretPositionChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_jScrollBarGFXCenterCaretPositionChanged
        System.out.println("ScrollBar CaretPositionChanged " + jScrollBarGFXCenter.getValue());
    }//GEN-LAST:event_jScrollBarGFXCenterCaretPositionChanged

    private void jComboBoxColorMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxColorMapActionPerformed
        switch (jComboBoxColorMap.getSelectedIndex()) {
            case 0:
                this.colorMap = SonogramGraphics.ColorMaps.BONE;
                break;
            case 1:
                this.colorMap = SonogramGraphics.ColorMaps.NISSE;
                break;
            case 2:
                this.colorMap = SonogramGraphics.ColorMaps.COPPER;
                break;
            case 3:
                this.colorMap = SonogramGraphics.ColorMaps.AFM_HOT;
                break;
            case 4:
                this.colorMap = SonogramGraphics.ColorMaps.HOT;
                break;
            case 5:
                this.colorMap = SonogramGraphics.ColorMaps.JET;
                break;
            case 6:
                this.colorMap = SonogramGraphics.ColorMaps.GRAY;
                break;
        }
    }//GEN-LAST:event_jComboBoxColorMapActionPerformed

    private void jMenuSensorWindowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSensorWindowActionPerformed
        this.sensorJDialog.setVisible(true);
    }//GEN-LAST:event_jMenuSensorWindowActionPerformed

    private void jMenuGpsWindowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuGpsWindowActionPerformed
        this.gpsJDialog.setVisible(true);
    }//GEN-LAST:event_jMenuGpsWindowActionPerformed

    private void jScrollBarGFXCenterMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jScrollBarGFXCenterMouseClicked
        if (evt.getClickCount() == 2) {
            System.out.println("jScrollBarGFXCenterMouseClicked Double-clicked" + jScrollBarGFXCenter.getValue());
            jScrollBarGFXCenter.setValue(0);
        }
    }//GEN-LAST:event_jScrollBarGFXCenterMouseClicked
      
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton6;
    private javax.swing.JComboBox jComboBoxColorMap;
    private javax.swing.JLabel jLabelRange;
    private javax.swing.JLabel jLabelSComStatus;
    private javax.swing.JLabel jLabelSFishStatus;
    private javax.swing.JLabel jLabelZoom;
    private javax.swing.JMenuItem jMenuAbout;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuGpsWindow;
    private javax.swing.JMenuItem jMenuPrefs;
    private javax.swing.JMenuItem jMenuQuit;
    private javax.swing.JMenuItem jMenuSensorWindow;
    private javax.swing.JMenu jMenuSonarCom;
    private javax.swing.JMenuItem jMenuStatus;
    private javax.swing.JMenu jMenuView;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanelHistogram;
    private javax.swing.JPanel jPanelInfo;
    private javax.swing.JPanel jPanelSonarComStatus;
    private javax.swing.JPanel jPanelSonarFishStatus;
    private javax.swing.JPanel jPanelSonogram;
    private javax.swing.JScrollBar jScrollBarGFXCenter;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JSplitPane jSplitPaneGraphics;
    // End of variables declaration//GEN-END:variables
}