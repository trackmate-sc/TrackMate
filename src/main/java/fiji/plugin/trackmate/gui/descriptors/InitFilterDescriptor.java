package fiji.plugin.trackmate.gui.descriptors;

import java.util.function.Function;

import javax.swing.Icon;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.ObjectType;
import fiji.plugin.trackmate.gui.panels.InitFilterPanel;

public class InitFilterDescriptor implements WizardPanelDescriptor
{

	private static final String KEY = "InitialFiltering";

	private final InitFilterPanel component;

	private final TrackMate trackmate;

	private final TrackMateGUIController controller;

	public InitFilterDescriptor( final TrackMate trackmate, final TrackMateGUIController controller, final FeatureFilter filter )
	{
		this.trackmate = trackmate;
		this.controller = controller;
		final Function< String, double[] > valuesCollector = key -> FeatureUtils.collectFeatureValues(
				Spot.QUALITY, ObjectType.SPOTS, trackmate.getModel(), trackmate.getSettings(), false );
		this.component = new InitFilterPanel( filter, valuesCollector );
	}

	@Override
	public InitFilterPanel getComponent()
	{
		return component;
	}

	@Override
	public void aboutToDisplayPanel()
	{}

	@Override
	public void displayingPanel()
	{
		new Thread( "TrackMate quality histogram calculation thread." )
		{
			@Override
			public void run()
			{
				final String oldText = controller.getGUI().getNextButton().getText();
				final Icon oldIcon = controller.getGUI().getNextButton().getIcon();
				controller.getGUI().getNextButton().setIcon( null );
				controller.getGUI().getNextButton().setText( "Please wait..." );

				trackmate.getModel().getLogger().log( "Computing spot quality histogram...\n", Logger.BLUE_COLOR );
				component.refresh();

				controller.getGUI().getNextButton().setText( oldText );
				controller.getGUI().getNextButton().setIcon( oldIcon );
				controller.getGUI().setNextButtonEnabled( true );
			}
		}.start();
	}

	@Override
	public void aboutToHidePanel()
	{
		trackmate.getSettings().initialSpotFilterValue = component.getFeatureThreshold().value;
	}

	@Override
	public void comingBackToPanel()
	{}

	@Override
	public String getKey()
	{
		return KEY;
	}
}
