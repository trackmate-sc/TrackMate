package fiji.plugin.trackmate.action.closegaps;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JLabel;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;

public class CloseGapsController
{

	private final TrackMate trackmate;

	private final Logger logger;

	private final CloseGapsPanel gui;

	public CloseGapsController( final TrackMate trackmate, final Logger logger )
	{
		this.trackmate = trackmate;
		this.logger = logger;

		final Collection< GapClosingMethod > gapClosingMethods = new ArrayList<>( 1 );
		gapClosingMethods.add( new CloseGapsByLinearInterpolation() );

		this.gui = new CloseGapsPanel( gapClosingMethods );
		gui.btnRun.addActionListener( e -> run( ( ( GapClosingMethod ) gui.cmbboxMethod.getSelectedItem() ) ) );
	}

	private void run( final GapClosingMethod gapClosingMethod )
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
		disabler.disable();
		new Thread( "TrackMateGapClosingThread" )
		{
			@Override
			public void run()
			{
				try
				{
					logger.log( "Applying gap-closing method: " + gapClosingMethod.toString() + ".\n" );
					logger.setStatus( "Gap-closing" );
					gapClosingMethod.execute( trackmate, logger );
					logger.log( "Gap-closing done.\n" );
				}
				finally
				{
					disabler.reenable();
				}
			}
		}.start();
	}

	public void show()
	{
		if ( gui.getParent() != null && gui.getParent().isVisible() )
			return;

		final JFrame frame = new JFrame( "TrackMate gap-closing" );
		frame.setIconImage( CloseGapsAction.ICON.getImage() );
		frame.setSize( 500, 400 );
		frame.getContentPane().add( gui );
		GuiUtils.positionWindow( frame, trackmate.getSettings().imp.getCanvas() );
		frame.setVisible( true );
	}
}
