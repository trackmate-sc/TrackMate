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
package fiji.plugin.trackmate.util;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imagej.axis.Axes;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Util;
import org.scijava.Context;
import org.scijava.util.DoubleArray;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

/**
 * List of static utilities for {@link fiji.plugin.trackmate.TrackMate}.
 */
public class TMUtils {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

    private static Context context;

    /*
     * STATIC METHODS
     */

    private TMUtils() {
    }

    /**
     * Return a new map sorted by its values.
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(final Map<K, V> map, final Comparator<V> comparator) {
        final List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        final Comparator<Map.Entry<K, V>> c = new Comparator<Map.Entry<K, V>>() {

            @Override
            public int compare(final Entry<K, V> o1, final Entry<K, V> o2) {
                final V val1 = o1.getValue();
                final V val2 = o2.getValue();
                return comparator.compare(val1, val2);
            }
        };
        Collections.sort(list, c);
        final LinkedHashMap<K, V> result = new LinkedHashMap<>();
        for (final Map.Entry<K, V> entry : list)
            result.put(entry.getKey(), entry.getValue());
        return result;
    }

    /**
     * Generate a string representation of a map, typically a settings map.
     */
    public static final String echoMap(final Map<String, Object> map, final int indent) {
        // Build string
        final StringBuilder builder = new StringBuilder();
        for (final String key : map.keySet()) {
            for (int i = 0; i < indent; i++)
                builder.append(" ");

            builder.append("- ");
            builder.append(key.toLowerCase().replace("_", " "));
            builder.append(": ");
            final Object obj = map.get(key);
            if (obj instanceof Map) {
                builder.append('\n');
                @SuppressWarnings("unchecked") final Map<String, Object> submap = (Map<String, Object>) obj;
                builder.append(echoMap(submap, indent + 2));
            } else if (obj instanceof Logger) {
                builder.append(obj.getClass().getSimpleName());
                builder.append('\n');
            } else {
                builder.append(obj.toString());
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    /**
     * Wraps an IJ {@link ImagePlus} in an imglib2 {@link ImgPlus}, without
     * parameterized types. The only way I have found to beat javac constraints
     * on bounded multiple wildcard.
     */
    @SuppressWarnings("rawtypes")
    public static final ImgPlus rawWraps(final ImagePlus imp) {
        final ImgPlus<DoubleType> img = ImagePlusAdapter.wrapImgPlus(imp);
        final ImgPlus raw = img;
        return raw;
    }

    /**
     * Check that the given map has all some keys. Two String collection allows
     * specifying that some keys are mandatory, other are optional.
     *
     * @param map           the map to inspect.
     * @param mandatoryKeys the collection of keys that are expected to be in the map. Can
     *                      be <code>null</code>.
     * @param optionalKeys  the collection of keys that can be - or not - in the map. Can
     *                      be <code>null</code>.
     * @param errorHolder   will be appended with an error message.
     * @return if all mandatory keys are found in the map, and possibly some
     * optional ones, but no others.
     */
    public static final <T> boolean checkMapKeys(final Map<T, ?> map, Collection<T> mandatoryKeys, Collection<T> optionalKeys, final StringBuilder errorHolder) {
        if (null == optionalKeys)
            optionalKeys = new ArrayList<>();

        if (null == mandatoryKeys)
            mandatoryKeys = new ArrayList<>();

        boolean ok = true;
        final Set<T> keySet = map.keySet();
        for (final T key : keySet) {
            if (!(mandatoryKeys.contains(key) || optionalKeys.contains(key))) {
                ok = false;
                errorHolder.append("Map contains unexpected key: " + key + ".\n");
            }
        }

        for (final T key : mandatoryKeys) {
            if (!keySet.contains(key)) {
                ok = false;
                errorHolder.append("Mandatory key " + key + " was not found in the map.\n");
            }
        }
        return ok;

    }

    /**
     * Check the presence and the validity of a key in a map, and test it is of
     * the desired class.
     *
     * @param map           the map to inspect.
     * @param key           the key to find.
     * @param expectedClass the expected class of the target value .
     * @param errorHolder   will be appended with an error message.
     * @return true if the key is found in the map, and map a value of the
     * desired class.
     */
    public static final boolean checkParameter(final Map<String, Object> map, final String key, final Class<?> expectedClass, final StringBuilder errorHolder) {
        final Object obj = map.get(key);
        if (null == obj) {
            errorHolder.append("Parameter " + key + " could not be found in settings map, or is null.\n");
            return false;
        }
        if (!expectedClass.isInstance(obj)) {
            errorHolder.append("Value for parameter " + key + " is not of the right class. Expected " + expectedClass.getName() + ", got " + obj.getClass().getName() + ".\n");
            return false;
        }
        return true;
    }

    /*
     * ImgPlus & calibration & axes
     */

    /**
     * Returns the mapping in a map that is targeted by a list of keys, in the
     * order given in the list.
     */
    public static final <J, K> List<K> getArrayFromMaping(final Collection<J> keys, final Map<J, K> mapping) {
        final List<K> names = new ArrayList<>(keys.size());
        for (final J key : keys)
            names.add(mapping.get(key));
        return names;
    }

    /**
     * Return the xyz calibration stored in an {@link ImgPlusMetadata} in a
     * 3-elements double array. Calibration is ordered as X, Y, Z. If one axis
     * is not found, then the calibration for this axis takes the value of 1.
     */
    public static final double[] getSpatialCalibration(final ImgPlusMetadata img) {
        final double[] calibration = Util.getArrayFromValue(1d, 3);

        for (int d = 0; d < img.numDimensions(); d++) {
            if (img.axis(d).type() == Axes.X)
                calibration[0] = img.averageScale(d);
            else if (img.axis(d).type() == Axes.Y)
                calibration[1] = img.averageScale(d);
            else if (img.axis(d).type() == Axes.Z)
                calibration[2] = img.averageScale(d);
        }
        return calibration;
    }

    public static double[] getSpatialCalibration(final ImagePlus imp) {
        final double[] calibration = Util.getArrayFromValue(1d, 3);
        calibration[0] = imp.getCalibration().pixelWidth;
        calibration[1] = imp.getCalibration().pixelHeight;
        if (imp.getNSlices() > 1)
            calibration[2] = imp.getCalibration().pixelDepth;

        return calibration;
    }

    /**
     * Return a String unit for the given dimension. When suitable, the unit is
     * taken from the settings field, which contains the spatial and time units.
     * Otherwise, default units are used.
     */
    public static final String getUnitsFor(final Dimension dimension, final String spaceUnits, final String timeUnits) {
        switch (dimension) {
            case ANGLE:
                return "radians";
            case INTENSITY:
                return "counts";
            case INTENSITY_SQUARED:
                return "counts^2";
            case NONE:
                return "";
            case POSITION:
            case LENGTH:
                return spaceUnits;
            case AREA:
                return spaceUnits + "^2";
            case QUALITY:
                return "quality";
            case COST:
                return "cost";
            case TIME:
                return timeUnits;
            case VELOCITY:
                return spaceUnits + "/" + timeUnits;
            case RATE:
                return "/" + timeUnits;
            case ANGLE_RATE:
                return "rad/" + timeUnits;
            default:
            case STRING:
                return null;
        }
    }

    public static final String getCurrentTimeString() {
        return DATE_FORMAT.format(new Date());
    }

    public static <T extends Type<T>> ImgPlus<T> hyperSlice(final ImgPlus<T> img, final long channel, final long frame) {
        final int timeDim = img.dimensionIndex(Axes.TIME);
        final ImgPlus<T> imgT = timeDim < 0 ? img : ImgPlusViews.hyperSlice(img, timeDim, frame);

        final int channelDim = imgT.dimensionIndex(Axes.CHANNEL);
        final ImgPlus<T> imgTC = channelDim < 0 ? imgT : ImgPlusViews.hyperSlice(imgT, channelDim, channel);

        // Squeeze Z dimension if its size is 1.
        final int zDim = imgTC.dimensionIndex(Axes.Z);
        final ImgPlus<T> imgTCZ;
        if (zDim >= 0 && imgTC.dimension(zDim) <= 1)
            imgTCZ = ImgPlusViews.hyperSlice(imgTC, zDim, imgTC.min(zDim));
        else
            imgTCZ = imgTC;

        return imgTCZ;
    }

    /**
     * Returns an interval object that slices in the specified {@link ImgPlus}
     * <b>in a single channel</b> (the channel dimension is dropped).
     * <p>
     * The specified {@link Settings} object is used to determine a crop-cube
     * that will determine the X,Y,Z size of the interval. The channel dimension
     * will be dropped.
     * <p>
     * If the specified {@link ImgPlus} has a time axis, it will be included,
     * using the {@link Settings#tstart} and {@link Settings#tend} as bounds. If
     * it is a singleton dimension (1 time-point) it won't be dropped.
     *
     * @param img      the source image into which the interval is to be defined.
     * @param settings the settings object that will determine the interval size.
     * @return a new interval.
     */
    public static final Interval getIntervalWithTime(final ImgPlus<?> img, final Settings settings) {
        final long[] max = new long[img.numDimensions()];
        final long[] min = new long[img.numDimensions()];

        // X, we must have it.
        final int xindex = img.dimensionIndex(Axes.X);
        min[xindex] = settings.getXstart();
        max[xindex] = settings.getXend();

        // Y, we must have it.
        final int yindex = img.dimensionIndex(Axes.Y);
        min[yindex] = settings.getYstart();
        max[yindex] = settings.getYend();

        // Z, we MIGHT have it.
        final int zindex = img.dimensionIndex(Axes.Z);
        if (zindex >= 0) {
            min[zindex] = settings.zstart;
            max[zindex] = settings.zend;
        }

        // TIME, we might have it, but anyway we leave the start & end
        // management to elsewhere.
        final int tindex = img.dimensionIndex(Axes.TIME);
        if (tindex >= 0) {
            min[tindex] = settings.tstart;
            max[tindex] = settings.tend;
        }

        // CHANNEL, we might have it, we drop it.
        final long[] max2;
        final long[] min2;
        final int cindex = img.dimensionIndex(Axes.CHANNEL);
        if (cindex >= 0) {
            max2 = new long[img.numDimensions() - 1];
            min2 = new long[img.numDimensions() - 1];
            int d2 = 0;
            for (int d = 0; d < min.length; d++) {
                if (d != cindex) {
                    min2[d2] = Math.max(0L, min[d]);
                    max2[d2] = Math.min(img.max(d), max[d]);
                    d2++;
                }
            }
        } else {
            min2 = new long[min.length];
            max2 = new long[min.length];
            for (int d = 0; d < min.length; d++) {
                min2[d] = Math.max(0L, min[d]);
                max2[d] = Math.min(img.max(d), max[d]);
            }
        }

        final FinalInterval interval = new FinalInterval(min2, max2);
        return interval;
    }

    /**
     * Returns an interval object that in the specified {@link ImgPlus} <b>slice
     * in a single time frame</b>.
     * <p>
     * The specified {@link Settings} object is used to determine a crop-cube
     * that will determine the X,Y,Z size of the interval. A single channel will
     * be taken in the case of a multi-channel image. If the detector set in the
     * settings object has a parameter for the target channel
     * {@link fiji.plugin.trackmate.detection.DetectorKeys#KEY_TARGET_CHANNEL},
     * it will be used; otherwise the first channel will be taken.
     * <p>
     * If the specified {@link ImgPlus} has a time axis, it will be dropped and
     * the returned interval will have one dimension less.
     *
     * @param img      the source image into which the interval is to be defined.
     * @param settings the settings object that will determine the interval size.
     * @return a new interval.
     */
    public static final Interval getInterval(final ImgPlus<?> img, final Settings settings) {
        final long[] max = new long[img.numDimensions()];
        final long[] min = new long[img.numDimensions()];

        // X, we must have it.
        final int xindex = img.dimensionIndex(Axes.X);
        min[xindex] = settings.getXstart();
        max[xindex] = settings.getXend();

        // Y, we must have it.
        final int yindex = img.dimensionIndex(Axes.Y);
        min[yindex] = settings.getYstart();
        max[yindex] = settings.getYend();

        // Z, we MIGHT have it.
        final int zindex = img.dimensionIndex(Axes.Z);
        if (zindex >= 0) {
            min[zindex] = settings.zstart;
            max[zindex] = settings.zend;
        }

        // CHANNEL, we might have it.
        final int cindex = img.dimensionIndex(Axes.CHANNEL);
        if (cindex >= 0) {
            Integer c = (Integer) settings.detectorSettings.get(KEY_TARGET_CHANNEL); // 1-based.
            if (null == c)
                c = 1;

            min[cindex] = c - 1; // 0-based.
            max[cindex] = min[cindex];
        }

        // TIME, we might have it, but anyway we leave the start & end
        // management to elsewhere.
        final int tindex = img.dimensionIndex(Axes.TIME);

        /*
         * We want to exclude time (if we have it) from out interval and source,
         * so that we can provide the detector instance with a hyperslice that
         * does NOT have time as a dimension.
         */
        final long[] intervalMin;
        final long[] intervalMax;
        if (tindex >= 0) {
            intervalMin = new long[min.length - 1];
            intervalMax = new long[min.length - 1];
            int nindex = -1;
            for (int d = 0; d < min.length; d++) {
                if (d == tindex)
                    continue;

                nindex++;
                intervalMin[nindex] = Math.max(0L, min[d]);
                intervalMax[nindex] = Math.min(img.max(d), max[d]);
            }
        } else {
            intervalMin = new long[min.length];
            intervalMax = new long[min.length];
            for (int d = 0; d < min.length; d++) {
                intervalMin[d] = Math.max(0L, min[d]);
                intervalMax[d] = Math.min(img.max(d), max[d]);
            }
        }
        final FinalInterval interval = new FinalInterval(intervalMin, intervalMax);
        return interval;
    }

    /**
     * Obtains the SciJava {@link Context} in use by ImageJ.
     */
    public static Context getContext() {
        final Context localContext = context;
        if (localContext != null)
            return localContext;

        synchronized (TMUtils.class) {
            if (context == null)
                context = (Context) IJ.runPlugIn("org.scijava.Context", "");
            return context;
        }
    }

    /**
     * Creates a default file path to save the TrackMate session to, based on
     * the image TrackMate works on.
     *
     * @param settings the settings object from which to read the image, its folder,
     *                 etc.
     * @param logger   a logger instance in which to echo problems if any.
     * @return a new file.
     */
    public static File proposeTrackMateSaveFile(final Settings settings, final Logger logger) {
        File folder;
        if (null != settings.imp && null != settings.imp.getOriginalFileInfo() && null != settings.imp.getOriginalFileInfo().directory) {
            final String directory = settings.imp.getOriginalFileInfo().directory;
            folder = Paths.get(directory).toAbsolutePath().toFile();
            /*
             * Update the settings field with the image file location now,
             * because it's valid.
             */
            settings.imageFolder = settings.imp.getOriginalFileInfo().directory;
        } else if (!settings.imageFolder.isEmpty()) {
            final String absolutePath = FileSystems.getDefault().getPath(settings.imageFolder).normalize().toAbsolutePath().toString();
            folder = new File(absolutePath);
        } else {
            folder = new File(System.getProperty("user.dir"));
            /*
             * Warn the user that the file cannot be reloaded properly because
             * the source image does not match a file.
             */
            logger.error("Warning: The source image does not match a file on the system." + "TrackMate won't be able to reload it when opening this XML file.\n" + "To fix this, save the source image to a TIF file before saving the TrackMate session.\n");
            settings.imageFolder = "";
        }

        File file;
        try {
            file = new File(folder.getPath(), settings.imp.getShortTitle() + ".xml");
        } catch (final NullPointerException npe) {
            if (settings.imageFileName.isEmpty())
                file = new File(folder, "TrackMateData.xml");
            else {
                final String imName = settings.imageFileName;
                final int i = imName.lastIndexOf('.');
                String xmlName;
                if (i < 0)
                    xmlName = imName + ".xml";
                else
                    xmlName = imName.substring(0, i) + ".xml";
                file = new File(folder, xmlName);
            }
        }
        return file;
    }

    public static final double variance(final double[] data) {
        final double mean = Util.average(data);
        double variance = 0;
        for (int i = 0; i < data.length; i++) {
            final double dx = data[i] - mean;
            variance += dx * dx;
        }
        variance /= (data.length - 1);
        return variance;
    }

    public static final double standardDeviation(final double[] data) {
        return Math.sqrt(variance(data));
    }

    public static double sum(final double[] data) {
        return Arrays.stream(data).sum();
    }

    public static double average(final DoubleArray data) {
        return sum(data) / data.size();
    }

    public static double sum(final DoubleArray data) {
        double sum = 0.;
        for (int i = 0; i < data.size(); i++)
            sum += data.getArray()[i];
        return sum;
    }

    public static final double variance(final DoubleArray data) {
        final double mean = average(data);
        double variance = 0;
        for (int i = 0; i < data.size(); i++) {
            final double dx = data.getArray()[i] - mean;
            variance += dx * dx;
        }
        variance /= (data.size() - 1);
        return variance;
    }

    public static double standardDeviation(final DoubleArray data) {
        return Math.sqrt(variance(data));
    }

    /**
     * Returns a string of the name of the image without the extension, with the
     * full path
     *
     * @return full name of the image without the extension
     */
    public static String getImagePathWithoutExtension(final Settings settings) {
        final String imageFolder = (settings.imageFolder == null)
                ? System.getProperty("user.home")
                : settings.imageFolder;

        final String imageFileName = settings.imageFileName;
        if (imageFileName != null) {
            final int lastIndexOf = imageFileName.lastIndexOf(".");
            if (lastIndexOf > 0)
                return imageFolder + imageFileName.substring(0, imageFileName.lastIndexOf("."));
            return imageFolder + imageFileName;
        } else {
            return imageFolder + File.separator + "TrackMate";
        }
    }
}
