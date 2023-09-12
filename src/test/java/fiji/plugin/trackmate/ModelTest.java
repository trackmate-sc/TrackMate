/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
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
package fiji.plugin.trackmate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Test;

public class ModelTest {


	/**
	 * Test if the track visibility is followed correctly.
	 */
	@Test
	public void testTrackVisibility() {
		final Model model = new Model();
		// Build track 1 with 5 spots
		final Spot s1 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S1" );
		final Spot s2 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S2" );
		final Spot s3 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S3" );
		final Spot s4 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S4" );
		final Spot s5 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S5" );
		// Build track 2 with 2 spots
		final Spot s6 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S6" );
		final Spot s7 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S7" );
		// Build track 3 with 2 spots
		final Spot s8 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S8" );
		final Spot s9 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S9" );

		model.beginUpdate();
		try {

			model.addSpotTo(s1, 0);
			model.addSpotTo(s2, 1);
			model.addSpotTo(s3, 2);
			model.addSpotTo(s4, 3);
			model.addSpotTo(s5, 4);
			model.getTrackModel().addEdge(s1, s2, 0);
			model.getTrackModel().addEdge(s2, s3, 0);
			model.getTrackModel().addEdge(s3, s4, 0);
			model.getTrackModel().addEdge(s4, s5, 0);

			model.addSpotTo(s6, 0);
			model.addSpotTo(s7, 1);
			model.getTrackModel().addEdge(s6, s7, 0);

			model.addSpotTo(s8, 0);
			model.addSpotTo(s9, 1);
			model.getTrackModel().addEdge(s8, s9, 0);

		} finally {
			model.endUpdate();
		}

		Set<Integer> visibleTracks = model.getTrackModel().trackIDs(true);

		// These must be 3 tracks visible
		assertEquals(3, visibleTracks.size());
		// all of the tracks must be visible
		Iterator<Integer> it = model.getTrackModel().trackIDs(false).iterator();
		assertTrue(visibleTracks.contains(it.next()));
		assertTrue(visibleTracks.contains(it.next()));
		assertTrue(visibleTracks.contains(it.next()));

		// Delete spot s3, make 2 tracks of the first one
		model.beginUpdate();
		try {
			model.removeSpot(s3);
		} finally {
			model.endUpdate();
		}

		// These must be 4 tracks visible
		visibleTracks = model.getTrackModel().trackIDs(true);
		assertEquals(4, visibleTracks.size());
		// all of the tracks must be visible
		it = model.getTrackModel().trackIDs(false).iterator();
		assertTrue(visibleTracks.contains(it.next()));
		assertTrue(visibleTracks.contains(it.next()));
		assertTrue(visibleTracks.contains(it.next()));
		assertTrue(visibleTracks.contains(it.next()));

		// Check in what track is the spot s4
		final int track2 = model.getTrackModel().trackIDOf(s4);
		//		System.out.println("The spot "+s4+" is in track "+track2);

		// Make it invisible
		model.beginUpdate();
		boolean modified;
		try {
			modified = model.setTrackVisibility(track2, false);
		} finally {
			model.endUpdate();
		}

		// We must have modified something: it was visible, now it is invisible
		assertTrue(modified);

		// These must be now 3 tracks visible
		visibleTracks = model.getTrackModel().trackIDs(true);
		assertEquals(3, visibleTracks.size());
		// out of 4
		assertEquals(4, model.getTrackModel().nTracks(false));
		// with indices different from track2
		for(final int index : visibleTracks) {
			assertTrue( track2 != index );
		}

		// Reconnect s2 and s4
		model.beginUpdate();
		try {
			model.getTrackModel().addEdge(s2, s4, 0);
		} finally {
			model.endUpdate();
		}

		// These must be now 3 tracks visible: connecting a visible track with an invisible makes
		// it all visible
		visibleTracks = model.getTrackModel().trackIDs(true);
		assertEquals(3, visibleTracks.size());
		// out of 3
		assertEquals(3, model.getTrackModel().nTracks(false));
	}


	/**
	 * Test the track number reported by the model as we modify it.
	 */
	@Test
	public void testTrackNumber() {
		final Model model = new Model();

		// Empty model, should get 0 tracks
		assertEquals(0, model.getTrackModel().nTracks(false));

		// Build track with 5 spots
		final Spot s1 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S1" );
		final Spot s2 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S2" );
		final Spot s3 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S3" );
		final Spot s4 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S4" );
		final Spot s5 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S5" );
		model.beginUpdate();
		try {
			model.addSpotTo(s1, 0);
			model.addSpotTo(s2, 0);
			model.addSpotTo(s3, 0);
			model.addSpotTo(s4, 0);
			model.addSpotTo(s5, 0);

			model.getTrackModel().addEdge(s1, s2, 0);
			model.getTrackModel().addEdge(s2, s3, 0);
			model.getTrackModel().addEdge(s3, s4, 0);
			model.getTrackModel().addEdge(s4, s5, 0);
		} finally {
			model.endUpdate();
		}

		// All spots are connected by edges, should build one track
		assertEquals(1, model.getTrackModel().nTracks(false));

		// Remove middle spot
		model.beginUpdate();
		try {
			model.removeSpot(s3);
		} finally {
			model.endUpdate();
		}

		// Track split in 2, should get 2 tracks
		assertEquals(2, model.getTrackModel().nTracks(false));

		// Stitch back the two tracks
		model.beginUpdate();
		try {
			model.getTrackModel().addEdge(s2, s4, -1);
		} finally {
			model.endUpdate();
		}

		// Stitched, so we should get back one track again
		assertEquals(1, model.getTrackModel().nTracks(false));
	}

	/**
	 * Test if manual adding spots and links in one update step is caught as a single
	 * event, and that this event is well configured.
	 */
	@Test
	public void testTrackModelChangeEvent() {

		// Create a model with 5 spots, that forms a single branch track
		final Model model = new Model();

		// Add an event listener for that checks for adding spots and edges
		ModelChangeListener eventLogger = new ModelChangeListener() {
			@Override
			public void modelChanged(final ModelChangeEvent event) {

				// Event must be of the right type
				assertEquals(ModelChangeEvent.MODEL_MODIFIED, event.getEventID());
				// I expect 5 new spots from this event
				assertEquals(5, event.getSpots().size());
				// I expect 4 new links from this event
				assertEquals(4, event.getEdges().size());
				// Check the correct flag type for spots
				for(final Spot spot : event.getSpots()) {
					assertEquals(ModelChangeEvent.FLAG_SPOT_ADDED, event.getSpotFlag(spot).intValue());
				}
				// Check the correct flag type for edges
				for(final DefaultWeightedEdge edge : event.getEdges()) {
					assertEquals(ModelChangeEvent.FLAG_EDGE_ADDED, event.getEdgeFlag(edge).intValue());
				}
			}
		};
		model.addModelChangeListener(eventLogger);


		final Spot s1 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S1" );
		final Spot s2 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S2" );
		final Spot s3 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S3" );
		final Spot s4 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S4" );
		final Spot s5 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S5" );

		//		System.out.println("Create the graph in one update:");
		model.beginUpdate();
		try {
			model.addSpotTo(s1, 0);
			model.addSpotTo(s2, 1);
			model.addSpotTo(s3, 2);
			model.addSpotTo(s4, 3);
			model.addSpotTo(s5, 4);

			model.addEdge(s1, s2, 0);
			model.addEdge(s2, s3, 0);
			model.addEdge(s3, s4, 0);
			model.addEdge(s4, s5, 0);
		} finally {
			model.endUpdate();
		}


		// Remove old eventLogger
		model.removeModelChangeListener(eventLogger);

		/*
		 * We will now remove the middle spot in the newly created track.
		 * This will generate an event where we will of course see the removal
		 * of the spot, but also the removal of 2 edges that were linking to
		 * this spot.
		 */


		// Add a new event logger that will monitor for a spot removal
		eventLogger = new ModelChangeListener() {
			@Override
			public void modelChanged(final ModelChangeEvent event) {
				// Event must be of the right type
				assertEquals(ModelChangeEvent.MODEL_MODIFIED, event.getEventID());
				// I expect 1 modified spot from this event
				assertEquals(1, event.getSpots().size());
				// It must be s3
				assertEquals(s3, event.getSpots().iterator().next());
				// It must be the removed flag
				assertEquals(ModelChangeEvent.FLAG_SPOT_REMOVED, event.getSpotFlag(s3).intValue());

				// I expect 2 links to be affected by this event
				assertEquals(2, event.getEdges().size());
				// Check the correct flag type for edges: they must be removed
				for(final DefaultWeightedEdge edge : event.getEdges()) {
					assertEquals(ModelChangeEvent.FLAG_EDGE_REMOVED, event.getEdgeFlag(edge).intValue());
				}
				// Check the removed edges identity
				for (final DefaultWeightedEdge edge : event.getEdges()) {

					assertTrue(
							( model.getTrackModel().getEdgeSource(edge).equals(s3) && model.getTrackModel().getEdgeTarget(edge).equals(s2) ||
									model.getTrackModel().getEdgeSource(edge).equals(s2) && model.getTrackModel().getEdgeTarget(edge).equals(s3)
									) || (
											model.getTrackModel().getEdgeSource(edge).equals(s3) && model.getTrackModel().getEdgeTarget(edge).equals(s4)
											|| model.getTrackModel().getEdgeSource(edge).equals(s4) && model.getTrackModel().getEdgeTarget(edge).equals(s3)
											)
							);

				}
			}
		};

		model.addModelChangeListener(eventLogger);

		model.beginUpdate();
		try {
			model.removeSpot(s3);
		} finally {
			model.endUpdate();
		}

		/*
		 * We ended up in having 2 tracks.
		 * We will now reconnect them by creating a new edge. This will generate an event
		 * with 1 edge and 0 spots.
		 */

		model.removeModelChangeListener(eventLogger);

		eventLogger = new ModelChangeListener() {
			@Override
			public void modelChanged(final ModelChangeEvent event) {
				// Event must be of the right type
				assertEquals(ModelChangeEvent.MODEL_MODIFIED, event.getEventID());
				// I expect 0 modified spot from this event, so spot field must be empty
				assertTrue(event.getSpots().isEmpty());
				// It must be s3

				// I expect 1 new link in this event
				assertEquals(1, event.getEdges().size());
				// Check the correct flag type for edges: they must be removed
				for(final DefaultWeightedEdge edge : event.getEdges()) {
					assertEquals(ModelChangeEvent.FLAG_EDGE_ADDED, event.getEdgeFlag(edge).intValue());
				}
				// Check the added edges identity
				for (final DefaultWeightedEdge edge : event.getEdges()) {

					assertTrue(
							( model.getTrackModel().getEdgeSource(edge).equals(s2) && model.getTrackModel().getEdgeTarget(edge).equals(s4) ||
									model.getTrackModel().getEdgeSource(edge).equals(s4) && model.getTrackModel().getEdgeTarget(edge).equals(s2)	)
							);
				}
			}
		};

		model.addModelChangeListener(eventLogger);

		model.beginUpdate();
		try {
			model.getTrackModel().addEdge(s2, s4, -1);
		} finally {
			model.endUpdate();
		}

	}

	@Test
	public void testRemovingWholeTracksAtOnce() {
		final int N_TRACKS = 2;
		final int DEPTH = 10;
		final Model model = new Model();
		final Collection<Spot> trackSpots = new HashSet<>();
		final Collection<DefaultWeightedEdge> trackEdges = new HashSet<>();

		// Create model
		model.beginUpdate();
		try {
			for (int i = 0; i < N_TRACKS ; i++) {
				Spot previous = null;
				Spot spot = null;
				for (int j = 0; j < DEPTH; j++) {
					spot = new SpotBase( 0d, 0d, 0d, 1d, -1d );
					model.addSpotTo(spot, j);
					if (i == 0) {
						trackSpots.add(spot);
					}
					if (null != previous) {
						final DefaultWeightedEdge edge = model.addEdge(previous, spot, 1);
						if (0 == i) {
							trackEdges.add(edge);
						}
					}
					previous = spot;
				}
			}
		} finally {
			model.endUpdate();
		}

		model.addModelChangeListener(new ModelChangeListener() {

			@Override
			public void modelChanged(final ModelChangeEvent event) {

				for (final DefaultWeightedEdge edge : event.getEdges()) {
					assertEquals(ModelChangeEvent.FLAG_EDGE_REMOVED, event.getEdgeFlag(edge).intValue());
					assertTrue(trackEdges.contains(edge));
					trackEdges.remove(edge);
				}
				assertTrue(trackEdges.isEmpty());

				for (final Spot spot : event.getSpots()) {
					assertEquals(ModelChangeEvent.FLAG_SPOT_REMOVED, event.getSpotFlag(spot).intValue());
					assertTrue(trackSpots.contains(spot));
					trackSpots.remove(spot);
				}
				assertTrue(trackSpots.isEmpty());

			}
		});

		// Remove a whole track
		model.beginUpdate();
		for (final DefaultWeightedEdge edge : trackEdges) {
			model.removeEdge(edge);
		}
		for (final Spot spot : trackSpots) {
			model.removeSpot(spot);
		}
		model.endUpdate();
	}

	/*
	 * EXAMPLE
	 */

	public void exampleManipulation() {

		// Create a model with 5 spots, that forms a single branch track
		final Model model = new Model();

		// Add an event listener now
		model.addModelChangeListener(new EventLogger());

		final Spot s1 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S1" );
		final Spot s2 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S2" );
		final Spot s3 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S3" );
		final Spot s4 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S4" );
		final Spot s5 = new SpotBase( 0d, 0d, 0d, 1d, -1d, "S5" );

		System.out.println("Create the graph in one update:");
		model.beginUpdate();
		try {
			model.addSpotTo(s1, 0);
			model.addSpotTo(s2, 0);
			model.addSpotTo(s3, 0);
			model.addSpotTo(s4, 0);
			model.addSpotTo(s5, 0);

			model.getTrackModel().addEdge(s1, s2, 0);
			model.getTrackModel().addEdge(s2, s3, 0);
			model.getTrackModel().addEdge(s3, s4, 0);
			model.getTrackModel().addEdge(s4, s5, 0);
		} finally {
			model.endUpdate();
		}

		System.out.println();
		System.out.println("Tracks are:");
		for (final Integer trackID : model.getTrackModel().trackIDs(false)) {
			System.out.println("\tTrack "+trackID+" with name: " + model.getTrackModel().name(trackID));
			System.out.println("\t\t"+model.getTrackModel().trackSpots(trackID));
			System.out.println("\t\t"+model.getTrackModel().trackEdges(trackID));
		}
		System.out.println();
		System.out.println();

		// Remove one spot and see what happens
		System.out.println("Removing a single spot in the middle of the track:");
		model.beginUpdate();
		try {
			model.removeSpot(s3);
		} finally {
			model.endUpdate();
		}
		System.out.println();
		System.out.println("Tracks are:");
		for (final Integer trackID : model.getTrackModel().trackIDs(false)) {
			System.out.println("\tTrack "+trackID+" with name: " + model.getTrackModel().name(trackID));
			System.out.println("\t\t"+model.getTrackModel().trackSpots(trackID));
			System.out.println("\t\t"+model.getTrackModel().trackEdges(trackID));
		}
		System.out.println("Track visibility is:");
		System.out.println(model.getTrackModel().trackIDs(true));

		System.out.println();
		System.out.println();

		System.out.println("Making the first track invisible:");
		model.beginUpdate();
		try {
			model.setTrackVisibility(model.getTrackModel().trackIDs(false).iterator().next(), false);
		} finally {
			model.endUpdate();
		}

		System.out.println("Track visibility is:");
		System.out.println(model.getTrackModel().trackIDs(true));

		System.out.println();
		System.out.println();

		System.out.println("Reconnect the 2 tracks:");
		model.beginUpdate();
		try {
			model.getTrackModel().addEdge(s2, s4, -1);
		} finally {
			model.endUpdate();
		}

		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("Tracks are:");
		for (final Integer trackID : model.getTrackModel().trackIDs(false)) {
			System.out.println("\tTrack "+trackID+" with name: " + model.getTrackModel().name(trackID));
			System.out.println("\t\t"+model.getTrackModel().trackSpots(trackID));
			System.out.println("\t\t"+model.getTrackModel().trackEdges(trackID));
		}
		System.out.println("Track visibility is:");
		System.out.println(model.getTrackModel().trackIDs(true));

	}

	public static void main(final String[] args) {
		new ModelTest().exampleManipulation();
	}

	private static class EventLogger implements ModelChangeListener {

		@Override
		public void modelChanged(final ModelChangeEvent event) {
			// Simply append it to sysout
			System.out.println(event);

		}
	}
}
