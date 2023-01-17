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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import net.imglib2.RealPoint;

public interface GapClosingMethod
{

	/**
	 * Returns a html string containing a descriptive information about this
	 * module.
	 *
	 * @return a html string.
	 */
	public String getInfoText();

	/**
	 * Performs the gap closing.
	 * 
	 * @param trackmate
	 * @param logger
	 */
	public void execute( TrackMate trackmate, Logger logger );

	/**
	 * Utility method that can return all the edges of a model that have a gap.
	 * <p>
	 * A gap corresponds to spots that are missing in one or several consecutive
	 * frames within a track. Gaps are returned as a list of edges. Each edge in
	 * this list has a source spot and a target spot separated by strictly more
	 * than 1 frame.
	 * 
	 * @param model
	 *            the model to search for gaps.
	 * @return a list of edges corresponding to gaps.
	 */
	public static List< DefaultWeightedEdge > getAllGaps( final Model model )
	{
		final List< DefaultWeightedEdge > gaps = new ArrayList<>();
		final TrackModel trackModel = model.getTrackModel();
		final Set< DefaultWeightedEdge > edges = trackModel.edgeSet();
		for ( final DefaultWeightedEdge edge : edges )
		{
			final Spot source = trackModel.getEdgeSource( edge );
			final int st = source.getFeature( Spot.FRAME ).intValue();
			final Spot target = trackModel.getEdgeTarget( edge );
			final int tt = target.getFeature( Spot.FRAME ).intValue();

			if ( Math.abs( tt - st ) > 1 )
				gaps.add( edge );
		}
		return gaps;
	}

	/**
	 * Returns a list of newly created spots obtained by interpolating the
	 * position, radius, quality and time features of the source and target spot
	 * of the specified edge.
	 * <p>
	 * The list is not empty only the edge bridges a gap, or if the source and
	 * target spots are separated by strictly more than 1 frame.
	 * <p>
	 * In the list, the spots are <b>not</b> added to the model and are <b>not
	 * linked</b> be edges. The spots are added in time order, from the frame
	 * just after the source spot, to the frame just before the target spot. The
	 * source and target spot are not in the list.
	 * 
	 * @param model
	 *            the model.
	 * @param edge
	 *            the edge to interpolate.
	 * @return a new list of spots.
	 */
	public static List< Spot > interpolate( final Model model, final DefaultWeightedEdge edge )
	{
		final TrackModel trackModel = model.getTrackModel();

		final Spot source = trackModel.getEdgeSource( edge );
		final double[] sPos = new double[ 3 ];
		source.localize( sPos );
		final int st = source.getFeature( Spot.FRAME ).intValue();

		final Spot target = trackModel.getEdgeTarget( edge );
		final double[] tPos = new double[ 3 ];
		target.localize( tPos );
		final int tt = target.getFeature( Spot.FRAME ).intValue();
		
		final List< Spot > interpolatedSpots = new ArrayList<>( Math.abs( tt - st ) - 1 );

		final int presign = tt > st ? 1 : -1;
		for ( int f = st + presign; ( f < tt && presign == 1 )
				|| ( f > tt && presign == -1 ); f += presign )
		{
			final double weight = ( double ) ( tt - f ) / ( tt - st );

			final double[] position = new double[ 3 ];
			for ( int d = 0; d < 3; d++ )
				position[ d ] = weight * sPos[ d ] + ( 1.0 - weight ) * tPos[ d ];

			final RealPoint rp = new RealPoint( position );
			final Spot newSpot = new Spot( rp, 0, 0 );
			newSpot.putFeature( Spot.FRAME, Double.valueOf( f ) );

			// Set some properties of the new spot
			interpolateFeature( newSpot, source, target, weight, Spot.RADIUS );
			interpolateFeature( newSpot, source, target, weight, Spot.QUALITY );
			interpolateFeature( newSpot, source, target, weight, Spot.POSITION_T );

			interpolatedSpots.add( newSpot );
		}
		return interpolatedSpots;
	}

	static void interpolateFeature( final Spot targetSpot, final Spot spot1, final Spot spot2, final double weight, final String feature )
	{
		if ( targetSpot.getFeatures().containsKey( feature ) )
			targetSpot.getFeatures().remove( feature );

		targetSpot.getFeatures().put( feature,
				weight * spot1.getFeature( feature ) + ( 1.0 - weight ) * spot2.getFeature( feature ) );
	}
}
