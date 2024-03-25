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
package fiji.plugin.trackmate.features.track;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@Plugin( type = TrackAnalyzer.class )
public class TrackSpotQualityFeatureAnalyzer extends AbstractTrackAnalyzer
{

	public static final String KEY = "Track quality";

	public static final String TRACK_MEAN_QUALITY = "TRACK_MEAN_QUALITY";
	public static final List< String > FEATURES = Collections.singletonList( TRACK_MEAN_QUALITY );
	public static final Map< String, String > FEATURE_NAMES = Collections.singletonMap( TRACK_MEAN_QUALITY, "Track mean quality" );
	public static final Map< String, String > FEATURE_SHORT_NAMES = Collections.singletonMap( TRACK_MEAN_QUALITY, "Mean Q" );
	public static final Map< String, Dimension > FEATURE_DIMENSIONS = Collections.singletonMap( TRACK_MEAN_QUALITY, Dimension.QUALITY );
	public static final Map< String, Boolean > IS_INT = Collections.singletonMap( TRACK_MEAN_QUALITY, Boolean.FALSE );

	public TrackSpotQualityFeatureAnalyzer()
	{
		super( KEY, KEY, FEATURES, FEATURE_NAMES, FEATURE_SHORT_NAMES, FEATURE_DIMENSIONS, IS_INT );
	}

	@Override
	protected void analyze( final Integer trackID, final Model model )
	{
		final FeatureModel fm = model.getFeatureModel();
		final Set< Spot > track = model.getTrackModel().trackSpots( trackID );
		final double mean = track
				.stream()
				.filter( Objects::nonNull )
				.mapToDouble( s -> s.getFeature( Spot.QUALITY ).doubleValue() )
				.average()
				.getAsDouble();
		fm.putTrackFeature( trackID, TRACK_MEAN_QUALITY, Double.valueOf( mean ) );
	}
}
