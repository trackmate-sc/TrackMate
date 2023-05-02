package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.function.DoubleUnaryOperator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.alg.zslicer.Contour;
import net.imagej.mesh.alg.zslicer.RamerDouglasPeucker;
import net.imagej.mesh.alg.zslicer.Slice;
import net.imagej.mesh.alg.zslicer.ZSlicer;
import net.imagej.mesh.obj.transform.TranslateMesh;

/**
 * Utility class to paint the {@link SpotMesh} component of spots.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class PaintSpotMesh extends TrackMatePainter
{

	private final Path2D.Double polygon;

	public PaintSpotMesh( final ImagePlus imp, final double[] calibration, final DisplaySettings displaySettings )
	{
		super( imp, calibration, displaySettings );
		this.polygon = new Path2D.Double();
	}

	public int paint( final Graphics2D g2d, final Spot spot )
	{
		final ImageCanvas canvas = canvas();
		if ( canvas == null )
			return -1;

		final SpotMesh sm = spot.getMesh();
		final double x = spot.getFeature( Spot.POSITION_X );
		final double y = spot.getFeature( Spot.POSITION_Y );

		// Don't paint if we are out of screen.
		if ( toScreenX( sm.boundingBox[ 0 ] + x ) > canvas.getWidth() )
			return -1;
		if ( toScreenX( sm.boundingBox[ 3 ] + x ) < 0 )
			return -1;
		if ( toScreenY( sm.boundingBox[ 1 ] + y ) > canvas.getHeight() )
			return -1;
		if ( toScreenY( sm.boundingBox[ 4 ] + y ) < 0 )
			return -1;

		// Z plane does not cross bounding box.
		final double xs = toScreenX( x );
		final double ys = toScreenY( y );
		final double z = spot.getFeature( Spot.POSITION_Z );
		final double dz = ( canvas.getImage().getSlice() - 1 ) * calibration[ 2 ];
		if ( sm.boundingBox[ 2 ] + z > dz || sm.boundingBox[ 5 ] + z < dz )
		{
			final double magnification = canvas.getMagnification();
			g2d.fillOval(
					( int ) Math.round( xs - 2 * magnification ),
					( int ) Math.round( ys - 2 * magnification ),
					( int ) Math.round( 4 * magnification ),
					( int ) Math.round( 4 * magnification ) );
			return -1;
		}

		final Mesh translated = TranslateMesh.translate( sm.mesh, spot );
		final Slice slice = ZSlicer.slice( translated, dz, calibration[ 2 ] );
		double maxTextPos = Double.NEGATIVE_INFINITY;
		for ( final Contour c : slice )
		{
			final Contour contour = RamerDouglasPeucker.simplify( c, calibration[ 0 ] * 0.25 );

			// Temporary set color by interior vs exterior.
			if ( !contour.isInterior() )
				g2d.setColor( Color.RED );
			else
				g2d.setColor( Color.GREEN );

			final double textPos = toPolygon( contour, polygon, this::toScreenX, this::toScreenY );
			if ( textPos > maxTextPos )
				maxTextPos = textPos;

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

	/**
	 * Maps the coordinates of this contour to a Path2D polygon, and return the
	 * max X coordinate of the produced shape.
	 *
	 * @param contour
	 *            the contour to convert.
	 * @param polygon
	 *            the polygon to write. Reset by this call.
	 * @param toScreenX
	 *            a function to convert the X coordinate of this contour to
	 *            screen coordinates.
	 * @param toScreenY
	 *            a function to convert the Y coordinate of this contour to
	 *            screen coordinates.
	 * @return the max X position in screen units of this shape.
	 */
	private static final double toPolygon( final Contour contour, final Path2D polygon, final DoubleUnaryOperator toScreenX, final DoubleUnaryOperator toScreenY )
	{
		double maxTextPos = Double.NEGATIVE_INFINITY;
		polygon.reset();
		final double x0 = toScreenX.applyAsDouble( contour.x( 0 ) );
		final double y0 = toScreenY.applyAsDouble( contour.y( 0 ) );
		polygon.moveTo( x0, y0 );
		if ( x0 > maxTextPos )
			maxTextPos = x0;

		for ( int i = 1; i < contour.size(); i++ )
		{
			final double xi = toScreenX.applyAsDouble( contour.x( i ) );
			final double yi = toScreenY.applyAsDouble( contour.y( i ) );
			polygon.lineTo( xi, yi );

			if ( xi > maxTextPos )
				maxTextPos = xi;
		}
		polygon.closePath();
		return maxTextPos;
	}
}
