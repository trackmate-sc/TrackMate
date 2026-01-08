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
package fiji.plugin.trackmate.detection.semiauto;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class SemiAutoTracker< T extends RealType< T > & NativeType< T > > extends AbstractSemiAutoTracker< T >
{

	protected final ImgPlus< T > img;

	private final double dt;

	private final ImagePlus imp;

	public SemiAutoTracker( final Model model, final SelectionModel selectionModel, final ImagePlus imp, final Logger logger )
	{
		super( model, selectionModel, logger );
		this.imp = imp;
		this.img = TMUtils.rawWraps( imp );
		final double ldt = imp.getCalibration().frameInterval;
		if ( ldt == 0 )
		{
			dt = 1d;
		}
		else
		{
			dt = ldt;
		}
	}

	@Override
	protected SearchRegion< T > getNeighborhood( final Spot spot, final int frame )
	{
		final double radius = spot.getFeature( Spot.RADIUS );

		/*
		 * Source, rai and transform
		 */

		final int tindex = img.dimensionIndex( Axes.TIME );
		final int cindex = img.dimensionIndex( Axes.CHANNEL );
		if ( frame >= img.dimension( tindex ) )
		{
			logger.log( "Spot: " + spot + ": No more time-points.\n" );
			return null;
		}

		/*
		 * Extract scales
		 */

		final double[] cal = TMUtils.getSpatialCalibration( img );
		final double dx = cal[ 0 ];
		final double dy = cal[ 1 ];
		final double dz = cal[ 2 ];

		/*
		 * Determine neighborhood size
		 */

		final double neighborhoodFactor = Math.max( NEIGHBORHOOD_FACTOR, distanceTolerance + 1 );

		/*
		 * Extract source coords
		 */

		final double[] location = new double[ 3 ];
		spot.localize( location );

		final long x = Math.round( location[ 0 ] / dx );
		final long y = Math.round( location[ 1 ] / dy );
		final long z = Math.round( location[ 2 ] / dz );
		final long r = ( long ) Math.ceil( neighborhoodFactor * radius / dx );
		final long rz = ( long ) Math.abs( Math.ceil( neighborhoodFactor * radius / dz ) );

		/*
		 * Extract crop cube
		 */

		final int targetChannel = imp.getC() - 1;

		final long width = img.dimension( 0 );
		final long height = img.dimension( 1 );
		final long x0 = Math.max( 0, x - r );
		final long y0 = Math.max( 0, y - r );
		final long x1 = Math.min( width - 1, x + r );
		final long y1 = Math.min( height - 1, y + r );

		final long[] min;
		final long[] max;
		if ( img.dimensionIndex( Axes.Z ) >= 0 )
		{
			// 3D
			final long depth = img.dimension( img.dimensionIndex( Axes.Z ) );
			final long z0 = Math.max( 0, z - rz );
			final long z1 = Math.min( depth - 1, z + rz );
			min = new long[] { x0, y0, z0 };
			max = new long[] { x1, y1, z1 };
		}
		else
		{
			// 2D
			min = new long[] { x0, y0 };
			max = new long[] { x1, y1 };
		}
		final FinalInterval interval = new FinalInterval( min, max );

		/*
		 * The transform that will put back the global coordinates. In our case
		 * it is just the identity.
		 */

		final AffineTransform3D transform = new AffineTransform3D();

		final SearchRegion< T > sn = new SearchRegion<>();
		RandomAccessible< T > source = img;
		if ( tindex >= 0 )
			source = Views.hyperSlice( source, tindex, frame );

		if ( cindex >= 0 )
			source = Views.hyperSlice( source, cindex, targetChannel );

		sn.source = source;
		sn.transform = transform;
		sn.interval = interval;
		sn.calibration = cal;

		return sn;
	}

	@Override
	protected void exposeSpot( final Spot newSpot, final Spot previousSpot )
	{
		final int frame = previousSpot.getFeature( Spot.FRAME ).intValue() + 1;
		newSpot.putFeature( Spot.POSITION_T, frame * dt );
	}

}
