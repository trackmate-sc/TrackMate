package fiji.plugin.trackmate.gui.wizard.descriptors;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.components.ActionChooserPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;
import fiji.plugin.trackmate.providers.ActionProvider;

public class ActionChooserDescriptor extends WizardPanelDescriptor
{

	private static final String KEY = "Actions";

	public ActionChooserDescriptor( final ActionProvider actionProvider, final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		super( KEY );
		this.targetPanel = new ActionChooserPanel( actionProvider, trackmate, selectionModel, displaySettings );
	}
}
