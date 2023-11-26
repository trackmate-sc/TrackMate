package fiji.plugin.trackmate.features;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

import java.util.*;

public class Defaults extends FeatureUtils {
    private static final String USE_UNIFORM_COLOR_NAME = "Uniform color";
    private static final String USE_RANDOM_COLOR_NAME = "Random color";

    public Map<String, String> collectFeatureKeys(DisplaySettings.TrackMateObject target, Model model, Settings settings) {
        final Map<String, String> inverseMap = new HashMap<>();
        inverseMap.put(USE_UNIFORM_COLOR_NAME, USE_UNIFORM_COLOR_KEY);
        inverseMap.put(USE_RANDOM_COLOR_NAME, USE_RANDOM_COLOR_KEY);

        // Sort by feature name.
        final List<String> featureNameList = new ArrayList<>(inverseMap.keySet());
        featureNameList.sort(null);

        final Map<String, String> featureNames = new LinkedHashMap<>(featureNameList.size());
        for (final String featureName : featureNameList)
            featureNames.put(inverseMap.get(featureName), featureName);

        return featureNames;
    }
}
