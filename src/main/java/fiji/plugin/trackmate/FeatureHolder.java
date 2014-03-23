package fiji.plugin.trackmate;

import java.util.Map;

public interface FeatureHolder {

	public Map< String, Double > getFeatures();

	public Double getFeature( final String feature );

	public void putFeature( final String feature, final Double value );
}
