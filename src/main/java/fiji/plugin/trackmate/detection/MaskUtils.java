package fiji.plugin.trackmate.detection;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.measure.Measurements;
import ij.process.FloatPolygon;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class MaskUtils
{

	/**
	 * Smoothing interval for ROIs.
	 */
	private static final double SMOOTH_INTERVAL = 2.;

	/**
	 * Douglas-Peucker polygon simplification max distance.
	 */
	private static final double DOUGLAS_PEUCKER_MAX_DISTANCE = 0.5;

	public static final < T extends RealType< T > > double otsuThreshold( final RandomAccessibleInterval< T > img )
	{
		// Min & max
		final T t = Util.getTypeFromInterval( img );
		final T max = t.createVariable();
		max.setReal( Double.NEGATIVE_INFINITY );
		final T min = t.createVariable();
		min.setReal( Double.POSITIVE_INFINITY );

		for ( final T pixel : Views.iterable( img ) )
		{
			if ( pixel.compareTo( min ) < 0 )
				min.set( pixel );

			if ( pixel.compareTo( max ) > 0 )
				max.set( pixel );
		}

		// Histogram.
		final Real1dBinMapper< T > mapper = new Real1dBinMapper<>( min.getRealDouble(), max.getRealDouble(), 256, false );
		final Histogram1d< T > histogram = new Histogram1d<>( Views.iterable( img ), mapper );

		// Threshold.
		final long k = getThreshold( histogram );
		final T val = t.createVariable();
		mapper.getCenterValue( k, val );
		return val.getRealDouble();
	}

	public static final long getThreshold( final Histogram1d< ? > hist )
	{
		final long[] histogram = hist.toLongArray();
		// Otsu's threshold algorithm
		// C++ code by Jordan Bevik <Jordan.Bevic@qtiworld.com>
		// ported to ImageJ plugin by G.Landini
		int k, kStar; // k = the current threshold; kStar = optimal threshold
		final int L = histogram.length; // The total intensity of the image
		long N1, N; // N1 = # points with intensity <=k; N = total number of
					// points
		long Sk; // The total intensity for all histogram points <=k
		long S;
		double BCV, BCVmax; // The current Between Class Variance and maximum
							// BCV
		double num, denom; // temporary bookkeeping

		// Initialize values:
		S = 0;
		N = 0;
		for ( k = 0; k < L; k++ )
		{
			S += k * histogram[ k ]; // Total histogram intensity
			N += histogram[ k ]; // Total number of data points
		}

		Sk = 0;
		N1 = histogram[ 0 ]; // The entry for zero intensity
		BCV = 0;
		BCVmax = 0;
		kStar = 0;

		// Look at each possible threshold value,
		// calculate the between-class variance, and decide if it's a max
		for ( k = 1; k < L - 1; k++ )
		{ // No need to check endpoints k = 0 or k = L-1
			Sk += k * histogram[ k ];
			N1 += histogram[ k ];

			// The float casting here is to avoid compiler warning about loss of
			// precision and
			// will prevent overflow in the case of large saturated images
			denom = ( double ) ( N1 ) * ( N - N1 ); // Maximum value of denom is
													// (N^2)/4 =
													// approx. 3E10

			if ( denom != 0 )
			{
				// Float here is to avoid loss of precision when dividing
				num = ( ( double ) N1 / N ) * S - Sk; // Maximum value of num =
														// 255*N =
														// approx 8E7
				BCV = ( num * num ) / denom;
			}
			else
				BCV = 0;

			if ( BCV >= BCVmax )
			{ // Assign the best threshold found so far
				BCVmax = BCV;
				kStar = k;
			}
		}
		// kStar += 1; // Use QTI convention that intensity -> 1 if intensity >=
		// k
		// (the algorithm was developed for I-> 1 if I <= k.)
		return kStar;
	}

	/**
	 * Creates a zero-min label image from a thresholded input image.
	 * 
	 * @param <T>
	 *            the type of the input image. Must be real, scalar.
	 * @param input
	 *            the input image.
	 * @param interval
	 *            the interval in the input image to analyze.
	 * @param threshold
	 *            the threshold to apply to the input image.
	 * @param numThreads
	 *            how many threads to use for multithreaded computation.
	 * @return a new label image.
	 */
	public static final < T extends RealType< T > > ImgLabeling< Integer, IntType > toLabeling(
			final RandomAccessible< T > input,
			final Interval interval,
			final double threshold,
			final int numThreads )
	{
		// Crop.
		final IntervalView< T > crop = Views.interval( input, interval );
		final IntervalView< T > in = Views.zeroMin( crop );
		final Converter< T, BitType > converter = ( a, b ) -> b.set( a.getRealDouble() > threshold );
		final RandomAccessible< BitType > bitMask = Converters.convertRAI( in, converter, new BitType() );

		// Prepare output.
		final ImgFactory< IntType > factory = Util.getArrayOrCellImgFactory( in, new IntType() );
		final Img< IntType > out = factory.create( in );
		final ImgLabeling< Integer, IntType > labeling = new ImgLabeling<>( out );

		// Structuring element.
		final StructuringElement se = StructuringElement.FOUR_CONNECTED;

		// Get connected components.
		final ExecutorService executorService = numThreads > 1
				? Executors.newFixedThreadPool( numThreads )
				: Executors.newSingleThreadExecutor();

		ConnectedComponents.labelAllConnectedComponents(
				bitMask,
				labeling,
				MaskUtils.labelGenerator(),
				se,
				executorService );
		executorService.shutdown();
		return labeling;
	}

	/**
	 * Creates spots from a grayscale image, thresholded to create a mask. A
	 * spot is created for each connected-component of the mask, with a size
	 * that matches the mask size.
	 * 
	 * @param <T>
	 *            the type of the input image. Must be real, scalar.
	 * @param input
	 *            the input image.
	 * @param interval
	 *            the interval in the input image to analyze.
	 * @param calibration
	 *            the physical calibration.
	 * @param threshold
	 *            the threshold to apply to the input image.
	 * @param numThreads
	 *            how many threads to use for multithreaded computation.
	 * @return a list of spots, without ROI.
	 */
	public static < T extends RealType< T > > List< Spot > fromThreshold(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final double threshold,
			final int numThreads )
	{
		// Get labeling from mask.
		final ImgLabeling< Integer, IntType > labeling = toLabeling( input, interval, threshold, numThreads );
		return fromLabeling(
				labeling,
				interval,
				calibration );
	}

	/**
	 * Creates spots from a label image.
	 * 
	 * @param <R>
	 *            the type that backs-up the labeling.
	 * @param labeling
	 *            the labeling, must be zero-min.
	 * @param interval
	 *            the interval, used to reposition the spots from the zero-min
	 *            labeling to the proper coordinates.
	 * @param calibration
	 *            the physical calibration.
	 * @return a list of spots, without ROI.
	 */
	public static < R extends IntegerType< R > > List< Spot > fromLabeling(
			final ImgLabeling< Integer, R > labeling,
			final Interval interval,
			final double[] calibration )
	{
		// Parse each component.
		final LabelRegions< Integer > regions = new LabelRegions<>( labeling );
		final Iterator< LabelRegion< Integer > > iterator = regions.iterator();
		final List< Spot > spots = new ArrayList<>( regions.getExistingLabels().size() );
		while ( iterator.hasNext() )
		{
			final LabelRegion< Integer > region = iterator.next();
			final LabelRegionCursor cursor = region.localizingCursor();
			final int[] cursorPos = new int[ labeling.numDimensions() ];
			final long[] sum = new long[ 3 ];
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				cursor.localize( cursorPos );
				for ( int d = 0; d < sum.length; d++ )
					sum[ d ] += cursorPos[ d ];
			}

			final double[] pos = new double[ 3 ];
			for ( int d = 0; d < pos.length; d++ )
				pos[ d ] = sum[ d ] / ( double ) region.size();

			final double x = calibration[ 0 ] * ( interval.min( 0 ) + pos[ 0 ] );
			final double y = calibration[ 1 ] * ( interval.min( 1 ) + pos[ 1 ] );
			final double z = calibration[ 2 ] * ( interval.min( 2 ) + pos[ 2 ] );

			double volume = region.size();
			for ( int d = 0; d < calibration.length; d++ )
				if ( calibration[ d ] > 0 )
					volume *= calibration[ d ];
			final double radius = ( labeling.numDimensions() == 2 )
					? Math.sqrt( volume / Math.PI )
					: Math.pow( 3. * volume / ( 4. * Math.PI ), 1. / 3. );
			final double quality = region.size();
			spots.add( new Spot( x, y, z, radius, quality ) );
		}

		return spots;
	}

	/**
	 * Creates spots from a grayscale image, thresholded to create a mask. A
	 * spot is created for each connected-component of the mask, with a size
	 * that matches the mask size. The quality of the spots is read from another
	 * image, by taking the max pixel value of this image with the ROI.
	 * 
	 * @param <T>
	 *            the type of the input image. Must be real, scalar.
	 * @param input
	 *            the input image.
	 * @param interval
	 *            the interval in the input image to analyze.
	 * @param calibration
	 *            the physical calibration.
	 * @param threshold
	 *            the threshold to apply to the input image.
	 * @param numThreads
	 *            how many threads to use for multithreaded computation.
	 * @param qualityImage
	 *            the image in which to read the quality value.
	 * @return a list of spots, without ROI.
	 */
	public static < T extends RealType< T >, R extends RealType< R > > List< Spot > fromThreshold(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final double threshold,
			final int numThreads,
			final RandomAccessibleInterval< R > qualityImage )
	{
		// Get labeling from mask.
		final ImgLabeling< Integer, IntType > labeling = toLabeling( input, interval, threshold, numThreads );

		// Crop of the quality image.
		final IntervalView< R > cropQuality = Views.interval( qualityImage, interval );
		final IntervalView< R > inQuality = Views.zeroMin( cropQuality );
		final RandomAccess< R > raQuality = inQuality.randomAccess( inQuality );

		// Parse each component.
		final LabelRegions< Integer > regions = new LabelRegions<>( labeling );
		final Iterator< LabelRegion< Integer > > iterator = regions.iterator();
		final List< Spot > spots = new ArrayList<>( regions.getExistingLabels().size() );
		while ( iterator.hasNext() )
		{
			final LabelRegion< Integer > region = iterator.next();
			final LabelRegionCursor cursor = region.localizingCursor();
			final int[] cursorPos = new int[ labeling.numDimensions() ];
			final long[] sum = new long[ 3 ];
			double quality = Double.NEGATIVE_INFINITY;
			while ( cursor.hasNext() )
			{
				cursor.fwd();

				// Position.
				cursor.localize( cursorPos );
				for ( int d = 0; d < sum.length; d++ )
					sum[ d ] += cursorPos[ d ];

				// Quality
				raQuality.setPosition( cursor );
				final double q = raQuality.get().getRealDouble();
				if ( q > quality )
					quality = q;
			}

			final double[] pos = new double[ 3 ];
			for ( int d = 0; d < pos.length; d++ )
				pos[ d ] = sum[ d ] / ( double ) region.size();

			final double x = calibration[ 0 ] * ( interval.min( 0 ) + pos[ 0 ] );
			final double y = calibration[ 1 ] * ( interval.min( 1 ) + pos[ 1 ] );
			final double z = calibration[ 2 ] * ( interval.min( 2 ) + pos[ 2 ] );

			double volume = region.size();
			for ( int d = 0; d < calibration.length; d++ )
				if ( calibration[ d ] > 0 )
					volume *= calibration[ d ];

			final double radius = ( labeling.numDimensions() == 2 )
					? Math.sqrt( volume / Math.PI )
					: Math.pow( 3. * volume / ( 4. * Math.PI ), 1. / 3. );
			spots.add( new Spot( x, y, z, radius, quality ) );
		}

		return spots;
	}

	/**
	 * Creates spots <b>with their ROIs</b> from a <b>2D</b> grayscale image,
	 * thresholded to create a mask. A spot is created for each
	 * connected-component of the mask, with a size that matches the mask size.
	 * The quality of the spots is read from another image, by taking the max
	 * pixel value of this image with the ROI.
	 * 
	 * @param <T>
	 *            the type of the input image. Must be real, scalar.
	 * @param <S>
	 *            the type of the quality image. Must be real, scalar.
	 * @param input
	 *            the input image. Must be 2D.
	 * @param interval
	 *            the interval in the input image to analyze.
	 * @param calibration
	 *            the physical calibration.
	 * @param threshold
	 *            the threshold to apply to the input image.
	 * @param simplify
	 *            if <code>true</code> the polygon will be post-processed to be
	 *            smoother and contain less points.
	 * @param numThreads
	 *            how many threads to use for multithreaded computation.
	 * @param qualityImage
	 *            the image in which to read the quality value.
	 * @return a list of spots, with ROI.
	 */
	public static final < T extends RealType< T >, S extends NumericType< S > > List< Spot > fromThresholdWithROI(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final double threshold,
			final boolean simplify,
			final int numThreads,
			final RandomAccessibleInterval< S > qualityImage )
	{
		if ( input.numDimensions() != 2 )
			throw new IllegalArgumentException( "Can only process 2D images with this method, but got " + input.numDimensions() + "D." );
		
		// Get labeling.
		final ImgLabeling< Integer, IntType > labeling = toLabeling( input, interval, threshold, numThreads );
		return fromLabelingWithROI( labeling, interval, calibration, simplify, qualityImage );
	}

	/**
	 * Creates spots <b>with ROIs</b> from a <b>2D</b> label image. The quality
	 * value is read from a secondary image, byt taking the max value in each
	 * ROI.
	 * 
	 * @param <R>
	 *            the type that backs-up the labeling.
	 * @param <S>
	 *            the type of the quality image. Must be real, scalar.
	 * @param labeling
	 *            the labeling, must be zero-min and 2D..
	 * @param interval
	 *            the interval, used to reposition the spots from the zero-min
	 *            labeling to the proper coordinates.
	 * @param calibration
	 *            the physical calibration.
	 * @param simplify
	 *            if <code>true</code> the polygon will be post-processed to be
	 *            smoother and contain less points.
	 * @param qualityImage
	 *            the image in which to read the quality value.
	 * @return a list of spots, with ROI.
	 */
	public static < R extends IntegerType< R >, S extends NumericType< S > > List< Spot > fromLabelingWithROI(
			final ImgLabeling< Integer, R > labeling,
			final Interval interval,
			final double[] calibration,
			final boolean simplify,
			final RandomAccessibleInterval< S > qualityImage )
	{
		if ( labeling.numDimensions() != 2 )
			throw new IllegalArgumentException( "Can only process 2D images with this method, but got " + labeling.numDimensions() + "D." );

		final LabelRegions< Integer > regions = new LabelRegions< Integer >( labeling );

		// Parse regions to create polygons on boundaries.
		final List< Polygon > polygons = new ArrayList<>( regions.getExistingLabels().size() );
		final Iterator< LabelRegion< Integer > > iterator = regions.iterator();
		while ( iterator.hasNext() )
		{
			final LabelRegion< Integer > region = iterator.next();
			// Analyze in zero-min region.
			final List< Polygon > pp = maskToPolygons( Views.zeroMin( region ) );
			// Translate back to interval coords.
			for ( final Polygon polygon : pp )
				polygon.translate( ( int ) region.min( 0 ), ( int ) region.min( 1 ) );

			polygons.addAll( pp );
		}

		// Quality image.
		final List< Spot > spots = new ArrayList<>( polygons.size() );
		final ImagePlus qualityImp = ( null == qualityImage )
				? null
				: ImageJFunctions.wrap( qualityImage, "QualityImage" );

		// Simplify them and compute a quality.
		for ( final Polygon polygon : polygons )
		{
			final PolygonRoi roi = new PolygonRoi( polygon, PolygonRoi.POLYGON );

			// Create Spot ROI.
			final PolygonRoi fRoi;
			if ( simplify )
				fRoi = simplify( roi, SMOOTH_INTERVAL, DOUGLAS_PEUCKER_MAX_DISTANCE );
			else
				fRoi = roi;

			// Don't include ROIs that have been shrunk to < 1 pixel.
			if ( fRoi.getNCoordinates() < 3 || fRoi.getStatistics().area <= 0. )
				continue;

			// Measure quality.
			final double quality;
			if ( null == qualityImp )
			{
				quality = fRoi.getStatistics().area;
			}
			else
			{
				qualityImp.setRoi( fRoi );
				quality = qualityImp.getStatistics( Measurements.MIN_MAX ).max;
			}

			final Polygon fPolygon = fRoi.getPolygon();
			final double[] xpoly = new double[ fPolygon.npoints ];
			final double[] ypoly = new double[ fPolygon.npoints ];
			for ( int i = 0; i < fPolygon.npoints; i++ )
			{
				xpoly[ i ] = calibration[ 0 ] * ( interval.min( 0 ) + fPolygon.xpoints[ i ] - 0.5 );
				ypoly[ i ] = calibration[ 1 ] * ( interval.min( 1 ) + fPolygon.ypoints[ i ] - 0.5 );
			}

			spots.add( SpotRoi.createSpot( xpoly, ypoly, quality ) );
		}
		return spots;
	}

	private static final double distanceSquaredBetweenPoints( final double vx, final double vy, final double wx, final double wy )
	{
		final double deltax = ( vx - wx );
		final double deltay = ( vy - wy );
		return deltax * deltax + deltay * deltay;
	}

	private static final double distanceToSegmentSquared( final double px, final double py, final double vx, final double vy, final double wx, final double wy )
	{
		final double l2 = distanceSquaredBetweenPoints( vx, vy, wx, wy );
		if ( l2 == 0 )
			return distanceSquaredBetweenPoints( px, py, vx, vy );
		final double t = ( ( px - vx ) * ( wx - vx ) + ( py - vy ) * ( wy - vy ) ) / l2;
		if ( t < 0 )
			return distanceSquaredBetweenPoints( px, py, vx, vy );
		if ( t > 1 )
			return distanceSquaredBetweenPoints( px, py, wx, wy );
		return distanceSquaredBetweenPoints( px, py, ( vx + t * ( wx - vx ) ), ( vy + t * ( wy - vy ) ) );
	}

	private static final double perpendicularDistance( final double px, final double py, final double vx, final double vy, final double wx, final double wy )
	{
		return Math.sqrt( distanceToSegmentSquared( px, py, vx, vy, wx, wy ) );
	}

	private static final void douglasPeucker( final List< double[] > list, final int s, final int e, final double epsilon, final List< double[] > resultList )
	{
		// Find the point with the maximum distance
		double dmax = 0;
		int index = 0;

		final int start = s;
		final int end = e - 1;
		for ( int i = start + 1; i < end; i++ )
		{
			// Point
			final double px = list.get( i )[ 0 ];
			final double py = list.get( i )[ 1 ];
			// Start
			final double vx = list.get( start )[ 0 ];
			final double vy = list.get( start )[ 1 ];
			// End
			final double wx = list.get( end )[ 0 ];
			final double wy = list.get( end )[ 1 ];
			final double d = perpendicularDistance( px, py, vx, vy, wx, wy );
			if ( d > dmax )
			{
				index = i;
				dmax = d;
			}
		}
		// If max distance is greater than epsilon, recursively simplify
		if ( dmax > epsilon )
		{
			// Recursive call
			douglasPeucker( list, s, index, epsilon, resultList );
			douglasPeucker( list, index, e, epsilon, resultList );
		}
		else
		{
			if ( ( end - start ) > 0 )
			{
				resultList.add( list.get( start ) );
				resultList.add( list.get( end ) );
			}
			else
			{
				resultList.add( list.get( start ) );
			}
		}
	}

	/**
	 * Given a curve composed of line segments find a similar curve with fewer
	 * points.
	 * <p>
	 * The Ramer–Douglas–Peucker algorithm (RDP) is an algorithm for reducing
	 * the number of points in a curve that is approximated by a series of
	 * points.
	 * <p>
	 * 
	 * @see <a href=
	 *      "https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm">Ramer–Douglas–Peucker
	 *      Algorithm (Wikipedia)</a> <br>
	 * @author Justin Wetherell <phishman3579@gmail.com>
	 * @param list
	 *            List of Double[] points (x,y)
	 * @param epsilon
	 *            Distance dimension
	 * @return Similar curve with fewer points
	 */
	public static final List< double[] > douglasPeucker( final List< double[] > list, final double epsilon )
	{
		final List< double[] > resultList = new ArrayList< >();
		douglasPeucker( list, 0, list.size(), epsilon, resultList );
		return resultList;
	}

	public static final PolygonRoi simplify( final PolygonRoi roi, final double smoothInterval, final double epsilon )
	{
		final FloatPolygon fPoly = roi.getInterpolatedPolygon( smoothInterval, true );

		final List< double[] > points = new ArrayList<>( fPoly.npoints );
		for ( int i = 0; i < fPoly.npoints; i++ )
			points.add( new double[] { fPoly.xpoints[ i ], fPoly.ypoints[ i ] } );

		final List< double[] > simplifiedPoints = douglasPeucker( points, epsilon );
		final float[] sX = new float[ simplifiedPoints.size() ];
		final float[] sY = new float[ simplifiedPoints.size() ];
		for ( int i = 0; i < sX.length; i++ )
		{
			sX[ i ] = ( float ) simplifiedPoints.get( i )[ 0 ];
			sY[ i ] = ( float ) simplifiedPoints.get( i )[ 1 ];
		}
		final FloatPolygon simplifiedPolygon = new FloatPolygon( sX, sY );
		final PolygonRoi fRoi = new PolygonRoi( simplifiedPolygon, PolygonRoi.POLYGON );
		return fRoi;
	}

	/**
	 * Start at 1.
	 * 
	 * @return a new iterator that goes like 1, 2, 3, ...
	 */
	public static final Iterator< Integer > labelGenerator()
	{
		return new Iterator< Integer >()
		{

			private int currentVal = 0;

			@Override
			public Integer next()
			{
				currentVal++;
				return Integer.valueOf( currentVal );
			}

			@Override
			public boolean hasNext()
			{
				return true;
			}
		};
	}

	/**
	 * Parse a 2D mask and return a list of polygons for the external contours
	 * of white objects.
	 * <p>
	 * Warning: cannot deal with holes, they are simply ignored.
	 * <p>
	 * Copied and adapted from ImageJ1 code by Wayne Rasband.
	 * 
	 * @param <T>
	 *            the type of the mask.
	 * @param mask
	 *            the mask image.
	 * @return a new list of polygons.
	 */
	private static final < T extends BooleanType< T > > List< Polygon > maskToPolygons( final RandomAccessibleInterval< T > mask )
	{
		final int w = ( int ) mask.dimension( 0 );
		final int h = ( int ) mask.dimension( 1 );
		final RandomAccess< T > ra = mask.randomAccess( mask );

		final List< Polygon > polygons = new ArrayList<>();
		boolean[] prevRow = new boolean[ w + 2 ];
		boolean[] thisRow = new boolean[ w + 2 ];
		final Outline[] outline = new Outline[ w + 1 ];

		for ( int y = 0; y <= h; y++ )
		{
			ra.setPosition( y, 1 );

			final boolean[] b = prevRow;
			prevRow = thisRow;
			thisRow = b;
			int xAfterLowerRightCorner = -1;
			Outline oAfterLowerRightCorner = null;

			ra.setPosition( 0, 0 );
			thisRow[ 1 ] = y < h ? ra.get().get() : false;

			for ( int x = 0; x <= w; x++ )
			{
				// we need to read one pixel ahead
				ra.setPosition( x + 1, 0 );
				if ( y < h && x < w - 1 )
					thisRow[ x + 2 ] = ra.get().get();
				else if ( x < w - 1 )
					thisRow[ x + 2 ] = false;

				if ( thisRow[ x + 1 ] )
				{ // i.e., pixel (x,y) is selected
					if ( !prevRow[ x + 1 ] )
					{
						// Upper edge of selected area:
						// - left and right outlines are null: new outline
						// - left null: append (line to left)
						// - right null: prepend (line to right), or
						// prepend&append (after lower right corner, two borders
						// from one corner)
						// - left == right: close (end of hole above) unless we
						// can continue at the right
						// - left != right: merge (prepend) unless we can
						// continue at the right
						if ( outline[ x ] == null )
						{
							if ( outline[ x + 1 ] == null )
							{
								outline[ x + 1 ] = outline[ x ] = new Outline();
								outline[ x ].append( x + 1, y );
								outline[ x ].append( x, y );
							}
							else
							{
								outline[ x ] = outline[ x + 1 ];
								outline[ x + 1 ] = null;
								outline[ x ].append( x, y );
							}
						}
						else if ( outline[ x + 1 ] == null )
						{
							if ( x == xAfterLowerRightCorner )
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = oAfterLowerRightCorner;
								outline[ x ].append( x, y );
								outline[ x + 1 ].prepend( x + 1, y );
							}
							else
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = null;
								outline[ x + 1 ].prepend( x + 1, y );
							}
						}
						else if ( outline[ x + 1 ] == outline[ x ] )
						{
							if ( x < w - 1 && y < h && x != xAfterLowerRightCorner
									&& !thisRow[ x + 2 ] && prevRow[ x + 2 ] )
							{ // at lower right corner & next pxl deselected
								outline[ x ] = null;
								// outline[x+1] unchanged
								outline[ x + 1 ].prepend( x + 1, y );
								xAfterLowerRightCorner = x + 1;
								oAfterLowerRightCorner = outline[ x + 1 ];
							}
							else
							{
								// MINUS (add inner hole)
								// We cannot handle holes in TrackMate.
//								polygons.add( outline[ x ].getPolygon() );
								outline[ x + 1 ] = null;
								outline[ x ] = ( x == xAfterLowerRightCorner ) ? oAfterLowerRightCorner : null;
							}
						}
						else
						{
							outline[ x ].prepend( outline[ x + 1 ] );
							for ( int x1 = 0; x1 <= w; x1++ )
								if ( x1 != x + 1 && outline[ x1 ] == outline[ x + 1 ] )
								{
									outline[ x1 ] = outline[ x ];
									outline[ x + 1 ] = null;
									outline[ x ] = ( x == xAfterLowerRightCorner ) ? oAfterLowerRightCorner : null;
									break;
								}
							if ( outline[ x + 1 ] != null )
								throw new RuntimeException( "assertion failed" );
						}
					}
					if ( !thisRow[ x ] )
					{
						// left edge
						if ( outline[ x ] == null )
							throw new RuntimeException( "assertion failed" );
						outline[ x ].append( x, y + 1 );
					}
				}
				else
				{ // !thisRow[x + 1], i.e., pixel (x,y) is deselected
					if ( prevRow[ x + 1 ] )
					{
						// Lower edge of selected area:
						// - left and right outlines are null: new outline
						// - left == null: prepend
						// - right == null: append, or append&prepend (after
						// lower right corner, two borders from one corner)
						// - right == left: close unless we can continue at the
						// right
						// - right != left: merge (append) unless we can
						// continue at the right
						if ( outline[ x ] == null )
						{
							if ( outline[ x + 1 ] == null )
							{
								outline[ x ] = outline[ x + 1 ] = new Outline();
								outline[ x ].append( x, y );
								outline[ x ].append( x + 1, y );
							}
							else
							{
								outline[ x ] = outline[ x + 1 ];
								outline[ x + 1 ] = null;
								outline[ x ].prepend( x, y );
							}
						}
						else if ( outline[ x + 1 ] == null )
						{
							if ( x == xAfterLowerRightCorner )
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = oAfterLowerRightCorner;
								outline[ x ].prepend( x, y );
								outline[ x + 1 ].append( x + 1, y );
							}
							else
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = null;
								outline[ x + 1 ].append( x + 1, y );
							}
						}
						else if ( outline[ x + 1 ] == outline[ x ] )
						{
							// System.err.println("add " + outline[x]);
							if ( x < w - 1 && y < h && x != xAfterLowerRightCorner
									&& thisRow[ x + 2 ] && !prevRow[ x + 2 ] )
							{ // at lower right corner & next pxl selected
								outline[ x ] = null;
								// outline[x+1] unchanged
								outline[ x + 1 ].append( x + 1, y );
								xAfterLowerRightCorner = x + 1;
								oAfterLowerRightCorner = outline[ x + 1 ];
							}
							else
							{
								polygons.add( outline[ x ].getPolygon() );
								outline[ x + 1 ] = null;
								outline[ x ] = x == xAfterLowerRightCorner ? oAfterLowerRightCorner : null;
							}
						}
						else
						{
							if ( x < w - 1 && y < h && x != xAfterLowerRightCorner
									&& thisRow[ x + 2 ] && !prevRow[ x + 2 ] )
							{ // at lower right corner && next pxl selected
								outline[ x ].append( x + 1, y );
								outline[ x + 1 ].prepend( x + 1, y );
								xAfterLowerRightCorner = x + 1;
								oAfterLowerRightCorner = outline[ x ];
								// outline[x + 1] unchanged (the one at the
								// right-hand side of (x, y-1) to the top)
								outline[ x ] = null;
							}
							else
							{
								outline[ x ].append( outline[ x + 1 ] ); // merge
								for ( int x1 = 0; x1 <= w; x1++ )
									if ( x1 != x + 1 && outline[ x1 ] == outline[ x + 1 ] )
									{
										outline[ x1 ] = outline[ x ];
										outline[ x + 1 ] = null;
										outline[ x ] = ( x == xAfterLowerRightCorner ) ? oAfterLowerRightCorner : null;
										break;
									}
								if ( outline[ x + 1 ] != null )
									throw new RuntimeException( "assertion failed" );
							}
						}
					}
					if ( thisRow[ x ] )
					{
						// right edge
						if ( outline[ x ] == null )
							throw new RuntimeException( "assertion failed" );
						outline[ x ].prepend( x, y + 1 );
					}
				}
			}
		}
		return polygons;
	}

	/**
	 * This class implements a Cartesian polygon in progress. The edges are
	 * supposed to be parallel to the x or y axis. It is implemented as a deque
	 * to be able to add points to both sides.
	 */
	private static class Outline
	{

		private int[] x, y;

		private int first, last, reserved;

		/**
		 * Default extra (spare) space when enlarging arrays (similar
		 * performance with 6-20)
		 */
		private final int GROW = 10;

		public Outline()
		{
			reserved = GROW;
			x = new int[ reserved ];
			y = new int[ reserved ];
			first = last = GROW / 2;
		}

		/**
		 * Makes sure that enough free space is available at the beginning and
		 * end of the list, by enlarging the arrays if required
		 */
		private void needs( final int neededAtBegin, final int neededAtEnd )
		{
			if ( neededAtBegin > first || neededAtEnd > reserved - last )
			{
				final int extraSpace = Math.max( GROW, Math.abs( x[ last - 1 ] - x[ first ] ) );
				final int newSize = reserved + neededAtBegin + neededAtEnd + extraSpace;
				final int newFirst = neededAtBegin + extraSpace / 2;
				final int[] newX = new int[ newSize ];
				final int[] newY = new int[ newSize ];
				System.arraycopy( x, first, newX, newFirst, last - first );
				System.arraycopy( y, first, newY, newFirst, last - first );
				x = newX;
				y = newY;
				last += newFirst - first;
				first = newFirst;
				reserved = newSize;
			}
		}

		/** Adds point x, y at the end of the list */
		public void append( final int x, final int y )
		{
			if ( last - first >= 2 && collinear( this.x[ last - 2 ], this.y[ last - 2 ], this.x[ last - 1 ], this.y[ last - 1 ], x, y ) )
			{
				this.x[ last - 1 ] = x; // replace previous point
				this.y[ last - 1 ] = y;
			}
			else
			{
				needs( 0, 1 ); // new point
				this.x[ last ] = x;
				this.y[ last ] = y;
				last++;
			}
		}

		/** Adds point x, y at the beginning of the list */
		public void prepend( final int x, final int y )
		{
			if ( last - first >= 2 && collinear( this.x[ first + 1 ], this.y[ first + 1 ], this.x[ first ], this.y[ first ], x, y ) )
			{
				this.x[ first ] = x; // replace previous point
				this.y[ first ] = y;
			}
			else
			{
				needs( 1, 0 ); // new point
				first--;
				this.x[ first ] = x;
				this.y[ first ] = y;
			}
		}

		/**
		 * Merge with another Outline by adding it at the end. Thereafter, the
		 * other outline must not be used any more.
		 */
		public void append( final Outline o )
		{
			final int size = last - first;
			final int oSize = o.last - o.first;
			if ( size <= o.first && oSize > reserved - last )
			{ // we don't have enough space in our own array but in that of 'o'
				System.arraycopy( x, first, o.x, o.first - size, size );
				System.arraycopy( y, first, o.y, o.first - size, size );
				x = o.x;
				y = o.y;
				first = o.first - size;
				last = o.last;
				reserved = o.reserved;
			}
			else
			{ // append to our own array
				needs( 0, oSize );
				System.arraycopy( o.x, o.first, x, last, oSize );
				System.arraycopy( o.y, o.first, y, last, oSize );
				last += oSize;
			}
		}

		/**
		 * Merge with another Outline by adding it at the beginning. Thereafter,
		 * the other outline must not be used any more.
		 */
		public void prepend( final Outline o )
		{
			final int size = last - first;
			final int oSize = o.last - o.first;
			if ( size <= o.reserved - o.last && oSize > first )
			{ /*
				 * We don't have enough space in our own array but in that of
				 * 'o' so append our own data to that of 'o'
				 */
				System.arraycopy( x, first, o.x, o.last, size );
				System.arraycopy( y, first, o.y, o.last, size );
				x = o.x;
				y = o.y;
				first = o.first;
				last = o.last + size;
				reserved = o.reserved;
			}
			else
			{ // prepend to our own array
				needs( oSize, 0 );
				first -= oSize;
				System.arraycopy( o.x, o.first, x, first, oSize );
				System.arraycopy( o.y, o.first, y, first, oSize );
			}
		}

		public Polygon getPolygon()
		{
			/*
			 * optimize out intermediate points of straight lines (created,
			 * e.g., by merging outlines)
			 */
			int i, j = first + 1;
			for ( i = first + 1; i + 1 < last; j++ )
			{
				if ( collinear( x[ j - 1 ], y[ j - 1 ], x[ j ], y[ j ], x[ j + 1 ], y[ j + 1 ] ) )
				{
					// merge i + 1 into i
					last--;
					continue;
				}
				if ( i != j )
				{
					x[ i ] = x[ j ];
					y[ i ] = y[ j ];
				}
				i++;
			}
			// wraparound
			if ( collinear( x[ j - 1 ], y[ j - 1 ], x[ j ], y[ j ], x[ first ], y[ first ] ) )
				last--;
			else
			{
				x[ i ] = x[ j ];
				y[ i ] = y[ j ];
			}
			if ( last - first > 2 && collinear( x[ last - 1 ], y[ last - 1 ], x[ first ], y[ first ], x[ first + 1 ], y[ first + 1 ] ) )
				first++;

			final int count = last - first;
			final int[] xNew = new int[ count ];
			final int[] yNew = new int[ count ];
			System.arraycopy( x, first, xNew, 0, count );
			System.arraycopy( y, first, yNew, 0, count );
			return new Polygon( xNew, yNew, count );
		}

		/** Returns whether three points are on one straight line */
		public boolean collinear( final int x1, final int y1, final int x2, final int y2, final int x3, final int y3 )
		{
			return ( x2 - x1 ) * ( y3 - y2 ) == ( y2 - y1 ) * ( x3 - x2 );
		}

		@Override
		public String toString()
		{
			String res = "[first:" + first + ",last:" + last +
					",reserved:" + reserved + ":";
			if ( last > x.length )
				System.err.println( "ERROR!" );
			int nmax = 10; // don't print more coordinates than this
			for ( int i = first; i < last && i < x.length; i++ )
			{
				if ( last - first > nmax && i - first > nmax / 2 )
				{
					i = last - nmax / 2;
					res += "...";
					nmax = last - first; // dont check again
				}
				else
					res += "(" + x[ i ] + "," + y[ i ] + ")";
			}
			return res + "]";
		}
	}

}
