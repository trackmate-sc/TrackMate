package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import gnu.trove.list.array.TDoubleArrayList;

/**
 * Utility class to paint the {@link SpotRoi} component of spots.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class PaintSpotRoi
{

	private final double[] calibration;

	private final DisplaySettings displaySettings;

	private final java.awt.geom.Path2D.Double polygon;

	private final TDoubleArrayList cx;

	private final TDoubleArrayList cy;

	public PaintSpotRoi( final double[] calibration, final DisplaySettings displaySettings )
	{
		this.calibration = calibration;
		this.displaySettings = displaySettings;
		this.polygon = new Path2D.Double();
		this.cx = new TDoubleArrayList();
		this.cy = new TDoubleArrayList();
	}

	/**
	 * Paint the specified spot using its {@link SpotRoi} field. The latter must
	 * not be <code>null</code>.
	 *
	 * @param g2d
	 *            the graphics object, configured to paint the spot with.
	 * @param roi
	 *            the spot roi.
	 * @param x
	 *            the X spot center in physical coordinates.
	 * @param y
	 *            the Y spot center in physical coordinates.
	 * @param xcorner
	 *            the X position of the displayed window.
	 * @param ycorner
	 *            the X position of the displayed window.
	 * @param magnification
	 *            the magnification of the displayed window.
	 * @return the text position X indent in pixels to use to paint a string
	 *         next to the painted contour.
	 */
	public int paint(
			final Graphics2D g2d,
			final SpotRoi roi,
			final double x,
			final double y,
			final double xcorner,
			final double ycorner,
			final double magnification )
	{
		// In pixel units.
		final double xp = x / calibration[ 0 ] + 0.5f;
		// Scale to image zoom.
		final double xs = ( xp - xcorner ) * magnification;
		// Contour in pixel coordinates.
		roi.toPolygon( calibration, xcorner, ycorner, x, y, magnification, cx, cy );
		// The 0.5 is here so that we plot vertices at pixel centers.
		polygon.reset();
		polygon.moveTo( cx.get( 0 ), cy.get( 0 ) );
		for ( int i = 1; i < cx.size(); ++i )
			polygon.lineTo( cx.get( i ), cy.get( i ) );
		polygon.closePath();

		if ( displaySettings.isSpotFilled() )
		{
			g2d.fill( polygon );
			g2d.setColor( Color.BLACK );
			g2d.draw( polygon );
		}
		else
		{
			g2d.draw( polygon );
		}

		final int textPos = ( int ) ( max( cx ) - xs );
		return textPos;
	}

	static final double max( final TDoubleArrayList l )
	{
		double max = Double.NEGATIVE_INFINITY;
		for ( int i = 0; i < l.size(); i++ )
		{
			final double v = l.getQuick( i );
			if ( v > max )
				max = v;
		}
		return max;
	}
}
