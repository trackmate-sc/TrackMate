package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.Arrays;
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
@Plugin( type = SpotAnalyzerFactory.class, enabled = false )
public class SpotMorphologyAnalyzerFactory< T extends RealType< T > & NativeType< T >> implements SpotAnalyzerFactory< T >
{

	/*
	 * CONSTANTS
	 */

	public final static String[] featurelist_sa = { "ELLIPSOIDFIT_SEMIAXISLENGTH_C", "ELLIPSOIDFIT_SEMIAXISLENGTH_B", "ELLIPSOIDFIT_SEMIAXISLENGTH_A" };

	public final static String[] featurelist_phi = { "ELLIPSOIDFIT_AXISPHI_C", "ELLIPSOIDFIT_AXISPHI_B", "ELLIPSOIDFIT_AXISPHI_A" };

	public final static String[] featurelist_theta = { "ELLIPSOIDFIT_AXISTHETA_C", "ELLIPSOIDFIT_AXISTHETA_B", "ELLIPSOIDFIT_AXISTHETA_A" };

	/** The key name of the morphology feature this analyzer computes. */
	public final static String MORPHOLOGY = "MORPHOLOGY";

	public static final ArrayList< String > FEATURES = new ArrayList< >( 10 );

	public static final HashMap< String, String > FEATURE_NAMES = new HashMap< >( 10 );

	public static final HashMap< String, String > FEATURE_SHORT_NAMES = new HashMap< >( 10 );

	public static final HashMap< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >( 10 );

	public static final Map< String, Boolean > IS_INT = new HashMap< >( 10 );

	static
	{
		FEATURES.add( MORPHOLOGY );
		FEATURES.addAll( Arrays.asList( featurelist_sa ) );
		FEATURES.addAll( Arrays.asList( featurelist_phi ) );
		FEATURES.addAll( Arrays.asList( featurelist_theta ) );

		FEATURE_NAMES.put( MORPHOLOGY, "Morphology" );
		FEATURE_NAMES.put( featurelist_sa[ 0 ], "Ellipsoid C semi-axis length" );
		FEATURE_NAMES.put( featurelist_sa[ 1 ], "Ellipsoid B semi-axis length" );
		FEATURE_NAMES.put( featurelist_sa[ 2 ], "Ellipsoid A semi-axis length" );
		FEATURE_NAMES.put( featurelist_phi[ 0 ], "Ellipsoid C axis φ azimuth" );
		FEATURE_NAMES.put( featurelist_phi[ 1 ], "Ellipsoid B axis φ azimuth" );
		FEATURE_NAMES.put( featurelist_phi[ 2 ], "Ellipsoid A axis φ azimuth" );
		FEATURE_NAMES.put( featurelist_theta[ 0 ], "Ellipsoid C axis θ azimuth" );
		FEATURE_NAMES.put( featurelist_theta[ 1 ], "Ellipsoid B axis θ azimuth" );
		FEATURE_NAMES.put( featurelist_theta[ 2 ], "Ellipsoid A axis θ azimuth" );

		FEATURE_SHORT_NAMES.put( MORPHOLOGY, "Morpho." );
		FEATURE_SHORT_NAMES.put( featurelist_sa[ 0 ], "lc" );
		FEATURE_SHORT_NAMES.put( featurelist_sa[ 1 ], "lb" );
		FEATURE_SHORT_NAMES.put( featurelist_sa[ 2 ], "la" );
		FEATURE_SHORT_NAMES.put( featurelist_phi[ 0 ], "φc" );
		FEATURE_SHORT_NAMES.put( featurelist_phi[ 1 ], "φb" );
		FEATURE_SHORT_NAMES.put( featurelist_phi[ 2 ], "φa" );
		FEATURE_SHORT_NAMES.put( featurelist_theta[ 0 ], "θc" );
		FEATURE_SHORT_NAMES.put( featurelist_theta[ 1 ], "θb" );
		FEATURE_SHORT_NAMES.put( featurelist_theta[ 2 ], "θa" );

		FEATURE_DIMENSIONS.put( MORPHOLOGY, Dimension.NONE );
		FEATURE_DIMENSIONS.put( featurelist_sa[ 0 ], Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( featurelist_sa[ 1 ], Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( featurelist_sa[ 2 ], Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( featurelist_phi[ 0 ], Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( featurelist_phi[ 1 ], Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( featurelist_phi[ 2 ], Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( featurelist_theta[ 0 ], Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( featurelist_theta[ 1 ], Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( featurelist_theta[ 2 ], Dimension.ANGLE );

		IS_INT.put( MORPHOLOGY, Boolean.FALSE );
		IS_INT.put( featurelist_sa[ 0 ], Boolean.FALSE );
		IS_INT.put( featurelist_sa[ 1 ], Boolean.FALSE );
		IS_INT.put( featurelist_sa[ 2 ], Boolean.FALSE );
		IS_INT.put( featurelist_phi[ 0 ], Boolean.FALSE );
		IS_INT.put( featurelist_phi[ 1 ], Boolean.FALSE );
		IS_INT.put( featurelist_phi[ 2 ], Boolean.FALSE );
		IS_INT.put( featurelist_theta[ 0 ], Boolean.FALSE );
		IS_INT.put( featurelist_theta[ 1 ], Boolean.FALSE );
		IS_INT.put( featurelist_theta[ 2 ], Boolean.FALSE );

	}

	public static final String KEY = "Spot morphology";

	/** Spherical shape, that is roughly a = b = c. */
	public static final Double SPHERE = Double.valueOf( 0. );

	/** Oblate shape, disk shaped, that is roughly a = b &gt; c. */
	public static final Double OBLATE = Double.valueOf( 1. );

	/** Prolate shape, rugby ball shape, that is roughly a = b &lt; c. */
	public static final Double PROLATE = Double.valueOf( 2. );

	/** Scalene shape, nothing particular, a &gt; b &gt; c. */
	public static final Double SCALENE = Double.valueOf( 3. );


	/*
	 * METHODS
	 */

	@Override
	public SpotMorphologyAnalyzer< T > getAnalyzer( final Model model, final ImgPlus< T > img, final int frame, final int channel )
	{
		final ImgPlus< T > imgC = HyperSliceImgPlus.fixChannelAxis( img, channel );
		final ImgPlus< T > imgCT = HyperSliceImgPlus.fixTimeAxis( imgC, frame );
		final Iterator< Spot > spots = model.getSpots().iterator( frame, false );
		return new SpotMorphologyAnalyzer< >( imgCT, spots );
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
