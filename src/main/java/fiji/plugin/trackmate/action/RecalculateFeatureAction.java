package fiji.plugin.trackmate.action;

import java.util.List;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

public class RecalculateFeatureAction extends AbstractTMAction
{

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/calculator.png" ) );

	public static final String NAME = "Recompute all features";

	public static final String KEY = "RECOMPUTE_FEATURES";

	public static final String INFO_TEXT = "<html>" +
			"Calling this action causes the model to recompute all the features <br>" +
			"for all spots, edges and tracks. The feature analyzers currently declared "
			+ "in TrackMate session are also calculated if not present in the data. " +
			"</html>";

	private final TrackMateGUIController controller;

	public RecalculateFeatureAction( final TrackMateGUIController controller )
	{
		this.controller = controller;
	}

	public RecalculateFeatureAction()
	{
		this( null );
	}

	@Override
	public void execute( final TrackMate trackmate )
	{
		logger.log( "Recalculating all features.\n" );
		final Model model = trackmate.getModel();
		final Logger oldLogger = model.getLogger();
		model.setLogger( logger );

		if ( null != controller )
		{
			final Settings settings = trackmate.getSettings();

			/*
			 * Configure settings object with spot, edge and track analyzers as
			 * specified in the providers.
			 */

			logger.log( "Registering spot analyzers:\n" );
			settings.clearSpotAnalyzerFactories();
			final SpotAnalyzerProvider spotAnalyzerProvider = controller.getSpotAnalyzerProvider();
			final List< String > spotAnalyzerKeys = spotAnalyzerProvider.getKeys();
			for ( final String key : spotAnalyzerKeys )
			{
				final SpotAnalyzerFactory< ? > spotFeatureAnalyzer = spotAnalyzerProvider.getFactory( key );
				settings.addSpotAnalyzerFactory( spotFeatureAnalyzer );
				logger.log( " - " + key + "\n" );
			}

			logger.log( "Registering edge analyzers:\n" );
			settings.clearEdgeAnalyzers();
			final EdgeAnalyzerProvider edgeAnalyzerProvider = controller.getEdgeAnalyzerProvider();
			final List< String > edgeAnalyzerKeys = edgeAnalyzerProvider.getKeys();
			for ( final String key : edgeAnalyzerKeys )
			{
				final EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getFactory( key );
				settings.addEdgeAnalyzer( edgeAnalyzer );
				logger.log( " - " + key + "\n" );
			}

			logger.log( "Registering track analyzers:\n" );
			settings.clearTrackAnalyzers();
			final TrackAnalyzerProvider trackAnalyzerProvider = controller.getTrackAnalyzerProvider();
			final List< String > trackAnalyzerKeys = trackAnalyzerProvider.getKeys();
			for ( final String key : trackAnalyzerKeys )
			{
				final TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getFactory( key );
				settings.addTrackAnalyzer( trackAnalyzer );
				logger.log( " - " + key + "\n" );
			}
		}

		trackmate.computeSpotFeatures( true );
		trackmate.computeEdgeFeatures( true );
		trackmate.computeTrackFeatures( true );

		model.setLogger( oldLogger );
		logger.log( "Done.\n" );
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}

		@Override
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new RecalculateFeatureAction( controller );
		}
	}
}
