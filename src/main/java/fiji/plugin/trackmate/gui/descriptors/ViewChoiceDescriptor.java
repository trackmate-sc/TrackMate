package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
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
		final String oldText = controller.getGUI().getNextButton().getText();
		final Icon oldIcon = controller.getGUI().getNextButton().getIcon();
		controller.getGUI().getNextButton().setText( "Please wait..." );
		controller.getGUI().getNextButton().setIcon( null );
		new Thread( "TrackMate spot feature calculation thread." )
		{
			@Override
			public void run()
			{
				final TrackMate trackmate = controller.getPlugin();
				final Model model = trackmate.getModel();
				final Logger logger = model.getLogger();
				final String str = "Initial thresholding with a quality threshold above " + String.format( "%.1f", trackmate.getSettings().initialSpotFilterValue ) + " ...\n";
				logger.log( str, Logger.BLUE_COLOR );
				final int ntotal = model.getSpots().getNObjects( false );
				trackmate.execInitialSpotFiltering();
				final int nselected = model.getSpots().getNObjects( false );
				logger.log( String.format( "Retained %d spots out of %d.\n", nselected, ntotal ) );

				/*
				 * We have some spots so we need to compute spot features will
				 * we render them.
				 */
				logger.log( "Calculating spot features...\n", Logger.BLUE_COLOR );
				// Calculate features
				final long start = System.currentTimeMillis();
				trackmate.computeSpotFeatures( true );
				final long end = System.currentTimeMillis();
				logger.log( String.format( "Calculating features done in %.1f s.\n", ( end - start ) / 1e3f ), Logger.BLUE_COLOR );
				controller.getGUI().getNextButton().setText( oldText );
				controller.getGUI().getNextButton().setIcon( oldIcon );
				controller.getGUI().setNextButtonEnabled( true );
			}
		}.start();
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
