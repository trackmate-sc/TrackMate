package fiji.plugin.trackmate.tracking.oldlap;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.panels.tracker.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

import org.scijava.plugin.Plugin;

@Plugin( type = SpotTrackerFactory.class, visible = false )
public class SimpleLAPTrackerFactory extends LAPTrackerFactory
{
	public static final String TRACKER_KEY = "SIMPLE_LAP_TRACKER";

	public static final String NAME = "Simple LAP tracker";

	public static final String INFO_TEXT = "<html>" + "This tracker is identical to the LAP tracker present in this trackmate, except that it <br>" + "proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>" + "a distance and time condition. Track splitting and merging are not allowed, resulting <br>" + "in having non-branching tracks." + " </html>";

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
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		final String spaceUnits = model.getSpaceUnits();
		return new SimpleLAPTrackerSettingsPanel( NAME, INFO_TEXT, spaceUnits );
	}
}
