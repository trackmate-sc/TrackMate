package fiji.plugin.trackmate.action.closegaps;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JLabel;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import fiji.plugin.trackmate.util.Threads;

public class CloseGapsController {

	private final TrackMate trackmate;
	private final Logger logger;
	private final CloseGapsPanel gui;

	public CloseGapsController(final TrackMate trackmate, final Logger logger) {
		this.trackmate = trackmate;
		this.logger = logger;

		final Collection<GapClosingMethod> gapClosingMethods = new ArrayList<>(2);
		gapClosingMethods.add(new CloseGapsByLinearInterpolation());
		gapClosingMethods.add(new CloseGapsByDetection());

		this.gui = new CloseGapsPanel(gapClosingMethods);
		gui.btnRun.addActionListener(e -> run((GapClosingMethod) gui.cmbboxMethod.getSelectedItem()));
	}

	private void run(final GapClosingMethod gapClosingMethod) {
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler(gui, new Class[]{JLabel.class});
		disabler.disable();
		Threads.run("TrackMateGapClosingThread", () -> {
			try {
				logger.log("INFO: Applying gap-closing method: " + gapClosingMethod.toString() + ".");
				logger.setStatus("Gap-closing");
				gapClosingMethod.execute(trackmate, logger);
				logger.log("INFO: Gap-closing done.");
			} finally {
				disabler.reenable();
			}
		});
	}

	public void show() {
		if (gui.getParent() != null && gui.getParent().isVisible())
			return;

		final JFrame frame = new JFrame("TrackMate gap-closing");
		frame.setIconImage(CloseGapsAction.ICON.getImage());
		frame.setSize(300, 500);
		frame.getContentPane().add(gui);
		GuiUtils.positionWindow(frame, trackmate.getSettings().imp.getCanvas());
		frame.setVisible(true);
	}
}
