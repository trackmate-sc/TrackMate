/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2022 TrackMate developers.
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
package fiji.plugin.trackmate.action.closegaps;

import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import net.imglib2.RealPoint;

/**
 * This action allows to close gaps in tracks by creating new intermediate spots
 * which are located at interpolated positions. This is useful if you want to
 * measure signal intensity changing during time, even if the spot is not
 * visible. Thus, TrackMate is usable for Fluorescence Recovery after
 * Photobleaching (FRAP) analysis.
 *
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG,
 * rhaase@mpi-cbg.de
 *
 * Date: June 2016
 *
 */
public class CloseGapsByLinearInterpolationAction implements GapClosingMethod
{

	public static final String INFO_TEXT = "<html>This action allows to close gaps in tracks by intercalating "
			+ "new spots in gaps which position is interpolated from the positions of the true spots "
			+ "at the gap beginning and end. The image data is not used to adjust the position of spots.</html>";

	public static final String NAME = "Close gaps by interpolating positions";

	@Override
	public void execute( final TrackMate trackmate, final Logger logger )
	{
		final Model model = trackmate.getModel();
		final TrackModel trackModel = model.getTrackModel();
		boolean changed = true;

		model.beginUpdate();
		try
		{

			while ( changed )
			{
				changed = false;

				/*
				 * Got through all edges, check if the frame distance between
				 * spots is larger than 1.
				 */
				final Set< DefaultWeightedEdge > edges = model.getTrackModel().edgeSet();
				for ( final DefaultWeightedEdge edge : edges )
				{
					final Spot currentSpot = trackModel.getEdgeSource( edge );
					final Spot nextSpot = trackModel.getEdgeTarget( edge );

					final int currentFrame = currentSpot.getFeature( Spot.FRAME ).intValue();
					final int nextFrame = nextSpot.getFeature( Spot.FRAME ).intValue();

					if ( Math.abs( nextFrame - currentFrame ) > 1 )
					{
						final int presign = nextFrame > currentFrame ? 1 : -1;

						final double[] currentPosition = new double[ 3 ];
						final double[] nextPosition = new double[ 3 ];

						nextSpot.localize( nextPosition );
						currentSpot.localize( currentPosition );

						/*
						 * Create new spots in between; interpolate coordinates
						 * and some features.
						 */
						Spot formerSpot = currentSpot;
						int nspots = 0;
						for ( int f = currentFrame + presign; ( f < nextFrame && presign == 1 )
								|| ( f > nextFrame && presign == -1 ); f += presign )
						{
							final double weight = ( double ) ( nextFrame - f ) / ( nextFrame - currentFrame );

							final double[] position = new double[ 3 ];
							for ( int d = 0; d < currentSpot.numDimensions(); d++ )
								position[ d ] = weight * currentPosition[ d ] + ( 1.0 - weight ) * nextPosition[ d ];

							final RealPoint rp = new RealPoint( position );
							final Spot newSpot = new Spot( rp, 0, 0 );

							// Set some properties of the new spot
							interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.RADIUS );
							interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.QUALITY );
							interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.POSITION_T );

							model.addSpotTo( newSpot, f );
							model.addEdge( formerSpot, newSpot, 1.0 );
							formerSpot = newSpot;
							nspots++;
						}
						model.addEdge( formerSpot, nextSpot, 1.0 );
						model.removeEdge( currentSpot, nextSpot );
						logger.log( "Added " + nspots + " new spots between spots " + currentSpot + " and " + nextSpot + ".\n" );

						/*
						 * Restart search to prevent
						 * ConcurrentModificationException.
						 */
						changed = true;
						break;
					}
				}
			}
		}
		finally
		{
			model.endUpdate();
		}
	}

	private void interpolateFeature( final Spot targetSpot, final Spot spot1, final Spot spot2, final double weight, final String feature )
	{
		if ( targetSpot.getFeatures().containsKey( feature ) )
			targetSpot.getFeatures().remove( feature );

		targetSpot.getFeatures().put( feature,
				weight * spot1.getFeature( feature ) + ( 1.0 - weight ) * spot2.getFeature( feature ) );
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String toString()
	{
		return NAME;
	}
}
