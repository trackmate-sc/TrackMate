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

	private static final InterpolatePaintScale generator = InterpolatePaintScale.Jet;

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
		setFeature( feature );
	}

	/**
	 * Set the track feature to set the color with.
	 * <p>
	 * First, the track features are <b>re-calculated</b> for the target feature
	 * values to be accurate. We rely on the {@link #model} instance for that.
	 * Then colors are calculated for all tracks when this method is called, and
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
		// Special case: if null, then all tracks should be green
		if ( null == feature )
		{
			this.feature = null;
			refreshNull();
			return;
		}

		this.feature = feature;
		// A hack if we are asked for track index, which is the default and
		// should never get caught to be null
		if ( feature.equals( TrackIndexAnalyzer.TRACK_INDEX ) )
		{
			refreshIndex();
		}
		else
		{
			refresh();
		}
	}

	@Override
	public String getFeature()
	{
		return feature;
	}

	private synchronized void refreshNull()
	{
		final TrackModel trackModel = model.getTrackModel();
		final Set< Integer > trackIDs = trackModel.trackIDs( true );

		// Create value->color map
		colorMap = new HashMap< Integer, Color >( trackIDs.size() );
		for ( final Integer trackID : trackIDs )
		{
			colorMap.put( trackID, DEFAULT_TRACK_COLOR );
		}
	}

	/**
	 * A shortcut for the track index feature
	 */
	private synchronized void refreshIndex()
	{
		final TrackModel trackModel = model.getTrackModel();
		final Set< Integer > trackIDs = trackModel.trackIDs( true );

		// Create value->color map
		colorMap = new HashMap< Integer, Color >( trackIDs.size() );
		int index = 0;
		min = Double.POSITIVE_INFINITY;
		max = Double.NEGATIVE_INFINITY;
		for ( final Integer trackID : trackIDs )
		{
			if ( trackID > max )
			{
				max = trackID;
			}
			if ( trackID < min )
			{
				min = trackID;
			}

			final Color color = generator.getPaint( ( double ) index++ / ( trackIDs.size() - 1 ) );
			colorMap.put( trackID, color );
		}
	}

	private synchronized void refresh()
	{
		final TrackModel trackModel = model.getTrackModel();
		final Set< Integer > trackIDs = trackModel.trackIDs( true );

		// Get min & max & all values
		final FeatureModel fm = model.getFeatureModel();
		min = Double.POSITIVE_INFINITY;
		max = Double.NEGATIVE_INFINITY;
		final HashMap< Integer, Double > values = new HashMap< Integer, Double >( trackIDs.size() );
		for ( final Integer trackID : trackIDs )
		{
			final Double val = fm.getTrackFeature( trackID, feature );
			values.put( trackID, val );

			if ( null == val || Double.isNaN( val.doubleValue() ) )
			{
				continue;
			}
			if ( val < min )
			{
				min = val;
			}
			if ( val > max )
			{
				max = val;
			}
		}

		// Create value->color map
		colorMap = new HashMap< Integer, Color >( trackIDs.size() );
		for ( final Integer trackID : values.keySet() )
		{
			final Double val = values.get( trackID );
			Color color;
			if ( null == val )
			{
				color = DEFAULT_TRACK_COLOR;
			}
			else if ( Double.isNaN( val.doubleValue() ) )
			{
				color = TrackMateModelView.DEFAULT_UNDEFINED_FEATURE_COLOR;
			}
			else
			{
				color = generator.getPaint( ( val - min ) / ( max - min ) );
			}
			colorMap.put( trackID, color );
		}
	}

	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		if ( !autoMode ) { return; }
		if ( event.getEventID() == ModelChangeEvent.MODEL_MODIFIED )
		{
			final Set< DefaultWeightedEdge > edges = event.getEdges();
			if ( edges.size() > 0 )
			{
				if ( null == feature )
				{
					refreshNull();
				}
				else if ( feature.equals( TrackIndexAnalyzer.TRACK_INDEX ) )
				{
					refreshIndex();
				}
				else
				{
					refresh();
				}
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
		{
			model.addModelChangeListener( this );
		}
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
		if ( feature.equals( TrackIndexAnalyzer.TRACK_INDEX ) )
		{
			refreshIndex();
		}
		else
		{
			refresh();
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
	}
}

