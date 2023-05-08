package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.function.DoubleUnaryOperator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.ImagePlus;
import net.imagej.mesh.alg.zslicer.Contour;
import net.imagej.mesh.alg.zslicer.Slice;
import net.imglib2.RealLocalizable;

/**
 * Utility class to paint the {@link SpotMesh} component of spots.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class PaintSpotMesh extends TrackMatePainter< SpotMesh >
{

	private final Path2D.Double polygon;

	private final Area shape;

	public PaintSpotMesh( final ImagePlus imp, final double[] calibration, final DisplaySettings displaySettings )
	{
		super( imp, calibration, displaySettings );
		this.polygon = new Path2D.Double();
		this.shape = new Area();

	}

	@Override
	public int paint( final Graphics2D g2d, final SpotMesh spot )
	{
		if ( !intersect( spot.boundingBox, spot ) )
			return -1;

		// Z plane does not cross bounding box.
		final double x = spot.getFeature( Spot.POSITION_X );
		final double y = spot.getFeature( Spot.POSITION_Y );
		final double xs = toScreenX( x );
		final double ys = toScreenY( y );
		final double z = spot.getFeature( Spot.POSITION_Z );
		final int zSlice = imp.getSlice() - 1;
		final double dz = zSlice * calibration[ 2 ];
		if ( spot.boundingBox.realMin( 2 ) + z > dz || spot.boundingBox.realMax( 2 ) + z < dz )
		{
			paintOutOfFocus( g2d, xs, ys );
			return -1;
		}

		// Convert to AWT shape. Only work in non-pathological cases, and
		// because contours are sorted by decreasing area.
		final Slice slice = spot.getZSlice( zSlice, calibration[ 0 ], calibration[ 2 ] );
		if ( slice == null )
		{
			paintOutOfFocus( g2d, xs, ys );
			return -1;
		}


		if ( displaySettings.isSpotFilled() )
		{
			// Should not be null.
			shape.reset();
			for ( final Contour c : slice )
			{
				toPolygon( spot, c, polygon, this::toScreenX, this::toScreenY );
				if ( c.isInterior() )
					shape.add( new Area( polygon ) );
				else
					shape.subtract( new Area( polygon ) );
			}
			g2d.fill( shape );
			g2d.setColor( Color.BLACK );
			g2d.draw( shape );
		}
		else
		{
			for ( final Contour c : slice )
			{
				toPolygon( spot, c, polygon, this::toScreenX, this::toScreenY );
				g2d.draw( polygon );
			}
		}
		final Rectangle bounds = shape.getBounds();
		final int maxTextPos = bounds.x + bounds.width;
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
	private static final double toPolygon( final RealLocalizable center, final Contour contour, final Path2D polygon, final DoubleUnaryOperator toScreenX, final DoubleUnaryOperator toScreenY )
	{
		double maxTextPos = Double.NEGATIVE_INFINITY;
		polygon.reset();
		final double x0 = toScreenX.applyAsDouble( contour.x( 0 ) + center.getDoublePosition( 0 ) );
		final double y0 = toScreenY.applyAsDouble( contour.y( 0 ) + center.getDoublePosition( 1 ) );
		polygon.moveTo( x0, y0 );
		if ( x0 > maxTextPos )
			maxTextPos = x0;

		for ( int i = 1; i < contour.size(); i++ )
		{
			final double xi = toScreenX.applyAsDouble( contour.x( i ) + center.getDoublePosition( 0 ) );
			final double yi = toScreenY.applyAsDouble( contour.y( i ) + center.getDoublePosition( 1 ) );
			polygon.lineTo( xi, yi );

			if ( xi > maxTextPos )
				maxTextPos = xi;
		}
		polygon.closePath();
		return maxTextPos;
	}
}
