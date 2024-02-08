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
package fiji.plugin.trackmate.mesh;

import java.io.File;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.MaskDetectorFactory;
import fiji.plugin.trackmate.detection.ThresholdDetectorFactory;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.mesh.io.stl.STLMeshIO;

public class ExportMeshForDemo
{

	public static void main( final String[] args )
	{
		try
		{
			final String filePath = "samples/mesh/CElegansMask3D.tif";
			final ImagePlus imp = IJ.openImage( filePath );

			final Settings settings = new Settings( imp );
			settings.detectorFactory = new MaskDetectorFactory<>();
			settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
			settings.detectorSettings.put( ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS, false );

			final TrackMate trackmate = new TrackMate( settings );
			trackmate.setNumThreads( 4 );
			trackmate.execDetection();

			final Model model = trackmate.getModel();
			final SpotCollection spots = model.getSpots();
			spots.setVisible( true );

			final String meshDir = "samples/mesh/io";
			for ( final File file : new File( meshDir ).listFiles() )
				if ( !file.isDirectory() )
					file.delete();

			for ( final Spot spot : spots.iterable( true ) )
			{
				final int t = spot.getFeature( Spot.FRAME ).intValue();
				final int id = spot.ID();
				final String savePath = String.format( "%s/mesh_t%2d_id_%04d.stl", meshDir, t, id );
				if ( spot instanceof SpotMesh )
				{
					final SpotMesh mesh = ( SpotMesh ) spot;
					STLMeshIO.save( mesh.getMesh(), savePath );
				}
			}
			System.out.println( "Export done." );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
