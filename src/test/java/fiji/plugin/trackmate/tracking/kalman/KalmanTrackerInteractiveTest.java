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
package fiji.plugin.trackmate.tracking.kalman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.jaqaman.SparseLAPFrameToFrameTracker;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.ImageJ;

public class KalmanTrackerInteractiveTest
{
	private static final int NFRAMES = 21;

	private static final double WIDTH = 400;

	private static final int NSINGLETRACKS = 30;

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		final KalmanTrackerInteractiveTest tester = new KalmanTrackerInteractiveTest();

		System.out.println( "Parallel lines with Kalman tracker:" );
		final Model test1a = tester.test( tester.createParallelLines() );
		tester.assertParallel( test1a );

		System.out.println( "\n\n\nParallel lines with LAP tracker:" );
		final Model test1b = tester.testLAP( tester.createParallelLines() );
		tester.assertParallel( test1b );

		System.out.println( "\n\n\nRadial lines with Kalman tracker:" );
		final Model test2a = tester.test( tester.createSpots() );
		tester.assertRadial( test2a );

		System.out.println( "\n\n\nRadial lines with LAP tracker:" );
		final Model test2b = tester.testLAP( tester.createSpots() );
		tester.assertRadial( test2b );
	}

	private void assertRadial( final Model model )
	{

		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{
			final List< Spot > track = new ArrayList<>( model.getTrackModel().trackSpots( trackID ) );
			Collections.sort( track, Spot.frameComparator );

			final Spot first = track.get( 0 );
			final Spot last = track.get( track.size() - 1 );

			final boolean mixed = first.getFeature( Spot.QUALITY ).doubleValue() != last.getFeature( Spot.QUALITY ).doubleValue();
			if ( mixed )
			{
				System.out.println( "Track ID " + trackID + " got confused: it went from spot line " + first.getFeature( Spot.QUALITY ).doubleValue() + " to " + last.getFeature( Spot.QUALITY ).doubleValue() + "." );
			}
			else
			{
				System.out.println( "Track ID " + trackID + " kept its identity, spot line " + first.getFeature( Spot.QUALITY ).doubleValue() + "." );
			}
		}
	}

	private void assertParallel( final Model model )
	{
		int totalChanges = 0;
		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{
			final List< Spot > track = new ArrayList<>( model.getTrackModel().trackSpots( trackID ) );
			Collections.sort( track, Spot.frameComparator );
			final Iterator< Spot > iterator = track.iterator();

			int nChanges = 0;
			double previousQuality = iterator.next().getFeature( Spot.QUALITY ).doubleValue();
			while ( iterator.hasNext() )
			{
				final double quality = iterator.next().getFeature( Spot.QUALITY ).doubleValue();
				if ( quality != previousQuality )
				{
					nChanges++;
				}
				previousQuality = quality;
			}

			if ( nChanges > 0 )
			{
				System.out.println( "Track ID " + trackID + " got confused: it changed " + nChanges + " times of identity." );
			}
			else
			{
				System.out.println( "Track ID " + trackID + " kept its identity." );
			}
			totalChanges += nChanges;
		}
		System.out.println( "In total, " + totalChanges + " changes of identity." );
	}

	private Model testLAP( final SpotCollection spots )
	{
		final double initialSearchRadius = 2 * WIDTH / ( NFRAMES );

		final Map< String, Object > settings = new HashMap<>();
		settings.put( TrackerKeys.KEY_LINKING_MAX_DISTANCE, initialSearchRadius );
		settings.put( TrackerKeys.KEY_LINKING_FEATURE_PENALTIES, Collections.emptyMap() );
		settings.put( TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR, 1.05d );
		final SparseLAPFrameToFrameTracker lap = new SparseLAPFrameToFrameTracker( spots, settings );
		if ( !lap.checkInput() || !lap.process() )
		{
			System.err.println( lap.getErrorMessage() );
		}

		final Model model = new Model();
		model.setSpots( spots, false );
		model.setTracks( lap.getResult(), false );

		final TrackIndexAnalyzer ta = new TrackIndexAnalyzer();
		ta.process( model.getTrackModel().trackIDs( true ), model );

		final DisplaySettings ds = DisplaySettings.defaultStyle().copy();
		ds.setSpotColorBy( TrackMateObject.SPOTS, Spot.QUALITY );
		ds.setTrackColorBy( TrackMateObject.TRACKS, TrackIndexAnalyzer.TRACK_INDEX );
		ds.setSpotShowName( true );

		final SelectionModel selectionModel = new SelectionModel( model );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, ds );
		view.render();

		final TrackScheme trackscheme = new TrackScheme( model, selectionModel, ds );
		trackscheme.render();

		return model;
	}

	private Model test( final SpotCollection spots )
	{
		final double maxSearchRadius = 2 * WIDTH / NFRAMES; // small
		final int maxFrameGap = 2;
		final double initialSearchRadius = 2 * WIDTH / ( NFRAMES );
		final KalmanTracker tracker = new KalmanTracker( spots, maxSearchRadius, maxFrameGap, initialSearchRadius, null );

		if ( !tracker.checkInput() || !tracker.process() )
			System.err.println( tracker.getErrorMessage() );

		final Model model = new Model();
		model.setSpots( spots, false );
		model.setTracks( tracker.getResult(), false );

		final SpotCollection predictions = tracker.getPredictions();
		model.beginUpdate();

		try
		{
			for ( final Integer f : predictions.keySet() )
				for ( final Spot s : predictions.iterable( f, true ) )
					model.addSpotTo( s, f );
		}
		finally
		{
			model.endUpdate();
		}

		final DisplaySettings ds = DisplaySettings.defaultStyle().copy();
		ds.setSpotColorBy( TrackMateObject.SPOTS, Spot.QUALITY );
		ds.setTrackColorBy( TrackMateObject.TRACKS, TrackIndexAnalyzer.TRACK_INDEX );
		ds.setSpotShowName( true );

		final TrackIndexAnalyzer ta = new TrackIndexAnalyzer();
		ta.process( model.getTrackModel().trackIDs( true ), model );

		final SelectionModel selectionModel = new SelectionModel( model );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, ds );
		view.render();

		final TrackScheme trackscheme = new TrackScheme( model, selectionModel, ds );
		trackscheme.render();

		return model;
	}

	private SpotCollection createParallelLines()
	{
		final SpotCollection spots = new SpotCollection();
		final Random ran = new Random( 1l );

		/*
		 * The 4th lonely riders.
		 */
		final double[] x0 = new double[ NSINGLETRACKS ];
		final double[] y0 = new double[ NSINGLETRACKS ];
		final double[] vx0 = new double[ NSINGLETRACKS ];
		final double[] vy0 = new double[ NSINGLETRACKS ];
		for ( int k = 0; k < NSINGLETRACKS; k++ )
		{
			x0[ k ] = WIDTH * k / NSINGLETRACKS;
			y0[ k ] = 0;
			vx0[ k ] = 0;
			vy0[ k ] = WIDTH / ( NFRAMES - 1 );
		}

		final double[] x = x0.clone();
		final double[] y = y0.clone();
		for ( int t = 0; t < NFRAMES; t++ )
		{
			for ( int k = 0; k < y.length; k++ )
			{
				final Spot spot = new SpotBase( x[ k ] + ran.nextGaussian() * WIDTH / 100, y[ k ] + ran.nextGaussian() * WIDTH / 100, 0, 2, k, "T_" + k + "_S_" + t );
				spots.add( spot, t );

				x[ k ] += vx0[ k ];
				y[ k ] += vy0[ k ];
			}
		}
		return spots;
	}

	private SpotCollection createSpots()
	{
		final SpotCollection spots = new SpotCollection();
		final Random ran = new Random( 1l );

		/*
		 * The 4th lonely riders.
		 */
		final double[] x0 = new double[ NSINGLETRACKS ];
		final double[] y0 = new double[ NSINGLETRACKS ];
		final double[] vx0 = new double[ NSINGLETRACKS ];
		final double[] vy0 = new double[ NSINGLETRACKS ];
		for ( int k = 0; k < NSINGLETRACKS; k++ )
		{
			final double angle = 2 * k * Math.PI / NSINGLETRACKS;
			x0[ k ] = WIDTH / 2 * ( 1 + Math.cos( angle ) );
			y0[ k ] = WIDTH / 2 * ( 1 + Math.sin( angle ) );
			vx0[ k ] = -WIDTH / ( NFRAMES - 1 ) * Math.cos( angle );
			vy0[ k ] = -WIDTH / ( NFRAMES - 1 ) * Math.sin( angle );
		}

		final double[] x = x0.clone();
		final double[] y = y0.clone();
		for ( int t = 0; t < NFRAMES; t++ )
		{
			for ( int k = 0; k < y.length; k++ )
			{
				final Spot spot = new SpotBase(
						x[ k ] + ran.nextGaussian() * WIDTH / 200,
						y[ k ] + ran.nextGaussian() * WIDTH / 200,
						0,
						2,
						k,
						"T_" + k + "_S_" + t );
				spots.add( spot, t );

				x[ k ] += vx0[ k ];
				y[ k ] += vy0[ k ];
			}
		}
		return spots;
	}

}
