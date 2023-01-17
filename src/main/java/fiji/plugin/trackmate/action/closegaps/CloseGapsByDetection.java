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

import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import net.imglib2.util.Util;

/**
 * This action allows to close gaps in tracks by executing detection in the
 * vicinity of position that are linearly interpolated.
 * 
 * @author Jean-Yves Tinevez
 */
public class CloseGapsByDetection implements GapClosingMethod
{

	public static final String INFO_TEXT = "<html>"
			+ "This method allows to close gaps in tracks by executing a detection "
			+ "in the vicinity of linearly interpolated position over a gap."
			+ "<p>"
			+ "The same detector and detection settings than for the initial detection step are used. "
			+ "The search volume is built by taking a box of twice the interpolated radius of the "
			+ "source and target spots at the gap."
			+ "<p>"
			+ "This method is best if there is a single spot near a missed detection that could "
			+ "be recognized by the detection process executed in a smaller region. It is well "
			+ "suited to be used with detectors that use a normalized threshold, like the Hessian "
			+ "detector."
			+ "</html>";

	public static final String NAME = "Close gaps by detection";

	/**
	 * Relative size of the seach radius, in spot radius units.
	 */
	private static final double SEARCH_RADIUS_FACTOR = 2.;

	@Override
	public void execute( final TrackMate trackmate, final Logger logger )
	{
		final Model model = trackmate.getModel();
		final TrackModel trackModel = model.getTrackModel();

		// Copy settings & make a new TrackMate.

		model.beginUpdate();
		try
		{
			final List< DefaultWeightedEdge > gaps = GapClosingMethod.getAllGaps( model );
			int progress = 0;
			final int nTasks = GapClosingMethod.countMissingSpots( gaps, model );
			for ( final DefaultWeightedEdge gap : gaps )
			{
				// Interpolate position.
				final List< Spot > spots = GapClosingMethod.interpolate( model, gap );
				final Spot source = trackModel.getEdgeSource( gap );
				Spot current = source;
				for ( final Spot spot : spots )
				{
					logger.setProgress( ( double ) ( progress++ ) / nTasks );

					final int t = spot.getFeature( Spot.FRAME ).intValue();

					// Re-execute detection.
					final Settings settings = GapClosingMethod.makeSettingsForRoiAround( spot, SEARCH_RADIUS_FACTOR, trackmate.getSettings() );
					final TrackMate localTM = new TrackMate( settings );
					localTM.getModel().setLogger( Logger.VOID_LOGGER );
					localTM.setNumThreads( 1 );
					if ( !localTM.execDetection() )
					{
						logger.error( "Error detecting spots around position " + Util.printCoordinates( source )
								+ " at frame " + t + ":\n"
								+ localTM.getErrorMessage() );
						continue;
					}
					// Did we find something?
					Spot candidate = null;
					for ( final Spot s : localTM.getModel().getSpots().iterable( t, false ) )
					{
						if ( candidate == null || s.diffTo( candidate, Spot.QUALITY ) > 0 )
							candidate = s;
					}
					if ( candidate == null )
					{
						logger.log( "Could not find a suitable spot around position " + Util.printCoordinates( source )
								+ " at frame " + t + ".\n" );
						continue;
					}
					// Add new spot to the model.
					model.addSpotTo( candidate, candidate.getFeature( Spot.FRAME ).intValue() );
					model.addEdge( current, candidate, 1.0 );
					current = candidate;
				}
				final Spot target = trackModel.getEdgeTarget( gap );
				model.addEdge( current, target, 1.0 );
				model.removeEdge( source, target );
				logger.log( "Added " + spots.size() + " new spots between spots " + source + " and " + target + ".\n" );
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
