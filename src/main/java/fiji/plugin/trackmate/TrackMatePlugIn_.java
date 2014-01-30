package fiji.plugin.trackmate;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class TrackMatePlugIn_ implements PlugIn
{

	protected TrackMate trackmate;

	protected Settings settings;

	protected Model model;

	@Override
	public void run( final String arg0 )
	{

		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( null == imp ) { return; }
		GuiUtils.userCheckImpDimensions( imp );

		settings = createSettings( imp );
		model = createModel();
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

	/*
	 * HOOKS
	 */

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Model} instance that will be used to store data in the
	 * {@link TrackMate} instance.
	 *
	 * @return a new {@link Model} instance.
	 */

	protected Model createModel()
	{
		return new Model();
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Settings} instance that will be used to tune the
	 * {@link TrackMate} instance. It is initialized by default with values
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
		return new TrackMate( model, settings );
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
		new ImagePlus( "samples/FakeTracks.tif" ).show();
		new TrackMatePlugIn_().run( null );
	}

}
