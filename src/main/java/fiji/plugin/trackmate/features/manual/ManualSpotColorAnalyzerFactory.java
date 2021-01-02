package fiji.plugin.trackmate.features.manual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotAnalyzerFactory.class )
public class ManualSpotColorAnalyzerFactory< T extends RealType< T > & NativeType< T > > implements SpotAnalyzerFactory< T >
{

	public static final String FEATURE = "MANUAL_SPOT_COLOR";

	public static final String KEY = "Manual spot color";

	static final List< String > FEATURES = new ArrayList<>( 1 );

	static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 1 );

	static final Map< String, String > FEATURE_NAMES = new HashMap<>( 1 );

	static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 1 );

	static final Map< String, Boolean > IS_INT = new HashMap<>( 1 );

	static final String INFO_TEXT = "<html>A dummy analyzer for the feature that stores the color manually assigned to each spot.</html>";

	static final String NAME = KEY;

	static
	{
		FEATURES.add( FEATURE );
		FEATURE_SHORT_NAMES.put( FEATURE, "Spot color" );
		FEATURE_NAMES.put( FEATURE, "Manual spot color" );
		FEATURE_DIMENSIONS.put( FEATURE, Dimension.NONE );
		IS_INT.put( FEATURE, Boolean.TRUE );
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
		return INFO_TEXT;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return true;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public SpotAnalyzer< T > getAnalyzer( final ImgPlus< T > img, final int frame, final int channel )
	{
		return SpotAnalyzer.dummyAnalyzer();
	}
}
