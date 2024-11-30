package fiji.plugin.trackmate.action.autonaming;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import org.scijava.plugin.Plugin;

import javax.swing.ImageIcon;
import java.awt.Frame;

public class AutoNamingAction extends AbstractTMAction implements TrackMateAction {

	@Override
	public void execute(TrackMate trackmate, SelectionModel selectionModel, DisplaySettings displaySettings, Frame parent) {
		final AutoNamingController controller = new AutoNamingController(trackmate, logger);
		controller.show();
	}

	@Plugin(type = TrackMateActionFactory.class)
	public static class Factory implements TrackMateActionFactory {

		@Override
		public TrackMateAction create() {
			return new AutoNamingAction();
		}

		@Override
		public String getInfoText() {
			// Static method from AbstractTMAction.
			return AbstractTMAction.getInfoText();
		}

		@Override
		public String getName() {
			// Static method from AbstractTMAction.
			return AbstractTMAction.getName();
		}

		@Override
		public String getKey() {
			// Static method from AbstractTMAction.
			return AbstractTMAction.getKey();
		}

		@Override
		public ImageIcon getIcon() {
			return Icons.PENCIL_ICON;
		}
	}
}
