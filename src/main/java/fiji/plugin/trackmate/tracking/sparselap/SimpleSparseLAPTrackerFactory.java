package fiji.plugin.trackmate.tracking.sparselap;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.tracker.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

@Plugin( type = SpotTrackerFactory.class )
public class SimpleSparseLAPTrackerFactory extends SparseLAPTrackerFactory
{
	public static final String THIS2_TRACKER_KEY = "SIMPLE_SPARSE_LAP_TRACKER";

	public static final String THIS2_NAME = "Simple LAP tracker";

	public static final String THIS2_INFO_TEXT = "<html>"
			+ "This tracker is identical to the sparse LAP tracker present in this trackmate, except that it <br>"
			+ "proposes fewer tuning options. Namely, only gap closing is allowed, based solely on <br>"
			+ "a distance and time condition. Track splitting and merging are not allowed, resulting <br>"
			+ "in having non-branching tracks."
			+ " </html>";

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
