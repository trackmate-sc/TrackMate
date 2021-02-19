package fiji.plugin.trackmate.gui.wizard.descriptors;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JLabel;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory;
import fiji.plugin.trackmate.gui.components.FeatureDisplaySelector;
import fiji.plugin.trackmate.gui.components.FilterGuiPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;
import fiji.plugin.trackmate.providers.SpotMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;

public class SpotFilterDescriptor extends WizardPanelDescriptor
{

	private static final String KEY = "SpotFilter";

	private final TrackMate trackmate;

	public SpotFilterDescriptor(
			final TrackMate trackmate,
			final List< FeatureFilter > filters,
			final FeatureDisplaySelector featureSelector )
	{
		super( KEY );
		this.trackmate = trackmate;
		final FilterGuiPanel component = new FilterGuiPanel(
				trackmate.getModel(),
				trackmate.getSettings(),
				TrackMateObject.SPOTS,
				filters,
				Spot.QUALITY,
				featureSelector );

		component.addChangeListener( e -> filterSpots() );
		this.targetPanel = component;
	}

	private void filterSpots()
	{
		final FilterGuiPanel component = ( FilterGuiPanel ) targetPanel;
		trackmate.getSettings().setSpotFilters( component.getFeatureFilters() );
		trackmate.execSpotFiltering( false );
	}

	@Override
	public Runnable getForwardRunnable()
	{
		return new Runnable()
		{
			@Override
			public void run()
			{
				final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( ( Container ) targetPanel, new Class[] { JLabel.class } );
				disabler.disable();
				try
				{

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
						logger.log( "\nAdding morphology analyzers...\n", Logger.BLUE_COLOR );
						final Settings settings = trackmate.getSettings();
						final SpotMorphologyAnalyzerProvider spotMorphologyAnalyzerProvider = new SpotMorphologyAnalyzerProvider( settings.imp.getNChannels() );
						@SuppressWarnings( "rawtypes" )
						final List< SpotMorphologyAnalyzerFactory > factories = spotMorphologyAnalyzerProvider
								.getKeys()
								.stream()
								.map( key -> spotMorphologyAnalyzerProvider.getFactory( key ) )
								.collect( Collectors.toList() );
						factories.forEach( settings::addSpotAnalyzerFactory );
						final StringBuilder strb = new StringBuilder();
						Settings.prettyPrintFeatureAnalyzer( factories, strb );
						logger.log( strb.toString() );
					}

					/*
					 * Hack to show a message in the filter GUI panel.
					 */

					final FilterGuiPanel panel = ( FilterGuiPanel ) targetPanel;
					final BorderLayout layout = ( BorderLayout ) panel.getLayout();
					final JLabel labelTop = ( JLabel ) layout.getLayoutComponent( BorderLayout.NORTH );
					final String originalText = labelTop.getText();
					labelTop.setText( "  Please wait while computing spot features..." );

					/*
					 * We have some spots so we need to compute spot features
					 * will we render them.
					 */
					logger.log( "\nCalculating spot features...\n", Logger.BLUE_COLOR );
					// Calculate features
					final long start = System.currentTimeMillis();
					trackmate.computeSpotFeatures( true );
					final long end = System.currentTimeMillis();
					logger.log( String.format( "Calculating features done in %.1f s.\n", ( end - start ) / 1e3f ) );
					labelTop.setText( originalText );

					// Refresh component.
					final FilterGuiPanel component = ( FilterGuiPanel ) targetPanel;
					component.refreshValues();
					filterSpots();
				}
				finally
				{
					disabler.reenable();
				}
			}
		};
	}

	@Override
	public void displayingPanel()
	{
		final FilterGuiPanel component = ( FilterGuiPanel ) targetPanel;
		trackmate.getSettings().setSpotFilters( component.getFeatureFilters() );
		trackmate.execSpotFiltering( false );
	}

	@Override
	public void aboutToHidePanel()
	{
		final Logger logger = trackmate.getModel().getLogger();
		logger.log( "\nPerforming spot filtering on the following features:\n", Logger.BLUE_COLOR );
		final Model model = trackmate.getModel();
		final FilterGuiPanel component = ( FilterGuiPanel ) targetPanel;
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
}
