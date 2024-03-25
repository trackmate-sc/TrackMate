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
package fiji.plugin.trackmate.tracking.sparselap;

import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_SPLITTING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.jaqaman.SparseLAPTracker;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class SparseLAPTrackerExample
{

	public static void main( final String[] args )
	{
		final File file = new File( "samples/FakeTracks.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		final Model model = reader.getModel();

		final Settings settings0 = reader.readSettings( null );

		final Map< String, Object > settings = settings0.trackerSettings;
		// Linking
		settings.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
		settings.put( KEY_LINKING_FEATURE_PENALTIES, DEFAULT_LINKING_FEATURE_PENALTIES );
		// Gap closing
		settings.put( KEY_ALLOW_GAP_CLOSING, true );
		settings.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, DEFAULT_GAP_CLOSING_MAX_FRAME_GAP );
		settings.put( KEY_GAP_CLOSING_MAX_DISTANCE, DEFAULT_GAP_CLOSING_MAX_DISTANCE );
		settings.put( KEY_GAP_CLOSING_FEATURE_PENALTIES, new HashMap<>( DEFAULT_GAP_CLOSING_FEATURE_PENALTIES ) );
		// Track splitting
		settings.put( KEY_ALLOW_TRACK_SPLITTING, true );
		settings.put( KEY_SPLITTING_MAX_DISTANCE, DEFAULT_SPLITTING_MAX_DISTANCE );
		settings.put( KEY_SPLITTING_FEATURE_PENALTIES, new HashMap<>( DEFAULT_SPLITTING_FEATURE_PENALTIES ) );
		// Track merging
		settings.put( KEY_ALLOW_TRACK_MERGING, true );
		settings.put( KEY_MERGING_MAX_DISTANCE, DEFAULT_MERGING_MAX_DISTANCE );
		settings.put( KEY_MERGING_FEATURE_PENALTIES, new HashMap<>( DEFAULT_MERGING_FEATURE_PENALTIES ) );
		// Others
		settings.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR );
		settings.put( KEY_CUTOFF_PERCENTILE, DEFAULT_CUTOFF_PERCENTILE );

		final SparseLAPTracker tracker = new SparseLAPTracker( model.getSpots(), settings );
		tracker.setLogger( Logger.DEFAULT_LOGGER );
		if ( !tracker.checkInput() || !tracker.process() )
		{
			System.err.println( tracker.getErrorMessage() );
			return;
		}

		model.setTracks( tracker.getResult(), true );

		final DisplaySettings ds = DisplaySettings.defaultStyle().copy();

		ij.ImageJ.main( args );
		final SelectionModel sm = new SelectionModel( model );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, sm, ds );
		view.render();
		final TrackScheme trackScheme = new TrackScheme( model, sm, ds );
		trackScheme.render();

	}

}
