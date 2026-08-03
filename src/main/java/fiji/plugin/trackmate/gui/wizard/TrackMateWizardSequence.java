/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.gui.wizard;

import java.awt.Window;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.FeatureDisplaySelector;
import fiji.plugin.trackmate.gui.components.LogPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.descriptors.ActionChooserDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.ChooseDetectorDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.ChooseTrackerDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.ExecuteDetectionDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.ExecuteTrackingDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.GrapherDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.InitFilterDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.LogPanelDescriptor2;
import fiji.plugin.trackmate.gui.wizard.descriptors.SaveDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.SpotDetectorDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.SpotFilterDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.SpotTrackerDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.StartDialogDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.TrackFilterDescriptor;
import fiji.plugin.trackmate.providers.ActionProvider;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.SpotImageTrackerFactory;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import fiji.plugin.trackmate.tracking.manual.ManualTrackerFactory;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelJFrameView;

public class TrackMateWizardSequence extends AbstractTrackMateModelJFrameView implements WizardSequence
{

	private WizardPanelDescriptor current;

	private final StartDialogDescriptor startDialogDescriptor;

	private final Map< WizardPanelDescriptor, WizardPanelDescriptor > next;

	private final Map< WizardPanelDescriptor, WizardPanelDescriptor > previous;

	private final LogPanelDescriptor2 logDescriptor;

	private final ChooseDetectorDescriptor chooseDetectorDescriptor;

	private final ExecuteDetectionDescriptor executeDetectionDescriptor;

	private final InitFilterDescriptor initFilterDescriptor;

	private final SpotFilterDescriptor spotFilterDescriptor;

	private final ChooseTrackerDescriptor chooseTrackerDescriptor;

	private final ExecuteTrackingDescriptor executeTrackingDescriptor;

	private final TrackFilterDescriptor trackFilterDescriptor;

	private final ConfigureViewsDescriptor configureViewsDescriptor;

	private final GrapherDescriptor grapherDescriptor;

	private final ActionChooserDescriptor actionChooserDescriptor;

	private final SaveDescriptor saveDescriptor;

	private JFrame frame;

	public TrackMateWizardSequence( final GuiModel guiModel )
	{
		super( guiModel );
		final Settings settings = guiModel.getSettings();
		final Model model = guiModel.getModel();
		final TrackMate trackmate = guiModel.getTrackMate();
		final DisplaySettings displaySettings = guiModel.getDisplaySettings();

		// Listen to changes in the model and update features accordingly.
		final ModelFeatureUpdater modelFeatureUpdater = new ModelFeatureUpdater( model, settings );
		modelFeatureUpdater.setNumThreads( trackmate.getNumThreads() );

		final LogPanel logPanel = new LogPanel();
		final Logger logger = logPanel.getLogger();
		model.setLogger( logger );

		final FeatureDisplaySelector featureSelector = new FeatureDisplaySelector( model, settings, displaySettings );
		final FeatureFilter initialFilter = new FeatureFilter( Spot.QUALITY, settings.initialSpotFilterValue.doubleValue(), true );
		final List< FeatureFilter > spotFilters = settings.getSpotFilters();
		final List< FeatureFilter > trackFilters = settings.getTrackFilters();

		logDescriptor = new LogPanelDescriptor2( logPanel );
		startDialogDescriptor = new StartDialogDescriptor( settings, logger );
		chooseDetectorDescriptor = new ChooseDetectorDescriptor( new DetectorProvider(), guiModel );
		executeDetectionDescriptor = new ExecuteDetectionDescriptor( guiModel, logPanel );
		initFilterDescriptor = new InitFilterDescriptor( guiModel, initialFilter );
		spotFilterDescriptor = new SpotFilterDescriptor( guiModel, spotFilters, featureSelector );
		chooseTrackerDescriptor = new ChooseTrackerDescriptor( new TrackerProvider(), guiModel );
		executeTrackingDescriptor = new ExecuteTrackingDescriptor( guiModel, logPanel );
		trackFilterDescriptor = new TrackFilterDescriptor( guiModel, trackFilters, featureSelector );
		configureViewsDescriptor = new ConfigureViewsDescriptor( guiModel, featureSelector );
		grapherDescriptor = new GrapherDescriptor( guiModel );
		actionChooserDescriptor = new ActionChooserDescriptor( new ActionProvider(), guiModel );
		saveDescriptor = new SaveDescriptor( guiModel, this );

		this.next = getForwardSequence();
		this.previous = getBackwardSequence();
		current = startDialogDescriptor;
	}

	@Override
	public WizardPanelDescriptor next()
	{
		if ( current == chooseDetectorDescriptor )
			getDetectorConfigDescriptor();

		if ( current == chooseTrackerDescriptor )
			getTrackerConfigDescriptor();

		current = next.get( current );
		return current;
	}

	@Override
	public WizardPanelDescriptor previous()
	{
		if ( current == trackFilterDescriptor )
			getTrackerConfigDescriptor();

		if ( current == spotFilterDescriptor )
			getDetectorConfigDescriptor();

		current = previous.get( current );
		return current;
	}

	@Override
	public boolean hasNext()
	{
		return current != actionChooserDescriptor;
	}

	@Override
	public WizardPanelDescriptor current()
	{
		return current;
	}

	@Override
	public WizardPanelDescriptor logDescriptor()
	{
		return logDescriptor;
	}

	@Override
	public WizardPanelDescriptor configDescriptor()
	{
		return configureViewsDescriptor;
	}

	@Override
	public WizardPanelDescriptor save()
	{
		return saveDescriptor;
	}

	@Override
	public boolean hasPrevious()
	{
		return current != startDialogDescriptor;
	}

	private Map< WizardPanelDescriptor, WizardPanelDescriptor > getBackwardSequence()
	{
		final Map< WizardPanelDescriptor, WizardPanelDescriptor > map = new HashMap<>();
		map.put( startDialogDescriptor, null );
		map.put( chooseDetectorDescriptor, startDialogDescriptor );
		map.put( chooseTrackerDescriptor, spotFilterDescriptor );
		map.put( configureViewsDescriptor, trackFilterDescriptor );
		map.put( grapherDescriptor, configureViewsDescriptor );
		map.put( actionChooserDescriptor, grapherDescriptor );
		return map;
	}

	private Map< WizardPanelDescriptor, WizardPanelDescriptor > getForwardSequence()
	{
		final Map< WizardPanelDescriptor, WizardPanelDescriptor > map = new HashMap<>();
		map.put( startDialogDescriptor, chooseDetectorDescriptor );
		map.put( executeDetectionDescriptor, initFilterDescriptor );
		map.put( initFilterDescriptor, spotFilterDescriptor );
		map.put( spotFilterDescriptor, chooseTrackerDescriptor );
		map.put( executeTrackingDescriptor, trackFilterDescriptor );
		map.put( trackFilterDescriptor, configureViewsDescriptor );
		map.put( configureViewsDescriptor, grapherDescriptor );
		map.put( grapherDescriptor, actionChooserDescriptor );
		return map;
	}

	@Override
	public void setCurrent( final String panelIdentifier )
	{
		if ( panelIdentifier.equals( SpotDetectorDescriptor.KEY ) )
		{
			current = getDetectorConfigDescriptor();
			return;
		}

		if ( panelIdentifier.equals( SpotTrackerDescriptor.KEY ) )
		{
			current = getTrackerConfigDescriptor();
			return;
		}

		if ( panelIdentifier.equals( InitFilterDescriptor.KEY ) )
		{
			getDetectorConfigDescriptor();
			current = initFilterDescriptor;
			return;
		}

		final List< WizardPanelDescriptor > descriptors = Arrays.asList( new WizardPanelDescriptor[] {
				logDescriptor,
				chooseDetectorDescriptor,
				executeDetectionDescriptor,
				initFilterDescriptor,
				spotFilterDescriptor,
				chooseTrackerDescriptor,
				executeTrackingDescriptor,
				trackFilterDescriptor,
				configureViewsDescriptor,
				grapherDescriptor,
				actionChooserDescriptor,
				saveDescriptor
		} );
		for ( final WizardPanelDescriptor w : descriptors )
		{
			if ( w.getPanelDescriptorIdentifier().equals( panelIdentifier ) )
			{
				current = w;
				break;
			}
		}
	}

	/**
	 * Determines and registers the descriptor used to configure the detector
	 * chosen in the {@link ChooseDetectorDescriptor}.
	 *
	 * @return a suitable {@link SpotDetectorDescriptor}.
	 */
	private SpotDetectorDescriptor getDetectorConfigDescriptor()
	{
		final Model model = guiModel.getModel();
		final Settings settings = guiModel.getSettings();

		final SpotDetectorFactoryBase< ? > detectorFactory = settings.detectorFactory;
		/*
		 * Special case: are we dealing with the manual detector? If yes, no
		 * config, no detection.
		 */
		if ( detectorFactory.getKey().equals( ManualDetectorFactory.DETECTOR_KEY ) )
		{
			// Position sequence next and previous.
			next.put( chooseDetectorDescriptor, spotFilterDescriptor );
			previous.put( spotFilterDescriptor, chooseDetectorDescriptor );
			previous.put( executeDetectionDescriptor, chooseDetectorDescriptor );
			previous.put( initFilterDescriptor, chooseDetectorDescriptor );
			return null;
		}

		/*
		 * Copy as much settings as we can to the potentially new config
		 * descriptor.
		 */
		// From settings.
		final Map< String, Object > oldSettings1 = new HashMap<>( settings.detectorSettings );
		// From previous panel.
		final Map< String, Object > oldSettings2 = new HashMap<>();
		final WizardPanelDescriptor previousDescriptor = next.get( chooseDetectorDescriptor );
		if ( previousDescriptor != null && previousDescriptor instanceof SpotDetectorDescriptor )
		{
			final SpotDetectorDescriptor previousSpotDetectorDescriptor = ( SpotDetectorDescriptor ) previousDescriptor;
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

		final ConfigurationPanel detectorConfigurationPanel = detectorFactory.getDetectorConfigurationPanel( settings, model );
		detectorConfigurationPanel.setSettings( defaultSettings );
		settings.detectorSettings = defaultSettings;
		final SpotDetectorDescriptor configDescriptor = new SpotDetectorDescriptor( settings, detectorConfigurationPanel, model.getLogger() );

		// Position sequence next and previous.
		next.put( chooseDetectorDescriptor, configDescriptor );
		next.put( configDescriptor, executeDetectionDescriptor );
		previous.put( configDescriptor, chooseDetectorDescriptor );
		previous.put( executeDetectionDescriptor, configDescriptor );
		previous.put( initFilterDescriptor, configDescriptor );
		previous.put( spotFilterDescriptor, configDescriptor );

		return configDescriptor;
	}

	/**
	 * Determines and registers the descriptor used to configure the tracker
	 * chosen in the {@link ChooseTrackerDescriptor}.
	 *
	 * @return a suitable {@link SpotTrackerDescriptor}.
	 */
	private SpotTrackerDescriptor getTrackerConfigDescriptor()
	{
		final Model model = guiModel.getModel();
		final Settings settings = guiModel.getSettings();
		final SpotTrackerFactory trackerFactory = settings.trackerFactory;

		/*
		 * Special case: are we dealing with the manual tracker? If yes, no
		 * config, no detection.
		 */
		if ( trackerFactory == null || trackerFactory.getKey().equals( ManualTrackerFactory.TRACKER_KEY ) )
		{
			// Position sequence next and previous.
			next.put( chooseTrackerDescriptor, trackFilterDescriptor );
			previous.put( executeTrackingDescriptor, chooseTrackerDescriptor );
			previous.put( trackFilterDescriptor, chooseTrackerDescriptor );
			return null;
		}
		/*
		 * Copy as much settings as we can to the potentially new config
		 * descriptor.
		 */
		// From settings.
		final Map< String, Object > oldSettings1 = new HashMap<>( settings.trackerSettings );
		// From previous panel.
		final Map< String, Object > oldSettings2 = new HashMap<>();
		final WizardPanelDescriptor previousDescriptor = next.get( chooseTrackerDescriptor );
		if ( previousDescriptor != null && previousDescriptor instanceof SpotTrackerDescriptor )
		{
			final SpotTrackerDescriptor previousTrackerDetectorDescriptor = ( SpotTrackerDescriptor ) previousDescriptor;
			final ConfigurationPanel detectorConfigPanel = ( ConfigurationPanel ) previousTrackerDetectorDescriptor.targetPanel;
			oldSettings2.putAll( detectorConfigPanel.getSettings() );
		}

		final Map< String, Object > defaultSettings = trackerFactory.getDefaultSettings();
		for ( final String skey : defaultSettings.keySet() )
		{
			Object previousValue = oldSettings2.get( skey );
			if ( previousValue == null )
				previousValue = oldSettings1.get( skey );

			defaultSettings.put( skey, previousValue );
		}

		final ConfigurationPanel trackerConfigurationPanel;
		if (trackerFactory instanceof SpotImageTrackerFactory)
		{
			trackerConfigurationPanel = ( ( SpotImageTrackerFactory ) trackerFactory ).getTrackerConfigurationPanel( model, settings.imp );
		}
		else
		{
			trackerConfigurationPanel = trackerFactory.getTrackerConfigurationPanel( model );
		}
		trackerConfigurationPanel.setSettings( defaultSettings );
		settings.trackerSettings = defaultSettings;
		final SpotTrackerDescriptor configDescriptor = new SpotTrackerDescriptor( settings, trackerConfigurationPanel, model.getLogger() );

		// Position sequence next and previous.
		next.put( chooseTrackerDescriptor, configDescriptor );
		next.put( configDescriptor, executeTrackingDescriptor );
		previous.put( configDescriptor, chooseTrackerDescriptor );
		previous.put( executeTrackingDescriptor, configDescriptor );
		previous.put( trackFilterDescriptor, configDescriptor );

		return configDescriptor;
	}

	@Override
	public JFrame run( final String title )
	{
		this.frame = WizardSequence.super.run( title );
		setWindow( frame );
		onClose( () -> {
			guiModel.getModel().setLogger( Logger.VOID_LOGGER );
			guiModel.getWindowManager().closeAll();
		} );
		return frame;
	}

	@Override
	public void render()
	{}

	@Override
	public void refresh()
	{}

	@Override
	public void clear()
	{}

	@Override
	public void centerViewOn( final Spot spot )
	{}

	@Override
	public String getKey()
	{
		return "TRACKMATE_WIZARD";
	}

	@Override
	public Window getWindow()
	{
		return frame;
	}

	@Override
	public void modelChanged( final ModelChangeEvent event )
	{}
}
