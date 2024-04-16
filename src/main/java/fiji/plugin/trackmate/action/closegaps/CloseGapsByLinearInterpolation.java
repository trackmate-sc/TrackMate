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

import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;

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
public class CloseGapsByLinearInterpolation implements GapClosingMethod
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

		model.beginUpdate();
		try
		{
			final List< DefaultWeightedEdge > gaps = GapClosingMethod.getAllGaps( model );
			int progress = 0;
			for ( final DefaultWeightedEdge gap : gaps )
			{
				final List< Spot > spots = GapClosingMethod.interpolate( model, gap );
				final Spot source = trackModel.getEdgeSource( gap );
				Spot current = source;
				for ( final Spot spot : spots )
				{
					model.addSpotTo( spot, spot.getFeature( Spot.FRAME ).intValue() );
					model.addEdge( current, spot, 1.0 );
					current = spot;
				}
				final Spot target = trackModel.getEdgeTarget( gap );
				model.addEdge( current, target, 1.0 );
				model.removeEdge( source, target );
				logger.log( "Added " + spots.size() + " new spots between spots " + source + " and " + target + ".\n" );
				logger.setProgress( ( double ) ( progress++ ) / gaps.size() );
			}
		}
		finally
		{
			model.endUpdate();
		}
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
