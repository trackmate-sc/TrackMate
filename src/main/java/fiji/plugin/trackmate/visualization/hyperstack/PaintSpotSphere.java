package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.Graphics2D;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

/**
 * Utility class to paint the spots as little spheres.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class PaintSpotSphere
{

	private final double[] calibration;

	private final DisplaySettings displaySettings;

	public PaintSpotSphere( final double[] calibration, final DisplaySettings displaySettings )
	{
		this.calibration = calibration;
		this.displaySettings = displaySettings;
	}

	public int paint(
			final Graphics2D g2d,
			final Spot spot,
			final double zslice,
			final double xs,
			final double ys,
			final int xcorner,
			final int ycorner,
			final double magnification )
	{
		final double z = spot.getFeature( Spot.POSITION_Z );
		final double dz = zslice - z;
		final double dz2 = dz * dz;
		final double radiusRatio = displaySettings.getSpotDisplayRadius();
		final double radius = spot.getFeature( Spot.RADIUS ) * radiusRatio;

		if ( dz2 >= radius * radius )
		{
			g2d.fillOval(
					( int ) Math.round( xs - 2 * magnification ),
					( int ) Math.round( ys - 2 * magnification ),
					( int ) Math.round( 4 * magnification ),
					( int ) Math.round( 4 * magnification ) );
			return -1; // Do not paint spot name.
		}

		final double apparentRadius = Math.sqrt( radius * radius - dz2 ) / calibration[ 0 ] * magnification;
		if ( displaySettings.isSpotFilled() )
			g2d.fillOval(
					( int ) Math.round( xs - apparentRadius ),
					( int ) Math.round( ys - apparentRadius ),
					( int ) Math.round( 2 * apparentRadius ),
					( int ) Math.round( 2 * apparentRadius ) );
		else
			g2d.drawOval(
					( int ) Math.round( xs - apparentRadius ),
					( int ) Math.round( ys - apparentRadius ),
					( int ) Math.round( 2 * apparentRadius ),
					( int ) Math.round( 2 * apparentRadius ) );

		final int textPos = ( int ) apparentRadius;
		return textPos;
	}
}
