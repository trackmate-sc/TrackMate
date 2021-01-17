package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotAnalyzerFactory.class )
public class SpotIntensityMultiCAnalyzerFactory< T extends RealType< T > & NativeType< T > > implements SpotAnalyzerFactory< T >
{

	private static final String KEY = "Spot intensity";
	
	static final String MEAN_INTENSITY = "MEAN_INTENSITY_CH";
	static final String MEDIAN_INTENSITY = "MEDIAN_INTENSITY_CH";
	static final String MIN_INTENSITY = "MIN_INTENSITY_CH";
	static final String MAX_INTENSITY = "MAX_INTENSITY_CH";
	static final String TOTAL_INTENSITY = "TOTAL_INTENSITY_CH";
	static final String STD_INTENSITY = "STD_INTENSITY_CH";

	private static final String MEAN_SHORT_NAME = "Mean ch";
	private static final String MEAN_NAME = "Mean intensity ch";
	private static final String MEDIAN_SHORT_NAME = "Median ch";
	private static final String MEDIAN_NAME = "Median intensity ch";
	private static final String MIN_SHORT_NAME = "Min ch";
	private static final String MIN_NAME = "Min intensity ch";
	private static final String MAX_SHORT_NAME = "Max ch";
	private static final String MAX_NAME = "Max intensity ch";
	private static final String SUM_SHORT_NAME = "Sum ch";
	private static final String SUM_NAME = "Sum intensity ch";
	private static final String STD_SHORT_NAME = "Std ch";
	private static final String STD_NAME = "Std intensity ch";

	private static final List< String > FEATURES = Arrays.asList( new String[] {
			MEAN_INTENSITY, MEDIAN_INTENSITY, MIN_INTENSITY, MAX_INTENSITY, TOTAL_INTENSITY, STD_INTENSITY } );
	private static final List< String > FEATURE_SHORTNAMES = Arrays.asList( new String[] {
			MEAN_SHORT_NAME, MEDIAN_SHORT_NAME, MIN_SHORT_NAME, MAX_SHORT_NAME, SUM_SHORT_NAME, STD_SHORT_NAME } );
	private static final List< String > FEATURE_NAMES = Arrays.asList( new String[] {
			MEAN_NAME, MEDIAN_NAME, MIN_NAME, MAX_NAME, SUM_NAME, STD_NAME } );

	private int nChannels = 1;

	@Override
	public void setNChannels( final int nChannels )
	{
		this.nChannels = nChannels;
	}


	@Override
	public SpotAnalyzer< T > getAnalyzer( final ImgPlus< T > img, final int frame, final int channel )
	{
		final ImgPlus< T > imgTC = TMUtils.hyperSlice( img, channel, frame );
		return new SpotIntensityMultiCAnalyzer<>( imgTC, channel );
	}

	static final String makeFeatureKey( final String feature, final int c )
	{
		return feature + ( c + 1 );
	}

	@Override
	public List< String > getFeatures()
	{
		final List< String > features = new ArrayList<>( nChannels * FEATURES.size() );
		for ( int c = 0; c < nChannels; c++ )
			for ( final String feature : FEATURES )
				features.add( makeFeatureKey( feature, c ) );

		return features;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		final Map< String, String > names = new LinkedHashMap<>( nChannels * FEATURES.size() );
		for ( int c = 0; c < nChannels; c++ )
			for ( int i = 0; i < FEATURES.size(); i++ )
			{
				final String feature = FEATURES.get( i );
				final String shortName = FEATURE_SHORTNAMES.get( i );
				names.put( makeFeatureKey( feature, c ), makeFeatureKey( shortName, c ) );
			}

		return names;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		final Map< String, String > names = new LinkedHashMap<>( nChannels * FEATURES.size() );
		for ( int c = 0; c < nChannels; c++ )
			for ( int i = 0; i < FEATURES.size(); i++ )
			{
				final String feature = FEATURES.get( i );
				final String shortName = FEATURE_NAMES.get( i );
				names.put( makeFeatureKey( feature, c ), makeFeatureKey( shortName, c ) );
			}

		return names;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		final List< String > features = getFeatures();
		final Map< String, Dimension > dimensions = new LinkedHashMap<>( features.size() );
		for ( final String feature : features )
			dimensions.put( feature, Dimension.INTENSITY );

		return dimensions;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		final List< String > features = getFeatures();
		final Map< String, Boolean > isints = new LinkedHashMap<>( features.size() );
		for ( final String feature : features )
			isints.put( feature, Boolean.FALSE );

		return isints;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
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
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return KEY;
	}
}
