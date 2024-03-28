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
package fiji.plugin.trackmate.features.edge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;

public class EdgeTimeAndLocationAnalyzerTest
{

	private static final int N_TRACKS = 10;

	private static final int DEPTH = 9; // must be at least 6 to avoid tracks
										// too shorts - may make this test fail
										// sometimes

	private Model model;

	private HashMap< DefaultWeightedEdge, Double > edgeTime;

	private HashMap< DefaultWeightedEdge, Double > edgePos;

	private Spot aspot;

	@Before
	public void setUp()
	{
		edgePos = new HashMap<>();
		edgeTime = new HashMap<>();

		model = new Model();
		model.beginUpdate();

		try
		{

			for ( int i = 0; i < N_TRACKS; i++ )
			{

				Spot previous = null;

				for ( int j = 0; j <= DEPTH; j++ )
				{
					final Spot spot = new SpotBase( i + j, i + j, i + j, 1d, -1d );
					spot.putFeature( Spot.POSITION_T, Double.valueOf( j ) );
					model.addSpotTo( spot, j );
					if ( null != previous )
					{
						final DefaultWeightedEdge edge = model.addEdge( previous, spot, j );
						final double xcurrent = spot.getFeature( Spot.POSITION_X ).doubleValue();
						final double xprevious = previous.getFeature( Spot.POSITION_X ).doubleValue();
						edgePos.put( edge, 0.5 * ( xcurrent + xprevious ) );
						edgeTime.put( edge, 0.5 * ( spot.getFeature( Spot.POSITION_T ) + previous.getFeature( Spot.POSITION_T ) ) );

					}
					previous = spot;

					// save one middle spot
					if ( i == 0 && j == DEPTH / 2 )
					{
						aspot = spot;
					}
				}
			}

		}
		finally
		{
			model.endUpdate();
		}
	}

	@Test
	public final void testProcess()
	{
		// Create analyzer
		final EdgeTimeLocationAnalyzer analyzer = new EdgeTimeLocationAnalyzer();
		// Register it in the model
		final Collection< String > features = analyzer.getFeatures();
		final Map< String, String > featureNames = analyzer.getFeatureNames();
		final Map< String, String > featureShortNames = analyzer.getFeatureShortNames();
		final Map< String, Dimension > featureDimensions = analyzer.getFeatureDimensions();
		final Map< String, Boolean > isIntFeature = analyzer.getIsIntFeature();
		model.getFeatureModel().declareEdgeFeatures( features, featureNames, featureShortNames, featureDimensions, isIntFeature );
		// Process model
		analyzer.process( model.getTrackModel().edgeSet(), model );

		// Collect features
		for ( final DefaultWeightedEdge edge : model.getTrackModel().edgeSet() )
		{
			assertEquals( edgePos.get( edge ).doubleValue(), model.getFeatureModel().getEdgeFeature( edge, EdgeTimeLocationAnalyzer.X_LOCATION ).doubleValue(), Double.MIN_VALUE );
			assertEquals( edgeTime.get( edge ).doubleValue(), model.getFeatureModel().getEdgeFeature( edge, EdgeTimeLocationAnalyzer.TIME ).doubleValue(), Double.MIN_VALUE );
		}
	}

	@Test
	public final void testModelChanged()
	{
		// Initial calculation
		final TestEdgeTimeLocationAnalyzer analyzer = new TestEdgeTimeLocationAnalyzer();
		// Register it in the model
		final Collection< String > features = analyzer.getFeatures();
		final Map< String, String > featureNames = analyzer.getFeatureNames();
		final Map< String, String > featureShortNames = analyzer.getFeatureShortNames();
		final Map< String, Dimension > featureDimensions = analyzer.getFeatureDimensions();
		final Map< String, Boolean > isIntFeature = analyzer.getIsIntFeature();
		model.getFeatureModel().declareEdgeFeatures( features, featureNames, featureShortNames, featureDimensions, isIntFeature );
		// Process model
		analyzer.process( model.getTrackModel().edgeSet(), model );

		// Prepare listener
		model.addModelChangeListener( new ModelChangeListener()
		{
			@Override
			public void modelChanged( final ModelChangeEvent event )
			{
				final HashSet< DefaultWeightedEdge > edgesToUpdate = new HashSet<>();
				for ( final DefaultWeightedEdge edge : event.getEdges() )
				{
					if ( event.getEdgeFlag( edge ) != ModelChangeEvent.FLAG_EDGE_REMOVED )
					{
						edgesToUpdate.add( edge );
					}
				}
				if ( analyzer.isLocal() )
				{

					analyzer.process( edgesToUpdate, model );

				}
				else
				{

					// Get the all the edges of the track they belong to
					final HashSet< DefaultWeightedEdge > globalEdgesToUpdate = new HashSet<>();
					for ( final DefaultWeightedEdge edge : edgesToUpdate )
					{
						final Integer motherTrackID = model.getTrackModel().trackIDOf( edge );
						globalEdgesToUpdate.addAll( model.getTrackModel().trackEdges( motherTrackID ) );
					}
					analyzer.process( globalEdgesToUpdate, model );
				}
			}
		} );

		// Move one spot
		model.beginUpdate();
		try
		{
			aspot.putFeature( Spot.POSITION_X, -1000d );
			model.updateFeatures( aspot );
		}
		finally
		{
			model.endUpdate();
		}

		// We must have received 2 edges to analyzer
		assertTrue( analyzer.hasBeenRun );
		assertEquals( 2, analyzer.edges.size() );
	}

	private static class TestEdgeTimeLocationAnalyzer extends EdgeTimeLocationAnalyzer
	{

		private boolean hasBeenRun = false;

		private Collection< DefaultWeightedEdge > edges;

		@Override
		public void process( final Collection< DefaultWeightedEdge > lEdges, final Model model )
		{
			this.hasBeenRun = true;
			this.edges = lEdges;
			super.process( lEdges, model );
		}
	}
}
