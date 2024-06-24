package fiji.plugin.trackmate.util.cli;

import javax.swing.JLabel;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

import fiji.plugin.trackmate.gui.Fonts;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.util.gui.GenericDialogPlus;

@Plugin( type = Command.class,
		label = "Configure the path to the Conda executable used in TrackMate...",
		iconPath = "/icons/commands/information.png",
		menuPath = "Edit >  Options > Configure TrackMate Conda path..." )
public class CondaPathConfigCommand implements Command
{

	@Override
	public void run()
	{
		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );
		String findPath;
		try
		{
			findPath = CLIUtils.findDefaultCondaPath();
		}
		catch ( final IllegalArgumentException e )
		{
			findPath = "/usr/local/opt/micromamba/bin/micromamba";
		}

		String condaPath = prefs.get( CLIUtils.class, CLIUtils.CONDA_PATH_PREF_KEY, findPath );

		final GenericDialogPlus dialog = new GenericDialogPlus( "TrackMate Conda path" );
		final JLabel lbl = dialog.addImage( GuiUtils.scaleImage( Icons.TRACKMATE_ICON, 64, 64 ) );
		lbl.setText( "TrackMate Conda path" );
		lbl.setFont( Fonts.BIG_FONT );
		dialog.addMessage(
				"Browse to the conda (or mamba, micromaba, ...) executable \n"
				+ "to use in the TrackMate modules that rely on Conda." );
		dialog.addMessage( "Conda executable path:" );
		dialog.addFileField( "", condaPath, 40 );
		dialog.showDialog();

		if ( dialog.wasCanceled() )
			return;

		condaPath = dialog.getNextString();
		prefs.put( CLIUtils.class, CLIUtils.CONDA_PATH_PREF_KEY, condaPath );
	}

	public static void main( final String[] args )
	{
		TMUtils.getContext().getService( CommandService.class ).run( CondaPathConfigCommand.class, false );
	}

}
