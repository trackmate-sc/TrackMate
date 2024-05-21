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

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.features.edges.EdgeSpeedAnalyzer;

public class EdgeVelocityAnalyzerTest
{

	private static final int N_TRACKS = 10;

	private static final int DEPTH = 9; // must be at least 6 to avoid tracks
										// too shorts - may make this test fail
										// sometimes

	private Model model;

	private HashMap< DefaultWeightedEdge, Double > edgeD;

	private HashMap< DefaultWeightedEdge, Double > edgeV;

	private Spot aspot;

	@Before
	public void setUp()
	{
		edgeV = new HashMap<>();
		edgeD = new HashMap<>();

		model = new Model();
		model.beginUpdate();

		final String[] posFeats = Spot.POSITION_FEATURES;

		try
		{

			for ( int i = 0; i < N_TRACKS; i++ )
			{

				Spot previous = null;

				for ( int j = 0; j <= DEPTH; j++ )
				{
					final Spot spot = new SpotBase( 0., 0., 0., 1., -1. );
					spot.putFeature( posFeats[ i % 3 ], Double.valueOf( i + j ) );
					// rotate displacement dimension
					spot.putFeature( Spot.POSITION_T, Double.valueOf( 2 * j ) );
					model.addSpotTo( spot, j );
					if ( null != previous )
					{
						final DefaultWeightedEdge edge = model.addEdge( previous, spot, j );
						final double d = spot.getFeature( posFeats[ i % 3 ] ).doubleValue() - previous.getFeature( posFeats[ i % 3 ] ).doubleValue();
						edgeD.put( edge, d );
						edgeV.put( edge, d / 2 );

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
		// Process model
		final EdgeSpeedAnalyzer analyzer = new EdgeSpeedAnalyzer();
		analyzer.process( model.getTrackModel().edgeSet(), model );

		// Collect features
		for ( final DefaultWeightedEdge edge : model.getTrackModel().edgeSet() )
		{
			assertEquals( edgeV.get( edge ).doubleValue(), model.getFeatureModel().getEdgeFeature( edge, EdgeSpeedAnalyzer.SPEED ).doubleValue(), Double.MIN_VALUE );
			assertEquals( edgeD.get( edge ).doubleValue(), model.getFeatureModel().getEdgeFeature( edge, EdgeSpeedAnalyzer.DISPLACEMENT ).doubleValue(), Double.MIN_VALUE );
		}
	}

	@Test
	public final void testModelChanged()
	{
		// Initial calculation
		final TestEdgeVelocityAnalyzer analyzer = new TestEdgeVelocityAnalyzer();
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

	private static class TestEdgeVelocityAnalyzer extends EdgeSpeedAnalyzer
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
