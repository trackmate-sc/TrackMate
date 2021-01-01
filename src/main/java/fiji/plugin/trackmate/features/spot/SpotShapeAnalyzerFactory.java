package fiji.plugin.trackmate.features.spot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.detection.DetectionUtils;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotMorphologyAnalyzerFactory.class )
public class SpotShapeAnalyzerFactory< T extends RealType< T > & NativeType< T > > implements SpotMorphologyAnalyzerFactory< T >
{

	public static final String KEY = "Spot 2D shape descriptors";
	
	public static final String AREA = "AREA";
	public static final String PERIMETER = "PERIMETER";
	public static final String CIRCULARITY = "CIRCULARITY";
	public static final String SOLIDITY = "SOLIDITY";

	private static final List< String > FEATURES = Arrays.asList( new String[] {
			AREA, PERIMETER, CIRCULARITY, SOLIDITY } );
	private static final Map< String, String > FEATURE_SHORTNAMES = new HashMap< >();
	private static final Map< String, String > FEATURE_NAMES = new HashMap< >();
	private static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >();
	private static final Map< String, Boolean > FEATURE_ISINTS = new HashMap< >();
	static
	{
		FEATURE_SHORTNAMES.put( AREA, "Area" );
		FEATURE_SHORTNAMES.put( PERIMETER, "Perim." );
		FEATURE_SHORTNAMES.put( CIRCULARITY, "Circ." );
		FEATURE_SHORTNAMES.put( SOLIDITY, "Solidity" );

		FEATURE_NAMES.put( AREA, "Area" );
		FEATURE_NAMES.put( PERIMETER, "Perimeter" );
		FEATURE_NAMES.put( CIRCULARITY, "Circularity" );
		FEATURE_NAMES.put( SOLIDITY, "Solidity" );

		FEATURE_DIMENSIONS.put( AREA, Dimension.AREA );
		FEATURE_DIMENSIONS.put( PERIMETER, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( CIRCULARITY, Dimension.NONE );
		FEATURE_DIMENSIONS.put( SOLIDITY, Dimension.NONE );

		FEATURE_ISINTS.put( AREA, Boolean.FALSE );
		FEATURE_ISINTS.put( PERIMETER, Boolean.FALSE );
		FEATURE_ISINTS.put( CIRCULARITY, Boolean.FALSE );
		FEATURE_ISINTS.put( SOLIDITY, Boolean.FALSE );
	}


	@Override
	public SpotAnalyzer< T > getAnalyzer( final ImgPlus< T > img, final int frame, final int channel )
	{
		// Don't run more than once.
		if ( channel != 0 )
			return SpotAnalyzer.dummyAnalyzer();

		return new SpotShapeAnalyzer<>( DetectionUtils.is2D( img ) );
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORTNAMES;
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
	public Map< String, Boolean > getIsIntFeature()
	{
		return FEATURE_ISINTS;
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
