package fiji.plugin.trackmate.action;

import ij.ImagePlus;
import ij3d.Image3DUniverse;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;
import fiji.plugin.trackmate.gui.panels.components.ImagePlusChooser;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;

public class CopyOverlayAction extends AbstractTMAction
{

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/page_copy.png" ) );

	public static final String NAME = "Copy overlay to...";

	public static final String KEY = "COPY_OVERLAY";

	public static final String INFO_TEXT = "<html>" + "This action copies the overlay (spots and tracks) to a new existing ImageJ window <br> " + "or to a new 3D viewer window. This can be useful to have the tracks and spots <br> " + "displayed on a modified image. " + "<p>" + "The new view will be independent, and will have its own control panel.<br> " + "</html>";

	@Override
	public void execute( final TrackMate trackmate )
	{
		final ImagePlusChooser impChooser = new ImagePlusChooser( "Copy overlay", "Copy overlay to:", "New 3D viewer" );
		impChooser.setLocationRelativeTo( null );
		impChooser.setVisible( true );
		final ActionListener copyOverlayListener = new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				if ( e == impChooser.OK_BUTTON_PUSHED )
				{
					new Thread( "TrackMate copying thread" )
					{
						@Override
						public void run()
						{
							final SelectionModel selectionModel = new SelectionModel( trackmate.getModel() );
							// Instantiate displayer
							final ImagePlus dest = impChooser.getSelectedImagePlus();
							impChooser.setVisible( false );
							TrackMateModelView newDisplayer;
							String title;
							if ( null == dest )
							{
								logger.log( "Copying data and overlay to new 3D viewer\n" );
								final Image3DUniverse universe = new Image3DUniverse();
								newDisplayer = new SpotDisplayer3D( trackmate.getModel(), selectionModel, universe );
								title = "3D viewer overlay";
							}
							else
							{
								logger.log( "Copying overlay to " + dest.getShortTitle() + "\n" );
								newDisplayer = new HyperStackDisplayer( trackmate.getModel(), selectionModel, dest );
								title = dest.getShortTitle() + " ctrl";
							}
							newDisplayer.render();

							final ConfigureViewsPanel newDisplayerPanel = new ConfigureViewsPanel( trackmate.getModel() );
							final JFrame newFrame = new JFrame();
							newFrame.getContentPane().add( newDisplayerPanel );
							newFrame.pack();
							newFrame.setTitle( title );
							newFrame.setSize( 300, 470 );
							newFrame.setLocationRelativeTo( null );
							newFrame.setVisible( true );
							logger.log( "Done.\n" );

						}
					}.start();
				}
				else
				{
					impChooser.removeActionListener( this );
					impChooser.setVisible( false );
				}
			}
		};
		impChooser.addActionListener( copyOverlayListener );
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
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new CopyOverlayAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}

	}
}
