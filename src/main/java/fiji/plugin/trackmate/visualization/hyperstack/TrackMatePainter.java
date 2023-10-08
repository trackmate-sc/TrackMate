package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.Graphics2D;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import net.imglib2.RealInterval;
import net.imglib2.RealLocalizable;

public abstract class TrackMatePainter< T extends Spot >
{

	protected final double[] calibration;

	protected final DisplaySettings displaySettings;

	protected final ImagePlus imp;

	public TrackMatePainter( final ImagePlus imp, final double[] calibration, final DisplaySettings displaySettings )
	{
		this.imp = imp;
		this.calibration = calibration;
		this.displaySettings = displaySettings;
	}

	public abstract int paint( final Graphics2D g2d, final T spot );

	/**
	 * Returns <code>true</code> if the specified bounding-box, shifted by the
	 * Specified amount, intersects with the display window.
	 *
	 * @param boundingBox
	 *            the bounding box, centered at (0,0), in physical coordinates.
	 * @param center
	 *            the center of the bounding-box, in physical coordinates.
	 * @return if the specified bounding-box intersects with the display window.
	 */
	protected boolean intersect( final RealInterval boundingBox, final RealLocalizable center )
	{
		final ImageCanvas canvas = imp.getCanvas();
		if ( canvas == null )
			return false;

		if ( toScreenX( boundingBox.realMin( 0 ) + center.getDoublePosition( 0 ) ) > canvas.getWidth() )
			return false;
		if ( toScreenX( boundingBox.realMax( 0 ) + center.getDoublePosition( 0 ) ) < 0 )
			return false;
		if ( toScreenY( boundingBox.realMin( 1 ) + center.getDoublePosition( 1 ) ) > canvas.getHeight() )
			return false;
		if ( toScreenY( boundingBox.realMax( 1 ) + center.getDoublePosition( 1 ) ) < 0 )
			return false;
		return true;
	}

	/**
	 * Returns <code>true</code> if the specified bounding-box intersects with
	 * the display window.
	 *
	 * @param boundingBox
	 *            the bounding box, in physical coordinates.
	 * @return <code>true</code> if the specified bounding-box intersects with
	 *         the display window.
	 */
	protected boolean intersect( final RealInterval boundingBox )
	{
		final ImageCanvas canvas = imp.getCanvas();
		if ( canvas == null )
			return false;

		if ( toScreenX( boundingBox.realMin( 0 ) ) > canvas.getWidth() )
			return false;
		if ( toScreenX( boundingBox.realMax( 0 ) ) < 0 )
			return false;
		if ( toScreenY( boundingBox.realMin( 1 ) ) > canvas.getHeight() )
			return false;
		if ( toScreenY( boundingBox.realMax( 1 ) ) < 0 )
			return false;
		return true;
	}

	/**
	 * Converts a X position in physical units (possible um) to screen
	 * coordinates to be used with the graphics object.
	 *
	 * @param x
	 *            the X position to convert.
	 * @return the screen X coordinate.
	 */
	protected double toScreenX( final double x )
	{
		final ImageCanvas canvas = imp.getCanvas();
		if ( canvas == null )
			return Double.NaN;

		final double xp = x / calibration[ 0 ] + 0.5; // pixel coords
		return canvas.screenXD( xp );
	}

	/**
	 * Converts a Y position in physical units (possible um) to screen
	 * coordinates to be used with the graphics object.
	 *
	 * @param y
	 *            the Y position to convert.
	 * @return the screen Y coordinate.
	 */
	protected double toScreenY( final double y )
	{
		final ImageCanvas canvas = imp.getCanvas();
		if ( canvas == null )
			return Double.NaN;

		final double yp = y / calibration[ 0 ] + 0.5; // pixel coords
		return canvas.screenYD( yp );
	}

	protected void paintOutOfFocus( final Graphics2D g2d, final double xs, final double ys )
	{
		final double magnification = getMagnification();
		g2d.fillOval(
				( int ) Math.round( xs - 2 * magnification ),
				( int ) Math.round( ys - 2 * magnification ),
				( int ) Math.round( 4 * magnification ),
				( int ) Math.round( 4 * magnification ) );
	}

	protected double getMagnification()
	{
		if ( imp.getCanvas() == null )
			return 1.;
		return imp.getCanvas().getMagnification();
	}
}
