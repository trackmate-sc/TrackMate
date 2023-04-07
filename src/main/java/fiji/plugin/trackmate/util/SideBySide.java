package fiji.plugin.trackmate.util;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SideBySide {

    /*
     * Create a new image that is the width of img1+img2. Take the
     * height of the taller image Paint the two images side-by-side.
     */
    public static BufferedImage combineImages(final BufferedImage img1, final BufferedImage img2){
        final BufferedImage combined = new BufferedImage( img1.getWidth() + img2.getWidth(),
                Math.max( img1.getHeight(), img2.getHeight() ), BufferedImage.TYPE_INT_RGB );
        final Graphics g = combined.getGraphics();
        g.drawImage( img1, 0, 0, null );
        g.drawImage( img2, img1.getWidth(), 0, null );
        return combined;
    }
}
