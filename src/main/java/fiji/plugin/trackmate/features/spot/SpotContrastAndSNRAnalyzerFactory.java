package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

/**
 * A factory for {@link SpotContrastAndSNRAnalyzer}s. Because the analyzers of
 * this factory depends on some features defined in
 * {@link SpotIntensityAnalyzer}s, we use a higher priority, so that computation
 * are done after the aforementioned analyzer are done.
 * 
 * @author Jean- Yves Tinevez
 */
@Plugin( type = SpotAnalyzerFactory.class, priority = 1d )
public class SpotContrastAndSNRAnalyzerFactory< T extends RealType< T > & NativeType< T >> implements SpotAnalyzerFactory< T >
{

	/*
	 * FIELDS
	 */

	/** The single feature key name that this analyzer computes. */
	public static final String CONTRAST = "CONTRAST";

	public static final String SNR = "SNR";

	public static final ArrayList< String > FEATURES = new ArrayList< String >( 2 );

	public static final HashMap< String, String > FEATURE_NAMES = new HashMap< String, String >( 2 );

	public static final HashMap< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >( 2 );

	public static final HashMap< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 2 );
	static
	{
		FEATURES.add( CONTRAST );
		FEATURES.add( SNR );
		FEATURE_NAMES.put( CONTRAST, "Contrast" );
		FEATURE_NAMES.put( SNR, "Signal/Noise ratio" );
		FEATURE_SHORT_NAMES.put( CONTRAST, "Constrast" );
		FEATURE_SHORT_NAMES.put( SNR, "SNR" );
		FEATURE_DIMENSIONS.put( CONTRAST, Dimension.NONE );
		FEATURE_DIMENSIONS.put( SNR, Dimension.NONE );
	}

	public static final String KEY = "Spot contrast and SNR";

	/*
	 * METHODS
	 */

	@Override
	public SpotContrastAndSNRAnalyzer< T > getAnalyzer( final Model model, final ImgPlus< T > img, final int frame, final int channel )
	{
		final ImgPlus< T > imgC = HyperSliceImgPlus.fixChannelAxis( img, channel );
		final ImgPlus< T > imgCT = HyperSliceImgPlus.fixTimeAxis( imgC, frame );
		final Iterator< Spot > spots = model.getSpots().iterator( frame, false );
		return new SpotContrastAndSNRAnalyzer< T >( imgCT, spots );
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

}
