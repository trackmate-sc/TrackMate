package fiji.plugin.trackmate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.imglib2.RealLocalizable;
import net.imglib2.util.Util;
import fiji.plugin.trackmate.tracking.AbstractTrackableObject;

/**
 * A {@link RealLocalizable} implementation, used in TrackMate to represent a
 * detection.
 * <p>
 * On top of being a {@link RealLocalizable}, it can store additional numerical
 * named features, with a {@link Map}-like syntax. Constructors enforce the
 * specification of the spot location in 3D space (if Z is unused, put 0), the
 * spot radius, and the spot quality. This somewhat cumbersome syntax is made to
 * avoid any bad surprise with missing features in a subsequent use. The spot
 * temporal features ({@link #FRAME} and {@link #POSITION_T}) are set upon
 * adding to a {@link SpotCollection}.
 * <p>
 * Each spot received at creation a unique ID (as an <code>int</code>), used
 * later for saving, retrieving and loading. Interfering with this value will
 * predictively cause undesired behavior.
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010, 2013
 * 
 */
public class Spot extends AbstractTrackableObject implements FeatureHolder {

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Creates a new spot.
	 * 
	 * @param x
	 *            the spot X coordinates, in image units.
	 * @param y
	 *            the spot Y coordinates, in image units.
	 * @param z
	 *            the spot Z coordinates, in image units.
	 * @param radius
	 *            the spot radius, in image units.
	 * @param quality
	 *            the spot quality.
	 * @param name
	 *            the spot name.
	 */
	public Spot(final double x, final double y, final double z,
			final double radius, final double quality, final String name) {
		super(new double[] { x, y, z }, name, -1);
		this.features = new ConcurrentHashMap<String, Double>();

		putFeature(TrackmateConstants.POSITION_X, Double.valueOf(x));
		putFeature(TrackmateConstants.POSITION_Y, Double.valueOf(y));
		putFeature(TrackmateConstants.POSITION_Z, Double.valueOf(z));
		putFeature(TrackmateConstants.RADIUS, Double.valueOf(radius));
		putFeature(TrackmateConstants.QUALITY, Double.valueOf(quality));

	}

	/**
	 * Creates a new spot, and gives it a default name.
	 * 
	 * @param x
	 *            the spot X coordinates, in image units.
	 * @param y
	 *            the spot Y coordinates, in image units.
	 * @param z
	 *            the spot Z coordinates, in image units.
	 * @param radius
	 *            the spot radius, in image units.
	 * @param quality
	 *            the spot quality.
	 */
	public Spot(final double x, final double y, final double z,
			final double radius, final double quality) {
		this(x, y, z, radius, quality, null);
	}

	/**
	 * Creates a new spot, taking its 3D coordinates from a
	 * {@link RealLocalizable}. The {@link RealLocalizable} must have at least 3
	 * dimensions, and must return coordinates in image units.
	 * 
	 * @param location
	 *            the {@link RealLocalizable} that contains the spot locatiob.
	 * @param radius
	 *            the spot radius, in image units.
	 * @param quality
	 *            the spot quality.
	 * @param name
	 *            the spot name.
	 */
	public Spot(final RealLocalizable location, final double radius,
			final double quality, final String name) {
		this(location.getDoublePosition(0), location.getDoublePosition(1),
				location.getDoublePosition(2), radius, quality, name);
	}

	/**
	 * Creates a new spot, taking its 3D coordinates from a
	 * {@link RealLocalizable}. The {@link RealLocalizable} must have at least 3
	 * dimensions, and must return coordinates in image units. The spot will get
	 * a default name.
	 * 
	 * @param location
	 *            the {@link RealLocalizable} that contains the spot locatiob.
	 * @param radius
	 *            the spot radius, in image units.
	 * @param quality
	 *            the spot quality.
	 */
	public Spot(final RealLocalizable location, final double radius,
			final double quality) {
		this(location, radius, quality, null);
	}

	/**
	 * Creates a new spot, taking its location, its radius, its quality value
	 * and its name from the specified spot.
	 * 
	 * @param spot
	 *            the spot to read from.
	 */
	public Spot(final Spot spot) {
		this(spot, spot.getFeature(TrackmateConstants.RADIUS), spot
				.getFeature(TrackmateConstants.QUALITY), spot.getName());
	}

	/**
	 * Blank constructor meant to be used when loading a spot collection from a
	 * file. <b>Will</b> mess with the {@link #IDcounter} field, so this
	 * constructor <u>should not be used for normal spot creation</u>.
	 * 
	 * @param ID
	 *            the spot ID to set
	 */
	public Spot(final int ID) {
		super(new double[3], ID);
		this.features = new ConcurrentHashMap<String, Double>();
	}

	protected final ConcurrentMap<String, Double> features;

	/**
	 * Exposes the storage map of features for this object. Altering the
	 * returned map will alter the spot.
	 * 
	 * @return a map of {@link String}s to {@link Double}s.
	 */
	@Override
	public Map<String, Double> getFeatures() {
		return features;
	}

	/**
	 * Returns the value corresponding to the specified object feature.
	 * 
	 * @param feature
	 *            The feature string to retrieve the stored value for.
	 * @return the feature value, as a {@link Double}. Will be <code>null</code>
	 *         if it has not been set.
	 */
	@Override
	public Double getFeature(final String feature) {
		return features.get(feature);
	}

	/**
	 * Stores the specified feature value for this object.
	 * 
	 * @param feature
	 *            the name of the feature to store, as a {@link String}.
	 * @param value
	 *            the value to store, as a {@link Double}. Using
	 *            <code>null</code> will have unpredicted outcomes.
	 */
	@Override
	public void putFeature(final String feature, final Double value) {
		features.put(feature, value);
	}

	/**
	 * Return a string representation of this spot, with calculated features.
	 */
	public String echo() {
		final StringBuilder s = new StringBuilder();

		// Name
		if (null == name)
			s.append("Spot: <no name>\n");
		else
			s.append("Spot: " + name + "\n");

		// Frame
		s.append("Time: " + getFeature(TrackmateConstants.FRAME) + '\n');

		// Coordinates
		final double[] coordinates = new double[3];
		localize(coordinates);
		s.append("Position: " + Util.printCoordinates(coordinates) + "\n");

		// Feature list
		if (null == features || features.size() < 1)
			s.append("No features calculated\n");
		else {
			s.append("Feature list:\n");
			double val;
			for (final String key : features.keySet()) {
				s.append("\t" + key.toString() + ": ");
				val = features.get(key);
				if (val >= 1e4)
					s.append(String.format("%.1g", val));
				else
					s.append(String.format("%.1f", val));
				s.append('\n');
			}
		}
		return s.toString();
	}

	@Override
	public int frame() {
		return getFeature(TrackmateConstants.FRAME).intValue();
	}

	@Override
	public void setFrame(int frame) {
		features.put(TrackmateConstants.FRAME, (double) frame);
	}

	@Override
	public void setVisible(boolean visibility) {
		features.put(TrackmateConstants.VISIBILITY,
				visibility ? TrackmateConstants.ONE : TrackmateConstants.ZERO);
	}

	@Override
	public boolean isVisible() {
		return features.get(TrackmateConstants.VISIBILITY).intValue() == 1;
	}

	@Override
	public double radius() {
		return features.get(TrackmateConstants.RADIUS).doubleValue();
	}

	@Override
	public void localize(final float[] position) {
		assert (position.length >= n);
		for (int d = 0; d < n; ++d)
			position[d] = getFloatPosition(d);
	}

	@Override
	public void localize(final double[] position) {
		assert (position.length >= n);
		for (int d = 0; d < n; ++d)
			position[d] = getDoublePosition(d);
	}

	@Override
	public float getFloatPosition(final int d) {
		return (float) getDoublePosition(d);
	}

	@Override
	public double getDoublePosition(final int d) {
		assert (d > 0 && d < n);
		return getFeature(TrackmateConstants.POSITION_FEATURES[d]);
	}

}
