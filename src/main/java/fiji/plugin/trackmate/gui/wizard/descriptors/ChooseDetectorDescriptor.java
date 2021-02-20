package fiji.plugin.trackmate.gui.wizard.descriptors;

import java.util.Map;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.components.ModuleChooserPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;
import fiji.plugin.trackmate.providers.DetectorProvider;

public class ChooseDetectorDescriptor extends WizardPanelDescriptor
{

	private static final String KEY = "ChooseDetector";

	private final TrackMate trackmate;

	private final DetectorProvider detectorProvider;

	public ChooseDetectorDescriptor( final DetectorProvider detectorProvider, final TrackMate trackmate )
	{
		super( KEY );
		this.trackmate = trackmate;
		this.detectorProvider = detectorProvider;

		String selectedDetector = LogDetectorFactory.DETECTOR_KEY; // default
		if ( null != trackmate.getSettings().detectorFactory )
			selectedDetector = trackmate.getSettings().detectorFactory.getKey();

		this.targetPanel = new ModuleChooserPanel<>( detectorProvider, "detector", selectedDetector );
	}

	private void setCurrentChoiceFromPlugin()
	{
		String key = LogDetectorFactory.DETECTOR_KEY; // back to default
		if ( null != trackmate.getSettings().detectorFactory )
			key = trackmate.getSettings().detectorFactory.getKey();

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ModuleChooserPanel< SpotDetectorFactoryBase > component = (fiji.plugin.trackmate.gui.components.ModuleChooserPanel< SpotDetectorFactoryBase > ) targetPanel;
		component.setSelectedModuleKey( key );
	}

	@Override
	public void displayingPanel()
	{
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void aboutToHidePanel()
	{
		// Configure the detector provider with choice made in panel
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		final ModuleChooserPanel< SpotDetectorFactoryBase > component = (fiji.plugin.trackmate.gui.components.ModuleChooserPanel< SpotDetectorFactoryBase > ) targetPanel;
		final String detectorKey = component.getSelectedModuleKey();

		// Configure trackmate settings with selected detector
		final SpotDetectorFactoryBase< ? > factory = detectorProvider.getFactory( detectorKey );

		if ( null == factory )
		{
			trackmate.getModel().getLogger().error( "[ChooseDetectorDescriptor] Cannot find detector named "
					+ detectorKey
					+ " in current TrackMate modules." );
			return;
		}
		trackmate.getSettings().detectorFactory = factory;

		/*
		 * Compare current settings with default ones, and substitute default
		 * ones only if the old ones are absent or not compatible with it.
		 */
		final Map< String, Object > currentSettings = trackmate.getSettings().detectorSettings;
		if ( !factory.checkSettings( currentSettings ) )
		{
			final Map< String, Object > defaultSettings = factory.getDefaultSettings();
			trackmate.getSettings().detectorSettings = defaultSettings;
		}
	}
}
