package fiji.plugin.trackmate.tracking.spot;

import java.util.Collection;
import java.util.Map;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.tracking.TrackableObjectCollection;

public interface SpotCollection extends TrackableObjectCollection<Spot> {
	
	@Override
	public String toString();

	public void filter( FeatureFilter featurefilter );

	public void filter( Collection< FeatureFilter > filters );

	public Map< String, double[] > collectValues( Collection< String > features, boolean visibleOnly );

	public double[] collectValues( String feature, boolean visibleOnly );

}
