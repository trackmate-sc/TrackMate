package fiji.plugin.trackmate.gui.featureselector;

import java.util.Map;
import java.util.TreeMap;

import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.Spot2DMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.Spot3DMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

public class AnalyzerSelection
{

	final Map< String, Boolean > spotAnalyzers = new TreeMap<>();

	final Map< String, Boolean > edgeAnalyzers = new TreeMap<>();

	final Map< String, Boolean > trackAnalyzers = new TreeMap<>();

	private AnalyzerSelection()
	{}

	public boolean isSpotAnalyzersSelected( final String key )
	{
		return spotAnalyzers.getOrDefault( key, false );
	}

	public boolean isEdgeAnalyzersSelected( final String key )
	{
		return spotAnalyzers.getOrDefault( key, false );
	}

	public boolean isTrackAnalyzersSelected( final String key )
	{
		return spotAnalyzers.getOrDefault( key, false );
	}

	/**
	 * Possibly adds the analyzers that are discovered at runtime, but not
	 * present in the analyzer selection, with the 'selected' flag.
	 */
	public void mergeWithDefault()
	{
		final AnalyzerSelection df = defaultSelection();

		for ( final String key : df.spotAnalyzers.keySet() )
			spotAnalyzers.putIfAbsent( key, true );

		for ( final String key : df.edgeAnalyzers.keySet() )
			edgeAnalyzers.putIfAbsent( key, true );

		for ( final String key : df.trackAnalyzers.keySet() )
			trackAnalyzers.putIfAbsent( key, true );
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder( super.toString() );
		str.append( "\nSpot analyzers:" );
		for ( final String key : spotAnalyzers.keySet() )
			str.append( String.format( "\n\t%25s \t-> %s", key, ( spotAnalyzers.get( key ).booleanValue() ? "selected" : "deselected" ) ) );
		str.append( "\nEdge analyzers:" );
		for ( final String key : edgeAnalyzers.keySet() )
			str.append( String.format( "\n\t%25s \t-> %s", key, ( edgeAnalyzers.get( key ).booleanValue() ? "selected" : "deselected" ) ) );
		str.append( "\nTrack analyzers:" );
		for ( final String key : trackAnalyzers.keySet() )
			str.append( String.format( "\n\t%25s \t-> %s", key, ( trackAnalyzers.get( key ).booleanValue() ? "selected" : "deselected" ) ) );

		return str.toString();
	}

	public static AnalyzerSelection defaultSelection()
	{
		final AnalyzerSelection fs = new AnalyzerSelection();

		for ( final String key : new SpotAnalyzerProvider( 1 ).getVisibleKeys() )
			fs.spotAnalyzers.put( key, true );

		for ( final String key : new Spot2DMorphologyAnalyzerProvider( 1 ).getKeys() )
			fs.spotAnalyzers.put( key, true );

		for ( final String key : new Spot3DMorphologyAnalyzerProvider( 1 ).getKeys() )
			fs.spotAnalyzers.put( key, true );

		for ( final String key : new EdgeAnalyzerProvider().getKeys() )
			fs.edgeAnalyzers.put( key, true );

		for ( final String key : new TrackAnalyzerProvider().getKeys() )
			fs.trackAnalyzers.put( key, true );

		// Fine tune.
		fs.spotAnalyzers.put( SpotContrastAndSNRAnalyzerFactory.KEY, false );

		return fs;
	}

	public void set( final AnalyzerSelection o )
	{
		spotAnalyzers.clear();
		spotAnalyzers.putAll( o.spotAnalyzers );
		edgeAnalyzers.clear();
		edgeAnalyzers.putAll( o.edgeAnalyzers );
		trackAnalyzers.clear();
		trackAnalyzers.putAll( o.trackAnalyzers );
		mergeWithDefault();
	}
}
