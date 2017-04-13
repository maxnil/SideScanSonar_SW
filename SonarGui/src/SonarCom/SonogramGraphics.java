/*
 * Copyright 2015 Max Nilsson
 * Each line should be prefixed with  * 
 */
package SonarCom;

/**
 *
 * @author max
 */
public class SonogramGraphics {
    
    public enum ColorMaps {
        BONE,
        NISSE,
        COPPER,
        AFM_HOT,
        HOT,
        JET,
        GRAY
    }
    
    public static int rgbColor(double x, ColorMaps cm) {
        int red;
        int blue;
        int green;

        switch (cm) {
            case BONE:
                red   = (int)((3.0 * x - 2.0) * 255);
                green = (int)((3.0 * x - 1.0) * 255);
                blue  = (int)((3.0 * x - 0.0) * 255);
                red   = red   > 255 ? 255 : red   < 0 ? 0 : red;
                green = green > 255 ? 255 : green < 0 ? 0 : green;
                blue  = blue  > 255 ? 255 : blue  < 0 ? 0 : blue;
                red   = (int)((7 * x * 255) + red) / 8;
                red   = (int)((7 * x * 255) + green) / 8;
                red   = (int)((7 * x * 255) + blue) / 8;
                break;
            case NISSE:
                red   = (int)((3.0 * x) * 255);
                green = (int)((3.0 * x - 1.0) * 255);
                blue  = (int)((3.0 * x - 2.0) * 255);
                break;
            case COPPER:
                red   = (int)((1.2500 * x) * 255);
                green = (int)((0.7812 * x) * 255);
                blue  = (int)((0.4975 * x) * 255);
                break;
            case AFM_HOT:
                red   = (int)((2.0 * x) * 255);
                green = (int)((2.0 * x - 0.5) * 255);
                blue  = (int)((2.0 * x - 1.0) * 255);
                break;
            case HOT:
                red   = (int)((3.0 * x) * 255);
                green = (int)((3.0 * x - 1.0) * 255);
                blue  = (int)((3.0 * x - 2.0) * 255);
                break;
            case JET:
                red   = (int)((x < 3.0/8 ? 0.0 : x < 5.0/8 ? 4.0 * x - 1.5 : x < 7.0/8 ? 1.0 : -4.0 * x + 4.5) * 255);
                green = (int)((x < 1.0/8 ? 0.0 : x < 3.0/8 ? 4.0 * x - 0.5 : x < 5.0/8 ? 1.0 : x < 7.0/8 ? -4.0 * x + 3.5 : 0.0) * 255);
                blue  = (int)((x < 1.0/8 ? 4.0 * x + 0.5 : x < 3.0/8 ? 1.0 : x < 5.0/8 ? -4.0 * x + 2.5 : 0.0) * 255);
                break;
            case GRAY:
            default:
                red   = (int)(x * 255);
                green = (int)(x * 255);
                blue  = (int)(x * 255);
                break;
        }

        // Saturate
        red   = red   > 255 ? 255 : red   < 0 ? 0 : red;
        green = green > 255 ? 255 : green < 0 ? 0 : green;
        blue  = blue  > 255 ? 255 : blue  < 0 ? 0 : blue;
                
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
