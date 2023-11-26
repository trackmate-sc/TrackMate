package fiji.plugin.trackmate.features;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

import java.util.*;

public class Tracks extends FeatureUtils {

    public Map<String, String> collectFeatureKeys(DisplaySettings.TrackMateObject target, Model model, Settings settings) {
        final Map<String, String> inverseMap = new HashMap<>();

        if (model != null) {
            for (final String featureKey : model.getFeatureModel().getTrackFeatureNames().keySet())
                inverseMap.put(model.getFeatureModel().getTrackFeatureNames().get(featureKey), featureKey);
        }
        if (settings != null) {
            for (final TrackAnalyzer ta : settings.getTrackAnalyzers())
                for (final String featureKey : ta.getFeatureNames().keySet())
                    inverseMap.put(ta.getFeatureNames().get(featureKey), featureKey);
        }

        // Sort by feature name.
        final List<String> featureNameList = new ArrayList<>(inverseMap.keySet());
        featureNameList.sort(null);

        final Map<String, String> featureNames = new LinkedHashMap<>(featureNameList.size());
        for (final String featureName : featureNameList)
            featureNames.put(inverseMap.get(featureName), featureName);

        return featureNames;
    }
}
