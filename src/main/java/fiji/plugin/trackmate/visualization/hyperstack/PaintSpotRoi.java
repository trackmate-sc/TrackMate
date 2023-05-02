package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.function.DoubleUnaryOperator;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import gnu.trove.list.TDoubleList;
import ij.ImagePlus;
import ij.gui.ImageCanvas;

/**
 * Utility class to paint the {@link SpotRoi} component of spots.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class PaintSpotRoi extends TrackMatePainter
{

	private final java.awt.geom.Path2D.Double polygon;

	public PaintSpotRoi( final ImagePlus imp, final double[] calibration, final DisplaySettings displaySettings )
	{
		super( imp, calibration, displaySettings );
		this.polygon = new Path2D.Double();
	}

	/**
	 * Paint the specified spot using its {@link SpotRoi} field. The latter must
	 * not be <code>null</code>.
	 *
	 * @param g2d
	 *            the graphics object, configured to paint the spot with.
	 * @param roi
	 *            the spot roi.
	 * @return the text position X indent in pixels to use to paint a string
	 *         next to the painted contour.
	 */
	@Override
	public int paint( final Graphics2D g2d, final Spot spot )
	{
		final ImageCanvas canvas = canvas();
		if ( canvas == null )
			return -1;

		final double maxTextPos = toPolygon( spot, polygon, this::toScreenX, this::toScreenY );
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

		final double xs = toScreenX( spot.getDoublePosition( 0 ) );
		final int textPos = ( int ) ( maxTextPos - xs );
		return textPos;
	}

	static final double max( final TDoubleList l )
	{
		double max = Double.NEGATIVE_INFINITY;
		for ( int i = 0; i < l.size(); i++ )
		{
			final double v = l.get( i );
			if ( v > max )
				max = v;
		}
		return max;
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
	private static final double toPolygon( final Spot spot, final Path2D polygon, final DoubleUnaryOperator toScreenX, final DoubleUnaryOperator toScreenY )
	{
		final SpotRoi roi = spot.getRoi();
		double maxTextPos = Double.NEGATIVE_INFINITY;
		polygon.reset();
		final double x0 = toScreenX.applyAsDouble( roi.x[ 0 ] + spot.getDoublePosition( 0 ) );
		final double y0 = toScreenY.applyAsDouble( roi.y[ 0 ] + spot.getDoublePosition( 1 ) );
		polygon.moveTo( x0, y0 );
		if ( x0 > maxTextPos )
			maxTextPos = x0;

		for ( int i = 1; i < roi.x.length; i++ )
		{
			final double xi = toScreenX.applyAsDouble( roi.x[ i ] + spot.getDoublePosition( 0 ) );
			final double yi = toScreenY.applyAsDouble( roi.y[ i ] + spot.getDoublePosition( 1 ) );
			polygon.lineTo( xi, yi );

			if ( xi > maxTextPos )
				maxTextPos = xi;
		}
		polygon.closePath();
		return maxTextPos;
	}
}
