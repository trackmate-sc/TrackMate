package fiji.plugin.trackmate.visualization;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.TrackMateOptionUtils;
import fiji.plugin.trackmate.features.manual.ManualEdgeColorAnalyzer;

import java.awt.Color;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

public class PerEdgeFeatureColorGenerator implements ModelChangeListener, TrackColorGenerator
{

	private InterpolatePaintScale generator;

	private final Model model;

	private String feature;

	private double min;

	private double max;

	private DefaultWeightedEdge edgeMin;

	private DefaultWeightedEdge edgeMax;

	private boolean autoMinMax = true;

	public PerEdgeFeatureColorGenerator( final Model model, final String feature )
	{
		this.model = model;
		model.addModelChangeListener( this );
		generator = TrackMateOptionUtils.getOptions().getPaintScale();
		setFeature( feature );
	}

	@Override
	public void setFeature( final String feature )
	{
		if ( feature.equals( this.feature ) || feature.equals( ManualEdgeColorAnalyzer.FEATURE ) ) { return; }
		this.feature = feature;
		// We recompute min and max whatever the mode is set, to have something
		// meaningful.
		resetMinAndMax();
	}

	@Override
	public String getFeature()
	{
		return feature;
	}

	@Override
	public Color color( final DefaultWeightedEdge edge )
	{
		final Double feat = model.getFeatureModel().getEdgeFeature( edge, feature );

		if ( null == feat )
			return TrackMateModelView.DEFAULT_UNASSIGNED_FEATURE_COLOR;

		if ( Double.isNaN( feat.doubleValue() ) )
			return TrackMateModelView.DEFAULT_UNDEFINED_FEATURE_COLOR;

		final double val = feat.doubleValue();
		return generator.getPaint( ( val - min ) / ( max - min ) );
	}

	@Override
	public void setCurrentTrackID( final Integer trackID )
	{} // ignored

	/**
	 * If the color scaling mode is set to automatic, monitors if the change
	 * induces some change in the colormap. Rescale it if so.
	 */
	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		if ( !autoMinMax || event.getEventID() != ModelChangeEvent.MODEL_MODIFIED || event.getEdges().size() == 0 ) { return; }

		for ( final DefaultWeightedEdge edge : event.getEdges() )
		{

			if ( event.getEdgeFlag( edge ) == ModelChangeEvent.FLAG_EDGE_ADDED || event.getEdgeFlag( edge ) == ModelChangeEvent.FLAG_EDGE_MODIFIED )
			{

				if ( edge.equals( edgeMax ) || edge.equals( edgeMin ) )
				{
					resetMinAndMax();
					return;
				}

				final double val = model.getFeatureModel().getEdgeFeature( edge, feature ).doubleValue();
				if ( val > max || val < min )
				{
					resetMinAndMax();
					return;
				}

			}
			else if ( event.getEdgeFlag( edge ) == ModelChangeEvent.FLAG_EDGE_REMOVED )
			{

				if ( edge.equals( edgeMax ) || edge.equals( edgeMin ) )
				{
					resetMinAndMax();
					return;
				}

			}
		}
	}

	private void resetMinAndMax()
	{
		min = Double.POSITIVE_INFINITY;
		max = Double.NEGATIVE_INFINITY;
		// Only iterate over filtered edges
		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{
			for ( final DefaultWeightedEdge edge : model.getTrackModel().trackEdges( trackID ) )
			{
				final Double feat = model.getFeatureModel().getEdgeFeature( edge, feature );
				if ( null == feat || Double.isNaN( feat.doubleValue() ) )
				{
					continue;
				}
				final double val = feat.doubleValue();
				if ( val < min )
				{
					min = val;
					edgeMin = edge;
				}
				if ( val > max )
				{
					max = val;
					edgeMax = edge;
				}
			}
		}
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
		{
			model.addModelChangeListener( this );
		}
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
		resetMinAndMax();
	}

	@Override
	public void setAutoMinMaxMode( final boolean autoMode )
	{
		this.autoMinMax = autoMode;
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
		return autoMinMax;
	}

	@Override
	public void setFrom( final MinMaxAdjustable minMaxAdjustable )
	{
		setAutoMinMaxMode( minMaxAdjustable.isAutoMinMaxMode() );
		if ( !minMaxAdjustable.isAutoMinMaxMode() )
		{
			setMinMax( minMaxAdjustable.getMin(), minMaxAdjustable.getMax() );
		}
	}
}
