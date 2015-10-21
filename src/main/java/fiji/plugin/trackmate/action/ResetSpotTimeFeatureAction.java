/**
 *
 */
package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;

import java.util.Iterator;
import java.util.Set;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

public class ResetSpotTimeFeatureAction extends AbstractTMAction {


	public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/time.png"));
	public static final String NAME = "Reset spot time";
	public static final String INFO_TEXT = "<html>" +
			"Reset the time feature of all spots: it is set to the frame number "  +
			"times the frame interval. " +
			"</html>";

	private static final String KEY = "RESET_SPOT_TIME";

	@Override
	public void execute(final TrackMate trackmate) {
		logger.log("Reset spot time.\n");
		double dt = trackmate.getSettings().dt;
		if (dt == 0) {
			dt = 1;
		}
		final SpotCollection spots = trackmate.getModel().getSpots();
		final Set<Integer> frames = spots.keySet();
		for(final int frame : frames) {
			for (final Iterator<Spot> iterator = spots.iterator(frame, true); iterator.hasNext();) {
				iterator.next().putFeature(Spot.POSITION_T, frame * dt);
			}
			logger.setProgress((double) (frame + 1) / frames.size());
		}
		logger.log("Done.\n");
		logger.setProgress(0);
	}

	@Plugin( type = TrackMateActionFactory.class, visible = false )
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
			return new ResetSpotTimeFeatureAction();
		}
	}
}
