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
package fiji.plugin.trackmate.features.edges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@Plugin( type = EdgeAnalyzer.class )
public class EdgeSpeedAnalyzer extends AbstractEdgeAnalyzer
{

	public static final String KEY = "Edge speed";

	public static final String SPEED = "SPEED";
	public static final String DISPLACEMENT = "DISPLACEMENT";
	public static final List< String > FEATURES = new ArrayList<>( 2 );
	public static final Map< String, String > FEATURE_NAMES = new HashMap<>( 2 );
	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 2 );
	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 2 );
	public static final Map< String, Boolean > IS_INT = new HashMap<>( 2 );

	static
	{
		FEATURES.add( SPEED );
		FEATURES.add( DISPLACEMENT );

		FEATURE_NAMES.put( SPEED, "Speed" );
		FEATURE_NAMES.put( DISPLACEMENT, "Displacement" );

		FEATURE_SHORT_NAMES.put( SPEED, "Speed" );
		FEATURE_SHORT_NAMES.put( DISPLACEMENT, "Disp." );

		FEATURE_DIMENSIONS.put( SPEED, Dimension.VELOCITY );
		FEATURE_DIMENSIONS.put( DISPLACEMENT, Dimension.LENGTH );

		IS_INT.put( SPEED, Boolean.FALSE );
		IS_INT.put( DISPLACEMENT, Boolean.FALSE );
	}

	public EdgeSpeedAnalyzer()
	{
		super( KEY, KEY, FEATURES, FEATURE_NAMES, FEATURE_SHORT_NAMES, FEATURE_DIMENSIONS, IS_INT );
	}

	@Override
	protected void analyze( final DefaultWeightedEdge edge, final Model model )
	{
		final FeatureModel featureModel = model.getFeatureModel();
		final Spot source = model.getTrackModel().getEdgeSource( edge );
		final Spot target = model.getTrackModel().getEdgeTarget( edge );

		final double dx = target.diffTo( source, Spot.POSITION_X );
		final double dy = target.diffTo( source, Spot.POSITION_Y );
		final double dz = target.diffTo( source, Spot.POSITION_Z );
		final double dt = target.diffTo( source, Spot.POSITION_T );
		final double D = Math.sqrt( dx * dx + dy * dy + dz * dz );
		final double S = D / Math.abs( dt );

		featureModel.putEdgeFeature( edge, SPEED, S );
		featureModel.putEdgeFeature( edge, DISPLACEMENT, D );
	}
}
