package fiji.plugin.trackmate.gui.wizard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.FeatureDisplaySelector;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.descriptors.ChooseDetectorDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.ExecuteDetectionDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.InitFilterDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.LogPanelDescriptor2;
import fiji.plugin.trackmate.gui.wizard.descriptors.SpotDetectorDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.SpotFilterDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.StartDialogDescriptor;
import fiji.plugin.trackmate.providers.DetectorProvider;

public class TrackMateWizardSequence implements WizardSequence
{

	private final TrackMate trackmate;

	private final SelectionModel selectionModel;

	private final DisplaySettings displaySettings;

	private final StartDialogDescriptor startDialogDescriptor;

	private WizardPanelDescriptor2 current;

	private final Map< WizardPanelDescriptor2, WizardPanelDescriptor2 > next;

	private final Map< WizardPanelDescriptor2, WizardPanelDescriptor2 > previous;

	private final LogPanelDescriptor2 logDescriptor;

	private final ChooseDetectorDescriptor chooseDetectorDescriptor;

	private final ExecuteDetectionDescriptor executeDetectionDescriptor;

	private final InitFilterDescriptor initFilterDescriptor;

	private final SpotFilterDescriptor spotFilterDescriptor;

	public TrackMateWizardSequence( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		this.trackmate = trackmate;
		this.selectionModel = selectionModel;
		this.displaySettings = displaySettings;
		final Settings settings = trackmate.getSettings();
		final Model model = trackmate.getModel();

		final LogPanel logPanel = new LogPanel();
		final Logger logger = logPanel.getLogger();
		model.setLogger( logger );

		final FeatureDisplaySelector featureSelector = new FeatureDisplaySelector( model, settings, displaySettings );
		final FeatureFilter initialFilter = new FeatureFilter( Spot.QUALITY, settings.initialSpotFilterValue.doubleValue(), true );
		final List< FeatureFilter > spotFilters = settings.getSpotFilters();

		logDescriptor = new LogPanelDescriptor2( logPanel );
		startDialogDescriptor = new StartDialogDescriptor( settings, logger );
		chooseDetectorDescriptor = new ChooseDetectorDescriptor( new DetectorProvider(), trackmate );
		executeDetectionDescriptor = new ExecuteDetectionDescriptor( trackmate, logPanel );
		initFilterDescriptor = new InitFilterDescriptor( trackmate, initialFilter );
		spotFilterDescriptor = new SpotFilterDescriptor( trackmate, spotFilters, featureSelector );

		this.next = getForwardSequence();
		this.previous = getBackwardSequence();
	}

	@Override
	public WizardPanelDescriptor2 init()
	{
		current = startDialogDescriptor;
		return current;
	}

	@Override
	public WizardPanelDescriptor2 next()
	{
		if ( current == chooseDetectorDescriptor )
		{
			final SpotDetectorDescriptor configDescriptor = getConfigDescriptor();
			next.put( chooseDetectorDescriptor, configDescriptor );
			next.put( configDescriptor, executeDetectionDescriptor );
			previous.put( configDescriptor, chooseDetectorDescriptor );
			previous.put( executeDetectionDescriptor, configDescriptor );
			previous.put( initFilterDescriptor, configDescriptor );
			previous.put( spotFilterDescriptor, configDescriptor );
		}
		current = next.get( current );
		return current;
	}

	@Override
	public boolean hasNext()
	{
		return true; // current != chooseDetectorDescriptor;
	}

	@Override
	public WizardPanelDescriptor2 current()
	{
		return current;
	}

	@Override
	public WizardPanelDescriptor2 previous()
	{
		current = previous.get( current );
		return current;
	}

	@Override
	public WizardPanelDescriptor2 logDescriptor()
	{
		return logDescriptor;
	}

	@Override
	public WizardPanelDescriptor2 configDescriptor()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasPrevious()
	{
		return current != startDialogDescriptor;
	}

	private Map< WizardPanelDescriptor2, WizardPanelDescriptor2 > getBackwardSequence()
	{
		final Map< WizardPanelDescriptor2, WizardPanelDescriptor2 > map = new HashMap<>();
		map.put( startDialogDescriptor, null );
		map.put( chooseDetectorDescriptor, startDialogDescriptor );
		return map;
	}

	private Map< WizardPanelDescriptor2, WizardPanelDescriptor2 > getForwardSequence()
	{
		final Map< WizardPanelDescriptor2, WizardPanelDescriptor2 > map = new HashMap<>();
		map.put( startDialogDescriptor, chooseDetectorDescriptor );
		map.put( executeDetectionDescriptor, initFilterDescriptor );
		map.put( initFilterDescriptor, spotFilterDescriptor );
		return map;
	}

	/**
	 * Determines and registers the descriptor used to configure the detector
	 * chosen in the {@link ChooseDetectorDescriptor}.
	 *
	 * @return a suitable {@link SpotDetectorDescriptor}.
	 */
	private SpotDetectorDescriptor getConfigDescriptor()
	{
		final SpotDetectorFactoryBase< ? > detectorFactory = trackmate.getSettings().detectorFactory;

		/*
		 * Copy as much settings as we can to the potentially new config
		 * descriptor.
		 */
		// From settings.
		final Map< String, Object > oldSettings1 = new HashMap<>( trackmate.getSettings().detectorSettings );
		// From previous panel.
		final Map< String, Object > oldSettings2 = new HashMap<>();
		final SpotDetectorDescriptor previousSpotDetectorDescriptor = ( SpotDetectorDescriptor ) next.get( chooseDetectorDescriptor );
		if ( previousSpotDetectorDescriptor != null )
		{
			final ConfigurationPanel detectorConfigPanel = ( ConfigurationPanel ) previousSpotDetectorDescriptor.targetPanel;
			oldSettings2.putAll( detectorConfigPanel.getSettings() );
		}

		final Map< String, Object > defaultSettings = detectorFactory.getDefaultSettings();
		for ( final String skey : defaultSettings.keySet() )
		{
			Object previousValue = oldSettings2.get( skey );
			if ( previousValue == null )
				previousValue = oldSettings1.get( skey );

			defaultSettings.put( skey, previousValue );
		}

		final ConfigurationPanel detectorConfigurationPanel = detectorFactory.getDetectorConfigurationPanel( trackmate.getSettings(), trackmate.getModel() );
		detectorConfigurationPanel.setSettings( defaultSettings );
		trackmate.getSettings().detectorSettings = defaultSettings;
		return new SpotDetectorDescriptor( detectorConfigurationPanel );
	}
}
