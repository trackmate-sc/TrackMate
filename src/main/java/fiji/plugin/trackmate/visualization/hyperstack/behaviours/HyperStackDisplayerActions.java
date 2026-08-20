package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import java.awt.event.ActionEvent;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;

import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.visualization.hyperstack.behaviours.semiautotracking.SemiAutoTracking;
import fiji.plugin.trackmate.visualization.hyperstack.behaviours.semiautotracking.SemiAutoTrackingParams;
import fiji.plugin.trackmate.visualization.ui.KeyConfigContexts;
import ij.IJ;
import ij.ImagePlus;

public class HyperStackDisplayerActions
{
	private static final String TOGGLE_AUTO_LINKING = "toggle auto-linking";
	private static final String NEXT_TIMEPOINT = "next timepoint";
	private static final String PREVIOUS_TIMEPOINT = "previous timepoint";
	private static final String SEMI_AUTOMATIC_TRACKING = "semi-automatic tracking";
	private static final String DO_NOTHING_ACTION = "do nothing";

	private static final String[] TOGGLE_AUTO_LINKING_KEYS = new String[] { "ctrl L" };
	private static final String[] NEXT_TIMEPOINT_KEYS = new String[] { "G" };
	private static final String[] PREVIOUS_TIMEPOINT_KEYS = new String[] { "F" };
	private static final String[] SEMI_AUTOMATIC_TRACKING_KEYS = new String[] { "shift T" };
	private static final String[] DO_NOTHING_ACTION_KEYS = new String[] { "W" };

	public static final void install( final Actions actions, final GuiModel guiModel, final ImagePlus imp )
	{
		final SemiAutoTrackingParams params = guiModel.getSemiAutoTrackingParams();

		// Toggle auto-linking
		actions.runnableAction( () -> {
			SpotEditBehaviours.autoLinkingmode = !SpotEditBehaviours.autoLinkingmode;
			IJ.showStatus( "Auto-linking mode " + ( SpotEditBehaviours.autoLinkingmode ? "on" : "off" ), "flash orange 100ms" );
		}, TOGGLE_AUTO_LINKING, TOGGLE_AUTO_LINKING_KEYS );

		// Avoid closing the window when pressing W
		actions.runnableAction( () -> {}, DO_NOTHING_ACTION, DO_NOTHING_ACTION_KEYS );

		// Change timepoint
		actions.namedAction( new StepWiseTimeBrowsingAction( imp, params, true ), NEXT_TIMEPOINT_KEYS );
		actions.namedAction( new StepWiseTimeBrowsingAction( imp, params, false ), PREVIOUS_TIMEPOINT_KEYS );

		// Semi-automatic tracking
		final SemiAutoTracking semiAutoTracking = new SemiAutoTracking( guiModel );
		actions.runnableAction( () -> semiAutoTracking.run(), SEMI_AUTOMATIC_TRACKING, SEMI_AUTOMATIC_TRACKING_KEYS );
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
			descriptions.add( TOGGLE_AUTO_LINKING, TOGGLE_AUTO_LINKING_KEYS, "Toggle the auto-linking mode.\n"
					+ "When the auto-linking mode is on:\n"
					+ "    - If a new spot is created (clicking outside "
					+ "any existing spot), it is automatically linked to the spot in the "
					+ "selection, if there is only one spot in the selection and if it is not "
					+ "in the same frame.\n"
					+ "    - The spot created is then put in the "
					+ "selection, so that it can be linked from in the next spot creation." );
			descriptions.add( NEXT_TIMEPOINT, NEXT_TIMEPOINT_KEYS, "Go to the next timepoint." );
			descriptions.add( PREVIOUS_TIMEPOINT, PREVIOUS_TIMEPOINT_KEYS, "Go to the previous timepoint." );
			descriptions.add( SEMI_AUTOMATIC_TRACKING, SEMI_AUTOMATIC_TRACKING_KEYS, "Run semi-automatic tracking on the selected spots." );
			descriptions.add( DO_NOTHING_ACTION, DO_NOTHING_ACTION_KEYS, "Do nothing. This is to avoid closing the window when pressing W." );
		}
	}

	/**
	 * Ensure we browse a fixed divisions of the timepoints, rather than just
	 * incrementing or decrementing by one. This is useful when the timepoints
	 * are not consecutive, e.g. when the user has selected a subset of
	 * timepoints to display. Taken from what we did in MaMuT.
	 */
	private static class StepWiseTimeBrowsingAction extends AbstractNamedAction
	{

		private static final long serialVersionUID = 1L;

		private final boolean forward;

		private final ImagePlus imp;

		private final SemiAutoTrackingParams params;

		public StepWiseTimeBrowsingAction( final ImagePlus imp, final SemiAutoTrackingParams params, final boolean forward )
		{
			super( forward ? NEXT_TIMEPOINT : PREVIOUS_TIMEPOINT );
			this.imp = imp;
			this.params = params;
			this.forward = forward;
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			final int currentT = imp.getT() - 1;
			final int timeStep = params.stepwiseTimeBrowsing();
			final int prevStep = ( currentT / timeStep ) * timeStep;
			int tp; // 0-based
			if ( forward )
			{
				tp = prevStep + timeStep;
			}
			else
			{
				if ( currentT == prevStep )
					tp = currentT - timeStep;
				else
					tp = prevStep;
			}

			if ( tp < 0 )
				tp = 0;

			if ( tp > imp.getNFrames() - 1 )
				tp = imp.getNFrames() - 1;

			imp.setT( tp + 1 ); // ImageJ is 1-based
		}
	}
}
