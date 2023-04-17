package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Path2D.Double;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import gnu.trove.list.array.TDoubleArrayList;

/**
 * Utility class to paint the {@link SpotMesh} component of spots.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class PaintSpotMesh
{

	private final double[] calibration;

	private final DisplaySettings displaySettings;

	private final TDoubleArrayList cx;

	private final TDoubleArrayList cy;

	private final Double polygon;

	public PaintSpotMesh( final double[] calibration, final DisplaySettings displaySettings )
	{
		this.calibration = calibration;
		this.displaySettings = displaySettings;
		this.cx = new TDoubleArrayList();
		this.cy = new TDoubleArrayList();
		this.polygon = new Path2D.Double();
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
		final double x = spot.getFeature( Spot.POSITION_X );
		final double y = spot.getFeature( Spot.POSITION_Y );
		final double z = spot.getFeature( Spot.POSITION_Z );
		final double dz = zslice;

		final SpotMesh mesh = spot.getMesh();
		if ( mesh.boundingBox[ 2 ] > dz || mesh.boundingBox[ 5 ] < dz )
		{
			g2d.fillOval(
					( int ) Math.round( xs - 2 * magnification ),
					( int ) Math.round( ys - 2 * magnification ),
					( int ) Math.round( 4 * magnification ),
					( int ) Math.round( 4 * magnification ) );
			return -1;
		}

		// Slice.
		mesh.slice( dz, cx, cy );
		// Scale to screen coordinates.
		for ( int i = 0; i < cx.size(); i++ )
		{
			// Pixel coords.
			final double xc = ( cx.get( i ) ) / calibration[ 0 ] + 0.5;
			final double yc = ( cy.get( i ) ) / calibration[ 1 ] + 0.5;
			// Window coords.
			cx.set( i, ( xc - xcorner ) * magnification );
			cy.set( i, ( yc - ycorner ) * magnification );
		}

		polygon.reset();
		for ( int i = 0; i < cx.size() - 1; i += 2 )
		{
			final double x0 = cx.get( i );
			final double x1 = cx.get( i + 1 );
			final double y0 = cy.get( i );
			final double y1 = cy.get( i + 1 );
			polygon.moveTo( x0, y0 );
			polygon.lineTo( x1, y1 );
		}

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

		final int textPos = ( int ) ( PaintSpotRoi.max( cx ) - xs );
		return textPos;
	}

}
