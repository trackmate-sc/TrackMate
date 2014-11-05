package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;

import java.util.Iterator;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

public class ResetRadiusAction extends AbstractTMAction {


	public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/lightbulb_off.png"));
	public static final String NAME = "Reset radius to default value";

	public static final String KEY = "RESET_RADIUS_TO_DEFAULT";
	public static final String INFO_TEXT = "<html>" +
				"This action resets the radius of all retained spots back to the value <br> " +
				"given in the detector settings. " +
				"</html>";
	private static final double FALL_BACK_RADIUS = 5;

	@Override
	public void execute(final TrackMate trackmate) {
		Double radius = (Double) trackmate.getSettings().detectorSettings.get(KEY_RADIUS);
		if (null == radius) {
			radius = FALL_BACK_RADIUS;
			logger.error("Could not determine expected radius from settings. Falling back to "+FALL_BACK_RADIUS+" "
					 + trackmate.getModel().getSpaceUnits());
		}

		logger.log(String.format("Setting all spot radiuses to %.1f " + trackmate.getModel().getSpaceUnits() + "\n", radius));
		final Model model = trackmate.getModel();
		final SpotCollection spots = model.getSpots();
		model.beginUpdate();
		try {
		for (final Iterator<Spot> iterator = spots.iterator(true); iterator.hasNext();) {
			final Spot spot = iterator.next();
			spot.putFeature(Spot.RADIUS, radius);
			model.updateFeatures(spot);
		}
		} finally {
			model.endUpdate();
		}
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
			return new ResetRadiusAction();
		}
	}
}
