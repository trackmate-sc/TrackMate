package fiji.plugin.trackmate.action.meshtools;

import java.awt.Frame;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class MeshSmootherAction extends AbstractTMAction
{

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		final MeshSmootherController controller = new MeshSmootherController( trackmate.getModel(), selectionModel, logger );
		controller.setNumThreads( trackmate.getNumThreads() );
		controller.show( parent );
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		public static final String NAME = "Smooth 3D meshes";

		public static final String KEY = "MESH_SMOOTHER";

		public static final String INFO_TEXT = "<html>"
				+ "Displays a tool to smooth the 3D mesh present in "
				+ "the data, using the Taubin smoothing algorithm.</html>";

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
			return new MeshSmootherAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return Icons.VECTOR_ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}

}
