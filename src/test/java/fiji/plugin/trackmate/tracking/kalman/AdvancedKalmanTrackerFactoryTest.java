package fiji.plugin.trackmate.tracking.kalman;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.SpotTracker;

public class AdvancedKalmanTrackerFactoryTest {

    @Test
    public void testCreate() {
        // Creating necessary mock objects
        Map<String, Object> settings = new HashMap<>();
        SpotCollection spots = mock(SpotCollection.class);

        // Creating AdvancedKalmanTrackerFactory instance
        AdvancedKalmanTrackerFactory factory = new AdvancedKalmanTrackerFactory();

        // Testing create method
        SpotTracker tracker = factory.create(spots, settings);
        assertNotNull(tracker);
        assertTrue(tracker instanceof AdvancedKalmanTracker);
    }

        
      
       // Returns False if the input settings map is empty.
    @Test
    public void test_empty_map() {
        Map<String, Object> settings = new HashMap<>();
        boolean result = new AdvancedKalmanTrackerFactory().checkSettingsValidity(settings);
        assertFalse(result);
    }

}
