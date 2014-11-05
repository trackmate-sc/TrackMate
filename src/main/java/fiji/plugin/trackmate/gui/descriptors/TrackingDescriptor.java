package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

import javax.swing.SwingUtilities;

public class TrackingDescriptor implements WizardPanelDescriptor
{

	private static final String KEY = "Tracking";

	private final LogPanel logPanel;

	private final TrackMate trackmate;

	private final TrackMateGUIController controller;

	public TrackingDescriptor( final TrackMateGUIController controller )
	{
		this.controller = controller;
		this.trackmate = controller.getPlugin();
		this.logPanel = controller.getGUI().getLogPanel();
	}

	@Override
	public LogPanel getComponent()
	{
		return logPanel;
	}

	@Override
	public void aboutToDisplayPanel()
	{}

	@Override
	public void displayingPanel()
	{
		final Logger logger = trackmate.getModel().getLogger();
		final SpotTrackerFactory trackerFactory = trackmate.getSettings().trackerFactory;
		logger.log( "Starting tracking using " + trackerFactory.getName() + "\n", Logger.BLUE_COLOR );
		logger.log( "with settings:\n" );
		logger.log( trackerFactory.toString( trackmate.getSettings().trackerSettings ) );
		controller.disableButtonsAndStoreState();

		new Thread( "TrackMate tracking thread" )
		{
			@Override
			public void run()
			{
				final long start = System.currentTimeMillis();
				final boolean trackingOK = trackmate.execTracking();
				final long end = System.currentTimeMillis();
				if ( trackingOK )
				{
					logger.log( "Found " + trackmate.getModel().getTrackModel().nTracks( false ) + " tracks.\n" );
					logger.log( String.format( "Tracking done in %.1f s.\n", ( end - start ) / 1e3f ), Logger.BLUE_COLOR );
				}
				else
				{
					logger.error( trackmate.getErrorMessage() + '\n' );
				}
				SwingUtilities.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						controller.restoreButtonsState();
					}
				} );
			}

		}.start();
	}

	@Override
	public void aboutToHidePanel()
	{
		final Thread trackFeatureCalculationThread = new Thread( "TrackMate track feature calculation thread" )
		{
			@Override
			public void run()
			{
				trackmate.computeTrackFeatures( true );
			}
		};
		trackFeatureCalculationThread.start();
		try
		{
			trackFeatureCalculationThread.join();
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public void comingBackToPanel()
	{}

	@Override
	public String getKey()
	{
		return KEY;
	}
}
