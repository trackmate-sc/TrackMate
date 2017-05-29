package fiji.plugin.trackmate.visualization.trajeditor;

import java.util.Map;

/**
 *
 * @author hadim
 */
public class TrajEditorUtil {

    public static Object getKeyFromValue(Map hm, Object value) {
        for (Object o : hm.keySet()) {
            if (hm.get(o).equals(value)) {
                return o;
            }
        }
        return null;
    }

}
