package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;

public class DetectorConfigurationDescriptor implements WizardPanelDescriptor
{

	private static final String KEY = "ConfigureDetector";

	private final TrackMate trackmate;

	private ConfigurationPanel configPanel;

	private final TrackMateGUIController controller;

	public DetectorConfigurationDescriptor( final TrackMate trackmate, final TrackMateGUIController controller )
	{
		this.trackmate = trackmate;
		this.controller = controller;
	}

	/*
	 * METHODS
	 */

	@Override
	public Component getComponent()
	{
		return configPanel;
	}

	/**
	 * Regenerates the config panel to reflect current settings stored in the
	 * trackmate.
	 */
	private void updateComponent()
	{
		final SpotDetectorFactory< ? > factory = trackmate.getSettings().detectorFactory;
		// Regenerate panel
		configPanel = factory.getDetectorConfigurationPanel( trackmate.getSettings(), trackmate.getModel() );
		// We assume the provider is already configured with the right target
		// detector factory
		Map< String, Object > settings = trackmate.getSettings().detectorSettings;
		// Bulletproof null
		if ( null == settings || !factory.checkSettings( settings ) )
		{
			settings = factory.getDefaultSettings();
			trackmate.getSettings().detectorSettings = settings;
		}
		configPanel.setSettings( settings );
	}

	@Override
	public void aboutToDisplayPanel()
	{
		updateComponent();
	}

	@Override
	public void displayingPanel()
	{
		if ( null == configPanel )
		{
			// May happen if we move backward here after loading
			updateComponent();
		}
		controller.getGUI().setNextButtonEnabled( true );
	}

	@Override
	public void aboutToHidePanel()
	{
		configPanel.clean();
		final SpotDetectorFactory< ? > factory = trackmate.getSettings().detectorFactory;
		Map< String, Object > settings = configPanel.getSettings();
		final boolean settingsOk = factory.checkSettings( settings );
		if ( !settingsOk )
		{
			final Logger logger = trackmate.getModel().getLogger();
			logger.error( "Config panel returned bad settings map:\n" + factory.getErrorMessage() + "Using defaults settings.\n" );
			settings = factory.getDefaultSettings();
		}
		trackmate.getSettings().detectorSettings = settings;
	}

	@Override
	public void comingBackToPanel()
	{
		/*
		 * We clear the spot content here.
		 */
		trackmate.getModel().clearSpots( true );
		controller.getSelectionModel().clearSpotSelection();
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

}
