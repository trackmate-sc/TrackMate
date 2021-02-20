package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.TrackMateModule;

public interface TrackMateActionFactory extends TrackMateModule
{
	public TrackMateAction create();
}
