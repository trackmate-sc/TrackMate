package fiji.plugin.trackmate.visualization.hyperstack;

import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.ImagePlus;
import ij.gui.ImageCanvas;

public abstract class TrackMatePainter
{

	protected final double[] calibration;

	protected final DisplaySettings displaySettings;

	private final ImagePlus imp;

	public TrackMatePainter( final ImagePlus imp, final double[] calibration, final DisplaySettings displaySettings )
	{
		this.imp = imp;
		this.calibration = calibration;
		this.displaySettings = displaySettings;
	}

	protected ImageCanvas canvas()
	{
		return imp.getCanvas();
	}

	/**
	 * Converts a X position in physical units (possible um) to screen
	 * coordinates to be used with the graphics object.
	 *
	 * @param x
	 *            the X position to convert.
	 * @return the screen X coordinate.
	 */
	public double toScreenX( final double x )
	{
		final ImageCanvas canvas = canvas();
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
	public double toScreenY( final double y )
	{
		final ImageCanvas canvas = canvas();
		if ( canvas == null )
			return Double.NaN;

		final double yp = y / calibration[ 0 ] + 0.5; // pixel coords
		return canvas.screenYD( yp );
	}

	/**
	 * Returns <code>true</code> of the point with the specified coordinates in
	 * physical units lays inside the painted window.
	 *
	 * @param x
	 *            the X coordinate in physical unit.
	 * @param y
	 *            the Y coordinate in physical unit.
	 * @return <code>true</code> if (x, y) is inside the painted window.
	 */
	public boolean isInside( final double x, final double y )
	{
		final ImageCanvas canvas = canvas();
		if ( canvas == null )
			return false;

		final double xs = toScreenX( x );
		if ( xs < 0 || xs > canvas.getWidth() )
			return false;
		final double ys = toScreenY( y );
		if ( ys < 0 || ys > canvas.getHeight() )
			return false;
		return true;
	}
}
