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

public class RecomputeFeatureAction extends AbstractTMAction
{

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/calculator.png" ) );

	public static final String NAME = "Recompute all features";

	public static final String KEY = "RECOMPUTE_FEATURES";

	public static final String INFO_TEXT = "<html>" +
			"Calling this action causes the model to recompute all the features values " +
			"for all spots, edges and tracks. All the feature analyzers discovered when "
			+ "running this action are added and computed. " +
			"</html>";

	@Override
	public void execute( final TrackMate trackmate )
	{
		recompute( trackmate, logger );
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
			return new RecomputeFeatureAction();
		}
	}

	public static void recompute( final TrackMate trackmate, final Logger logger )
	{
		logger.log( "Recalculating all features.\n" );
		final Model model = trackmate.getModel();
		final Logger oldLogger = model.getLogger();
		model.setLogger( logger );

		final Settings settings = trackmate.getSettings();

		/*
		 * Configure settings object with spot, edge and track analyzers as
		 * specified in the providers.
		 */

		logger.log( "Registering spot analyzers:\n" );
		settings.clearSpotAnalyzerFactories();
		final SpotAnalyzerProvider spotAnalyzerProvider = new SpotAnalyzerProvider( settings.imp.getNChannels() );
		final List< String > spotAnalyzerKeys = spotAnalyzerProvider.getKeys();
		for ( final String key : spotAnalyzerKeys )
		{
			final SpotAnalyzerFactory< ? > spotFeatureAnalyzer = spotAnalyzerProvider.getFactory( key );
			settings.addSpotAnalyzerFactory( spotFeatureAnalyzer );
			logger.log( " - " + key + "\n" );
		}

		logger.log( "Registering edge analyzers:\n" );
		settings.clearEdgeAnalyzers();
		final EdgeAnalyzerProvider edgeAnalyzerProvider = new EdgeAnalyzerProvider();
		final List< String > edgeAnalyzerKeys = edgeAnalyzerProvider.getKeys();
		for ( final String key : edgeAnalyzerKeys )
		{
			final EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getFactory( key );
			settings.addEdgeAnalyzer( edgeAnalyzer );
			logger.log( " - " + key + "\n" );
		}

		logger.log( "Registering track analyzers:\n" );
		settings.clearTrackAnalyzers();
		final TrackAnalyzerProvider trackAnalyzerProvider = new TrackAnalyzerProvider();
		final List< String > trackAnalyzerKeys = trackAnalyzerProvider.getKeys();
		for ( final String key : trackAnalyzerKeys )
		{
			final TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getFactory( key );
			settings.addTrackAnalyzer( trackAnalyzer );
			logger.log( " - " + key + "\n" );
		}

		trackmate.computeSpotFeatures( true );
		trackmate.computeEdgeFeatures( true );
		trackmate.computeTrackFeatures( true );

		model.setLogger( oldLogger );
		logger.log( "Done.\n" );
	}
}
