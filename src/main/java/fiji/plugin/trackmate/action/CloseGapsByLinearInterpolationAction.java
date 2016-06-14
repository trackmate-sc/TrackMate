package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import ij.gui.WaitForUserDialog;
import net.imglib2.RealPoint;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Set;

/**
 * This action allows to close gaps in tracks by creating new intermediate spots which are located at interpolated positions.
 * This is useful if you want to measure signal intensity changing during time, even if the spot is not visible. Thus, trackmate
 * is utilisable for Fluorescence Recovery after Photobleaching (FRAP) analysis.
 *
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG, rhaase@mpi-cbg.de
 *
 * Date: June 2016
 *
 */
public class CloseGapsByLinearInterpolationAction extends AbstractTMAction {


    public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/spot_icon.png"));
    public static final String NAME = "Close gaps by introducing new spots";

    public static final String KEY = "CLOSE_GAPS_BY_LINEAR_INPERPOLATION";
    public static final String INFO_TEXT = "<html>" +
            "This action closes gaps in tracks by introducing new spots. The spots positions and size are calculated using linear interpolation." +
            "</html>";

    @Override
    public void execute(final TrackMate trackmate) {
        final Model model = trackmate.getModel();


        Set<Integer> trackIDs = model.getTrackModel().trackIDs(true);

        // go through all tracks
        for (Integer trackID : trackIDs)
        {
            Set<Spot> spots = model.getTrackModel().trackSpots(trackID);
            if (spots != null) {
                model.beginUpdate();

                // go through all spots, search for gaps
                for (Spot currentSpot : spots) {
                    int currentFrame = currentSpot.getFeatures().get("FRAME").intValue();

                    Spot nextSpot = null;
                    int nextFrame = 0;

                    // find the next following spot
                    for (Spot futureSpot : spots) {
                        int futureFrame = futureSpot.getFeatures().get("FRAME").intValue();
                        if (futureFrame > currentFrame) {
                            if (nextSpot == null || nextFrame > futureFrame) {
                                nextSpot = futureSpot;
                                nextFrame = futureFrame;
                            }
                        }
                    }

                    // if a following spot was found and there is a gap between current and next
                    if (nextSpot != null && (nextFrame - currentFrame > 1)) {

                        double[] currentPosition = new double[3];
                        double[] nextPosition = new double[3];

                        nextSpot.localize(nextPosition);
                        currentSpot.localize(currentPosition);

                        model.removeEdge(currentSpot, nextSpot);

                        Spot formerSpot = currentSpot;
                        for (int f = currentFrame + 1; f < nextFrame; f++) {
                            double weight = (double) (nextFrame - f) / (nextFrame - currentFrame);


                            double[] position = new double[3];
                            for (int d = 0; d < currentSpot.numDimensions(); d++) {
                                position[d] = weight * currentPosition[d] + (1.0 - weight) * nextPosition[d];
                            }

                            RealPoint rp = new RealPoint(position);

                            Spot newSpot = new Spot(rp, 0, 0);

                            // Set some properties of the new spot
                            interpolateFeature(newSpot, currentSpot, nextSpot, weight, "RADIUS");
                            interpolateFeature(newSpot, currentSpot, nextSpot, weight, "QUALITY");
                            interpolateFeature(newSpot, currentSpot, nextSpot, weight, "POSITION_T");
                            newSpot.getFeatures().put("TRACK_ID", trackID.doubleValue());
                            newSpot.getFeatures().put("FRAME", (double) f);

                            model.addSpotTo(newSpot, f);
                            model.addEdge(formerSpot, newSpot, 1.0);
                            formerSpot = newSpot;
                        }
                    }
                }
                model.endUpdate();
            }
        }
    }

    private void interpolateFeature(Spot targetSpot, Spot spot1, Spot spot2, double weight, String feature)
    {
        if  (targetSpot.getFeatures().containsKey(feature)) {
            targetSpot.getFeatures().remove(feature);
        }

        targetSpot.getFeatures().put(feature, weight * spot1.getFeature(feature) + (1.0 - weight) * spot2.getFeature(feature));
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
