package fiji.plugin.trackmate.detection.findmaxima;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_THRESHOLD;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeRadius;
import static fiji.plugin.trackmate.io.IOUtils.writeTargetChannel;
import static fiji.plugin.trackmate.io.IOUtils.writeThreshold;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;
import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.panels.detector.LogDetectorConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.util.NumberParser;

@Plugin( type = SpotDetectorFactory.class )
public class FindMaximaSpotDetectorFactory< T extends RealType< T > & NativeType< T >> implements SpotDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "FIND_MAXIMA_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Find maxima";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>" + "This detector emulates the 'Find maxima' ImageJ plugin, "
			+ "for any dimensionality."
			+ "<p>"
			+ "It requires a pre-processed image where the detections are localized at local maxima. "
			+ "A threshold value allows for rejecting weak maxima. "
			+ "<p>"
			+ "The 'Threshold' parameter here emulates the 'Noise tolerance' parameter in the ImageJ plugin. "
			+ "Maxima on the edges of the image are excluded." + "</html>";

	/*
	 * FIELDS
	 */

	/** The image to operate on. Multiple frames, single channel. */
	protected ImgPlus< T > img;

	protected Map< String, Object > settings;

	protected String errorMessage;

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
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public SpotDetector< T > getDetector( final Interval interval, final int frame )
	{
		final double threshold = ( Double ) settings.get( KEY_THRESHOLD );
		final double radius = ( Double ) settings.get(KEY_RADIUS);
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		RandomAccessible< T > imFrame;
		final int cDim = TMUtils.findCAxisIndex( img );
		if ( cDim < 0 )
		{
			imFrame = img;
		}
		else
		{
			// In ImgLib2, dimensions are 0-based.
			final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ) - 1;
			imFrame = Views.hyperSlice( img, cDim, channel );
		}

		int timeDim = TMUtils.findTAxisIndex( img );
		if ( timeDim >= 0 )
		{
			if ( cDim >= 0 && timeDim > cDim )
			{
				timeDim--;
			}
			imFrame = Views.hyperSlice( imFrame, timeDim, frame );
		}

		// In case we have a 1D image.
		if ( img.dimension( 0 ) < 2 )
		{ // Single column image, will be rotated internally.
			calibration[ 0 ] = calibration[ 1 ]; // It gets NaN otherwise
			calibration[ 1 ] = 1;
			imFrame = Views.hyperSlice( imFrame, 0, 0 );
		}
		if ( img.dimension( 1 ) < 2 )
		{ // Single line image
			imFrame = Views.hyperSlice( imFrame, 1, 0 );
		}

		final FindMaximaSpotDetector< T > detector = new FindMaximaSpotDetector< T >( imFrame, interval, calibration, threshold,radius );
		detector.setNumThreads( 1 );
		return detector;
	}

	@Override
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
		return checkSettings( settings );
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok =
				writeRadius( settings, element, errorHolder )
						&& writeTargetChannel( settings, element, errorHolder )
						&& writeThreshold( settings, element, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readDoubleAttribute( element, settings, KEY_THRESHOLD, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_RADIUS, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_TARGET_CHANNEL, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( settings );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new LogDetectorConfigurationPanel( settings.imp, INFO_TEXT, NAME, model )
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void initGUI()
			{
				// Customize GUI.

				super.initGUI();
				jCheckBoxMedianFilter.setVisible( false );
				jCheckSubPixel.setVisible( false );
				jLabelThreshold.setText( "Tolerance:" );
			}

			@Override
			public void setSettings( final Map< String, Object > settings )
			{
				sliderChannel.setValue( ( Integer ) settings.get( KEY_TARGET_CHANNEL ) );
				double radius = ( Double ) settings.get( KEY_RADIUS );
				if ( imp != null )
				{
					final Calibration calibration = imp.getCalibration();
					// Not too large
					final double maxWidth = imp.getWidth() * 0.5 * ( calibration == null ? 1 : calibration.pixelWidth );
					final double maxHeight = imp.getHeight() * 0.5 * ( calibration == null ? 1 : calibration.pixelHeight );
					final double max = maxWidth < maxHeight ? maxWidth : maxHeight;
					if ( radius > max )
					{
						radius *= max * 4 / ( imp.getWidth() + imp.getHeight() );
					}
					// Not too small
					final double pw = calibration == null ? 1 : calibration.pixelWidth;
					radius = Math.max( radius / pw, 1.5 ) * pw;
				}
				jTextFieldBlobDiameter.setText( "" + ( 2 * radius ) );
				jTextFieldThreshold.setText( "" + settings.get( KEY_THRESHOLD ) );
			}

			@Override
			public Map< String, Object > getSettings()
			{
				final HashMap< String, Object > settings = new HashMap< String, Object >( 5 );
				final int targetChannel = sliderChannel.getValue();
				final double expectedRadius = NumberParser.parseDouble( jTextFieldBlobDiameter.getText() ) / 2;
				final double threshold = NumberParser.parseDouble( jTextFieldThreshold.getText() );
				settings.put( KEY_TARGET_CHANNEL, targetChannel );
				settings.put( KEY_RADIUS, expectedRadius );
				settings.put( KEY_THRESHOLD, threshold );
				return settings;
			}

			@SuppressWarnings( { "rawtypes", "unchecked" } )
			@Override
			protected SpotDetectorFactory getDetectorFactory()
			{
				return new FindMaximaSpotDetectorFactory();
			}
		};
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = new HashMap< String, Object >();
		settings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		settings.put( KEY_THRESHOLD, DEFAULT_THRESHOLD );
		settings.put( KEY_RADIUS, DEFAULT_RADIUS );
		return settings;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_RADIUS, Double.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_THRESHOLD );
		mandatoryKeys.add( KEY_RADIUS );
		ok = ok & checkMapKeys( settings, mandatoryKeys, null, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

}
