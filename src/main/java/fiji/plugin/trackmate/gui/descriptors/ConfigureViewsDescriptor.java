package fiji.plugin.trackmate.gui.descriptors;

import javax.swing.Action;

import fiji.plugin.trackmate.gui.FeatureDisplaySelector;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;

public class ConfigureViewsDescriptor implements WizardPanelDescriptor
{

	public static final String KEY = "ConfigureViews";

	private final ConfigureViewsPanel panel;

	private final TrackMateGUIController controller;

	public ConfigureViewsDescriptor(
			final DisplaySettings ds,
			final FeatureDisplaySelector featureSelector,
			final TrackMateGUIController controller,
			final Action launchTrackSchemeAction,
			final Action showTrackTablesAction,
			final Action showSpotTableAction )
	{
		this.controller = controller;
		this.panel = new ConfigureViewsPanel(
				ds, 
				featureSelector, 
				controller.getPlugin().getModel().getSpaceUnits(),
				launchTrackSchemeAction,
				showTrackTablesAction,
				showSpotTableAction );
	}

	@Override
	public ConfigureViewsPanel getComponent()
	{
		return panel;
	}

	@Override
	public void aboutToDisplayPanel()
	{}

	@Override
	public void displayingPanel()
	{
		controller.getGUI().setNextButtonEnabled( true );
	}

	@Override
	public void aboutToHidePanel()
	{}

	@Override
	public void comingBackToPanel()
	{}

	@Override
	public String getKey()
	{
		return KEY;
	}
}
