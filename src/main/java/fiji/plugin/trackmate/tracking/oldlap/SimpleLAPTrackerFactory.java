package fiji.plugin.trackmate.tracking.oldlap;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.panels.tracker.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

import org.scijava.plugin.Plugin;

@Plugin( type = SpotTrackerFactory.class, visible = false )
public class SimpleLAPTrackerFactory extends LAPTrackerFactory
{
	public static final String THIS_TRACKER_KEY = "SIMPLE_LAP_TRACKER";

	public static final String THIS_NAME = "Simple LAP tracker";

	public static final String THIS_INFO_TEXT = "<html>" + "This tracker is identical to the LAP tracker present in this trackmate, except that it <br>" + "proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>" + "a distance and time condition. Track splitting and merging are not allowed, resulting <br>" + "in having non-branching tracks." + " </html>";

	@Override
	public String getKey()
	{
		return THIS_TRACKER_KEY;
	}

	@Override
	public String getName()
	{
		return THIS_NAME;
	}

	@Override
	public String getInfoText()
	{
		return THIS_INFO_TEXT;
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		final String spaceUnits = model.getSpaceUnits();
		return new SimpleLAPTrackerSettingsPanel( THIS_NAME, THIS_INFO_TEXT, spaceUnits );
	}
}
