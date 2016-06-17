package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import ij.gui.WaitForUserDialog;
import net.imglib2.RealPoint;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Iterator;
import java.util.Set;

/**
 * This action allows to close gaps in tracks by creating new intermediate spots
 * which are located at interpolated positions. This is useful if you want to
 * measure signal intensity changing during time, even if the spot is not
 * visible. Thus, trackmate is utilisable for Fluorescence Recovery after
 * Photobleaching (FRAP) analysis.
 *
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG,
 * rhaase@mpi-cbg.de
 *
 * Date: June 2016
 *
 */
public class CloseGapsByLinearInterpolationAction extends AbstractTMAction
{

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/spot_icon.png" ) );

	public static final String NAME = "Close gaps by introducing new spots";

	public static final String KEY = "CLOSE_GAPS_BY_LINEAR_INPERPOLATION";

	public static final String INFO_TEXT = "<html>" +
			"This action closes gaps in tracks by introducing new spots. The spots positions and size are calculated using linear interpolation." +
			"</html>";

	@Override
	public void execute( final TrackMate trackmate )
	{
		final Model model = trackmate.getModel();

		TrackModel trackModel = model.getTrackModel();

		boolean changed = true;

		while ( changed )
		{
			changed = false;

			// Got through all edges, check if the frame distance between spots
			// is larger than 1
			Set< DefaultWeightedEdge > edges = model.getTrackModel().edgeSet();
			for ( DefaultWeightedEdge edge : edges )
			{
				Spot currentSpot = trackModel.getEdgeSource( edge );
				Spot nextSpot = trackModel.getEdgeTarget( edge );

				int currentFrame = currentSpot.getFeature( Spot.FRAME ).intValue();
				int nextFrame = nextSpot.getFeature( Spot.FRAME ).intValue();

				if ( nextSpot != null && ( nextFrame - currentFrame > 1 ) )
				{
					model.beginUpdate();

					double[] currentPosition = new double[ 3 ];
					double[] nextPosition = new double[ 3 ];

					nextSpot.localize( nextPosition );
					currentSpot.localize( currentPosition );

					model.removeEdge( currentSpot, nextSpot );

					// create new spots in between; interpolate coordinates and
					// some features
					Spot formerSpot = currentSpot;
					for ( int f = currentFrame + 1; f < nextFrame; f++ )
					{
						double weight = ( double ) ( nextFrame - f ) / ( nextFrame - currentFrame );

						double[] position = new double[ 3 ];
						for ( int d = 0; d < currentSpot.numDimensions(); d++ )
						{
							position[ d ] = weight * currentPosition[ d ] + ( 1.0 - weight ) * nextPosition[ d ];
						}

						RealPoint rp = new RealPoint( position );

						Spot newSpot = new Spot( rp, 0, 0 );

						// Set some properties of the new spot
						interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.RADIUS );
						interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.QUALITY );
						interpolateFeature( newSpot, currentSpot, nextSpot, weight, Spot.POSITION_T );

						model.addSpotTo( newSpot, f );
						model.addEdge( formerSpot, newSpot, 1.0 );
						formerSpot = newSpot;
					}
					model.addEdge( formerSpot, nextSpot, 1.0 );
					model.endUpdate();

					// Restart search to prevent ConcurrentModificationException
					changed = true;
					break;
				}
			}
		}
	}

	private void interpolateFeature( Spot targetSpot, Spot spot1, Spot spot2, double weight, String feature )
	{
		if ( targetSpot.getFeatures().containsKey( feature ) )
		{
			targetSpot.getFeatures().remove( feature );
		}

		targetSpot.getFeatures().put( feature, weight * spot1.getFeature( feature ) + ( 1.0 - weight ) * spot2.getFeature( feature ) );
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}

		@Override
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new CloseGapsByLinearInterpolationAction();
		}
	}

}
