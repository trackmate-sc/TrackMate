package fiji.plugin.trackmate;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

import javax.swing.JOptionPane;

public class TrackMatePlugIn_ implements PlugIn
{

	protected TrackMate trackmate;

	protected Settings settings;

	@Override
	public void run( final String arg0 )
	{

		final ImagePlus imp = WindowManager.getCurrentImage();

		final int[] dims = imp.getDimensions();
		if ( dims[ 4 ] == 1 && dims[ 3 ] > 1 )
		{
			switch ( JOptionPane.showConfirmDialog( null, "It appears this image has 1 timepoint but " + dims[ 3 ] + " slices.\n" + "Do you want to swap Z and T?", "Z/T swapped?", JOptionPane.YES_NO_CANCEL_OPTION ) )
			{
			case JOptionPane.YES_OPTION:
				imp.setDimensions( dims[ 2 ], dims[ 4 ], dims[ 3 ] );
				final Calibration calibration = imp.getCalibration();
				if ( calibration.frameInterval == 0 )
				{
					calibration.frameInterval = 1;
				}
				break;
			case JOptionPane.CANCEL_OPTION:
				return;
			}
		}

		settings = createSettings( imp );
		trackmate = createTrackMate();

		/*
		 * Launch GUI.
		 */

		final TrackMateGUIController controller = new TrackMateGUIController( trackmate );
		if ( imp != null )
		{
			GuiUtils.positionWindow( controller.getGUI(), imp.getWindow() );
		}
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Settings} instance that will be used to tune the
	 * {@link TrackMate} instance. It is iniatialized by default with values
	 * taken from the current {@link ImagePlus}.
	 * 
	 * @return a new {@link Settings} instance.
	 */
	protected Settings createSettings( final ImagePlus imp )
	{
		final Settings settings = new Settings();
		settings.setFrom( imp );
		return settings;
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the TrackMate instance that will be controlled in the GUI.
	 * 
	 * @return a new {@link TrackMate} instance.
	 */
	protected TrackMate createTrackMate()
	{
		return new TrackMate( settings );
	}

	/*
	 * MAIN METHOD
	 */

	/**
	 * @param args
	 */
	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new TrackMatePlugIn_().run( null );
	}

}
