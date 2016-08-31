package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.ManualEdgeColorGenerator;
import fiji.plugin.trackmate.visualization.ManualSpotColorGenerator;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;

public class ConfigureViewsDescriptor implements WizardPanelDescriptor
{

	public static final String KEY = "ConfigureViews";

	private final ConfigureViewsPanel panel;

	private final TrackMateGUIController controller;

	public ConfigureViewsDescriptor( final TrackMate trackmate, final FeatureColorGenerator< Spot > spotColorGenerator, final PerEdgeFeatureColorGenerator edgeColorGenerator, final PerTrackFeatureColorGenerator trackColorGenerator, final FeatureColorGenerator< Spot > spotColorGeneratorPerTrackFeature, final ManualSpotColorGenerator manualSpotColorGenerator, final ManualEdgeColorGenerator manualEdgeColorGenerator, final TrackMateGUIController controller )
	{
		this.controller = controller;
		this.panel = new ConfigureViewsPanel( trackmate.getModel() );
		panel.setSpotColorGenerator( spotColorGenerator );
		panel.setEdgeColorGenerator( edgeColorGenerator );
		panel.setTrackColorGenerator( trackColorGenerator );
		panel.setManualSpotColorGenerator( manualSpotColorGenerator );
		panel.setManualEdgeColorGenerator( manualEdgeColorGenerator );
		panel.setSpotColorGeneratorPerTrackFeature( spotColorGeneratorPerTrackFeature );
	}

	@Override
	public ConfigureViewsPanel getComponent()
	{
		return panel;
	}

	@Override
	public void aboutToDisplayPanel()
	{
		panel.refreshGUI();
		controller.getGUI().setNextButtonEnabled( true );
	}

	@Override
	public void displayingPanel()
	{
		panel.refreshColorFeatures();
	}

	@Override
	public void aboutToHidePanel()
	{}

	@Override
	public void comingBackToPanel()
	{
		panel.refreshGUI();
		panel.refreshColorFeatures();
	}

	@Override
	public String getKey()
	{
		return KEY;
	}
}
