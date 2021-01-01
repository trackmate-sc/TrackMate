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
public class SpotFitEllipseAnalyzerFactory< T extends RealType< T > & NativeType< T > > implements SpotMorphologyAnalyzerFactory< T >
{

	public static final String KEY = "Spot fit 2D ellipse";
	
	public static final String X0 = "ELLIPSE_X0";
	public static final String Y0 = "ELLIPSE_Y0";
	public static final String MAJOR = "ELLIPSE_MAJOR";
	public static final String MINOR = "ELLIPSE_MINOR";
	public static final String THETA = "ELLIPSE_THETA";
	public static final String ASPECTRATIO = "ELLIPSE_ASPECTRATIO";
	
	private static final List< String > FEATURES = Arrays.asList( new String[] {
			X0, Y0, MAJOR, MINOR, THETA, ASPECTRATIO } );
	private static final Map< String, String > FEATURE_SHORTNAMES = new HashMap< >();
	private static final Map< String, String > FEATURE_NAMES = new HashMap< >();
	private static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >();
	private static final Map< String, Boolean > FEATURE_ISINTS = new HashMap< >();
	static
	{
		FEATURE_SHORTNAMES.put( X0, "El. x0" );
		FEATURE_SHORTNAMES.put( Y0, "El. y0" );
		FEATURE_SHORTNAMES.put( MAJOR, "El. long axis" );
		FEATURE_SHORTNAMES.put( MINOR, "El. sh. axis" );
		FEATURE_SHORTNAMES.put( THETA, "El. angle" );
		FEATURE_SHORTNAMES.put( ASPECTRATIO, "El. a.r." );

		FEATURE_NAMES.put( X0, "Ellipse center x0" );
		FEATURE_NAMES.put( Y0, "Ellipse center y0" );
		FEATURE_NAMES.put( MAJOR, "Ellipse long axis" );
		FEATURE_NAMES.put( MINOR, "Ellipse short axis" );
		FEATURE_NAMES.put( THETA, "Ellipse angle" );
		FEATURE_NAMES.put( ASPECTRATIO, "Ellipse aspect ratio" );

		FEATURE_DIMENSIONS.put( X0, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( Y0, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( MAJOR, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( MINOR, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( THETA, Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( ASPECTRATIO, Dimension.NONE );

		FEATURE_ISINTS.put( X0, Boolean.FALSE );
		FEATURE_ISINTS.put( Y0, Boolean.FALSE );
		FEATURE_ISINTS.put( MAJOR, Boolean.FALSE );
		FEATURE_ISINTS.put( MINOR, Boolean.FALSE );
		FEATURE_ISINTS.put( THETA, Boolean.FALSE );
		FEATURE_ISINTS.put( ASPECTRATIO, Boolean.FALSE );
	}


	@Override
	public SpotAnalyzer< T > getAnalyzer( final ImgPlus< T > img, final int frame, final int channel )
	{
		// Don't run more than once.
		if ( channel != 0 )
			return SpotAnalyzer.dummyAnalyzer();

		return new SpotFitEllipseAnalyzer<>( DetectionUtils.is2D( img ) );
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
