package fiji.plugin.trackmate.action.closegaps;

import java.util.ArrayList;
import java.util.Collection;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.closegaps.GapClosingMethod;
import fiji.plugin.trackmate.action.closegaps.CloseGapsByLinearInterpolation;
import fiji.plugin.trackmate.action.closegaps.CloseGapsByDetection;


public class CloseGapsManager {

    private final Collection<GapClosingMethod> gapClosingMethods;

    public CloseGapsManager() {
        this.gapClosingMethods = new ArrayList<>();
        gapClosingMethods.add(new CloseGapsByLinearInterpolation());
        gapClosingMethods.add(new CloseGapsByDetection());
    }

    public Collection<GapClosingMethod> getGapClosingMethods() {
        return gapClosingMethods;
    }

    public void runGapClosingMethod(final GapClosingMethod gapClosingMethod, final TrackMate trackmate, final Logger logger) {
        logger.log("Applying gap-closing method: " + gapClosingMethod.toString() + ".\n");
        gapClosingMethod.execute(trackmate, logger);
        logger.log("Gap-closing done.\n");
    }
}
