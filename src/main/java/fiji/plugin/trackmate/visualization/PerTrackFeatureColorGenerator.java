/**
 *
 */
package fiji.plugin.trackmate.visualization;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_TRACK_COLOR;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.TrackMateOptionUtils;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;

/**
 * A {@link TrackColorGenerator} that generate colors based on the whole track
 * feature.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class PerTrackFeatureColorGenerator implements TrackColorGenerator, ModelChangeListener
{

	private final InterpolatePaintScale generator;

	private Map< Integer, Color > colorMap;

	private final Model model;

	private String feature;

	private Color color;

	private double min;

	private double max;

	private boolean autoMode = true;

	public PerTrackFeatureColorGenerator( final Model model, final String feature )
	{
		this.model = model;
		model.addModelChangeListener( this );
		generator = TrackMateOptionUtils.getOptions().getPaintScale();
		setFeature( feature );
	}

	/**
	 * Sets the track feature to generate color with.
	 * <p>
	 * First, the track features are <b>re-calculated</b> for the target feature
	 * values to be accurate. We rely on the model instance for that. Then
	 * colors are calculated for all tracks when this method is called, and
	 * cached.
	 *
	 * @param feature
	 *            the track feature that will control coloring.
	 * @throws IllegalArgumentException
	 *             if the specified feature is unknown to the feature model.
	 */
	@Override
	public void setFeature( final String feature )
	{
		this.feature = feature;
		autoMinMax();
		refreshColorMap();
	}

	@Override
	public String getFeature()
	{
		return feature;
	}

	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		if ( !autoMode )
		{ return; }
		if ( event.getEventID() == ModelChangeEvent.MODEL_MODIFIED )
		{
			final Set< DefaultWeightedEdge > edges = event.getEdges();
			if ( edges.size() > 0 )
				refreshColorMap();
		}
	}

	private void refreshColorMap()
	{
		final TrackModel trackModel = model.getTrackModel();
		final Set< Integer > trackIDs = trackModel.trackIDs( true );

		if ( null == feature )
		{

			// Create value->color map
			colorMap = new HashMap<>( trackIDs.size() );
			for ( final Integer trackID : trackIDs )
				colorMap.put( trackID, DEFAULT_TRACK_COLOR );
		}
		else if ( feature.equals( TrackIndexAnalyzer.TRACK_INDEX ) )
		{
			// Create value->color map
			colorMap = new HashMap<>( trackIDs.size() );
			int index = 0;
			for ( final Integer trackID : trackIDs )
			{
				final Color lColor = generator.getPaint( ( double ) index++ / ( trackIDs.size() - 1 ) );
				colorMap.put( trackID, lColor );
			}
		}
		else
		{
			// Get min & max & all values
			if ( autoMode )
				autoMinMax();

			// Create value->color map
			final FeatureModel fm = model.getFeatureModel();
			colorMap = new HashMap<>( trackIDs.size() );
			for ( final Integer trackID : trackIDs )
			{
				final Double val = fm.getTrackFeature( trackID, feature );
				final Color lColor;
				if ( null == val )
					lColor = TrackMateModelView.DEFAULT_UNASSIGNED_FEATURE_COLOR;
				else if ( Double.isNaN( val.doubleValue() ) )
					lColor = TrackMateModelView.DEFAULT_UNDEFINED_FEATURE_COLOR;
				else
					lColor = generator.getPaint( ( val - min ) / ( max - min ) );

				colorMap.put( trackID, lColor );
			}
		}
	}

	@Override
	public Color color( final DefaultWeightedEdge edge )
	{
		return color;
	}

	@Override
	public void setCurrentTrackID( final Integer trackID )
	{
		this.color = colorMap.get( trackID );
	}

	@Override
	public void terminate()
	{
		model.removeModelChangeListener( this );
	}

	@Override
	public void activate()
	{
		if ( !model.getModelChangeListener().contains( this ) )
			model.addModelChangeListener( this );
	}

	/**
	 * Returns the color currently associated to the track with the specified
	 * ID.
	 *
	 * @param trackID
	 *            the ID of the track.
	 * @return a color.
	 */
	public Color colorOf( final Integer trackID )
	{
		return colorMap.get( trackID );
	}

	/*
	 * MINMAXADJUSTABLE
	 */

	@Override
	public double getMin()
	{
		return min;
	}

	@Override
	public double getMax()
	{
		return max;
	}

	@Override
	public void setMinMax( final double min, final double max )
	{
		this.min = min;
		this.max = max;
	}

	@Override
	public void autoMinMax()
	{
		final TrackModel trackModel = model.getTrackModel();
		final Set< Integer > trackIDs = trackModel.trackIDs( true );
		final FeatureModel fm = model.getFeatureModel();

		min = Double.POSITIVE_INFINITY;
		max = Double.NEGATIVE_INFINITY;
		for ( final Integer trackID : trackIDs )
		{
			final Double val = fm.getTrackFeature( trackID, feature );
			if ( null == val || Double.isNaN( val.doubleValue() ) )
				continue;

			if ( val < min )
				min = val;

			if ( val > max )
				max = val;
		}
	}

	@Override
	public void setAutoMinMaxMode( final boolean autoMode )
	{
		this.autoMode = autoMode;
		if ( autoMode )
		{
			activate();
		}
		else
		{
			// No need to listen.
			terminate();
		}
	}

	@Override
	public boolean isAutoMinMaxMode()
	{
		return autoMode;
	}

	@Override
	public void setFrom( final MinMaxAdjustable minMaxAdjustable )
	{
		setAutoMinMaxMode( minMaxAdjustable.isAutoMinMaxMode() );
		if ( !minMaxAdjustable.isAutoMinMaxMode() )
		{
			setMinMax( minMaxAdjustable.getMin(), minMaxAdjustable.getMax() );
		}
		else
		{
			autoMinMax();
		}
		refreshColorMap();
	}
}
