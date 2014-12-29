package fiji.plugin.trackmate.tracking;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.ConfigurationPanel;

import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

@Plugin( type = SpotTrackerFactory.class, priority = -1d )
public class ManualTrackerFactory implements SpotTrackerFactory
{
	public static final String TRACKER_KEY = "MANUAL_TRACKER";

	public static final String NAME = "Manual tracking";

	public static final String INFO_TEXT = "<html>" + "Choosing this tracker skips the automated tracking step <br>" + "and keeps the current annotation.</html>";

	private String errorMessage;

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
		return TRACKER_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public SpotTracker create( final SpotCollection spots, final Map< String, Object > settings )
	{
		return null;
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		return null;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		return true;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		return true;
	}

	@Override
	public String toString( final Map< String, Object > sm )
	{
		if ( !checkSettingsValidity( sm ) ) { return errorMessage; }
		return "  Manual tracking.\n";
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		return null;
	}

	@Override
	public boolean checkSettingsValidity( final Map< String, Object > settings )
	{
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

}
