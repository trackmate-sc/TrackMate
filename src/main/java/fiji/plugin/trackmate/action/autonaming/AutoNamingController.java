package fiji.plugin.trackmate.action.autonaming;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JLabel;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import fiji.plugin.trackmate.util.Threads;

public class AutoNamingController {

	private final TrackMate trackmate;
	private final AutoNamingPanel gui;
	private final Logger logger;

	public AutoNamingController(final TrackMate trackmate, final Logger logger) {
		this.trackmate = trackmate;
		this.logger = logger;

		final Collection<AutoNamingRule> namingRules = new ArrayList<>(3);
		namingRules.add(new CopyTrackNameNamingRule());
		namingRules.add(new DefaultAutoNamingRule(".", "", false));
		namingRules.add(new DefaultAutoNamingRule(".", "", true));

		this.gui = new AutoNamingPanel(namingRules);

		// Update to use renamed variables
		gui.runAutoNamingButton.addActionListener(e -> run(((AutoNamingRule) gui.ruleSelectionDropdown.getSelectedItem())));
	}

	private void run(final AutoNamingRule autoNaming) {
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler(gui, new Class[]{JLabel.class});
		disabler.disable();
		Threads.run("TrackMateAutoNamingThread", () -> {
			try {
				logger.log("Applying naming rule: " + autoNaming.toString() + ".\n");
				logger.setStatus("Spot auto-naming");
				AutoNamingPerformer.autoNameSpots(trackmate.getModel(), autoNaming);
				trackmate.getModel().notifyFeaturesComputed();
				logger.log("Spot auto-naming done.\n");
			} finally {
				disabler.reenable();
			}
		});
	}

	public void show() {
		if (gui.getParent() != null && gui.getParent().isVisible())
			return;

		// Other logic to display GUI...
	}
}
