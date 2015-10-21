package fiji.plugin.trackmate.visualization;

/**
 * Simple interface for objects whose effects depend on a minimal and maximal
 * double value that can be adjusted manually or automatically.
 *
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; Mar 20, 2014
 *
 */
public interface MinMaxAdjustable
{
	/**
	 * Returns the min value currently set.
	 *
	 * @return the min value.
	 */
	public double getMin();

	/**
	 * Returns the max value currently set.
	 *
	 * @return the max value.
	 */
	public double getMax();

	/**
	 * Sets the min and max of this object.
	 *
	 * @param min
	 *            the min value.
	 * @param max
	 *            the max value.
	 */
	public void setMinMax( double min, double max );

	/**
	 * Automatically computes the min &amp; max values from this object current
	 * content.
	 */
	public void autoMinMax();

	/**
	 * Sets the behavior of this object regarding the min &amp; max calculation.
	 * <p>
	 * If the flag passed is <code>true</code>, then the min &amp; max will be
	 * recalculated by the instance everytime it is meaningful to do so, by
	 * calling the {@link #autoMinMax()} method.
	 * <p>
	 * If <code>false</code>, the min &amp; max will not be recalculated. They
	 * will keep their current value unless {@link #setMinMax(double, double)}
	 * is called.
	 *
	 * @param autoMode
	 *            the behavior flag.
	 */
	public void setAutoMinMaxMode( boolean autoMode );

	/**
	 * Returns whether the automatic scaling mode is activated or not for this
	 * object.
	 *
	 * @return <code>true</code> if the automatic scaling mode is on.
	 */
	public boolean isAutoMinMaxMode();

	/**
	 * Copies the min, max and auto fields from the specified
	 * {@link MinMaxAdjustable} to this object.
	 * <p>
	 * Not that if {@link #isAutoMinMaxMode()} is set to <code>true</code>, the
	 * min and max value will <b>not</b> be copied over.
	 */
	public void setFrom( MinMaxAdjustable minMaxAdjustable );

}
