package fiji.plugin.trackmate.gui.wizard.descriptors;

import java.util.function.Function;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.components.InitFilterPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;

public class InitFilterDescriptor extends WizardPanelDescriptor
{

	public static final String KEY = "InitialFiltering";

	private final TrackMate trackmate;

	public InitFilterDescriptor( final TrackMate trackmate, final FeatureFilter filter )
	{
		super( KEY );
		this.trackmate = trackmate;
		final Function< String, double[] > valuesCollector = key -> FeatureUtils.collectFeatureValues(
				Spot.QUALITY, TrackMateObject.SPOTS, trackmate.getModel(), trackmate.getSettings(), false );
		this.targetPanel = new InitFilterPanel( filter, valuesCollector );
	}

	@Override
	public Runnable getForwardRunnable()
	{
		return new Runnable()
		{

			@Override
			public void run()
			{
				trackmate.getModel().getLogger().log( "\nComputing spot quality histogram...\n", Logger.BLUE_COLOR );
				final InitFilterPanel component = ( InitFilterPanel ) targetPanel;
				component.refresh();
			}
		};
	}

	@Override
	public void aboutToHidePanel()
	{
		final InitFilterPanel component = ( InitFilterPanel ) targetPanel;
		trackmate.getSettings().initialSpotFilterValue = component.getFeatureThreshold().value;
	}
}
