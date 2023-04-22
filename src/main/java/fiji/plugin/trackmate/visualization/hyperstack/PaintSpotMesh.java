package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Path2D.Double;
import java.util.List;
import java.util.Random;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import gnu.trove.list.linked.TDoubleLinkedList;

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

	private final Double polygon;

	public PaintSpotMesh( final double[] calibration, final DisplaySettings displaySettings )
	{
		this.calibration = calibration;
		this.displaySettings = displaySettings;
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
		final List< TDoubleLinkedList[] > contours = mesh.slice( dz );
		for ( final TDoubleLinkedList[] contour : contours )
		{
			final TDoubleLinkedList cxs = contour[ 0 ];
			final TDoubleLinkedList cys = contour[ 1 ];
			// Scale to screen coordinates.
			for ( int i = 0; i < cxs.size(); i++ )
			{
				// Pixel coords.
				final double xc = ( cxs.get( i ) ) / calibration[ 0 ] + 0.5;
				final double yc = ( cys.get( i ) ) / calibration[ 1 ] + 0.5;
				// Window coords.
				cxs.set( i, ( xc - xcorner ) * magnification );
				cys.set( i, ( yc - ycorner ) * magnification );
			}
		}

		final Random ran = new Random( 1l );
		g2d.setStroke( new BasicStroke( 2f ) );
		for ( final TDoubleLinkedList[] contour : contours )
		{
			final TDoubleLinkedList cxs = contour[ 0 ];
			final TDoubleLinkedList cys = contour[ 1 ];
			if ( cxs.size() < 2 )
				continue;

			polygon.reset();
			polygon.moveTo( cxs.get( 0 ), cys.get( 0 ) );
			for ( int i = 1; i < cxs.size() - 1; i += 2 )
				polygon.lineTo( cxs.get( i ), cys.get( i ) );
			polygon.closePath();

			g2d.setColor( new Color(
					0.5f * ( 1f + ran.nextFloat() ),
					0.5f * ( 1f + ran.nextFloat() ),
					0.5f * ( 1f + ran.nextFloat() ) ) );
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
		}

		int textPos = -1;
		for ( final TDoubleLinkedList[] contour : contours )
		{
			final TDoubleLinkedList cxs = contour[ 0 ];
			textPos = Math.max( textPos, ( int ) ( PaintSpotRoi.max( cxs ) - xs ) );
		}
		return textPos;
	}

}
