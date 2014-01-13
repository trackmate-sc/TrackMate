package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;

/**
 * A provider for the edge analyzers provided in the GUI.
 */
public class EdgeAnalyzerProvider extends AbstractFeatureAnalyzerProvider< EdgeAnalyzer >
{

	protected EdgeTargetAnalyzer edgeTargetAnalyzer;

	protected EdgeVelocityAnalyzer edgeVelocityAnalyzer;

	protected EdgeTimeLocationAnalyzer edgeTimeLocationAnalyzer;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model spotFeatureAnalyzers
	 * currently available in the TrackMate trackmate. Each spotFeatureAnalyzer
	 * is identified by a key String, which can be used to retrieve new instance
	 * of the spotFeatureAnalyzer.
	 * <p>
	 * If you want to add custom spotFeatureAnalyzers to TrackMate, a simple way
	 * is to extend this factory so that it is registered with the custom
	 * spotFeatureAnalyzers and provide this extended factory to the
	 * {@link TrackMate} trackmate.
	 */
	public EdgeAnalyzerProvider()
	{
		registerEdgeFeatureAnalyzers();
	}

	/*
	 * METHODS
	 */

	/**
	 * Register the standard spotFeatureAnalyzers shipped with TrackMate.
	 */
	protected void registerEdgeFeatureAnalyzers()
	{
		this.edgeTargetAnalyzer = new EdgeTargetAnalyzer();
		this.edgeTimeLocationAnalyzer = new EdgeTimeLocationAnalyzer();
		this.edgeVelocityAnalyzer = new EdgeVelocityAnalyzer();
		// Names
		registerAnalyzer( EdgeVelocityAnalyzer.KEY, edgeVelocityAnalyzer );
		registerAnalyzer( EdgeTimeLocationAnalyzer.KEY, edgeTimeLocationAnalyzer );
		registerAnalyzer( EdgeTargetAnalyzer.KEY, edgeTargetAnalyzer );
	}

}
