package fiji.plugin.trackmate.features.manual;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;

@Plugin( type = SpotAnalyzerFactory.class )
public class ManualSpotColorAnalyzerFactory< T extends RealType< T > & NativeType< T >> implements SpotAnalyzerFactory< T >
{

	public static final String FEATURE = "MANUAL_COLOR";

	static final String KEY = "MANUAL_EDGE_COLOR_ANALYZER";

	static final List< String > FEATURES = new ArrayList< String >( 1 );

	static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >( 1 );

	static final Map< String, String > FEATURE_NAMES = new HashMap< String, String >( 1 );

	static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 1 );

	static final String INFO_TEXT = "<html>A dummy analyzer for the feature that stores the color manually assigned to each spot.</html>";

	static final String NAME = "Manual edge color analyzer";

	private static final Color DEFAULT_COLOR = Color.GRAY.darker();

	private static final Double DEFAULT_COLOR_VALUE = Double.valueOf( DEFAULT_COLOR.getRGB() );

	static
	{
		FEATURES.add( FEATURE );
		FEATURE_SHORT_NAMES.put( FEATURE, "Edge color" );
		FEATURE_NAMES.put( FEATURE, "Manual edge color" );
		FEATURE_DIMENSIONS.put( FEATURE, Dimension.NONE );
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
	public SpotAnalyzer< T > getAnalyzer( final Model model, final ImgPlus< T > img, final int frame, final int channel )
	{
		return new SpotAnalyzer< T >()
		{

			private long processingTime;

			@Override
			public boolean checkInput()
			{
				return true;
			}

			@Override
			public boolean process()
			{
				final long start = System.currentTimeMillis();
				for ( final Spot spot : model.getSpots().iterable( false ) )
				{
					spot.putFeature( FEATURE, DEFAULT_COLOR_VALUE );
				}
				final long end = System.currentTimeMillis();
				processingTime = end - start;
				return true;
			}

			@Override
			public String getErrorMessage()
			{
				return "";
			}

			@Override
			public long getProcessingTime()
			{
				return processingTime;
			}
		};
	}
}
