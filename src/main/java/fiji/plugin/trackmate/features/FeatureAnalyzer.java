package fiji.plugin.trackmate.features;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.TrackMateModule;

import java.util.List;
import java.util.Map;

public interface FeatureAnalyzer extends TrackMateModule
{

	/**
	 * Returns the list of features this analyzer can compute.
	 */
	public List<String> getFeatures();

	/**
	 * Returns the map of short names for any feature the analyzer
	 * can compute.
	 */
	public Map<String, String> getFeatureShortNames();

	/**
	 * Returns the map of names for any feature this analyzer can compute.
	 */
	public Map<String, String> getFeatureNames();

	/**
	 * Returns the map of feature dimension this analyzer can compute.
	 */
	public Map<String, Dimension> getFeatureDimensions();

	/**
	 * Returns the map that states whether the key feature is a feature that
	 * returns integers. If <code>true</code>, then special treatment is applied
	 * when saving/loading, etc. for clarity and precision.
	 */
	public Map< String, Boolean > getIsIntFeature();

	/**
	 * Returns whether <b>all</b> the features declared in this
	 * {@link FeatureAnalyzer} are <b>manual</b> features.
	 * <p>
	 * Manual features are <b>not</b> calculated normally using an analyzer, nor
	 * cleared at each recalculation. Another classes are responsible to set
	 * their value. The concrete {@link FeatureAnalyzer} calculation method is
	 * <b>not</b> called by TrackMate when a change happens to the model.
	 * Therefore the calculation routine of the {@link FeatureAnalyzer} can be
	 * used to discard the manually stored value of these features.
	 *
	 * @return <code>true</code> if the features declared in this analyzer are
	 *         manual feature.
	 */
	public boolean isManualFeature();

}
