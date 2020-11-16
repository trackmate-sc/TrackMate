package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@SuppressWarnings( "deprecation" )
@Plugin( type = SpotAnalyzerFactory.class )
public class SpotMeanIntensityMultiCAnalyzerFactory< T extends RealType< T > & NativeType< T > > implements SpotAnalyzerFactory< T >
{

	private static final String FEATURE = "MEAN_INTENSITY_CH";

	private static final String FEATURE_SHORT_NAME = "Mean ch";

	private static final String FEATURE_NAME = "Mean intensity ch";

	private static final String KEY = "Spot mean intensity multi-channel";

	private int nChannels = 1;

	@Override
	public void setSource( final ImagePlus imp )
	{
		if ( null != imp )
			this.nChannels = imp.getNChannels();
		else
			this.nChannels = 1;
	}

	@Override
	public SpotAnalyzer< T > getAnalyzer( final Model model, final ImgPlus< T > img, final int frame, final int channel )
	{
		final ImgPlus< T > imgT = HyperSliceImgPlus.fixTimeAxis( img, frame );
		final Iterator< Spot > spots = model.getSpots().iterator( frame, false );
		return new SpotMeanIntensityMultiCAnalyzer< T >( imgT, spots, nChannels );
	}

	static final String makeFeatureKey( final int c )
	{
		return FEATURE + ( c + 1 );
	}

	@Override
	public List< String > getFeatures()
	{
		final List< String > features = new ArrayList<>( nChannels );
		for ( int c = 0; c < nChannels; c++ )
			features.add( makeFeatureKey( c ) );

		return features;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		final Map< String, String > names = new LinkedHashMap<>( nChannels );
		for ( int c = 0; c < nChannels; c++ )
			names.put( makeFeatureKey( c ), FEATURE_SHORT_NAME + ( c + 1 ) );

		return names;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		final Map< String, String > names = new LinkedHashMap<>( nChannels );
		for ( int c = 0; c < nChannels; c++ )
			names.put( makeFeatureKey( c ), FEATURE_NAME + ( c + 1 ) );

		return names;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		final List< String > features = getFeatures();
		final Map< String, Dimension > dimensions = new LinkedHashMap<>( nChannels );
		for ( final String feature : features )
			dimensions.put( feature, Dimension.INTENSITY );

		return dimensions;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		final List< String > features = getFeatures();
		final Map< String, Boolean > isints = new LinkedHashMap<>( nChannels );
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
