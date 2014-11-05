package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

public class RecalculateFeatureAction extends AbstractTMAction {

	public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/calculator.png"));
	public static final String NAME = "Recompute all spot features";

	public static final String KEY = "RECOMPUTE_SPOT_FEATURES";
	public static final String INFO_TEXT = "<html>" +
			"Calling this action causes the model to recompute all the features <br>" +
			"for all spots." +
			"</html>";

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

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}

		@Override
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new RecalculateFeatureAction();
		}
	}
}
