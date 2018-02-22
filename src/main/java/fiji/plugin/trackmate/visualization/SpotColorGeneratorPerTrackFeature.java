package fiji.plugin.trackmate.visualization;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

import java.awt.Color;

public class SpotColorGeneratorPerTrackFeature implements FeatureColorGenerator< Spot >
{

	private final PerTrackFeatureColorGenerator trackColorGenerator;

	private final Model model;

	public SpotColorGeneratorPerTrackFeature( final Model model, final String feature )
	{
		this.model = model;
		this.trackColorGenerator = new PerTrackFeatureColorGenerator( model, feature );
	}

	@Override
	public Color color( final Spot spot )
	{
		final Integer trackID = model.getTrackModel().trackIDOf( spot );
		if ( null == trackID )
			return TrackMateModelView.DEFAULT_SPOT_COLOR;

		return trackColorGenerator.colorOf( trackID );
	}

	@Override
	public void setFeature( final String feature )
	{
		trackColorGenerator.setFeature( feature );
	}

	@Override
	public String getFeature()
	{
		return trackColorGenerator.getFeature();
	}

	@Override
	public void terminate()
	{
		trackColorGenerator.terminate();
	}

	@Override
	public void activate()
	{
		trackColorGenerator.activate();
	}

	/*
	 * MINMAXADJUSTABLE
	 */

	@Override
	public double getMin()
	{
		return trackColorGenerator.getMin();
	}

	@Override
	public double getMax()
	{
		return trackColorGenerator.getMax();
	}

	@Override
	public void setMinMax( final double min, final double max )
	{
		trackColorGenerator.setMinMax( min, max );
	}

	@Override
	public void autoMinMax()
	{
		trackColorGenerator.autoMinMax();
	}

	@Override
	public void setAutoMinMaxMode( final boolean autoMode )
	{
		trackColorGenerator.setAutoMinMaxMode( autoMode );
	}

	@Override
	public boolean isAutoMinMaxMode()
	{
		return trackColorGenerator.isAutoMinMaxMode();
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
