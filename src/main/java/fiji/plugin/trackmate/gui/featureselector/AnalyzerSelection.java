package fiji.plugin.trackmate.gui.featureselector;

import static fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject.EDGES;
import static fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject.SPOTS;
import static fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject.TRACKS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.Spot2DMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.Spot3DMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

public class AnalyzerSelection
{

	static final List< TrackMateObject > objs = Arrays.asList( new TrackMateObject[] { SPOTS, EDGES, TRACKS } );

	private final Map< TrackMateObject, Map< String, Boolean > > allAnalyzers = new LinkedHashMap<>();

	private AnalyzerSelection()
	{
		allAnalyzers.put( SPOTS, new TreeMap<>() );
		allAnalyzers.put( EDGES, new TreeMap<>() );
		allAnalyzers.put( TRACKS, new TreeMap<>() );
	}

	public boolean isSelected( final TrackMateObject obj, final String key )
	{
		final Map< String, Boolean > map = allAnalyzers.get( obj );
		if ( map == null )
			return false;
		return map.getOrDefault( key, false );
	}

	public void setSelected( final TrackMateObject obj, final String key, final boolean selected )
	{
		final Map< String, Boolean > map = allAnalyzers.get( obj );
		if ( map == null )
			return;

		map.put( key, selected );
	}

	public List< String > getKeys( final TrackMateObject obj )
	{
		final Map< String, Boolean > map = allAnalyzers.get( obj );
		if ( map == null )
			return Collections.emptyList();

		return new ArrayList<>( map.keySet() );
	}

	/**
	 * Possibly adds the analyzers that are discovered at runtime, but not
	 * present in the analyzer selection, with the 'selected' flag.
	 */
	public void mergeWithDefault()
	{
		final AnalyzerSelection df = defaultSelection();
		for ( final TrackMateObject obj : objs )
		{
			final Map< String, Boolean > source = df.allAnalyzers.get( obj );
			final Map< String, Boolean > target = allAnalyzers.get( obj );
			for ( final String key : source.keySet() )
				target.putIfAbsent( key, true );
		}
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder( super.toString() );
		for ( final TrackMateObject obj : objs )
		{
			str.append( "\n" + toName( obj ) + " analyzers:" );

			final Map< String, Boolean > map = allAnalyzers.get( obj );
			for ( final String key : map.keySet() )
				str.append( String.format( "\n\t%25s \t-> %s", key, ( map.get( key ).booleanValue() ? "selected" : "deselected" ) ) );
		}
		return str.toString();
	}

	public static AnalyzerSelection defaultSelection()
	{
		final AnalyzerSelection fs = new AnalyzerSelection();

		for ( final String key : new SpotAnalyzerProvider( 1 ).getVisibleKeys() )
			fs.setSelected( SPOTS, key, true );

		for ( final String key : new Spot2DMorphologyAnalyzerProvider( 1 ).getKeys() )
			fs.setSelected( SPOTS, key, true );

		for ( final String key : new Spot3DMorphologyAnalyzerProvider( 1 ).getKeys() )
			fs.setSelected( SPOTS, key, true );

		for ( final String key : new EdgeAnalyzerProvider().getKeys() )
			fs.setSelected( EDGES, key, true );

		for ( final String key : new TrackAnalyzerProvider().getKeys() )
			fs.setSelected( TRACKS, key, true );

		// Fine tune.
		fs.setSelected( SPOTS, SpotContrastAndSNRAnalyzerFactory.KEY, false );

		return fs;
	}

	public void set( final AnalyzerSelection o )
	{
		allAnalyzers.clear();
		for ( final TrackMateObject obj : objs )
			allAnalyzers.put( obj, new TreeMap<>( o.allAnalyzers.get( obj ) ) );

		mergeWithDefault();
	}

	public static final String toName(final TrackMateObject obj)
	{
		final String str = obj.toString();
		return StringUtils.capitalize( str ).substring( 0, str.length() - 1 );
	}
}
