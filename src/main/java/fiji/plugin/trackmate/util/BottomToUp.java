package fiji.plugin.trackmate.util;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BottomToUp {
    public static BufferedImage combineImages(final BufferedImage img1, final BufferedImage img2){
        final BufferedImage combined = new BufferedImage(
                Math.max( img1.getWidth(), img2.getWidth() ),
                img1.getHeight() + img2.getHeight(),
                BufferedImage.TYPE_INT_RGB );
        final Graphics g = combined.getGraphics();
        g.drawImage( img1, 0, 0, null );
        g.drawImage( img2, 0, img1.getHeight(), null );
        return combined;
    }
}
