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
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;

public class EdgeTargetAnalyzerTest
{

	private static final int N_TRACKS = 10;

	private static final int DEPTH = 9; // must be at least 6 to avoid tracks
										// too shorts - may make this test fail
										// sometimes

	private Model model;

	private HashMap< DefaultWeightedEdge, Spot > edgeTarget;

	private HashMap< DefaultWeightedEdge, Spot > edgeSource;

	private HashMap< DefaultWeightedEdge, Double > edgeCost;

	@Before
	public void setUp()
	{
		edgeSource = new HashMap<>();
		edgeTarget = new HashMap<>();
		edgeCost = new HashMap<>();

		model = new Model();
		model.beginUpdate();
		try
		{

			for ( int i = 0; i < N_TRACKS; i++ )
			{

				Spot previous = null;

				for ( int j = 0; j <= DEPTH; j++ )
				{
					final Spot spot = new SpotBase( 0d, 0d, 0d, 1d, -1d );
					model.addSpotTo( spot, j );
					if ( null != previous )
					{
						final DefaultWeightedEdge edge = model.addEdge( previous, spot, j );
						edgeSource.put( edge, previous );
						edgeTarget.put( edge, spot );
						edgeCost.put( edge, Double.valueOf( j ) );

					}
					previous = spot;
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
		final EdgeTargetAnalyzer analyzer = new EdgeTargetAnalyzer();
		analyzer.process( model.getTrackModel().edgeSet(), model );

		// Collect features
		for ( final DefaultWeightedEdge edge : model.getTrackModel().edgeSet() )
		{
			assertEquals( edgeSource.get( edge ).ID(), model.getFeatureModel().getEdgeFeature( edge, EdgeTargetAnalyzer.SPOT_SOURCE_ID ).intValue() );
			assertEquals( edgeTarget.get( edge ).ID(), model.getFeatureModel().getEdgeFeature( edge, EdgeTargetAnalyzer.SPOT_TARGET_ID ).intValue() );
			assertEquals( edgeCost.get( edge ).doubleValue(), model.getFeatureModel().getEdgeFeature( edge, EdgeTargetAnalyzer.EDGE_COST ).doubleValue(), Double.MIN_VALUE );
		}
	}

	@Test
	public final void testModelChanged()
	{
		// Initial calculation
		final TestEdgeTargetAnalyzer analyzer = new TestEdgeTargetAnalyzer();
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

		// Change the cost of one edge
		final DefaultWeightedEdge edge = edgeSource.keySet().iterator().next();
		final double val = 67.43;
		model.beginUpdate();
		try
		{
			model.setEdgeWeight( edge, val );
		}
		finally
		{
			model.endUpdate();
		}

		// We must have received only one edge to analyzer
		assertTrue( analyzer.hasBeenRun );
		assertEquals( 1, analyzer.edges.size() );
		final DefaultWeightedEdge analyzedEdge = analyzer.edges.iterator().next();
		assertEquals( edge, analyzedEdge );
		assertEquals( val, model.getFeatureModel().getEdgeFeature( edge, EdgeTargetAnalyzer.EDGE_COST ).doubleValue(), Double.MIN_VALUE );

	}

	private static class TestEdgeTargetAnalyzer extends EdgeTargetAnalyzer
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
