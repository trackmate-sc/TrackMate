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
package fiji.plugin.trackmate.action.closegaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.util.TMUtils;
import ij.gui.Roi;
import net.imglib2.RealPoint;

public interface GapClosingMethod
{

	public static class GapClosingParameter
	{

		public final String name;

		public double value;

		public final double minValue;

		public final double maxValue;

		public GapClosingParameter( final String name, final double value, final double minValue, final double maxValue )
		{
			this.name = name;
			this.value = value;
			this.minValue = minValue;
			this.maxValue = maxValue;
		}
	}

	/**
	 * Returns the list of parameters required to configure this method.
	 * <p>
	 * The list will be used to autogenerate a configuration panel.
	 * 
	 * @return a list of parameters.
	 */
	public default List< GapClosingParameter > getParameters()
	{
		return Collections.emptyList();
	}

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
	 *            the trackmate instance to operate on.
	 * @param logger
	 *            a logger instance to echoes the gap-closing process.
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
		// Only inspect visible tracks.
		for ( final Integer trackID : trackModel.trackIDs( true ) )
		{
			final Set< DefaultWeightedEdge > edges = trackModel.trackEdges( trackID );
			for ( final DefaultWeightedEdge edge : edges )
			{
				final Spot source = trackModel.getEdgeSource( edge );
				final int st = source.getFeature( Spot.FRAME ).intValue();
				final Spot target = trackModel.getEdgeTarget( edge );
				final int tt = target.getFeature( Spot.FRAME ).intValue();

				if ( Math.abs( tt - st ) > 1 )
					gaps.add( edge );
			}

		}

		return gaps;
	}

	/**
	 * Returns a list of newly created spots obtained by interpolating the
	 * position, radius, quality and time features of the source and target spot
	 * of the specified edge.
	 * <p>
	 * The list is not empty only if the edge bridges a gap, or if the source
	 * and target spots are separated by strictly more than 1 frame.
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
			final Spot newSpot = new SpotBase( rp, 0, 0 );
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

	/**
	 * Returns a new {@link Settings} object copied from the specified settings,
	 * but configured with a small ROI centered on the specified spot, with a
	 * radius proportional to the radius of the specified spot, and set to
	 * operate only on the frame in which the specified spot it.
	 * 
	 * @param spot
	 *            the spot to read the coordinates and the frame from.
	 * @param neighborhoodFactor
	 *            the ROI proportionality factor. The ROI radius will be equal
	 *            to the spot radius times this factor.
	 * @param settings
	 *            a source settings to copy from. Its {@link Settings#imp} field
	 *            must not be <code>null</code>.
	 * @return a new {@link Settings} object.
	 */
	public static Settings makeSettingsForRoiAround( final Spot spot, final double neighborhoodFactor, final Settings settings )
	{
		// Extract scales.
		final double[] cal = TMUtils.getSpatialCalibration( settings.imp );
		final double dx = cal[ 0 ];
		final double dy = cal[ 1 ];
		final double dz = cal[ 2 ];

		// Extract source coords.
		final double[] location = new double[ 3 ];
		spot.localize( location );
		final double radius = spot.getFeature( Spot.RADIUS );

		final long x = Math.round( location[ 0 ] / dx );
		final long y = Math.round( location[ 1 ] / dy );
		final long z = Math.round( location[ 2 ] / dz );
		final long r = ( long ) Math.ceil( neighborhoodFactor * radius / dx );
		final long rz = ( long ) Math.abs( Math.ceil( neighborhoodFactor * radius / dz ) );

		// Extract crop cube
		final long width = settings.imp.getWidth();
		final long height = settings.imp.getHeight();
		final long x0 = Math.max( 0, x - r );
		final long y0 = Math.max( 0, y - r );
		final long x1 = Math.min( width - 1, x + r );
		final long y1 = Math.min( height - 1, y + r );

		// Make a new settings with a smaller ROI.
		final Settings settingsCopy = settings.copyOn( settings.imp );
		settingsCopy.setRoi( new Roi( x0, y0, x1 - x0, y1 - y0 ) );
		// Time.
		final int t = spot.getFeature( Spot.FRAME ).intValue();
		settingsCopy.tstart = t;
		settingsCopy.tend = t;

		if ( !DetectionUtils.is2D( settings.imp ) )
		{
			// 3D
			final long depth = settings.imp.getNSlices();
			final long z0 = Math.max( 0, z - rz );
			final long z1 = Math.min( depth - 1, z + rz );
			settingsCopy.zstart = ( int ) z0;
			settingsCopy.zend = ( int ) z1;
		}
		return settingsCopy;
	}

	public static int countMissingSpots( final Collection< DefaultWeightedEdge > gaps, final Model model )
	{
		int nSpots = 0;
		for ( final DefaultWeightedEdge gap : gaps )
			nSpots += countMissingSpots( gap, model );
		return nSpots;
	}

	public static int countMissingSpots( final DefaultWeightedEdge gap, final Model model )
	{
		final TrackModel trackModel = model.getTrackModel();
		final Spot source = trackModel.getEdgeSource( gap );
		final int st = source.getFeature( Spot.FRAME ).intValue();
		final Spot target = trackModel.getEdgeTarget( gap );
		final int tt = target.getFeature( Spot.FRAME ).intValue();
		return Math.abs( tt - st ) - 1;
	}

}
