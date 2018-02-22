package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.imagej.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@SuppressWarnings( "deprecation" )
@Plugin( type = SpotAnalyzerFactory.class, priority = 0d )
public class SpotIntensityAnalyzerFactory< T extends RealType< T > & NativeType< T >> implements SpotAnalyzerFactory< T >
{

	/*
	 * CONSTANTS
	 */

	public static final String KEY = "Spot descriptive statistics";

	public static final String MEAN_INTENSITY = "MEAN_INTENSITY";

	public static final String MEDIAN_INTENSITY = "MEDIAN_INTENSITY";

	public static final String MIN_INTENSITY = "MIN_INTENSITY";

	public static final String MAX_INTENSITY = "MAX_INTENSITY";

	public static final String TOTAL_INTENSITY = "TOTAL_INTENSITY";

	public static final String STANDARD_DEVIATION = "STANDARD_DEVIATION";

	// public static final String VARIANCE = "VARIANCE";
	// public static final String KURTOSIS = "KURTOSIS";
	// public static final String SKEWNESS = "SKEWNESS";

	public static final ArrayList< String > FEATURES = new ArrayList< >( 9 );

	public static final HashMap< String, String > FEATURE_NAMES = new HashMap< >( 9 );

	public static final HashMap< String, String > FEATURE_SHORT_NAMES = new HashMap< >( 9 );

	public static final HashMap< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >( 9 );

	public static final Map< String, Boolean > IS_INT = new HashMap< >( 9 );
	static
	{
		FEATURES.add( MEAN_INTENSITY );
		FEATURES.add( MEDIAN_INTENSITY );
		FEATURES.add( MIN_INTENSITY );
		FEATURES.add( MAX_INTENSITY );
		FEATURES.add( TOTAL_INTENSITY );
		FEATURES.add( STANDARD_DEVIATION );
		// FEATURES.add(VARIANCE);
		// FEATURES.add(KURTOSIS);
		// FEATURES.add(SKEWNESS);

		FEATURE_NAMES.put( MEAN_INTENSITY, "Mean intensity" );
		FEATURE_NAMES.put( MEDIAN_INTENSITY, "Median intensity" );
		FEATURE_NAMES.put( MIN_INTENSITY, "Minimal intensity" );
		FEATURE_NAMES.put( MAX_INTENSITY, "Maximal intensity" );
		FEATURE_NAMES.put( TOTAL_INTENSITY, "Total intensity" );
		FEATURE_NAMES.put( STANDARD_DEVIATION, "Standard deviation" );
		// FEATURE_NAMES.put(VARIANCE, "Variance");
		// FEATURE_NAMES.put(KURTOSIS, "Kurtosis");
		// FEATURE_NAMES.put(SKEWNESS, "Skewness");

		FEATURE_SHORT_NAMES.put( MEAN_INTENSITY, "Mean" );
		FEATURE_SHORT_NAMES.put( MEDIAN_INTENSITY, "Median" );
		FEATURE_SHORT_NAMES.put( MIN_INTENSITY, "Min" );
		FEATURE_SHORT_NAMES.put( MAX_INTENSITY, "Max" );
		FEATURE_SHORT_NAMES.put( TOTAL_INTENSITY, "Total int." );
		FEATURE_SHORT_NAMES.put( STANDARD_DEVIATION, "Stdev." );
		// FEATURE_SHORT_NAMES.put(VARIANCE, "Var.");
		// FEATURE_SHORT_NAMES.put(KURTOSIS, "Kurtosis");
		// FEATURE_SHORT_NAMES.put(SKEWNESS, "Skewness");

		FEATURE_DIMENSIONS.put( MEAN_INTENSITY, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( MEDIAN_INTENSITY, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( MIN_INTENSITY, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( MAX_INTENSITY, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( TOTAL_INTENSITY, Dimension.INTENSITY );
		FEATURE_DIMENSIONS.put( STANDARD_DEVIATION, Dimension.INTENSITY );
		// FEATURE_DIMENSIONS.put(VARIANCE, Dimension.INTENSITY_SQUARED);
		// FEATURE_DIMENSIONS.put(KURTOSIS, Dimension.NONE);
		// FEATURE_DIMENSIONS.put(SKEWNESS, Dimension.NONE);

		IS_INT.put( MEAN_INTENSITY, Boolean.FALSE );
		IS_INT.put( MEDIAN_INTENSITY, Boolean.FALSE );
		IS_INT.put( MIN_INTENSITY, Boolean.FALSE );
		IS_INT.put( MAX_INTENSITY, Boolean.FALSE );
		IS_INT.put( TOTAL_INTENSITY, Boolean.FALSE );
		IS_INT.put( STANDARD_DEVIATION, Boolean.FALSE );
		// IS_INT.put( VARIANCE, Boolean.FALSE );
		// IS_INT.put( KURTOSIS, Boolean.FALSE );
		// IS_INT.put( SKEWNESS, Boolean.FALSE );
	}

	/*
	 * METHODS
	 */

	@Override
	public SpotIntensityAnalyzer< T > getAnalyzer( final Model model, final ImgPlus< T > img, final int frame, final int channel )
	{
		final ImgPlus< T > imgC = HyperSliceImgPlus.fixChannelAxis( img, channel );
		final ImgPlus< T > imgCT = HyperSliceImgPlus.fixTimeAxis( imgC, frame );
		final Iterator< Spot > spots = model.getSpots().iterator( frame, false );
		return new SpotIntensityAnalyzer< >( imgCT, spots );
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return FEATURE_NAMES;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return FEATURE_DIMENSIONS;
	}

	@Override
	public String getInfoText()
	{
		return null;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return KEY;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}

}
