/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.interactivetests;

import java.io.File;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.Spot2DMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.Spot3DMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
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
				new Spot2DMorphologyAnalyzerProvider( imp.getNChannels() ),
				new Spot3DMorphologyAnalyzerProvider( imp.getNChannels() ) );

		System.out.println( settings );
		System.out.println( model );
		System.out.println( model.getFeatureModel().echo() );

	}

}
