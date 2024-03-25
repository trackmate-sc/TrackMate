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

import java.io.File;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.ImageJ;
import ij.ImagePlus;
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
	 * Relative size of the search radius, in spot radius units.
	 */
	private final GapClosingParameter radiusFactor;

	public CloseGapsByDetection()
	{
		this.radiusFactor = new GapClosingParameter( "Radius factor", 2., 0.1, 10. );
	}

	@Override
	public List< GapClosingParameter > getParameters()
	{
		return Collections.singletonList( radiusFactor );
	}

	@Override
	public void execute( final TrackMate trackmate, final Logger logger )
	{
		final Model model = trackmate.getModel();
		final TrackModel trackModel = model.getTrackModel();

		// Copy settings & make a new TrackMate.

		model.beginUpdate();
		try
		{
			final ArrayDeque< DefaultWeightedEdge > gaps = new ArrayDeque<>( GapClosingMethod.getAllGaps( model ) );

			int progress = 0;
			final int nTasks = GapClosingMethod.countMissingSpots( gaps, model );

			while ( !gaps.isEmpty() )
			{
				final DefaultWeightedEdge gap = gaps.poll();

				// Interpolate position.
				final List< Spot > spots = GapClosingMethod.interpolate( model, gap );
				final Spot source = trackModel.getEdgeSource( gap );
				for ( final Spot spot : spots )
				{
					logger.setProgress( ( double ) ( progress++ ) / nTasks );

					final int t = spot.getFeature( Spot.FRAME ).intValue();

					// Re-execute detection.
					final Settings settings = GapClosingMethod.makeSettingsForRoiAround(
							spot,
							radiusFactor.value,
							trackmate.getSettings() );
					final TrackMate localTM = new TrackMate( settings );
					localTM.getModel().setLogger( Logger.VOID_LOGGER );
					localTM.setNumThreads( trackmate.getNumThreads() );
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
						logger.log( "Could not find a suitable spot around position " + Util.printCoordinates( spot )
								+ " at frame " + t + ".\n" );
						continue;
					}

					// Add new spot to the model.
					model.addSpotTo( candidate, t );

					// Add link from source to candidate.
					final DefaultWeightedEdge sourceCandidateEdge = model.addEdge( source, candidate, 1.0 );

					// Add link from candidate to target.
					final Spot target = trackModel.getEdgeTarget( gap );
					final DefaultWeightedEdge candidateTargetEdge = model.addEdge( candidate, target, 1.0 );

					// Remove old edge.
					model.removeEdge( gap );
					
					// Should we re-add the new edges?
					if ( GapClosingMethod.countMissingSpots( sourceCandidateEdge, model ) > 1 )
						gaps.push( sourceCandidateEdge );
					if ( GapClosingMethod.countMissingSpots( candidateTargetEdge, model ) > 1 )
						gaps.push( candidateTargetEdge );

					// Reiterate.
					break;
				}
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

	public static void main( final String[] args )
	{
		ImageJ.main( args );

//		final String filePath = "/Users/tinevez/Desktop/GaelleGapClosingWeirdBug/Simple2-211116MovieFDBYFP_Movie0-01-Scene-64-TR114.xml";
		final String filePath = "/Users/tinevez/Desktop/GaelleGapClosingWeirdBug/211116 Movie FDB YFP_Movie 0-01-Scene-64-TR114-zeroed.xml";
		final TmXmlReader reader = new TmXmlReader( new File( filePath ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}
		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		final Settings settings = reader.readSettings( imp );
		final TrackMate trackmate = new TrackMate( model, settings );
		trackmate.setNumThreads( 5 );

		settings.detectorSettings.put( DetectorKeys.KEY_RADIUS, 8. );

		final CloseGapsByDetection gapCloser = new CloseGapsByDetection();
		gapCloser.getParameters().get( 0 ).value = 2.;
		gapCloser.execute( trackmate, Logger.DEFAULT_LOGGER );

		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		ds.setSpotColorBy( TrackMateObject.TRACKS, TrackIndexAnalyzer.TRACK_INDEX );
		ds.setTrackColorBy( TrackMateObject.TRACKS, TrackIndexAnalyzer.TRACK_INDEX );

		new TrackScheme( model, selectionModel, ds ).render();
		new HyperStackDisplayer( model, selectionModel, settings.imp, ds ).render();
	}
}
