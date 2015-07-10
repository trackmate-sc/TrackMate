package fiji.plugin.trackmate.action.brownianmotion;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.TrackMateGUIController;

@Plugin( type = TrackMateActionFactory.class )
public class BrownianMotionSizerActionFactory implements TrackMateActionFactory {
	private static final String INFO_TEXT = "<html>This action will estimate the size distribution based on the brownian motion particl tracks. "
			+ "Therefore it will estimate the diffusion coefficient and convert it to the hydrodynamic diameter.</html>";
	
	private static final String KEY = "BROWNIAN_MOTION_SIZER";
	
	private static final String NAME = "Brownian Motion Sizer";
			
	@Override
	public String getInfoText() {
		// TODO Auto-generated method stub
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return KEY;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return NAME;
	}

	@Override
	public TrackMateAction create(TrackMateGUIController controller) {
		// TODO Auto-generated method stub
		return new BrowianMotionSizerAction(controller.getPlugin().getModel(), controller.getSelectionModel());
	}

}
