/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.features;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.manual.ManualEdgeColorAnalyzer;
import fiji.plugin.trackmate.features.manual.ManualSpotColorAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.visualization.*;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.util.DoubleArray;

import java.awt.*;
import java.util.Map;
import java.util.Random;

public abstract class FeatureUtils {

    public static final String USE_UNIFORM_COLOR_KEY = "UNIFORM_COLOR";
    public static final String USE_RANDOM_COLOR_KEY = "RANDOM_COLOR";
    public static final Model DUMMY_MODEL = new Model();
    private static final String USE_UNIFORM_COLOR_NAME = "Uniform color";
    private static final String USE_RANDOM_COLOR_NAME = "Random color";

    static {
        final Random ran = new Random();
        DUMMY_MODEL.beginUpdate();
        try {

            for (int i = 0; i < 100; i++) {
                Spot previous = null;
                for (int t = 0; t < 20; t++) {

                    final double x = ran.nextDouble();
                    final double y = ran.nextDouble();
                    final double z = ran.nextDouble();
                    final double r = ran.nextDouble();
                    final double q = ran.nextDouble();
                    final Spot spot = new Spot(x, y, z, r, q);
                    DUMMY_MODEL.addSpotTo(spot, t);
                    if (previous != null)
                        DUMMY_MODEL.addEdge(previous, spot, ran.nextDouble());

                    previous = spot;
                }
            }
        } finally {
            DUMMY_MODEL.endUpdate();
        }
    }

    /**
     * Missing or undefined values are not included.
     *
     * @param featureKey
     * @param target
     * @param model
     * @param visibleOnly
     * @return a new <code>double[]</code> array containing the numerical
     * feature values.
     */
    public static double[] collectFeatureValues(
            final String featureKey,
            final TrackMateObject target,
            final Model model,
            final boolean visibleOnly) {
        final FeatureModel fm = model.getFeatureModel();
        switch (target) {
            case DEFAULT:
                return new double[]{};

            case EDGES: {
                final DoubleArray val = new DoubleArray();
                for (final Integer trackID : model.getTrackModel().trackIDs(visibleOnly)) {
                    for (final DefaultWeightedEdge edge : model.getTrackModel().trackEdges(trackID)) {
                        final Double ef = fm.getEdgeFeature(edge, featureKey);
                        if (ef != null && !ef.isNaN())
                            val.add(ef.doubleValue());
                    }
                }
                return val.copyArray();
            }
            case SPOTS: {

                final DoubleArray val = new DoubleArray();
                for (final Spot spot : model.getSpots().iterable(visibleOnly)) {
                    final Double sf = spot.getFeature(featureKey);
                    if (sf != null && !sf.isNaN())
                        val.add(sf.doubleValue());
                }
                return val.copyArray();
            }
            case TRACKS: {
                final DoubleArray val = new DoubleArray();
                for (final Integer trackID : model.getTrackModel().trackIDs(visibleOnly)) {
                    final Double tf = fm.getTrackFeature(trackID, featureKey);
                    if (tf != null && !tf.isNaN())
                        val.add(tf.doubleValue());
                }
                return val.copyArray();
            }
            default:
                throw new IllegalArgumentException("Unknown object type: " + target);
        }
    }

    public static final FeatureColorGenerator<Spot> createSpotColorGenerator(final Model model, final DisplaySettings displaySettings) {
        switch (displaySettings.getSpotColorByType()) {
            case DEFAULT:
                switch (displaySettings.getSpotColorByFeature()) {
                    case FeatureUtils.USE_RANDOM_COLOR_KEY:
                        return new RandomSpotColorGenerator();
                    default:
                    case FeatureUtils.USE_UNIFORM_COLOR_KEY:
                        return new UniformSpotColorGenerator(displaySettings.getSpotUniformColor());
                }

            case EDGES:

                if (displaySettings.getSpotColorByFeature().equals(ManualEdgeColorAnalyzer.FEATURE))
                    return new ManualSpotPerEdgeColorGenerator(model, displaySettings.getMissingValueColor());

                return new SpotColorGeneratorPerEdgeFeature(
                        model,
                        displaySettings.getSpotColorByFeature(),
                        displaySettings.getMissingValueColor(),
                        displaySettings.getUndefinedValueColor(),
                        displaySettings.getColormap(),
                        displaySettings.getSpotMin(),
                        displaySettings.getSpotMax());

            case SPOTS:

                if (displaySettings.getSpotColorByFeature().equals(ManualSpotColorAnalyzerFactory.FEATURE))
                    return new ManualSpotColorGenerator(displaySettings.getMissingValueColor());

                return new SpotColorGenerator(
                        displaySettings.getSpotColorByFeature(),
                        displaySettings.getMissingValueColor(),
                        displaySettings.getUndefinedValueColor(),
                        displaySettings.getColormap(),
                        displaySettings.getSpotMin(),
                        displaySettings.getSpotMax());

            case TRACKS:
                return new SpotColorGeneratorPerTrackFeature(
                        model,
                        displaySettings.getSpotColorByFeature(),
                        displaySettings.getMissingValueColor(),
                        displaySettings.getUndefinedValueColor(),
                        displaySettings.getColormap(),
                        displaySettings.getSpotMin(),
                        displaySettings.getSpotMax());

            default:
                throw new IllegalArgumentException("Unknown type: " + displaySettings.getSpotColorByType());
        }
    }

    public static final FeatureColorGenerator<DefaultWeightedEdge> createTrackColorGenerator(final Model model, final DisplaySettings displaySettings) {
        switch (displaySettings.getTrackColorByType()) {
            case DEFAULT:
                switch (displaySettings.getTrackColorByFeature()) {
                    case FeatureUtils.USE_RANDOM_COLOR_KEY:
                        return new PerTrackFeatureColorGenerator(
                                model,
                                TrackIndexAnalyzer.TRACK_INDEX,
                                displaySettings.getMissingValueColor(),
                                displaySettings.getUndefinedValueColor(),
                                displaySettings.getColormap(),
                                displaySettings.getTrackMin(),
                                displaySettings.getTrackMax());
                    default:
                    case FeatureUtils.USE_UNIFORM_COLOR_KEY:
                        return new UniformTrackColorGenerator(displaySettings.getTrackUniformColor());
                }

            case EDGES:

                if (displaySettings.getTrackColorByFeature().equals(ManualEdgeColorAnalyzer.FEATURE))
                    return new ManualEdgeColorGenerator(model, displaySettings.getMissingValueColor());

                return new PerEdgeFeatureColorGenerator(
                        model,
                        displaySettings.getTrackColorByFeature(),
                        displaySettings.getMissingValueColor(),
                        displaySettings.getUndefinedValueColor(),
                        displaySettings.getColormap(),
                        displaySettings.getTrackMin(),
                        displaySettings.getTrackMax());

            case SPOTS:

                if (displaySettings.getTrackColorByFeature().equals(ManualSpotColorAnalyzerFactory.FEATURE))
                    return new ManualEdgePerSpotColorGenerator(model, displaySettings.getMissingValueColor());

                return new PerSpotFeatureColorGenerator(
                        model,
                        displaySettings.getTrackColorByFeature(),
                        displaySettings.getMissingValueColor(),
                        displaySettings.getUndefinedValueColor(),
                        displaySettings.getColormap(),
                        displaySettings.getTrackMin(),
                        displaySettings.getTrackMax());

            case TRACKS:
                return new PerTrackFeatureColorGenerator(
                        model,
                        displaySettings.getTrackColorByFeature(),
                        displaySettings.getMissingValueColor(),
                        displaySettings.getUndefinedValueColor(),
                        displaySettings.getColormap(),
                        displaySettings.getTrackMin(),
                        displaySettings.getTrackMax());

            default:
                throw new IllegalArgumentException("Unknown type: " + displaySettings.getTrackColorByType());
        }
    }

    public static final FeatureColorGenerator<Integer> createWholeTrackColorGenerator(final Model model, final DisplaySettings displaySettings) {
        switch (displaySettings.getTrackColorByType()) {
            case DEFAULT:
            case SPOTS:
                return id -> Color.WHITE;

            case EDGES:
            case TRACKS:
                return new WholeTrackFeatureColorGenerator(
                        model,
                        displaySettings.getTrackColorByFeature(),
                        displaySettings.getMissingValueColor(),
                        displaySettings.getUndefinedValueColor(),
                        displaySettings.getColormap(),
                        displaySettings.getTrackMin(),
                        displaySettings.getTrackMax());

            default:
                throw new IllegalArgumentException("Unknown type: " + displaySettings.getTrackColorByType());
        }
    }

    public static final double[] autoMinMax(final Model model, final TrackMateObject type, final String feature) {
        switch (type) {
            case DEFAULT:
                return new double[]{0., 0.};

            case EDGES:
            case SPOTS:
            case TRACKS: {
                final double[] values = collectFeatureValues(feature, type, model, true);
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (final double val : values) {
                    if (val < min)
                        min = val;

                    if (val > max)
                        max = val;
                }
                return new double[]{min, max};
            }

            default:
                throw new IllegalArgumentException("Unexpected TrackMate object type: " + type);
        }
    }

    public static final int nObjects(final Model model, final TrackMateObject target, final boolean visibleOnly) {
        switch (target) {
            case DEFAULT:
                throw new UnsupportedOperationException("Cannot return the number of objects for type DEFAULT.");
            case EDGES: {
                int nEdges = 0;
                for (final Integer trackID : model.getTrackModel().unsortedTrackIDs(visibleOnly))
                    nEdges += model.getTrackModel().trackEdges(trackID).size();
                return nEdges;
            }
            case SPOTS:
                return model.getSpots().getNSpots(visibleOnly);
            case TRACKS:
                return model.getTrackModel().nTracks(visibleOnly);
            default:
                throw new IllegalArgumentException("Unknown TrackMate object: " + target);
        }
    }

    public abstract Map<String, String> collectFeatureKeys(final TrackMateObject target, final Model model, final Settings settings);
}
