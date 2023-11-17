package fiji.plugin.trackmate.action.meshtools;

import java.awt.Frame;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;

public class MeshSmootherAction extends AbstractTMAction
{

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		final MeshSmoother smoother = new MeshSmoother( trackmate.getModel().getSpots().iterable( true ), logger );

		final MeshSmootherModel model = new MeshSmootherModel();
		final MeshSmootherPanel panel = new MeshSmootherPanel( model );

		panel.btnRun.addActionListener( e -> {
			new Thread( () -> {
				final EverythingDisablerAndReenabler enabler = new EverythingDisablerAndReenabler( panel, new Class[] { JLabel.class } );
				try
				{
					enabler.disable();
					smoother.smooth( model.getNIters(), model.getMu(), model.getLambda(), model.getWeightType() );
					// Trigger refresh.
					trackmate.getModel().getModelChangeListener().forEach( l -> l.modelChanged( new ModelChangeEvent( this, ModelChangeEvent.SPOTS_COMPUTED ) ) );
				}
				finally
				{
					enabler.reenable();
				}
			}, "TrackMate mesh smoother" ).start();
		} );

		panel.btnUndo.addActionListener( e -> {
			new Thread( () -> {
				final EverythingDisablerAndReenabler enabler = new EverythingDisablerAndReenabler( panel, new Class[] { JLabel.class } );
				try
				{
					enabler.disable();
					smoother.undo();
					// Trigger refresh.
					trackmate.getModel().getModelChangeListener().forEach( l -> l.modelChanged( new ModelChangeEvent( this, ModelChangeEvent.SPOTS_COMPUTED ) ) );
				}
				finally
				{
					enabler.reenable();
				}
			}, "TrackMate mesh smoother" ).start();
		} );

		final JFrame frame = new JFrame( "Smoothing params" );
		frame.getContentPane().add( panel );
		frame.setSize( 400, 300 );
		GuiUtils.positionWindow( frame, parent );
		frame.setVisible( true );
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
