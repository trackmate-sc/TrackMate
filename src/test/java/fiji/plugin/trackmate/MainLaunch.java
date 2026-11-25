package fiji.plugin.trackmate;

import java.io.IOException;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.MaskDetectorFactory;
import fiji.plugin.trackmate.gui.wizard.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.io.geff.GeffTestBase;
import fiji.plugin.trackmate.io.geff.TmGeffWriter;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.jaqaman.SparseLAPTrackerFactory;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class MainLaunch extends GeffTestBase
{

	public static void main( final String[] args )
	{
		launch( args );
	}

	private static void launch( final String[] args )
	{
		try
		{
			if ( IJ.isMacintosh() )
				FlatMacLightLaf.setup();
			else
				FlatLightLaf.setup();
			ImageJ.main( args );
			new TrackMatePlugIn().run( "samples/MAX_Merged.tif" );
//			new TrackMatePlugIn().run( "samples/FakeTracks.tif" );
//			new TrackMatePlugIn().run( "samples/CElegans3D-smoothed-mask-orig.tif" );
		}
		catch ( final Throwable t )
		{
			t.printStackTrace();
		}
	}

	@SuppressWarnings( "unused" )
	private static void autoExecAndSave()
	{
		final String imagePath = "samples/MAX_Merged.tif";
		final String geffPath = imagePath.replace( ".tif", ".geff" );

		final ImagePlus imp = IJ.openImage( imagePath );
		imp.killRoi();
		final Settings settings = new Settings( imp );
		settings.detectorFactory = new MaskDetectorFactory<>();
		settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
		settings.detectorSettings.put( DetectorKeys.KEY_TARGET_CHANNEL, 2 );
		settings.trackerFactory = new SparseLAPTrackerFactory();
		settings.trackerSettings = settings.trackerFactory.getDefaultSettings();
		settings.trackerSettings.put( TrackerKeys.KEY_ALLOW_TRACK_SPLITTING, true );
		settings.addAllAnalyzers();

		final TrackMate trackmate = new TrackMate( settings );
		if ( !trackmate.checkInput() )
		{
			System.err.println( "Invalid settings: " + trackmate.getErrorMessage() );
			return;
		}
		if ( !trackmate.process() )
		{
			System.err.println( "Processing failed: " + trackmate.getErrorMessage() );
			return;
		}
		final TmGeffWriter writer = new TmGeffWriter( geffPath );
		writer.appendSettings( settings );
		writer.appendModel( trackmate.getModel() );
		writer.appendGUIState( ConfigureViewsDescriptor.KEY );
		try
		{
			writer.write();
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		System.out.println( "Saved to: " + geffPath );
	}
}
