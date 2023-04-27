package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.gui.ImageCanvas;
import net.imagej.mesh.ZSlicer;
import net.imagej.mesh.ZSlicer.Contour;

/**
 * Utility class to paint the {@link SpotMesh} component of spots.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class PaintSpotMesh extends TrackMatePainter
{

	private final Path2D.Double polygon;

	public PaintSpotMesh( final ImageCanvas canvas, final double[] calibration, final DisplaySettings displaySettings )
	{
		super( canvas, calibration, displaySettings );
		this.polygon = new Path2D.Double();
	}

	public int paint( final Graphics2D g2d, final Spot spot )
	{
		final SpotMesh sm = spot.getMesh();

		// Don't paint if we are out of screen.
		if ( toScreenX( sm.boundingBox[ 0 ] ) > canvas.getSrcRect().width )
			return -1;
		if ( toScreenX( sm.boundingBox[ 3 ] ) < 0 )
			return -1;
		if ( toScreenY( sm.boundingBox[ 1 ] ) > canvas.getSrcRect().height )
			return -1;
		if ( toScreenY( sm.boundingBox[ 4 ] ) < 0 )
			return -1;

		// Z plane does not cross bounding box.
		final double x = spot.getFeature( Spot.POSITION_X );
		final double y = spot.getFeature( Spot.POSITION_Y );
		final double xs = toScreenX( x );
		final double ys = toScreenY( y );
		final double dz = ( canvas.getImage().getSlice() - 1 ) * calibration[ 2 ];
		if ( sm.boundingBox[ 2 ] > dz || sm.boundingBox[ 5 ] < dz )
		{
			final double magnification = canvas.getMagnification();
			g2d.fillOval(
					( int ) Math.round( xs - 2 * magnification ),
					( int ) Math.round( ys - 2 * magnification ),
					( int ) Math.round( 4 * magnification ),
					( int ) Math.round( 4 * magnification ) );
			return -1;
		}

		final List< Contour > contours = ZSlicer.slice( sm.mesh, dz );
		double maxTextPos = Double.NEGATIVE_INFINITY;
		for ( final Contour contour : contours )
		{
			if ( contour.x.size() < 2 )
				continue;

			polygon.reset();
			final double x0 =toScreenX( contour.x.getQuick( 0 ) ); 
			final double y0 =toScreenY( contour.y.getQuick( 0 ) );
			polygon.moveTo( x0, y0 );
			if ( x0 > maxTextPos )
				maxTextPos = x0;

			for ( int i = 1; i < contour.x.size(); i++ )
			{
				final double xi = toScreenX( contour.x.getQuick( i ) );
				final double yi = toScreenY( contour.y.getQuick( i ) );
				polygon.lineTo( xi, yi );
				if ( xi > maxTextPos )
					maxTextPos = xi;
			}
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
		}
		return ( int ) ( maxTextPos - xs );
	}
}
