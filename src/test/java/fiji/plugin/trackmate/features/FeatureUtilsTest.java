package fiji.plugin.trackmate.features;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import fiji.plugin.trackmate.Model;

import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;

public class FeatureUtilsTest {


    @Test
    public void testCollectFeatureValuesForEdges() {
        Model model = FeatureUtils.DUMMY_MODEL;
        double[] values = FeatureUtils.collectFeatureValues("SomeFeature", TrackMateObject.EDGES, model, true);
        assertNotNull(values);
    }

}
