package fiji.plugin.trackmate.gui.wizard.descriptors;

import javax.swing.Action;

import fiji.plugin.trackmate.gui.FeatureDisplaySelector;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor2;

public class ConfigureViewsDescriptor extends WizardPanelDescriptor2
{

	public static final String KEY = "ConfigureViews";

	public ConfigureViewsDescriptor(
			final DisplaySettings ds,
			final FeatureDisplaySelector featureSelector,
			final Action launchTrackSchemeAction,
			final Action showTrackTablesAction,
			final Action showSpotTableAction,
			final String spaceUnits )
	{
		super( KEY );
		this.targetPanel = new ConfigureViewsPanel(
				ds, 
				featureSelector, 
				spaceUnits,
				launchTrackSchemeAction,
				showTrackTablesAction,
				showSpotTableAction );
	}
}
