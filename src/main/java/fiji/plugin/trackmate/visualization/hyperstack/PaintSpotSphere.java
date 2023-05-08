package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.Graphics2D;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.ImagePlus;
import net.imglib2.RealInterval;
import net.imglib2.util.Intervals;

/**
 * Utility class to paint the spots as little spheres.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class PaintSpotSphere extends TrackMatePainter< SpotBase >
{

	public PaintSpotSphere( final ImagePlus imp, final double[] calibration, final DisplaySettings displaySettings )
	{
		super( imp, calibration, displaySettings );
	}

	@Override
	public int paint( final Graphics2D g2d, final SpotBase spot )
	{
		if ( !intersect( boundingBox( spot ), spot ) )
			return -1;

		final double x = spot.getFeature( Spot.POSITION_X );
		final double y = spot.getFeature( Spot.POSITION_Y );
		final double z = spot.getFeature( Spot.POSITION_Z );
		final double zslice = ( imp.getSlice() - 1 ) * calibration[ 2 ];
		final double dz = zslice - z;
		final double dz2 = dz * dz;
		final double radiusRatio = displaySettings.getSpotDisplayRadius();
		final double radius = spot.getFeature( Spot.RADIUS ) * radiusRatio;

		final double xs = toScreenX( x );
		final double ys = toScreenY( y );
		final double magnification = getMagnification();

		if ( dz2 >= radius * radius )
		{
			paintOutOfFocus( g2d, xs, ys );
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

	private static final RealInterval boundingBox( final Spot spot )
	{
		final double r = spot.getFeature( Spot.RADIUS ).doubleValue();
		return Intervals.createMinMaxReal( -r, -r, r, r );
	}
}
