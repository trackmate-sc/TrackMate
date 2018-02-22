package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.ListChooserPanel;
import fiji.plugin.trackmate.providers.DetectorProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DetectorChoiceDescriptor implements WizardPanelDescriptor
{

	private static final String KEY = "ChooseDetector";

	private final ListChooserPanel component;

	private final TrackMate trackmate;

	private final DetectorProvider detectorProvider;

	private final TrackMateGUIController controller;

	public DetectorChoiceDescriptor( final DetectorProvider detectorProvider, final TrackMate trackmate, final TrackMateGUIController controller )
	{
		this.trackmate = trackmate;
		this.detectorProvider = detectorProvider;
		this.controller = controller;
		final List< String > visibleKeys = detectorProvider.getVisibleKeys();
		final List< String > detectorNames = new ArrayList< >( visibleKeys.size() );
		final List< String > infoTexts = new ArrayList< >( visibleKeys.size() );
		for ( final String key : visibleKeys )
		{
			detectorNames.add( detectorProvider.getFactory( key ).getName() );
			infoTexts.add( detectorProvider.getFactory( key ).getInfoText() );
		}
		this.component = new ListChooserPanel( detectorNames, infoTexts, "detector" );
		setCurrentChoiceFromPlugin();
	}

	/*
	 * METHODS
	 */

	@Override
	public ListChooserPanel getComponent()
	{
		return component;
	}

	@Override
	public void aboutToDisplayPanel()
	{
		setCurrentChoiceFromPlugin();
	}

	private void setCurrentChoiceFromPlugin()
	{
		String key;
		if ( null != trackmate.getSettings().detectorFactory )
		{
			key = trackmate.getSettings().detectorFactory.getKey();
		}
		else
		{
			key = LogDetectorFactory.DETECTOR_KEY; // back to default
		}
		final int index = detectorProvider.getKeys().indexOf( key );
		if ( index < 0 )
		{
			trackmate.getModel().getLogger().error( "[DetectorChoiceDescriptor] Cannot find detector named " + key + " in current trackmate." );
			return;
		}
		component.setChoice( index );
	}

	@Override
	public void displayingPanel()
	{
		setCurrentChoiceFromPlugin();
		controller.getGUI().setNextButtonEnabled( true );
	}

	@Override
	public void aboutToHidePanel()
	{

		// Configure the detector provider with choice made in panel
		final int index = component.getChoice();
		final String key = detectorProvider.getVisibleKeys().get( index );

		// Configure trackmate settings with selected detector
		final SpotDetectorFactory< ? > factory = detectorProvider.getFactory( key );

		if ( null == factory )
		{
			trackmate.getModel().getLogger().error( "[DetectorChoiceDescriptor] Cannot find detector named " + key + " in current trackmate." );
			return;
		}

		trackmate.getSettings().detectorFactory = factory;

		// Compare current settings with default ones, and substitute default
		// ones only if the old ones are absent or not compatible with it.
		final Map< String, Object > currentSettings = trackmate.getSettings().detectorSettings;
		if ( !factory.checkSettings( currentSettings ) )
		{
			final Map< String, Object > defaultSettings = factory.getDefaultSettings();
			trackmate.getSettings().detectorSettings = defaultSettings;
		}

		if ( factory.getKey().equals( ManualDetectorFactory.DETECTOR_KEY ) )
		{
			/*
			 * Compute spot features now to ensure they will be available in the
			 * next descriptor.
			 */
			final Thread spotFeatureCalculationThread = new Thread( "TrackMate spot feature calculation thread" )
			{
				@Override
				public void run()
				{
					trackmate.computeSpotFeatures( true );
				}
			};
			spotFeatureCalculationThread.start();
			try
			{
				spotFeatureCalculationThread.join();
			}
			catch ( final InterruptedException e )
			{
				e.printStackTrace();
			}
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
