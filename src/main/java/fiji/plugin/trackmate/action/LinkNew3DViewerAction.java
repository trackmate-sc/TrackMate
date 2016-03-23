package fiji.plugin.trackmate.action;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;
import ij3d.Image3DUniverse;
import ij3d.ImageWindow3D;

public class LinkNew3DViewerAction extends AbstractTMAction
{

	private static final String NAME = "Link with new 3D viewer";

	private final static String KEY = "NEW_3DVIEWER";

	private static final String INFO_TEXT = "<html>"
			+ "This action opens a new 3D viewer, containing only the overlay (spot and tracks), <br> "
			+ "properly linked to the current controller."
			+ "<p>"
			+ "Useful to have synchronized 2D vs 3D views, but note that this 3D view <br> "
			+ "is not kept in sync with manual editing of the model."
			+ "If you manually edit the model (add, remove, move or modify a spot; delete, <br>"
			+ "cut, merge a track, etc...) this view will not show the modifications. <br>"
			+ "It contains an immutable snapshot of the model taken at the time when <br>"
			+ "it was launched. "
			+ "</html>";

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/page_white_link.png" ) );

	private final TrackMateGUIController controller;

	public LinkNew3DViewerAction( final TrackMateGUIController controller )
	{
		this.controller = controller;
	}

	@Override
	public void execute( final TrackMate trackmate )
	{
		new Thread( "TrackMate new 3D viewer thread" )
		{
			@Override
			public void run()
			{
				logger.log( "Rendering 3D overlay...\n" );
				final Image3DUniverse universe = new Image3DUniverse();
				final ImageWindow3D win = new ImageWindow3D( "TrackMate 3D Viewer", universe );

				win.setIconImage( TrackMateWizard.TRACKMATE_ICON.getImage() );
				universe.init( win );
				win.pack();
				win.setVisible( true );
				final SpotDisplayer3D newDisplayer = new SpotDisplayer3D( trackmate.getModel(), controller.getSelectionModel(), universe );

				// Deregister on window closing.
				win.addWindowListener( new WindowListener()
				{

					@Override
					public void windowOpened( final WindowEvent e )
					{}

					@Override
					public void windowIconified( final WindowEvent e )
					{}

					@Override
					public void windowDeiconified( final WindowEvent e )
					{}

					@Override
					public void windowDeactivated( final WindowEvent e )
					{}

					@Override
					public void windowClosing( final WindowEvent e )
					{
						controller.getSelectionModel().removeSelectionChangeListener( newDisplayer );
						trackmate.getModel().removeModelChangeListener( newDisplayer );
					}

					@Override
					public void windowClosed( final WindowEvent e )
					{}

					@Override
					public void windowActivated( final WindowEvent e )
					{}
				} );

				for ( final String key : controller.getGuimodel().getDisplaySettings().keySet() )
				{
					newDisplayer.setDisplaySettings( key, controller.getGuimodel().getDisplaySettings().get( key ) );
				}
				controller.getGuimodel().addView( newDisplayer );
				newDisplayer.render();
				logger.log( "Done.\n" );
			}
		}.start();
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
			return new LinkNew3DViewerAction( controller );
		}
	}
}
