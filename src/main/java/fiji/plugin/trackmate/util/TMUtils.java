package fiji.plugin.trackmate.util;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scijava.Context;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;

/**
 * List of static utilities for {@link fiji.plugin.trackmate.TrackMate}.
 */
public class TMUtils
{

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss" );

	/*
	 * STATIC METHODS
	 */

	/**
	 * Return a new map sorted by its values. Adapted from
	 * http://stackoverflow.com
	 * /questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
	 */
	public static < K, V extends Comparable< ? super V > > Map< K, V > sortByValue( final Map< K, V > map, final Comparator< V > comparator )
	{
		final List< Map.Entry< K, V > > list = new LinkedList< >( map.entrySet() );
		Collections.sort( list, Comparator.comparing( Map.Entry::getValue ) );

		final LinkedHashMap< K, V > result = new LinkedHashMap< >();
		for ( final Map.Entry< K, V > entry : list )
			result.put( entry.getKey(), entry.getValue() );
		return result;
	}

	/**
	 * Generate a string representation of a map, typically a settings map.
	 */
	public static final String echoMap( final Map< String, Object > map, final int indent )
	{
		// Build string
		final StringBuilder builder = new StringBuilder();
		for ( final String key : map.keySet() )
		{
			for ( int i = 0; i < indent; i++ )
				builder.append( " " );

			builder.append( "- " );
			builder.append( key.toLowerCase().replace( "_", " " ) );
			builder.append( ": " );
			final Object obj = map.get( key );
			if ( obj instanceof Map )
			{
				builder.append( '\n' );
				@SuppressWarnings( "unchecked" )
				final Map< String, Object > submap = ( Map< String, Object > ) obj;
				builder.append( echoMap( submap, indent + 2 ) );
			}
			else
			{
				builder.append( obj.toString() );
				builder.append( '\n' );
			}
		}
		return builder.toString();
	}

	/**
	 * Wraps an IJ {@link ImagePlus} in an imglib2 {@link ImgPlus}, without
	 * parameterized types. The only way I have found to beat javac constraints
	 * on bounded multiple wildcard.
	 */
	@SuppressWarnings( "rawtypes" )
	public static final ImgPlus rawWraps( final ImagePlus imp )
	{
		final ImgPlus< DoubleType > img = ImagePlusAdapter.wrapImgPlus( imp );
		final ImgPlus raw = img;
		return raw;
	}

	/**
	 * Check that the given map has all some keys. Two String collection allows
	 * specifying that some keys are mandatory, other are optional.
	 *
	 * @param map
	 *            the map to inspect.
	 * @param mandatoryKeys
	 *            the collection of keys that are expected to be in the map. Can
	 *            be <code>null</code>.
	 * @param optionalKeys
	 *            the collection of keys that can be - or not - in the map. Can
	 *            be <code>null</code>.
	 * @param errorHolder
	 *            will be appended with an error message.
	 * @return if all mandatory keys are found in the map, and possibly some
	 *         optional ones, but no others.
	 */
	public static final < T > boolean checkMapKeys( final Map< T, ? > map, Collection< T > mandatoryKeys, Collection< T > optionalKeys, final StringBuilder errorHolder )
	{
		if ( null == optionalKeys )
			optionalKeys = new ArrayList< >();

		if ( null == mandatoryKeys )
			mandatoryKeys = new ArrayList< >();

		boolean ok = true;
		final Set< T > keySet = map.keySet();
		for ( final T key : keySet )
		{
			if ( !( mandatoryKeys.contains( key ) || optionalKeys.contains( key ) ) )
			{
				ok = false;
				errorHolder.append( "Map contains unexpected key: " + key + ".\n" );
			}
		}

		for ( final T key : mandatoryKeys )
		{
			if ( !keySet.contains( key ) )
			{
				ok = false;
				errorHolder.append( "Mandatory key " + key + " was not found in the map.\n" );
			}
		}
		return ok;

	}

	/**
	 * Check the presence and the validity of a key in a map, and test it is of
	 * the desired class.
	 *
	 * @param map
	 *            the map to inspect.
	 * @param key
	 *            the key to find.
	 * @param expectedClass
	 *            the expected class of the target value .
	 * @param errorHolder
	 *            will be appended with an error message.
	 * @return true if the key is found in the map, and map a value of the
	 *         desired class.
	 */
	public static final boolean checkParameter( final Map< String, Object > map, final String key, final Class< ? > expectedClass, final StringBuilder errorHolder )
	{
		final Object obj = map.get( key );
		if ( null == obj )
		{
			errorHolder.append( "Parameter " + key + " could not be found in settings map, or is null.\n" );
			return false;
		}
		if ( !expectedClass.isInstance( obj ) )
		{
			errorHolder.append( "Value for parameter " + key + " is not of the right class. Expected " + expectedClass.getName() + ", got " + obj.getClass().getName() + ".\n" );
			return false;
		}
		return true;
	}

	/**
	 * Returns the mapping in a map that is targeted by a list of keys, in the
	 * order given in the list.
	 */
	public static final < J, K > List< K > getArrayFromMaping( final List< J > keys, final Map< J, K > mapping )
	{
		final List< K > names = new ArrayList< >( keys.size() );
		for ( int i = 0; i < keys.size(); i++ )
			names.add( mapping.get( keys.get( i ) ) );
		return names;
	}

	/*
	 * ImgPlus & calibration & axes
	 */

	/**
	 * Returns the index of the target axis in the given metadata. Return -1 if
	 * the axis was not found.
	 */
	private static final int findAxisIndex( final ImgPlusMetadata img, final AxisType axis )
	{
		return img.dimensionIndex( axis );
	}

	public static final int findXAxisIndex( final ImgPlusMetadata img )
	{
		return findAxisIndex( img, Axes.X );
	}

	public static final int findYAxisIndex( final ImgPlusMetadata img )
	{
		return findAxisIndex( img, Axes.Y );
	}

	public static final int findZAxisIndex( final ImgPlusMetadata img )
	{
		return findAxisIndex( img, Axes.Z );
	}

	public static final int findTAxisIndex( final ImgPlusMetadata img )
	{
		return findAxisIndex( img, Axes.TIME );
	}

	public static final int findCAxisIndex( final ImgPlusMetadata img )
	{
		return findAxisIndex( img, Axes.CHANNEL );
	}

	/**
	 * Return the xyz calibration stored in an {@link ImgPlusMetadata} in a
	 * 3-elements double array. Calibration is ordered as X, Y, Z. If one axis
	 * is not found, then the calibration for this axis takes the value of 1.
	 */
	public static final double[] getSpatialCalibration( final ImgPlusMetadata img )
	{
		final double[] calibration = Util.getArrayFromValue( 1d, 3 );

		for ( int d = 0; d < img.numDimensions(); d++ )
		{
			if ( img.axis( d ).type() == Axes.X )
				calibration[ 0 ] = img.averageScale( d );
			else if ( img.axis( d ).type() == Axes.Y )
				calibration[ 1 ] = img.averageScale( d );
			else if ( img.axis( d ).type() == Axes.Z )
				calibration[ 2 ] = img.averageScale( d );
		}
		return calibration;
	}

	public static double[] getSpatialCalibration( final ImagePlus imp )
	{
		final double[] calibration = Util.getArrayFromValue( 1d, 3 );
		calibration[ 0 ] = imp.getCalibration().pixelWidth;
		calibration[ 1 ] = imp.getCalibration().pixelHeight;
		if ( imp.getNSlices() > 1 )
			calibration[ 2 ] = imp.getCalibration().pixelDepth;

		return calibration;
	}

	/**
	 * Returns an estimate of the <code>p</code>th percentile of the values in
	 * the <code>values</code> array. Taken from commons-math.
	 */
	public static final double getPercentile( final double[] values, final double p )
	{

		final int size = values.length;
		if ( ( p > 1 ) || ( p <= 0 ) )
			throw new IllegalArgumentException( "invalid quantile value: " + p );
		// always return single value for n = 1
		if ( size == 0 )
			return Double.NaN;
		if ( size == 1 )
			return values[ 0 ];
		final double n = size;
		final double pos = p * ( n + 1 );
		final double fpos = Math.floor( pos );
		final int intPos = ( int ) fpos;
		final double dif = pos - fpos;
		final double[] sorted = new double[ size ];
		System.arraycopy( values, 0, sorted, 0, size );
		Arrays.sort( sorted );

		if ( pos < 1 )
			return sorted[ 0 ];
		if ( pos >= n )
			return sorted[ size - 1 ];
		final double lower = sorted[ intPos - 1 ];
		final double upper = sorted[ intPos ];
		return lower + dif * ( upper - lower );
	}

	/**
	 * Returns <code>[range, min, max]</code> of the given double array.
	 *
	 * @return A double[] of length 3, where index 0 is the range, index 1 is
	 *         the min, and index 2 is the max.
	 */
	private static final double[] getRange( final double[] data )
	{
		final double min = Arrays.stream( data ).min().getAsDouble();
		final double max = Arrays.stream( data ).max().getAsDouble();
		return new double[] { ( max - min ), min, max };
	}

	/**
	 * Store the x, y, z coordinates of the specified spot in the first 3
	 * elements of the specified double array.
	 */
	public static final void localize( final Spot spot, final double[] coords )
	{
		coords[ 0 ] = spot.getFeature( Spot.POSITION_X ).doubleValue();
		coords[ 1 ] = spot.getFeature( Spot.POSITION_Y ).doubleValue();
		coords[ 2 ] = spot.getFeature( Spot.POSITION_Z ).doubleValue();
	}

	/**
	 * Return the optimal bin number for a histogram of the data given in array,
	 * using the Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)). It is
	 * ensured that the bin number returned is not smaller and no bigger than
	 * the bounds given in argument.
	 */
	public static final int getNBins( final double[] values, final int minBinNumber, final int maxBinNumber )
	{
		final int size = values.length;
		final double q1 = getPercentile( values, 0.25 );
		final double q3 = getPercentile( values, 0.75 );
		final double iqr = q3 - q1;
		final double binWidth = 2 * iqr * Math.pow( size, -0.33 );
		final double[] range = getRange( values );
		int nBin = ( int ) ( range[ 0 ] / binWidth + 1 );

		if ( nBin > maxBinNumber )
			nBin = maxBinNumber;
		else if ( nBin < minBinNumber )
			nBin = minBinNumber;

		return nBin;
	}

	/**
	 * Return the optimal bin number for a histogram of the data given in array,
	 * using the Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)). It is
	 * ensured that the bin number returned is not smaller than 8 and no bigger
	 * than 256.
	 */
	private static final int getNBins( final double[] values )
	{
		return getNBins( values, 8, 256 );
	}

	/**
	 * Create a histogram from the data given.
	 */
	private static final int[] histogram( final double data[], final int nBins )
	{
		final double[] range = getRange( data );
		final double binWidth = range[ 0 ] / nBins;
		final int[] hist = new int[ nBins ];
		int index;

		if ( nBins > 0 )
		{
			for ( int i = 0; i < data.length; i++ )
			{
				index = Math.min( ( int ) Math.floor( ( data[ i ] - range[ 1 ] ) / binWidth ), nBins - 1 );
				hist[ index ]++;
			}
		}
		return hist;
	}

	/**
	 * Return a threshold for the given data, using an Otsu histogram
	 * thresholding method.
	 */
	public static final double otsuThreshold( final double[] data )
	{
		return otsuThreshold( data, getNBins( data ) );
	}

	/**
	 * Return a threshold for the given data, using an Otsu histogram
	 * thresholding method with a given bin number.
	 */
	private static final double otsuThreshold( final double[] data, final int nBins )
	{
		final int[] hist = histogram( data, nBins );
		final int thresholdIndex = otsuThresholdIndex( hist, data.length );
		final double[] range = getRange( data );
		final double binWidth = range[ 0 ] / nBins;
		return range[ 1 ] + binWidth * thresholdIndex;
	}

	/**
	 * Given a histogram array <code>hist</code>, built with an initial amount
	 * of <code>nPoints</code> data item, this method return the bin index that
	 * thresholds the histogram in 2 classes. The threshold is performed using
	 * the Otsu Threshold Method.
	 *
	 * @param hist
	 *            the histogram array
	 * @param nPoints
	 *            the number of data items this histogram was built on
	 * @return the bin index of the histogram that thresholds it
	 */
	private static final int otsuThresholdIndex( final int[] hist, final int nPoints )
	{
		final int total = nPoints;

		double sum = 0;
		for ( int t = 0; t < hist.length; t++ )
			sum += t * hist[ t ];

		double sumB = 0;
		int wB = 0;
		int wF = 0;

		double varMax = 0;
		int threshold = 0;

		for ( int t = 0; t < hist.length; t++ )
		{
			wB += hist[ t ]; // Weight Background
			if ( wB == 0 )
				continue;

			wF = total - wB; // Weight Foreground
			if ( wF == 0 )
				break;

			sumB += ( t * hist[ t ] );

			final double mB = sumB / wB; // Mean Background
			final double mF = ( sum - sumB ) / wF; // Mean Foreground

			// Calculate Between Class Variance
			final double varBetween = wB * wF * ( mB - mF ) * ( mB - mF );

			// Check if new maximum found
			if ( varBetween > varMax )
			{
				varMax = varBetween;
				threshold = t;
			}
		}
		return threshold;
	}

	/**
	 * Return a String unit for the given dimension. When suitable, the unit is
	 * taken from the settings field, which contains the spatial and time units.
	 * Otherwise, default units are used.
	 */
	public static final String getUnitsFor( final Dimension dimension, final String spaceUnits, final String timeUnits )
	{
		String units = "no unit";
		switch ( dimension )
		{
		case ANGLE:
			units = "Radians";
			break;
		case INTENSITY:
			units = "Counts";
			break;
		case INTENSITY_SQUARED:
			units = "Counts^2";
			break;
		case NONE:
			units = "";
			break;
		case POSITION:
		case LENGTH:
			units = spaceUnits;
			break;
		case QUALITY:
			units = "Quality";
			break;
		case TIME:
			units = timeUnits;
			break;
		case VELOCITY:
			units = spaceUnits + "/" + timeUnits;
			break;
		case RATE:
			units = "/" + timeUnits;
			break;
		default:
			break;
		case STRING:
			return null;
		}
		return units;
	}

	public static final String getCurrentTimeString()
	{
		return DATE_FORMAT.format( new Date() );
	}

	/**
	 * Returns an interval object that in the specified {@link ImgPlus} <b>slice
	 * in a single time frame</b>.
	 * <p>
	 * The specified {@link Settings} object is used to determine a crop-cube
	 * that will determine the X,Y,Z size of the interval. A single channel will
	 * be taken in the case of a multi-channel image. If the detector set in the
	 * settings object has a parameter for the target channel
	 * {@link fiji.plugin.trackmate.detection.DetectorKeys#KEY_TARGET_CHANNEL},
	 * it will be used; otherwise the first channel will be taken.
	 * <p>
	 * If the specified {@link ImgPlus} has a time axis, it will be dropped and
	 * the returned interval will have one dimension less.
	 *
	 * @param img
	 *            the source image into which the interval is to be defined.
	 * @param settings
	 *            the settings object that will determine the interval size.
	 * @return a new interval.
	 */
	public static final Interval getInterval( final ImgPlus< ? > img, final Settings settings )
	{
		final long[] max = new long[ img.numDimensions() ];
		final long[] min = new long[ img.numDimensions() ];

		// X, we must have it.
		final int xindex = TMUtils.findXAxisIndex( img );
		min[ xindex ] = settings.xstart;
		max[ xindex ] = settings.xend;

		// Y, we must have it.
		final int yindex = TMUtils.findYAxisIndex( img );
		min[ yindex ] = settings.ystart;
		max[ yindex ] = settings.yend;

		// Z, we MIGHT have it.
		final int zindex = TMUtils.findZAxisIndex( img );
		if ( zindex >= 0 )
		{
			min[ zindex ] = settings.zstart;
			max[ zindex ] = settings.zend;
		}

		// CHANNEL, we might have it.
		final int cindex = TMUtils.findCAxisIndex( img );
		if ( cindex >= 0 )
		{
			Integer c = ( Integer ) settings.detectorSettings.get( KEY_TARGET_CHANNEL ); // 1-based.
			if ( null == c )
				c = 1;

			min[ cindex ] = c - 1; // 0-based.
			max[ cindex ] = min[ cindex ];
		}

		// TIME, we might have it, but anyway we leave the start & end
		// management to elsewhere.
		final int tindex = TMUtils.findTAxisIndex( img );

		/*
		 * We want to exclude time (if we have it) from out interval and source,
		 * so that we can provide the detector instance with a hyperslice that
		 * does NOT have time as a dimension.
		 */
		final long[] intervalMin;
		final long[] intervalMax;
		if ( tindex >= 0 )
		{
			intervalMin = new long[ min.length - 1 ];
			intervalMax = new long[ min.length - 1 ];
			int nindex = -1;
			for ( int d = 0; d < min.length; d++ )
			{
				if ( d == tindex )
					continue;

				nindex++;
				intervalMin[ nindex ] = Math.max( 0l, min[ d ] );
				intervalMax[ nindex ] = Math.min( img.max( d ), max[ d ] );
			}
		}
		else
		{
			intervalMin = min;
			intervalMax = max;
		}
		final FinalInterval interval = new FinalInterval( intervalMin, intervalMax );
		return interval;
	}

	/** Obtains the SciJava {@link Context} in use by ImageJ. */
	public static Context getContext() {
		return ( Context ) IJ.runPlugIn( "org.scijava.Context", "" );
	}

	/**
	 * Declare all feature analyzers (spot, edge and track analyzers) that can
	 * be found at runtime to the specified settings.
	 *
	 * @param settings
	 *            the {@link Settings} object to declare the analyzers in.
	 */
	public static void declareAllFeatures( final Settings settings )
	{

		settings.clearSpotAnalyzerFactories();
		final SpotAnalyzerProvider spotAnalyzerProvider = new SpotAnalyzerProvider( settings.imp );
		final List< String > spotAnalyzerKeys = spotAnalyzerProvider.getKeys();
		for ( final String key : spotAnalyzerKeys )
		{
			final SpotAnalyzerFactory< ? > spotFeatureAnalyzer = spotAnalyzerProvider.getFactory( key );
			settings.addSpotAnalyzerFactory( spotFeatureAnalyzer );
		}

		settings.clearEdgeAnalyzers();
		final EdgeAnalyzerProvider edgeAnalyzerProvider = new EdgeAnalyzerProvider();
		final List< String > edgeAnalyzerKeys = edgeAnalyzerProvider.getKeys();
		for ( final String key : edgeAnalyzerKeys )
		{
			final EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getFactory( key );
			settings.addEdgeAnalyzer( edgeAnalyzer );
		}

		settings.clearTrackAnalyzers();
		final TrackAnalyzerProvider trackAnalyzerProvider = new TrackAnalyzerProvider();
		final List< String > trackAnalyzerKeys = trackAnalyzerProvider.getKeys();
		for ( final String key : trackAnalyzerKeys )
		{
			final TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getFactory( key );
			settings.addTrackAnalyzer( trackAnalyzer );
		}
	}

	/**
	 * Creates a default file path to save the TrackMate session to, based on
	 * the image TrackMate works on.
	 *
	 * @param settings
	 *            the settings object from which to read the image, its folder,
	 *            etc.
	 * @param logger
	 *            a logger instance in which to echo problems if any.
	 * @return a new file.
	 */
	public static File proposeTrackMateSaveFile( final Settings settings, final Logger logger )
	{
		File folder, file;
		if ( null != settings.imp
				&& null != settings.imp.getOriginalFileInfo()
				&& null != settings.imp.getOriginalFileInfo().directory )
		{
			folder = new File( settings.imp.getOriginalFileInfo().directory );
			/*
			 * Update the settings field with the image file location now,
			 * because it's valid.
			 */
			settings.imageFolder = settings.imp.getOriginalFileInfo().directory;
		}
		else
		{
			folder = new File( System.getProperty( "user.dir" ) );
			/*
			 * Warn the user that the file cannot be reloaded properly because
			 * the source image does not match a file.
			 */
			logger.error( "Warning: The source image does not match a file on the system."
					+ "TrackMate won't be able to reload it when opening this XML file.\n"
					+ "To fix this, save the source image to a TIF file before saving the TrackMate session.\n" );
			settings.imageFolder = "";
		}
		try
		{
			file = new File( folder.getPath() + File.separator + settings.imp.getShortTitle() + ".xml" );
		}
		catch ( final NullPointerException npe )
		{
			file = new File( folder.getPath() + File.separator + "TrackMateData.xml" );
		}
		return file;
	}

	private TMUtils()
	{}
}
