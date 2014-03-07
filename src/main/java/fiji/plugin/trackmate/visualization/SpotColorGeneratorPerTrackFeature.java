package fiji.plugin.trackmate.visualization;

import java.awt.Color;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

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
		if (null == trackID) {
			return TrackMateModelView.DEFAULT_SPOT_COLOR;
		} else {
			return trackColorGenerator.colorOf( trackID );
		}
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

}
