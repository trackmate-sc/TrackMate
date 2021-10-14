package fiji.plugin.trackmate.action.fit;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class SpotGaussianFittingAction extends AbstractTMAction
{

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final java.awt.Frame parent )
	{
		final SpotFitterController controller = new SpotFitterController( trackmate, selectionModel, logger );
		controller.show();
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		public static final String NAME = "Refine spot position with gaussian fitting";

		public static final String KEY = "GAUSS_FIT";

		public static final String INFO_TEXT = "<html>" +
				"This action launches a GUI for the sub-localization of spots using gaussian peak fitting. "
				+ "<p>"
				+ "The fit process will update the spot position and their radius, using the "
				+ "results from the gaussian fit. Of course it works best when the peaks in the image "
				+ "ressemble gaussian functions. The fitting process uses the spots information (position "
				+ "and radius) as initial values for the fit."
				+ "<p>"
				+ "It works for both 2D and 3D images. "
				+ "In 3D it accounts for non-isotropic calibration (and possible PSF "
				+ "deformation in the Z direction) thanks to an elliptic gaussian function, "
				+ "with axes constrained to be along X, Y and Z. "
				+ "In 2D we use an isotropic gaussian."
				+ "</html>";

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new SpotGaussianFittingAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return Icons.SPOT_ICON_16x16;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}

}
