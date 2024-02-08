/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.Spot2DMorphologyAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.Spot3DMorphologyAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
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

	public List< String > getSelectedAnalyzers( final TrackMateObject obj )
	{
		final Map< String, Boolean > map = allAnalyzers.get( obj );
		if ( map == null )
			return Collections.emptyList();

		return map.entrySet()
				.stream()
				.filter( e -> e.getValue() )
				.map( e -> e.getKey() )
				.collect( Collectors.toList() );
	}

	/**
	 * Configure the specified settings object so that it includes only all the
	 * analyzers in this selection.
	 * 
	 * @param settings
	 *            the settings to configure.
	 */
	public void configure( final Settings settings )
	{
		settings.clearSpotAnalyzerFactories();
		settings.clearEdgeAnalyzers();
		settings.clearTrackAnalyzers();

		final List< String > selectionSpotAnalyzers = getSelectedAnalyzers( SPOTS );

		// Base spot analyzers, in priority order.
		final SpotAnalyzerProvider spotAnalyzerProvider = new SpotAnalyzerProvider( settings.imp == null
				? 1 : settings.imp.getNChannels() );
		for ( final String key : spotAnalyzerProvider.getVisibleKeys() )
		{
			if ( selectionSpotAnalyzers.contains( key ) )
			{
				final SpotAnalyzerFactory< ? > factory = spotAnalyzerProvider.getFactory( key );
				if ( factory != null )
					settings.addSpotAnalyzerFactory( factory );
			}
		}

		// Shall we add 2D morphology analyzers?
		if ( settings.imp != null
				&& DetectionUtils.is2D( settings.imp )
				&& settings.detectorFactory != null
				&& settings.detectorFactory.has2Dsegmentation() )
		{
			final Spot2DMorphologyAnalyzerProvider spotMorphologyAnalyzerProvider = new Spot2DMorphologyAnalyzerProvider( settings.imp.getNChannels() );
			for ( final String key : spotMorphologyAnalyzerProvider.getVisibleKeys() )
			{
				if ( selectionSpotAnalyzers.contains( key ) )
				{
					final Spot2DMorphologyAnalyzerFactory< ? > factory = spotMorphologyAnalyzerProvider.getFactory( key );
					if ( factory != null )
						settings.addSpotAnalyzerFactory( factory );
				}
			}
		}

		// Shall we add 3D morphology analyzers?
		if ( settings.imp != null
				&& !DetectionUtils.is2D( settings.imp )
				&& settings.detectorFactory != null
				&& settings.detectorFactory.has3Dsegmentation() )
		{
			final Spot3DMorphologyAnalyzerProvider spotMorphologyAnalyzerProvider = new Spot3DMorphologyAnalyzerProvider( settings.imp.getNChannels() );
			for ( final String key : spotMorphologyAnalyzerProvider.getVisibleKeys() )
			{
				if ( selectionSpotAnalyzers.contains( key ) )
				{
					final Spot3DMorphologyAnalyzerFactory< ? > factory = spotMorphologyAnalyzerProvider.getFactory( key );
					if ( factory != null )
						settings.addSpotAnalyzerFactory( factory );
				}
			}
		}

		// Edge analyzers.
		final List< String > selectedEdgeAnalyzers = getSelectedAnalyzers( EDGES );
		final EdgeAnalyzerProvider edgeAnalyzerProvider = new EdgeAnalyzerProvider();
		for ( final String key : edgeAnalyzerProvider.getVisibleKeys() )
		{
			if ( selectedEdgeAnalyzers.contains( key ) )
			{
				final EdgeAnalyzer factory = edgeAnalyzerProvider.getFactory( key );
				if ( factory != null )
					settings.addEdgeAnalyzer( factory );
			}
		}

		// Track analyzers.
		final List< String > selectedTrackAnalyzers = getSelectedAnalyzers( TRACKS );
		final TrackAnalyzerProvider trackAnalyzerProvider = new TrackAnalyzerProvider();
		for ( final String key : trackAnalyzerProvider.getVisibleKeys() )
		{
			if ( selectedTrackAnalyzers.contains( key ) )
			{
				final TrackAnalyzer factory = trackAnalyzerProvider.getFactory( key );
				if ( factory != null )
					settings.addTrackAnalyzer( factory );
			}
		}
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

		for ( final String key : new Spot2DMorphologyAnalyzerProvider( 1 ).getVisibleKeys() )
			fs.setSelected( SPOTS, key, true );

		for ( final String key : new Spot3DMorphologyAnalyzerProvider( 1 ).getVisibleKeys() )
			fs.setSelected( SPOTS, key, true );

		for ( final String key : new EdgeAnalyzerProvider().getVisibleKeys() )
			fs.setSelected( EDGES, key, true );

		for ( final String key : new TrackAnalyzerProvider().getVisibleKeys() )
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

	public static final String toName( final TrackMateObject obj )
	{
		final String str = obj.toString();
		return StringUtils.capitalize( str ).substring( 0, str.length() - 1 );
	}

}
