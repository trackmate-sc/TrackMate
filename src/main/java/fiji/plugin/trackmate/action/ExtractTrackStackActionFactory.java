package fiji.plugin.trackmate.action;

import java.util.Arrays;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import ij.gui.GenericDialog;

@Plugin( type = TrackMateActionFactory.class )
public class ExtractTrackStackActionFactory implements TrackMateActionFactory
{

	private static double diameterFactor = 1.5d;

	private static int dimChoice = 0;

	@Override
	public String getInfoText()
	{
		return ExtractTrackStackAction.INFO_TEXT;
	}

	@Override
	public String getName()
	{
		return ExtractTrackStackAction.NAME;
	}

	@Override
	public String getKey()
	{
		return ExtractTrackStackAction.KEY;
	}

	@Override
	public ImageIcon getIcon()
	{
		return ExtractTrackStackAction.ICON;
	}

	@Override
	public TrackMateAction create( final TrackMateGUIController controller )
	{
		final GenericDialog dialog = new GenericDialog( "Extract track stack", controller.getGUI() );

		// Radius factor
		dialog.addSlider( "Image size (spot\ndiameter units):", 0.1, 5.1, diameterFactor );

		// Central slice vs 3D
		final String[] dimChoices = new String[] { "Central slice ", "3D" };
		dialog.addRadioButtonGroup( "Dimensionality:", dimChoices, 2, 1, dimChoices[ dimChoice ] );

		// Show & Read user input
		dialog.showDialog();
		if ( dialog.wasCanceled() )
		{
			// Return dummy action.
			return new TrackMateAction()
			{
				@Override
				public void setLogger( final Logger logger )
				{}

				@Override
				public void execute( final TrackMate trackmate )
				{}
			};
		}

		diameterFactor = dialog.getNextNumber();
		dimChoice = Arrays.asList( dimChoices ).indexOf( dialog.getNextRadioButton() );
		final boolean do3D = dimChoice == 1;

		return new ExtractTrackStackAction( controller.getSelectionModel(), diameterFactor, do3D );
	}

}
