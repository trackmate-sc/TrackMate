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
package fiji.plugin.trackmate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.features.FeatureFilter;

public class SpotCollectionTest
{

	private static final int N_SPOTS = 100;

	private static final int N_FRAMES = 50;

	private SpotCollection sc;

	private ArrayList< Integer > frames;

	@Before
	public void setUp() throws Exception
	{

		// Create a spot collection of 50 odd frame number, ranging from 1 to 99
		frames = new ArrayList<>( 50 );
		sc = new SpotCollection();

		for ( int i = 1; i < N_FRAMES * 2; i = i + 2 )
		{

			// Store frames
			frames.add( i );

			// For each frame, create 100 spots, with X, Y, Z, T and QUALITY
			// linearly increasing
			final HashSet< Spot > spots = new HashSet<>( 100 );
			for ( int j = 0; j < N_SPOTS; j++ )
			{
				final Spot spot = new SpotBase( j, j, j, 1d, -1d );
				spot.putFeature( Spot.POSITION_T, Double.valueOf( i ) );
				spot.putFeature( Spot.QUALITY, Double.valueOf( j ) );
				spot.putFeature( Spot.RADIUS, Double.valueOf( j / 2 ) );
				spots.add( spot );
			}
			sc.put( i, spots );
		}

	}

	@Test
	public void testCrop()
	{
		final FeatureFilter filter = new FeatureFilter( Spot.QUALITY, 2d, false );
		sc.filter( filter );
		sc.crop();
		assertEquals( 3 * N_FRAMES, sc.getNSpots( false ) );
		assertEquals( 3 * N_FRAMES, sc.getNSpots( true ) );
	}

	@Test
	public void testAdd()
	{
		// Pre-Test
		for ( final Integer frame : frames )
		{
			assertEquals( N_SPOTS, sc.getNSpots( frame, false ) );
		}
		// Add a spot to target frame
		final int targetFrame = 1 + 2 * new Random().nextInt( 50 );
		final Spot spot = new SpotBase( 0d, 0d, 0d, 1d, -1d );
		sc.add( spot, targetFrame );
		// Test
		for ( final Integer frame : frames )
		{
			if ( frame == targetFrame )
			{
				assertEquals( N_SPOTS + 1, sc.getNSpots( frame, false ) );
			}
			else
			{
				assertEquals( N_SPOTS, sc.getNSpots( frame, false ) );
			}
		}
	}

	@Test
	public void testRemove()
	{
		// Pre-Test
		for ( final Integer frame : frames )
		{
			assertEquals( N_SPOTS, sc.getNSpots( frame, false ) );
		}

		// Remove a random spot from target frame
		int targetFrame = 1 + 2 * new Random().nextInt( 50 );
		final Iterator< Spot > it = sc.iterator( targetFrame, false );
		Spot targetSpot = null;
		for ( int i = 0; i < new Random().nextInt( N_SPOTS ); i++ )
		{
			targetSpot = it.next();
		}
		boolean flag = sc.remove( targetSpot, targetFrame );
		assertTrue( "The target spot " + targetSpot + " could not be removed from target frame " + targetFrame + ".", flag );

		// Test
		for ( final Integer frame : frames )
		{
			if ( frame == targetFrame )
			{
				assertEquals( N_SPOTS - 1, sc.getNSpots( frame, false ) );
			}
			else
			{
				assertEquals( N_SPOTS, sc.getNSpots( frame, false ) );
			}
		}

		// Remove the spot from the wrong frame - we should fail
		targetFrame++;
		flag = sc.remove( targetSpot, targetFrame );
		assertFalse( "The target spot " + targetSpot + " could be removed from wrong frame " + targetFrame + ".", flag );
	}

	@Test
	public void testIsVisible()
	{
		// In the beginning, none shall be visible
		Iterator< Spot > it = sc.iterator( false );
		while ( it.hasNext() )
		{
			final Spot spot = it.next();
			assertFalse( "Spot " + spot + " is visible, but should not.", isVisible( spot ) );
		}
		// Mark a random spot as visible
		final int targetFrame = 1 + 2 * new Random().nextInt( N_FRAMES );
		it = sc.iterator( targetFrame, false );
		Spot targetSpot = null;
		for ( int i = 0; i < new Random().nextInt( N_SPOTS ); i++ )
			targetSpot = it.next();

		assertNotNull( targetSpot );
		targetSpot.putFeature( SpotCollection.VISIBILITY, SpotCollection.ONE );
		// Test for visibility
		it = sc.iterator( false );
		while ( it.hasNext() )
		{
			final Spot spot = it.next();
			if ( spot == targetSpot )
				assertTrue( "Target spot " + spot + " should be visible, but is not.", isVisible( spot ) );
			else
				assertFalse( "Spot " + spot + " is visible, but should not.", isVisible( spot ) );
		}
	}

	@Test
	public void testFilter()
	{
		// Test that all are invisible for now
		assertEquals( 0, sc.getNSpots( true ) );
		// Filter by quality below 2. Should leave 3 spots per frame (0, 1 & 2)
		final FeatureFilter filter = new FeatureFilter( Spot.QUALITY, 2d, false );
		sc.filter( filter );
		assertEquals( 3 * N_FRAMES, sc.getNSpots( true ) );
	}

	@Test
	public void testFilters()
	{
		// Test that all are invisible for now
		assertEquals( 0, sc.getNSpots( true ) );
		// Filter by quality below 2. Should leave 3 spots per frame (0, 1 & 2).
		final FeatureFilter filter1 = new FeatureFilter( Spot.QUALITY, 2d, false );
		// Filter by FRAME above 91. Should leave 5 frames (91, 93, 95, 97 &
		// 99).
		final FeatureFilter filter2 = new FeatureFilter( Spot.FRAME, 91d, true );

		final List< FeatureFilter > filters = Arrays.asList( new FeatureFilter[] { filter1, filter2 } );
		sc.filter( filters );
		assertEquals( 3 * 5, sc.getNSpots( true ) );
	}

	@Test
	public void testGetClosestSpot()
	{
		// Filter by QUALITY lower than 20
		final FeatureFilter filter = new FeatureFilter( Spot.QUALITY, 20d, false );
		sc.filter( filter );

		final Spot location = new SpotBase( 50.1, 50.1, 50.1, 1d, -1d );
		for ( final Integer frame : frames )
		{
			// Closest non-visible spot should be the one with QUALITY = 50
			final Spot target1 = sc.getClosestSpot( location, frame, false );
			assertEquals( 50d, target1.getFeature( Spot.QUALITY ), Double.MIN_VALUE );
			// Closest visible spot should be the one with QUALITY = 20
			final Spot target2 = sc.getClosestSpot( location, frame, true );
			assertEquals( 20d, target2.getFeature( Spot.QUALITY ), Double.MIN_VALUE );
		}
	}

	@Test
	public void testGetSpotAt()
	{
		// Filter by QUALITY lower than 20
		final FeatureFilter filter = new FeatureFilter( Spot.QUALITY, 20d, false );
		sc.filter( filter );

		final Spot location1 = new SpotBase( 50.1, 50.1, 50.1, 1d, -1d );
		final Spot location2 = new SpotBase( 10.1, 10.1, 10.1, 1d, -1d );
		for ( final Integer frame : frames )
		{
			// The closest non-visible spot should be the one with QUALITY = 50
			final Spot target1 = sc.getSpotAt( location1, frame, false );
			assertEquals( 50d, target1.getFeature( Spot.QUALITY ), Double.MIN_VALUE );
			/*
			 * Closest visible spot should be the one with QUALITY = 20, but
			 * since it has a radius of 10, it is not within reach of our
			 * search.
			 */
			final Spot target2 = sc.getSpotAt( location1, frame, true );
			assertNull( target2 );
			/*
			 * There are several visible spots that are within radius, but the
			 * one with a QUALITY of 10 is the closest.
			 */
			final Spot target3 = sc.getSpotAt( location2, frame, true );
			assertNotNull( target3 );
			assertEquals( 10d, target3.getFeature( Spot.QUALITY ), Double.MIN_VALUE );
		}
	}

	@Test
	public void testGetNSpots()
	{
		// Filter by QUALITY lower than 20
		final FeatureFilter filter = new FeatureFilter( Spot.QUALITY, 20d, false );
		sc.filter( filter );
		assertEquals( N_SPOTS * N_FRAMES, sc.getNSpots( false ) );
		assertEquals( 21 * N_FRAMES, sc.getNSpots( true ) );
	}

	@Test
	public void testGetNSpotsInt()
	{
		// Filter by QUALITY lower than 20
		final FeatureFilter filter = new FeatureFilter( Spot.QUALITY, 20d, false );
		sc.filter( filter );
		for ( final Integer frame : frames )
		{
			assertEquals( N_SPOTS, sc.getNSpots( frame, false ) );
			assertEquals( 21, sc.getNSpots( frame, true ) );
		}
	}

	@Test
	public void testIterator()
	{
		// Iterate over all
		Iterator< Spot > it = sc.iterator( false );
		int iteratedOver = 0;
		while ( it.hasNext() )
		{
			it.next();
			iteratedOver++;
		}
		assertEquals( N_SPOTS * N_FRAMES, iteratedOver );

		// Iterate over visible
		it = sc.iterator( true );
		iteratedOver = 0;
		while ( it.hasNext() )
		{
			it.next();
			iteratedOver++;
		}
		assertEquals( 0, iteratedOver );

		// Mark 10 spots as visible in eaxh frame
		final int N_MARKED_PER_FRAME = 10;
		final List< Spot > markedSpots = new ArrayList<>( N_MARKED_PER_FRAME * N_FRAMES );
		for ( final Integer frame : frames )
		{
			it = sc.iterator( frame, false );
			for ( int i = 0; i < N_MARKED_PER_FRAME; i++ )
			{
				final Spot spot = it.next();
				markedSpots.add( spot );
				spot.putFeature( SpotCollection.VISIBILITY, SpotCollection.ONE );
			}
		}

		// See if we iterate over them.
		it = sc.iterator( true );
		iteratedOver = 0;
		while ( it.hasNext() )
		{
			final Spot spot = it.next();
			assertTrue( "The spot " + spot + " should be contained in the list of marked spot, but is not.", markedSpots.contains( spot ) );
			iteratedOver++;
		}
		assertEquals( "We should have iterated over " + ( N_MARKED_PER_FRAME * N_FRAMES ) + " marked spots, but have iterated over " + iteratedOver + ".", N_MARKED_PER_FRAME * N_FRAMES, iteratedOver );
	}

	@Test
	public void testIteratorFrame()
	{
		final int targetFrame = frames.get( 0 );
		// Iterate over all
		Iterator< Spot > it = sc.iterator( targetFrame, false );
		int iteratedOver = 0;
		while ( it.hasNext() )
		{
			it.next();
			iteratedOver++;
		}
		assertEquals( N_SPOTS, iteratedOver );
		// Iterate over visible
		it = sc.iterator( targetFrame, true );
		iteratedOver = 0;
		while ( it.hasNext() )
		{
			it.next();
			iteratedOver++;
		}
		assertEquals( 0, iteratedOver );
		// Mark 10 spots as visible
		it = sc.iterator( targetFrame, false );
		final int N_MARKED = 10;
		final List< Spot > markedSpots = new ArrayList<>( N_MARKED );
		for ( int i = 0; i < N_MARKED; i++ )
		{
			final Spot spot = it.next();
			markedSpots.add( spot );
			spot.putFeature( SpotCollection.VISIBILITY, SpotCollection.ONE );
		}
		// See if we iterate over them.
		it = sc.iterator( targetFrame, true );
		iteratedOver = 0;
		while ( it.hasNext() )
		{
			final Spot spot = it.next();
			assertTrue( "The spot " + spot + " should be contained in the list of marked spot, but is not.", markedSpots.contains( spot ) );
			iteratedOver++;
		}
		assertEquals( "We should have iterated over " + N_MARKED + " marked spots, but have iterated over " + iteratedOver + ".", N_MARKED, iteratedOver );
	}

	@Test
	public void testPut()
	{
		// Filter by QUALITY lower than 20
		final FeatureFilter filter = new FeatureFilter( Spot.QUALITY, 20d, false );
		sc.filter( filter );
		// Create a new frame content
		final int N_SPOTS_TO_ADD = 20;
		final HashSet< Spot > spots = new HashSet<>( N_SPOTS_TO_ADD );
		for ( int i = 0; i < N_SPOTS_TO_ADD; i++ )
		{
			spots.add( new SpotBase( -1d, -1d, -1d, 1d, -1d ) );
		}
		// Add it to a new frame
		int targetFrame = 1000;
		sc.put( targetFrame, spots );
		// Check that we updated the number of all spots
		assertEquals( N_FRAMES * N_SPOTS + N_SPOTS_TO_ADD, sc.getNSpots( false ) );
		// But we should not have modified the number of visible spots, as new
		// content is not-visible by default
		assertEquals( N_FRAMES * 21, sc.getNSpots( true ) );
		// Check that all newly added spots have the tight FRAME value
		Iterator< Spot > it = sc.iterator( targetFrame, false );
		while ( it.hasNext() )
		{
			assertEquals( targetFrame, it.next().getFeature( Spot.FRAME ), Double.MIN_VALUE );
		}

		// Replace content of first frame
		targetFrame = frames.get( 0 );
		sc.put( targetFrame, spots );
		// Check that we updated the number of all spots
		assertEquals( ( N_FRAMES - 1 ) * N_SPOTS + 2 * N_SPOTS_TO_ADD, sc.getNSpots( false ) );
		// We modified the number of visible spots, as new content is
		// not-visible by default
		assertEquals( ( N_FRAMES - 1 ) * 21, sc.getNSpots( true ) );
		// Check that all newly added spots have the tight FRAME value
		it = sc.iterator( targetFrame, false );
		while ( it.hasNext() )
		{
			assertEquals( targetFrame, it.next().getFeature( Spot.FRAME ), Double.MIN_VALUE );
		}
	}

	@Test
	public void testFirstKey()
	{
		// First key should be frame 1
		assertEquals( frames.get( 0 ), sc.firstKey() );

		// Create a new frame content
		final int N_SPOTS_TO_ADD = 20;
		final HashSet< Spot > spots = new HashSet<>( N_SPOTS_TO_ADD );
		for ( int i = 0; i < N_SPOTS_TO_ADD; i++ )
		{
			spots.add( new SpotBase( -1d, -1d, -1d, 1d, -1d ) );
		}
		// Add it to a new frame
		final int targetFrame = -1;
		sc.put( targetFrame, spots );

		// First key should be new frame
		assertEquals( targetFrame, sc.firstKey().longValue() );
	}

	@Test
	public void testLastKey()
	{
		// First key should be last frame
		assertEquals( frames.get( N_FRAMES - 1 ), sc.lastKey() );

		// Create a new frame content
		final int N_SPOTS_TO_ADD = 20;
		final HashSet< Spot > spots = new HashSet<>( N_SPOTS_TO_ADD );
		for ( int i = 0; i < N_SPOTS_TO_ADD; i++ )
		{
			spots.add( new SpotBase( -1d, -1d, -1d, 1d, -1d ) );
		}
		// Add it to a new frame
		final int targetFrame = 1000;
		sc.put( targetFrame, spots );

		// Last key should be new frame
		assertEquals( targetFrame, sc.lastKey().longValue() );
	}

	@Test
	public void testKeySet()
	{
		assertArrayEquals( frames.toArray( new Integer[] {} ), sc.keySet().toArray( new Integer[] {} ) );
	}

	private static final boolean isVisible( final Spot spot )
	{
		return spot.getFeature( SpotCollection.VISIBILITY ).compareTo( SpotCollection.ZERO ) > 0;
	}

}
