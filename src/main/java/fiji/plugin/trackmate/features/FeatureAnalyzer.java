/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
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

import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.TrackMateModule;

public interface FeatureAnalyzer extends TrackMateModule
{

	/**
	 * Returns the list of features this analyzer can compute.
	 * 
	 * @return the list of features.
	 */
	public List< String > getFeatures();

	/**
	 * Returns the map of short names for any feature the analyzer can compute.
	 * 
	 * @return the map of feature short names.
	 */
	public Map< String, String > getFeatureShortNames();

	/**
	 * Returns the map of names for any feature this analyzer can compute.
	 * 
	 * @return the map of feature names.
	 */
	public Map< String, String > getFeatureNames();

	/**
	 * Returns the map of feature dimension this analyzer can compute.
	 * 
	 * @return the map of feature dimension.
	 */
	public Map< String, Dimension > getFeatureDimensions();

	/**
	 * Returns the map that states whether the key feature is a feature that
	 * returns integers. If <code>true</code>, then special treatment is applied
	 * when saving/loading, etc. for clarity and precision.
	 * 
	 * @return the map.
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
