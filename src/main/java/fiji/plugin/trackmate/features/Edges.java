package fiji.plugin.trackmate.features;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

import java.util.*;

public class Edges extends FeatureUtils {


    public Map<String, String> collectFeatureKeys(DisplaySettings.TrackMateObject target, Model model, Settings settings) {
        final Map<String, String> inverseMap = new HashMap<>();
        if (model != null) {
            for (final String featureKey : model.getFeatureModel().getEdgeFeatureNames().keySet())
                inverseMap.put(model.getFeatureModel().getEdgeFeatureNames().get(featureKey), featureKey);
        }
        if (settings != null) {
            for (final EdgeAnalyzer ea : settings.getEdgeAnalyzers())
                for (final String featureKey : ea.getFeatureNames().keySet())
                    inverseMap.put(ea.getFeatureNames().get(featureKey), featureKey);
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
