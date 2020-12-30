package fiji.plugin.trackmate.interactivetests;

import java.io.File;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import ij.ImagePlus;

public class TmXmlReaderTestDrive
{

	public static void main( final String args[] )
	{

		final File file = new File( AppUtils.getBaseDirectory( TrackMate.class ), "samples/FakeTracks.xml" );
		System.out.println( "Opening file: " + file.getAbsolutePath() );
		final TmXmlReader reader = new TmXmlReader( file );
		final Model model = reader.getModel();

		final ImagePlus imp = reader.readImage();

		final Settings settings = reader.readSettings(
				imp,
				new DetectorProvider(),
				new TrackerProvider(),
				new SpotAnalyzerProvider( imp.getNChannels() ),
				new EdgeAnalyzerProvider(),
				new TrackAnalyzerProvider(),
				new SpotMorphologyAnalyzerProvider( imp.getNChannels() ) );

		System.out.println( settings );
		System.out.println( model );
		System.out.println( model.getFeatureModel().echo() );

	}

}
