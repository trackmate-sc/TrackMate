package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateGUIModel;
import fiji.plugin.trackmate.gui.panels.ListChooserPanel;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewFactory;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class ViewChoiceDescriptor implements WizardPanelDescriptor
{

	private static final String KEY = "ChooseView";

	private final ListChooserPanel component;

	private final ViewProvider viewProvider;

	private final TrackMateGUIModel guimodel;

	private final TrackMateGUIController controller;

	public ViewChoiceDescriptor( final ViewProvider viewProvider, final TrackMateGUIModel guimodel, final TrackMateGUIController controller )
	{
		this.viewProvider = viewProvider;
		this.guimodel = guimodel;
		this.controller = controller;
		// Only views that are set to be visible in the menu.
		final List< String > visibleKeys = viewProvider.getVisibleViews();
		final List< String > viewerNames = new ArrayList< String >( visibleKeys.size() );
		final List< String > infoTexts = new ArrayList< String >( visibleKeys.size() );
		for ( final String key : visibleKeys )
		{
			infoTexts.add( viewProvider.getFactory( key ).getInfoText() );
			viewerNames.add( viewProvider.getFactory( key ).getName() );
		}
		this.component = new ListChooserPanel( viewerNames, infoTexts, "view" );
	}

	/*
	 * METHODS
	 */

	@Override
	public Component getComponent()
	{
		return component;
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
	{
		final int index = component.getChoice();
		final TrackMate trackmate = controller.getPlugin();
		final SelectionModel selectionModel = controller.getSelectionModel();
		new Thread( "TrackMate view rendering thread" )
		{
			@Override
			public void run()
			{
				final String viewName = viewProvider.getVisibleViews().get( index );

				// The HyperStack view is already used in the GUI, no need to
				// re-instantiate it.
				if ( viewName.equals( HyperStackDisplayer.KEY ) ) { return; }

				final ViewFactory factory = viewProvider.getFactory( viewName );
				final TrackMateModelView view = factory.create( trackmate.getModel(), trackmate.getSettings(), selectionModel );
				for ( final String settingKey : guimodel.getDisplaySettings().keySet() )
				{
					view.setDisplaySettings( settingKey, guimodel.getDisplaySettings().get( settingKey ) );
				}
				guimodel.addView( view );
				view.render();
			};
		}.start();
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
