package fiji.plugin.trackmate.features;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

import java.util.*;

public class Spots extends FeatureUtils {
    public Map<String, String> collectFeatureKeys(DisplaySettings.TrackMateObject target, Model model, Settings settings) {
        final Map<String, String> inverseMap = new HashMap<>();
        // Collect all.
        if (model != null) {
            for (final String featureKey : model.getFeatureModel().getSpotFeatureNames().keySet())
                inverseMap.put(model.getFeatureModel().getSpotFeatureNames().get(featureKey), featureKey);
        } else {
            // If we have no model, we still want to add spot features.
            for (final String featureKey : Spot.FEATURE_NAMES.keySet())
                inverseMap.put(Spot.FEATURE_NAMES.get(featureKey), featureKey);
        }
        if (settings != null) {
            for (final SpotAnalyzerFactoryBase<?> sf : settings.getSpotAnalyzerFactories())
                for (final String featureKey : sf.getFeatureNames().keySet())
                    inverseMap.put(sf.getFeatureNames().get(featureKey), featureKey);
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
