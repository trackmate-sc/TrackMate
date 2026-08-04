package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.visualization.ui.KeyConfigContexts;
import ij.ImagePlus;

public class HyperStackDisplayerActions
{
	private static final String TOGGLE_AUTO_LINKING = "toggle auto-linking";
	private static final String[] TOGGLE_AUTO_LINKING_KEYS = new String[] { "ctrl L" };
	
	private static final String NEXT_TIMEPOINT = "next timepoint";
	private static final String PREVIOUS_TIMEPOINT = "previous timepoint";
	private static final String[] NEXT_TIMEPOINT_KEYS = new String[] { "G" };
	private static final String[] PREVIOUS_TIMEPOINT_KEYS = new String[] { "F" };

	private static final String SEMI_AUTOMATIC_TRACKING = "semi-automatic tracking";
	private static final String[] SEMI_AUTOMATIC_TRACKING_KEYS = new String[] { "shift T" };
	
	private static final String DO_NOTHING_ACTION = "do nothing";
	private static final String[] DO_NOTHING_ACTION_KEYS = new String[] { "W" };

	public static final void install( final Actions actions, final Model model, final SelectionModel selectionModel, final ImagePlus imp )
	{
		// Toggle auto-linking
		actions.runnableAction( () -> SpotEditBehaviours.autoLinkingmode = !SpotEditBehaviours.autoLinkingmode, TOGGLE_AUTO_LINKING, TOGGLE_AUTO_LINKING_KEYS );

		// Avoid closing the window when pressing W
		actions.runnableAction( () -> {}, DO_NOTHING_ACTION, DO_NOTHING_ACTION_KEYS );

		// Change timepoint
		actions.runnableAction( () -> imp.setT( imp.getT() + SemiAutoTracking.params.stepwiseTimeBrowsing ), NEXT_TIMEPOINT, NEXT_TIMEPOINT_KEYS );
		actions.runnableAction( () -> imp.setT( imp.getT() - SemiAutoTracking.params.stepwiseTimeBrowsing ), PREVIOUS_TIMEPOINT, PREVIOUS_TIMEPOINT_KEYS );
		
		// Semi-automatic tracking
		final SemiAutoTracking semiAutoTracking = new SemiAutoTracking( model, selectionModel, imp );
		actions.runnableAction( () -> semiAutoTracking.run(), SEMI_AUTOMATIC_TRACKING, SEMI_AUTOMATIC_TRACKING_KEYS );

		actions.runnableAction( () -> System.out.println( "[HyperStackDisplayer] TROLOLO" ), "refresh", new String[] { "R" } ); // DEBUG
	}

	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.KEY_CONFIG_SCOPE, KeyConfigContexts.HYPERSTACK_DISPLAYER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( TOGGLE_AUTO_LINKING, TOGGLE_AUTO_LINKING_KEYS, "Toggle the auto-linking mode." );
			descriptions.add( NEXT_TIMEPOINT, NEXT_TIMEPOINT_KEYS, "Go to the next timepoint." );
			descriptions.add( PREVIOUS_TIMEPOINT, PREVIOUS_TIMEPOINT_KEYS, "Go to the previous timepoint." );
			descriptions.add( SEMI_AUTOMATIC_TRACKING, SEMI_AUTOMATIC_TRACKING_KEYS, "Run semi-automatic tracking on the selected spots." );
			descriptions.add( DO_NOTHING_ACTION, DO_NOTHING_ACTION_KEYS, "Do nothing. This is to avoid closing the window when pressing W." );
		}
	}
}
