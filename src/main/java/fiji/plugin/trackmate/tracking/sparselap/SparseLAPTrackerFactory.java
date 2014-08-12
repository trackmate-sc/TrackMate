package fiji.plugin.trackmate.tracking.sparselap;

import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.LAPTrackerFactory;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

@Plugin( type = SpotTrackerFactory.class )
public class SparseLAPTrackerFactory extends LAPTrackerFactory
{

	public static final String TRACKER_KEY = "SPARSE_LAP_TRACKER";

	public static final String NAME = "Sparse LAP Tracker";

	public static final String INFO_TEXT = "<html>" + "This tracker is quasi-identical to the LAP tracker, " + "but is <u>optimized for memory usage</u>. " + "<p>" + "It benefits from the sparse structure of the cost matrices used in the <i>Jaqaman et al., 2008</i> framework, " + "and exploits it through dedicated algorithms. Use this tracker if you have a very large number of " + "spots to track, and if RAM is limited.</html>";

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
		return new SparseLAPTracker( spots, settings );
	}

}
