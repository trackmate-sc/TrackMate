package fiji.plugin.trackmate;

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
	public int[] toPolygonX( final double calibration, final int xcorner, final double spotXCenter, final double magnification )
	{
		final int[] xp = new int[ x.length ];
		for ( int i = 0; i < xp.length; i++ )
		{
			final double xc = ( spotXCenter + x[ i ] ) / calibration + 0.5;
			final double xs = ( xc - xcorner ) * magnification;
			xp[ i ] = ( int ) Math.round( xs );
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
	public int[] toPolygonY( final double calibration, final int ycorner, final double spotYCenter, final double magnification )
	{
		final int[] yp = new int[ y.length ];
		for ( int i = 0; i < yp.length; i++ )
		{
			final double yc = ( spotYCenter + y[ i ] ) / calibration + 0.5;
			final double ys = ( yc - ycorner ) * magnification;
			yp[ i ] = ( int ) Math.round( ys );
		}
		return yp;
	}
}
