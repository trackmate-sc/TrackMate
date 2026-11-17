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
package fiji.plugin.trackmate.tracking.jaqaman;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;

import java.util.Map;

import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

/**
 * Base class for trackers with split/merge actions.
 */
public abstract class SegmentTrackerFactory implements SpotTrackerFactory
{

	@Override
	@SuppressWarnings( "unchecked" )
	public String toString( final Map< String, Object > sm )
	{
		final String error = checkSettings( sm );
		if ( null != error )
			return error;

		final StringBuilder str = new StringBuilder();
		if ( ( Boolean ) sm.get( KEY_ALLOW_GAP_CLOSING ) )
		{
			str.append( "  Gap-closing conditions:\n" );
			str.append( String.format( "    - max distance: %.1f\n", ( Double ) sm.get( KEY_GAP_CLOSING_MAX_DISTANCE ) ) );
			str.append( String.format( "    - max frame gap: %d\n", ( Integer ) sm.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) ) );
			str.append( LAPUtils.echoFeaturePenalties( ( Map< String, Double > ) sm.get( KEY_GAP_CLOSING_FEATURE_PENALTIES ) ) );
		}
		else
		{
			str.append( "  Gap-closing not allowed.\n" );
		}

		if ( ( Boolean ) sm.get( KEY_ALLOW_TRACK_SPLITTING ) )
		{
			str.append( "  Track splitting conditions:\n" );
			str.append( String.format( "    - max distance: %.1f\n", ( Double ) sm.get( KEY_SPLITTING_MAX_DISTANCE ) ) );
			str.append( LAPUtils.echoFeaturePenalties( ( Map< String, Double > ) sm.get( KEY_SPLITTING_FEATURE_PENALTIES ) ) );
		}
		else
		{
			str.append( "  Track splitting not allowed.\n" );
		}

		if ( ( Boolean ) sm.get( KEY_ALLOW_TRACK_MERGING ) )
		{
			str.append( "  Track merging conditions:\n" );
			str.append( String.format( "    - max distance: %.1f\n", ( Double ) sm.get( KEY_MERGING_MAX_DISTANCE ) ) );
			str.append( LAPUtils.echoFeaturePenalties( ( Map< String, Double > ) sm.get( KEY_MERGING_FEATURE_PENALTIES ) ) );
		}
		else
		{
			str.append( "  Track merging not allowed.\n" );
		}

		return str.toString();
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		return LAPUtils.getDefaultSegmentSettingsMap();
	}

	@Override
	public String checkSettings( final Map< String, Object > settings )
	{
		if ( null == settings )
			return "Settings map is null.\n";

		final StringBuilder str = new StringBuilder();
		final boolean ok = LAPUtils.checkSettingsValidity( settings, str, false );
		if ( !ok )
			return str.toString();

		return null;
	}
}
