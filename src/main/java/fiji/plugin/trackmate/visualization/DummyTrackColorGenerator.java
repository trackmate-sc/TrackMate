package fiji.plugin.trackmate.visualization;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_TRACK_COLOR;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel;

import java.awt.Color;

import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * A dummy track color generator that always return the default color.
 *
 * @author Jean-Yves Tinevez - 2013
 */
public class DummyTrackColorGenerator implements TrackColorGenerator
{

	@Override
	public Color color( final DefaultWeightedEdge obj )
	{
		return DEFAULT_TRACK_COLOR;
	}

	@Override
	public void setFeature( final String feature )
	{}

	@Override
	public void terminate()
	{}

	@Override
	public void activate()
	{}

	@Override
	public void setCurrentTrackID( final Integer trackID )
	{}

	@Override
	public String getFeature()
	{
		return ColorByFeatureGUIPanel.UNIFORM_KEY;
	}

	@Override
	public double getMin()
	{
		return Double.NaN;
	}

	@Override
	public double getMax()
	{
		return Double.NaN;
	}

	@Override
	public void setMinMax( final double min, final double max )
	{}

	@Override
	public void autoMinMax()
	{}

	@Override
	public void setAutoMinMaxMode( final boolean autoMode )
	{}

	@Override
	public boolean isAutoMinMaxMode()
	{
		return false;
	}

	@Override
	public void setFrom( final MinMaxAdjustable minMaxAdjustable )
	{}

}
