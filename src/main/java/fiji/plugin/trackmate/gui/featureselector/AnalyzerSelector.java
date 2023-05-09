package fiji.plugin.trackmate.gui.featureselector;

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import javax.swing.JDialog;
import javax.swing.JFrame;

public class AnalyzerSelector
{

	private final JDialog dialog;

	private final AnalyzerSelectorPanel gui;

	public AnalyzerSelector()
	{
		dialog = new JDialog( ( JFrame ) null, "TrackMate feature analyzers selection" );
		dialog.setLocationByPlatform( true );
		dialog.setLocationRelativeTo( null );
		gui = new AnalyzerSelectorPanel( AnalyzerSelectionIO.readUserDefault() );
		dialog.getContentPane().add( gui );
		dialog.setIconImage( TRACKMATE_ICON.getImage() );
		dialog.pack();
	}

	public JDialog getDialog()
	{
		return dialog;
	}

	public static void main( final String[] args )
	{
		new AnalyzerSelector().getDialog().setVisible( true );
	}
}
