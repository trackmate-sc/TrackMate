package fiji.plugin.trackmate.gui.descriptors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory;
import fiji.plugin.trackmate.gui.FeatureDisplaySelector;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.panels.components.FilterGuiPanel;
import fiji.plugin.trackmate.providers.SpotMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;

public class SpotFilterDescriptor implements WizardPanelDescriptor
{

	private final ArrayList< ActionListener > actionListeners = new ArrayList<>();

	private final ArrayList< ChangeListener > changeListeners = new ArrayList<>();

	private static final String KEY = "SpotFilter";

	private final FilterGuiPanel component;

	private final TrackMateGUIController controller;

	public SpotFilterDescriptor(
			final TrackMateGUIController controller,
			final List< FeatureFilter > filters,
			final FeatureDisplaySelector featureSelector )
	{
		this.controller = controller;
		this.component = new FilterGuiPanel(
				controller.getPlugin().getModel(),
				controller.getPlugin().getSettings(),
				TrackMateObject.SPOTS,
				filters,
				Spot.QUALITY,
				featureSelector );

		component.addActionListener( e -> fireAction( e ) );
		component.addChangeListener( e -> fireThresholdChanged( e ) );
	}

	@Override
	public FilterGuiPanel getComponent()
	{
		return component;
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final String oldText = controller.getGUI().getNextButton().getText();
		final Icon oldIcon = controller.getGUI().getNextButton().getIcon();
		controller.getGUI().getNextButton().setText( "Please wait..." );
		controller.getGUI().getNextButton().setIcon( null );

		new Thread( "TrackMate spot feature calculation thread." )
		{
			@Override
			public void run()
			{
				final EverythingDisablerAndReenabler enabler1 = new EverythingDisablerAndReenabler( component, new Class[] { JLabel.class } );
				final EverythingDisablerAndReenabler enabler2 = new EverythingDisablerAndReenabler( controller.getGUI(), new Class[] { JLabel.class } );
				try
				{
					enabler1.disable();
					enabler2.disable();
					controller.getGUI().setLogButtonEnabled( true );
					controller.getGUI().setPreviousButtonEnabled( false );

					final TrackMate trackmate = controller.getPlugin();
					final Model model = trackmate.getModel();
					final Logger logger = model.getLogger();
					final String str = "Initial thresholding with a quality threshold above "
							+ String.format( "%.1f", trackmate.getSettings().initialSpotFilterValue )
							+ " ...\n";
					logger.log( str, Logger.BLUE_COLOR );
					final int ntotal = model.getSpots().getNSpots( false );
					trackmate.execInitialSpotFiltering();
					final int nselected = model.getSpots().getNSpots( false );
					logger.log( String.format( "Retained %d spots out of %d.\n", nselected, ntotal ) );

					/*
					 * Should we add morphology feature analyzers?
					 */

					if ( trackmate.getSettings().detectorFactory != null
							&& trackmate.getSettings().detectorFactory.has2Dsegmentation()
							&& DetectionUtils.is2D( trackmate.getSettings().imp ) )
					{
						logger.log( "Adding morphology analyzers...\n", Logger.BLUE_COLOR );
						final Settings settings = trackmate.getSettings();
						final SpotMorphologyAnalyzerProvider spotMorphologyAnalyzerProvider = new SpotMorphologyAnalyzerProvider( settings.imp.getNChannels() );
						@SuppressWarnings( "rawtypes" )
						final List< SpotMorphologyAnalyzerFactory > factories = spotMorphologyAnalyzerProvider
								.getKeys()
								.stream()
								.map( key -> spotMorphologyAnalyzerProvider.getFactory(	key ) )
								.collect( Collectors.toList() );
						factories.forEach( settings::addSpotAnalyzerFactory );
						final StringBuilder strb = new StringBuilder();
						Settings.prettyPrintFeatureAnalyzer( factories, strb );
						logger.log( strb.toString() );
					}

					/*
					 * We have some spots so we need to compute spot features
					 * will we render them.
					 */
					logger.log( "Calculating spot features...\n", Logger.BLUE_COLOR );
					// Calculate features
					final long start = System.currentTimeMillis();
					trackmate.computeSpotFeatures( true );
					final long end = System.currentTimeMillis();
					logger.log( String.format( "Calculating features done in %.1f s.\n", ( end - start ) / 1e3f ), Logger.BLUE_COLOR );

					// Refresh component.
					component.refreshValues();
					fireThresholdChanged( null );
				}
				finally
				{
					controller.getGUI().getNextButton().setText( oldText );
					controller.getGUI().getNextButton().setIcon( oldIcon );
					enabler1.reenable();
					enabler2.reenable();
					controller.getGUI().setNextButtonEnabled( true );
				}
			}
		}.start();
	}

	@Override
	public void displayingPanel()
	{
		final TrackMate trackmate = controller.getPlugin();
		trackmate.getSettings().setSpotFilters( component.getFeatureFilters() );
		trackmate.execSpotFiltering( false );
	}

	@Override
	public void aboutToHidePanel()
	{
		final TrackMate trackmate = controller.getPlugin();
		final Logger logger = trackmate.getModel().getLogger();
		logger.log( "Performing spot filtering on the following features:\n", Logger.BLUE_COLOR );
		final Model model = trackmate.getModel();
		final List< FeatureFilter > featureFilters = component.getFeatureFilters();
		trackmate.getSettings().setSpotFilters( featureFilters );
		trackmate.execSpotFiltering( false );

		final int ntotal = model.getSpots().getNSpots( false );
		if ( featureFilters == null || featureFilters.isEmpty() )
		{
			logger.log( "No feature threshold set, kept the " + ntotal + " spots.\n" );
		}
		else
		{
			for ( final FeatureFilter ft : featureFilters )
			{
				String str = "  - on " + trackmate.getModel().getFeatureModel().getSpotFeatureNames().get( ft.feature );
				if ( ft.isAbove )
					str += " above ";
				else
					str += " below ";
				str += String.format( "%.1f", ft.value );
				str += '\n';
				logger.log( str );
			}
			final int nselected = model.getSpots().getNSpots( true );
			logger.log( "Kept " + nselected + " spots out of " + ntotal + ".\n" );
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

	/**
	 * Adds an {@link ActionListener} to this panel. These listeners will be
	 * notified when a button is pushed or when the feature to color is changed.
	 */
	public void addActionListener( final ActionListener listener )
	{
		actionListeners.add( listener );
	}

	/**
	 * Removes an ActionListener from this panel.
	 *
	 * @return true if the listener was in the ActionListener collection of this
	 *         instance.
	 */
	public boolean removeActionListener( final ActionListener listener )
	{
		return actionListeners.remove( listener );
	}

	public Collection< ActionListener > getActionListeners()
	{
		return actionListeners;
	}

	/**
	 * Forwards the given {@link ActionEvent} to all the {@link ActionListener}
	 * of this panel.
	 */
	private void fireAction( final ActionEvent e )
	{
		for ( final ActionListener l : actionListeners )
			l.actionPerformed( e );
	}

	/**
	 * Add an {@link ChangeListener} to this panel. The {@link ChangeListener}
	 * will be notified when a change happens to the thresholds displayed by
	 * this panel, whether due to the slider being move, the auto-threshold
	 * button being pressed, or the combo-box selection being changed.
	 */
	public void addChangeListener( final ChangeListener listener )
	{
		changeListeners.add( listener );
	}

	/**
	 * Remove a ChangeListener from this panel.
	 *
	 * @return true if the listener was in listener collection of this instance.
	 */
	public boolean removeChangeListener( final ChangeListener listener )
	{
		return changeListeners.remove( listener );
	}

	public Collection< ChangeListener > getChangeListeners()
	{
		return changeListeners;
	}

	private void fireThresholdChanged( final ChangeEvent e )
	{
		for ( final ChangeListener cl : changeListeners )
			cl.stateChanged( e );
	}
}
