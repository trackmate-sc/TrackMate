package fiji.plugin.trackmate.tracking.oldlap;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.panels.tracker.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

import org.scijava.plugin.Plugin;

@Plugin( type = SpotTrackerFactory.class, priority = 10d )
public class SimpleFastLAPTrackerFactory extends FastLAPTrackerFactory
{
	public static final String THIS2_TRACKER_KEY = "SIMPLE_FAST_LAP_TRACKER";

	public static final String THIS2_NAME = "Old Simple LAP tracker";

	public static final String THIS2_INFO_TEXT = "<html>" + "This tracker is identical to the LAP tracker present in this trackmate, except that it <br>" + "proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>" + "a distance and time condition. Track splitting and merging are not allowed, resulting <br>" + "in having non-branching tracks."
			+ "<p>"
			+ "This is the old version of the LAP tracker (pre v2.5.0) that uses a dense cost matrix, and is therefore NOT suited for large problems." + " </html>";

	@Override
	public String getKey()
	{
		return THIS2_TRACKER_KEY;
	}

	@Override
	public String getName()
	{
		return THIS2_NAME;
	}

	@Override
	public String getInfoText()
	{
		return THIS2_INFO_TEXT;
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		final String spaceUnits = model.getSpaceUnits();
		return new SimpleLAPTrackerSettingsPanel( getName(), THIS2_INFO_TEXT, spaceUnits );
	}

}
