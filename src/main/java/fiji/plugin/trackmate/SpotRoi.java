package fiji.plugin.trackmate;

import net.imagej.ImgPlus;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Masks;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geom.GeomMasks;
import net.imglib2.roi.geom.real.WritablePolygon2D;
import net.imglib2.type.logic.BoolType;
import net.imglib2.view.Views;

public class SpotRoi
{

	/**
	 * Polygon points X coordinates, in physical units.
	 */
	public final double[] x;

	/**
	 * Polygon points Y coordinates, in physical units.
	 */
	public final double[] y;

	public SpotRoi( final double[] x, final double[] y )
	{
		this.x = x;
		this.y = y;
	}

	/**
	 * Returns a new <code>int</code> array containing the X pixel coordinates
	 * to which to paint this polygon.
	 * 
	 * @param calibration
	 *            the pixel size in X, to convert physical coordinates to pixel
	 *            coordinates.
	 * @param xcorner
	 *            the top-left X corner of the view in the image to paint.
	 * @param magnification
	 *            the magnification of the view.
	 * @param magnification2
	 * @return a new <code>int</code> array.
	 */
	public double[] toPolygonX( final double calibration, final double xcorner, final double spotXCenter, final double magnification )
	{
		final double[] xp = new double[ x.length ];
		for ( int i = 0; i < xp.length; i++ )
		{
			final double xc = ( spotXCenter + x[ i ] ) / calibration;
			xp[ i ] = ( xc - xcorner ) * magnification;
		}
		return xp;
	}

	/**
	 * Returns a new <code>int</code> array containing the Y pixel coordinates
	 * to which to paint this polygon.
	 * 
	 * @param calibration
	 *            the pixel size in Y, to convert physical coordinates to pixel
	 *            coordinates.
	 * @param ycorner
	 *            the top-left Y corner of the view in the image to paint.
	 * @param magnification
	 *            the magnification of the view.
	 * @return a new <code>int</code> array.
	 */
	public double[] toPolygonY( final double calibration, final double ycorner, final double spotYCenter, final double magnification )
	{
		final double[] yp = new double[ y.length ];
		for ( int i = 0; i < yp.length; i++ )
		{
			final double yc = ( spotYCenter + y[ i ] ) / calibration;
			yp[ i ] = ( yc - ycorner ) * magnification;
		}
		return yp;
	}

	public < T > IterableInterval< T > sample( final Spot spot, final ImgPlus< T > img )
	{
		return sample( spot.getDoublePosition( 0 ), spot.getDoublePosition( 1 ), img, img.averageScale( 0 ), img.averageScale( 1 ) );
	}

	public < T > IterableInterval< T > sample( final double spotXCenter, final double spotYCenter, final RandomAccessibleInterval< T > img, final double xScale, final double yScale )
	{
		final double[] xp = toPolygonX( xScale, 0, spotXCenter, 1. );
		final double[] yp = toPolygonY( yScale, 0, spotYCenter, 1. );
		final WritablePolygon2D polygon = GeomMasks.closedPolygon2D( xp, yp );
		final IterableRegion< BoolType > region = Masks.toIterableRegion( polygon );
		return Regions.sample( region, Views.extendMirrorDouble( img ) );
	}
}
