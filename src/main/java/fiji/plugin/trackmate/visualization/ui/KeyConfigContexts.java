package fiji.plugin.trackmate.visualization.ui;

import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider.Scope;

import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public interface KeyConfigContexts
{

	/**
	 * The scope of the TrackMate app.
	 */
	Scope KEY_CONFIG_SCOPE = new Scope( "TrackMate" );

	/**
	 * The action or behaviour applies to the whole app.
	 */
	String TRACKMATE = "trackmate";

	/**
	 * The action or behaviour applies to the {@link HyperStackDisplayer} view
	 * (the main view).
	 */
	String HYPERSTACK_DISPLAYER = "trackmate-main-view";

	/**
	 * The action or behaviour applies to TrackScheme views.
	 */
	String TRACKSCHEME = "trackscheme";

	/**
	 * The action or behaviour applies to the all spot table views.
	 */
	String ALL_SPOTS_TABLE = "all-spots-table";

	/**
	 * The action or behaviour applies to the track table views.
	 */
	String TRACK_TABLE = "track-table";

	/**
	 * The action or behaviour applies to the BVV views.
	 */
	String BIGVOLUMEVIEWER = "bigvolumeviewer";

}
