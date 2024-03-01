package fiji.plugin.trackmate.graph;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;


public class FromContinousBranchesTest {

    // Returns true if branches and links are not null and links are made of two spots.
    @Test
    public void test_behaviour_true() {
        FromContinuousBranches obj = new FromContinuousBranches(new ArrayList<>(), new ArrayList<>());
        boolean result = obj.checkInput();
        assertTrue(result);
    }

    // Returns false if branches is null.
    @Test
    public void test_behaviour_false() {
        FromContinuousBranches obj = new FromContinuousBranches(null, new ArrayList<>());
        boolean result = obj.checkInput();
        assertFalse(result);
    }

}
