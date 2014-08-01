package fiji.plugin.trackmate.tracking.sparselap;

import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.SimpleLAPTrackerFactory;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

@Plugin( type = SpotTrackerFactory.class )
public class SparseLAPTrackerFactory extends SimpleLAPTrackerFactory
{

	public static final String TRACKER_KEY = "SPARSE_LAP_TRACKER";

	public static final String NAME = "Sparse LAP Tracker";

	public static final String INFO_TEXT = "<html>" + "This is the SPARSE version of the LAP tracker.</html>";

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
