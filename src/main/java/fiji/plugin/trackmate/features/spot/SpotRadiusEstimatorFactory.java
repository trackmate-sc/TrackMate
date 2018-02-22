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
public class SpotRadiusEstimatorFactory< T extends RealType< T > & NativeType< T >> implements SpotAnalyzerFactory< T >
{

	/*
	 * CONSTANT
	 */

	/** The single feature key name that this analyzer computes. */
	public static final String ESTIMATED_DIAMETER = "ESTIMATED_DIAMETER";

	public static final ArrayList< String > FEATURES = new ArrayList< >( 1 );

	public static final HashMap< String, String > FEATURE_NAMES = new HashMap< >( 1 );

	public static final HashMap< String, String > FEATURE_SHORT_NAMES = new HashMap< >( 1 );

	public static final HashMap< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >( 1 );

	public static final Map< String, Boolean > IS_INT = new HashMap< >( 1 );

	static
	{
		FEATURES.add( ESTIMATED_DIAMETER );
		FEATURE_NAMES.put( ESTIMATED_DIAMETER, "Estimated diameter" );
		FEATURE_SHORT_NAMES.put( ESTIMATED_DIAMETER, "Diam." );
		FEATURE_DIMENSIONS.put( ESTIMATED_DIAMETER, Dimension.LENGTH );
		IS_INT.put( ESTIMATED_DIAMETER, Boolean.FALSE );
	}

	public static final String KEY = "Spot radius estimator";


	/*
	 * METHODS
	 */
	@Override
	public SpotRadiusEstimator< T > getAnalyzer( final Model model, final ImgPlus< T > img, final int frame, final int channel )
	{
		final ImgPlus< T > imgC = HyperSliceImgPlus.fixChannelAxis( img, channel );
		final ImgPlus< T > imgCT = HyperSliceImgPlus.fixTimeAxis( imgC, frame );
		final Iterator< Spot > spots = model.getSpots().iterator( frame, false );
		return new SpotRadiusEstimator< >( imgCT, spots );
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
