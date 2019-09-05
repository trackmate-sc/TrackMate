package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeRadius;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.panels.detector.BasicDetectorConfigurationPanel;

@Plugin( type = SpotDetectorFactory.class )
public class ManualDetectorFactory< T extends RealType< T > & NativeType< T >> implements SpotDetectorFactory< T >
{

	public static final String DETECTOR_KEY = "MANUAL_DETECTOR";

	public static final String NAME = "Manual annotation";

	public static final String INFO_TEXT = "<html>" + "Selecting this will skip the automatic detection phase, and jump directly <br>" + "to manual segmentation. A default spot size will be asked for. " + "</html>";

	protected String errorMessage;

	protected Map< String, Object > settings;

	@Override
	public SpotDetector< T > getDetector( final Interval interval, final int frame )
	{
		return new SpotDetector< T >()
		{

			@Override
			public List< Spot > getResult()
			{
				return Collections.emptyList();
			}

			@Override
			public boolean checkInput()
			{
				return true;
			}

			@Override
			public boolean process()
			{
				return true;
			}

			@Override
			public String getErrorMessage()
			{
				return null;
			}

			@Override
			public long getProcessingTime()
			{
				return 0;
			}
		};
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String toString()
	{
		return NAME;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.settings = settings;
		return checkSettings( settings );
	}

	@Override
	public boolean checkSettings( final Map< String, Object > lSettings )
	{
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & checkParameter( lSettings, KEY_RADIUS, Double.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList< >();
		mandatoryKeys.add( KEY_RADIUS );
		ok = ok & checkMapKeys( lSettings, mandatoryKeys, null, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean marshall( final Map< String, Object > lSettings, final Element element )
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = writeRadius( lSettings, element, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > lSettings )
	{
		lSettings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = readDoubleAttribute( element, lSettings, KEY_RADIUS, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( lSettings );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings lSettings, final Model model )
	{
		return new BasicDetectorConfigurationPanel( lSettings, model, INFO_TEXT, NAME );
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > lSettings = new HashMap< >();
		lSettings.put( KEY_RADIUS, DEFAULT_RADIUS );
		return lSettings;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}
}
