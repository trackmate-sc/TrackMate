package fiji.plugin.trackmate.util;

import fiji.plugin.trackmate.Spot;

import java.util.Arrays;

public class HistogramUtils {
    /**
     * Returns an estimate of the <code>p</code>th percentile of the values in
     * the <code>values</code> array. Taken from commons-math.
     */
    public static final double getPercentile(final double[] values, final double p) {

        final int size = values.length;
        if ((p > 1) || (p <= 0))
            throw new IllegalArgumentException("invalid quantile value: " + p);
        // always return single value for n = 1
        if (size == 0)
            return Double.NaN;
        if (size == 1)
            return values[0];
        final double n = size;
        final double pos = p * (n + 1);
        final double fpos = Math.floor(pos);
        final int intPos = (int) fpos;
        final double dif = pos - fpos;
        final double[] sorted = new double[size];
        System.arraycopy(values, 0, sorted, 0, size);
        Arrays.sort(sorted);

        if (pos < 1)
            return sorted[0];
        if (pos >= n)
            return sorted[size - 1];
        final double lower = sorted[intPos - 1];
        final double upper = sorted[intPos];
        return lower + dif * (upper - lower);
    }

    /**
     * Returns <code>[range, min, max]</code> of the given double array.
     *
     * @return A double[] of length 3, where index 0 is the range, index 1 is
     * the min, and index 2 is the max.
     */
    private static final double[] getRange(final double[] data) {
        if (data.length == 0)
            return new double[]{1., 0., 1.};

        final double min = Arrays.stream(data).min().getAsDouble();
        final double max = Arrays.stream(data).max().getAsDouble();
        return new double[]{(max - min), min, max};
    }

    /**
     * Store the x, y, z coordinates of the specified spot in the first 3
     * elements of the specified double array.
     */
    public static final void localize(final Spot spot, final double[] coords) {
        coords[0] = spot.getFeature(Spot.POSITION_X).doubleValue();
        coords[1] = spot.getFeature(Spot.POSITION_Y).doubleValue();
        coords[2] = spot.getFeature(Spot.POSITION_Z).doubleValue();
    }

    /**
     * Return the optimal bin number for a histogram of the data given in array,
     * using the Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)). It is
     * ensured that the bin number returned is not smaller and no bigger than
     * the bounds given in argument.
     */
    public static final int getNBins(final double[] values, final int minBinNumber, final int maxBinNumber) {
        final int size = values.length;
        final double q1 = getPercentile(values, 0.25);
        final double q3 = getPercentile(values, 0.75);
        final double iqr = q3 - q1;
        final double binWidth = 2 * iqr * Math.pow(size, -0.33);
        final double[] range = getRange(values);
        int nBin = (int) (range[0] / binWidth + 1);

        if (nBin > maxBinNumber)
            nBin = maxBinNumber;
        else if (nBin < minBinNumber)
            nBin = minBinNumber;

        return nBin;
    }

    /**
     * Return the optimal bin number for a histogram of the data given in array,
     * using the Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)). It is
     * ensured that the bin number returned is not smaller than 8 and no bigger
     * than 256.
     */
    private static final int getNBins(final double[] values) {
        return getNBins(values, 8, 256);
    }

    /**
     * Create a histogram from the data given.
     */
    private static final int[] histogram(final double[] data, final int nBins) {
        final double[] range = getRange(data);
        final double binWidth = range[0] / nBins;
        final int[] hist = new int[nBins];
        int index;

        if (nBins > 0) {
            for (int i = 0; i < data.length; i++) {
                index = Math.min((int) Math.floor((data[i] - range[1]) / binWidth), nBins - 1);
                hist[index]++;
            }
        }
        return hist;
    }

    /**
     * Return a threshold for the given data, using an Otsu histogram
     * thresholding method.
     */
    public static final double otsuThreshold(final double[] data) {
        return otsuThreshold(data, getNBins(data));
    }

    /**
     * Return a threshold for the given data, using an Otsu histogram
     * thresholding method with a given bin number.
     */
    private static final double otsuThreshold(final double[] data, final int nBins) {
        final int[] hist = histogram(data, nBins);
        final int thresholdIndex = otsuThresholdIndex(hist, data.length);
        final double[] range = getRange(data);
        final double binWidth = range[0] / nBins;
        return range[1] + binWidth * thresholdIndex;
    }

    /**
     * Given a histogram array <code>hist</code>, built with an initial amount
     * of <code>nPoints</code> data item, this method return the bin index that
     * thresholds the histogram in 2 classes. The threshold is performed using
     * the Otsu Threshold Method.
     *
     * @param hist    the histogram array
     * @param nPoints the number of data items this histogram was built on
     * @return the bin index of the histogram that thresholds it
     */
    private static final int otsuThresholdIndex(final int[] hist, final int nPoints) {
        final int total = nPoints;

        double sum = 0;
        for (int t = 0; t < hist.length; t++)
            sum += t * hist[t];

        double sumB = 0;
        int wB = 0;
        int wF = 0;

        double varMax = 0;
        int threshold = 0;

        for (int t = 0; t < hist.length; t++) {
            wB += hist[t]; // Weight Background
            if (wB == 0)
                continue;

            wF = total - wB; // Weight Foreground
            if (wF == 0)
                break;

            sumB += (t * hist[t]);

            final double mB = sumB / wB; // Mean Background
            final double mF = (sum - sumB) / wF; // Mean Foreground

            // Calculate Between Class Variance
            final double varBetween = wB * wF * (mB - mF) * (mB - mF);

            // Check if new maximum found
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }
        return threshold;
    }
}
