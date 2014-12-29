package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.panels.InitFilterPanel;

import javax.swing.Icon;

public class InitFilterDescriptor implements WizardPanelDescriptor
{

	private static final String KEY = "InitialFiltering";

	private final InitFilterPanel component;

	private final TrackMate trackmate;

	private final TrackMateGUIController controller;

	public InitFilterDescriptor( final TrackMate trackmate, final TrackMateGUIController controller )
	{
		this.trackmate = trackmate;
		this.controller = controller;
		this.component = new InitFilterPanel();
	}

	@Override
	public InitFilterPanel getComponent()
	{
		return component;
	}

	@Override
	public void aboutToDisplayPanel()
	{
}

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
				final long start = System.currentTimeMillis();
				final SpotCollection spots = trackmate.getModel().getSpots();

				final double[] values = new double[ spots.getNSpots( false ) ];
				int index = 0;
				for ( final Spot spot : spots.iterable( false ) )
				{
					values[ index++ ] = spot.getFeature( Spot.QUALITY );
				}
				component.setValues( values );

				final Double initialFilterValue = trackmate.getSettings().initialSpotFilterValue;
				component.setInitialFilterValue( initialFilterValue );
				final long end = System.currentTimeMillis();
				trackmate.getModel().getLogger().log( String.format( "Histogram calculated in %.1f s.\n", ( end - start ) / 1e3f ), Logger.BLUE_COLOR );

				controller.getGUI().getNextButton().setText( oldText );
				controller.getGUI().getNextButton().setIcon( oldIcon );
				controller.getGUI().setNextButtonEnabled( true );

			}
		}.start();
	}

	@Override
	public void aboutToHidePanel()
	{

		final FeatureFilter initialThreshold = component.getFeatureThreshold();
		trackmate.getSettings().initialSpotFilterValue = initialThreshold.value;

		/*
		 * We will do the filtering in the next panel.
		 */
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
