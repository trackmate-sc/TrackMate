package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.ListChooserPanel;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.ManualTrackerFactory;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import fiji.plugin.trackmate.tracking.sparselap.SimpleSparseLAPTrackerFactory;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public class TrackerChoiceDescriptor implements WizardPanelDescriptor
{

	private static final String KEY = "ChooseTracker";

	private final ListChooserPanel component;

	private final TrackMate trackmate;

	private final TrackerProvider trackerProvider;

	private final TrackMateGUIController controller;

	public TrackerChoiceDescriptor( final TrackerProvider trackerProvider, final TrackMate trackmate, final TrackMateGUIController controller )
	{
		this.trackmate = trackmate;
		this.controller = controller;
		this.trackerProvider = trackerProvider;
		final List< String > keys = trackerProvider.getVisibleKeys();
		final List< String > trackerNames = new ArrayList< >( keys.size() );
		final List< String > infoTexts = new ArrayList< >( keys.size() );
		for ( final String key : keys )
		{
			trackerNames.add( trackerProvider.getFactory( key ).getName() );
			infoTexts.add( trackerProvider.getFactory( key ).getInfoText() );
		}
		this.component = new ListChooserPanel( trackerNames, infoTexts, "tracker" );
		setCurrentChoiceFromPlugin();
	}

	/*
	 * METHODS
	 */

	@Override
	public Component getComponent()
	{
		return component;
	}

	@Override
	public void aboutToDisplayPanel()
	{
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void displayingPanel()
	{
		controller.getGUI().setNextButtonEnabled( true );
	}

	@Override
	public void aboutToHidePanel()
	{

		// Configure the detector provider with choice made in panel
		final int index = component.getChoice();
		final String key = trackerProvider.getVisibleKeys().get( index );
		final SpotTrackerFactory trackerFactory = trackerProvider.getFactory( key );

		// Check
		if ( trackerFactory == null )
		{
			final Logger logger = trackmate.getModel().getLogger();
			logger.error( "Choice panel returned a tracker unkown to this trackmate: " + key + "\n" );
			return;
		}

		trackmate.getSettings().trackerFactory = trackerFactory;

		if ( trackerFactory.getKey().equals( ManualTrackerFactory.TRACKER_KEY ) )
		{
			/*
			 * Compute track and edge features now to ensure they will be
			 * available in the next descriptor.
			 */
			final Thread trackFeatureCalculationThread = new Thread( "TrackMate track feature calculation thread" )
			{
				@Override
				public void run()
				{
					trackmate.computeTrackFeatures( true );
					trackmate.computeEdgeFeatures( true );
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
	}

	@Override
	public void comingBackToPanel()
	{
		/*
		 * We clear the tracks here. We don't do it at the tracker configuration
		 * panel, because we want the user to be able to visually see the
		 * changes a parameter tuning cause.
		 */
		trackmate.getModel().clearTracks( true );
		controller.getSelectionModel().clearEdgeSelection();
	}

	private void setCurrentChoiceFromPlugin()
	{

		String key;
		if ( null != trackmate.getSettings().trackerFactory )
		{
			key = trackmate.getSettings().trackerFactory.getKey();
		}
		else
		{
			key = SimpleSparseLAPTrackerFactory.THIS2_TRACKER_KEY;
		}
		final int index = trackerProvider.getVisibleKeys().indexOf( key );

		if ( index < 0 )
		{
			trackmate.getModel().getLogger().error( "[TrackerChoiceDescriptor] Cannot find tracker named " + key + " in Trackmate." );
			return;
		}
		component.setChoice( index );
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

}
