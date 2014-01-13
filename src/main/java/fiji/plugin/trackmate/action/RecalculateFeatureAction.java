package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateWizard;

public class RecalculateFeatureAction extends AbstractTMAction {


	public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/calculator.png"));
	public static final String NAME = "Recompute all spot features";
	public static final String INFO_TEXT = "<html>" +
			"Calling this action causes the model to recompute all the features <br>" +
			"for all spots." +
			"</html>";

	public RecalculateFeatureAction() {
		this.icon = ICON;
	}

	@Override
	public void execute(final TrackMate trackmate) {
		logger.log("Recalculating all features.\n");
		final Model model = trackmate.getModel();
		final Logger oldLogger = model.getLogger();
		model.setLogger(logger);
		trackmate.computeSpotFeatures(true);
		model.setLogger(oldLogger);
		logger.log("Done.\n");
	}

	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

	@Override
	public String toString() {
		return NAME;
	}
}
