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
package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.ORANGE_ASTERISK_ICON;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.IOUtils;
import net.imglib2.mesh.Mesh;
import net.imglib2.mesh.Meshes;
import net.imglib2.mesh.impl.nio.BufferMesh;
import net.imglib2.mesh.io.ply.PLYMeshIO;
import net.imglib2.mesh.view.TranslateMesh;

public class MeshSeriesExporter extends AbstractTMAction
{

	public static final String NAME = "Export spot 3D meshes to a file series";

	public static final String KEY = "MESH_SERIES_EXPORTER";

	public static final String INFO_TEXT = "<html>" +
			"Export the 3D meshes in the spot of the current model "
			+ "to a PLY file series. "
			+ "<p>"
			+ "A folder is created with the file name, in which "
			+ "there will be one PLY file per time-point. "
			+ "The series can be easily imported in mesh visualization "
			+ "softwares, such as ParaView. "
			+ "<p> "
			+ "Only the visible spots containing 3D meshes are exported. "
			+ "If there are no such spots, no file is created. " +
			"</html>";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		logger.log( "Exporting spot 3D meshes to a file series.\n" );
		final Model model = trackmate.getModel();
		File file;
		final File folder = new File( System.getProperty( "user.dir" ) ).getParentFile().getParentFile();
		try
		{
			String filename = trackmate.getSettings().imageFileName;
			int i = filename.indexOf( "." );
			if ( i < 0 )
				i = filename.length();
			filename = filename.substring( 0, i );
			file = new File( folder.getPath() + File.separator + filename + "-meshes.ply" );
		}
		catch ( final NullPointerException npe )
		{
			file = new File( folder.getPath() + File.separator + "TrackMateMeshes.ply" );
		}
		file = IOUtils.askForFileForSaving( file, parent );
		if ( null == file )
		{
			logger.log( "Aborted.\n" );
			return;
		}

		exportMeshesToFileSeries( model.getSpots(), file, logger );
	}

	public static void exportMeshesToFileSeries( final SpotCollection spots, final File file, final Logger logger )
	{
		String folderName = file.getAbsolutePath();
		folderName = folderName.substring( 0, folderName.indexOf( "." ) );
		final File folder = new File( folderName );
		folder.mkdirs();
		
		final NavigableSet< Integer > frames = spots.keySet();
		for ( final Integer frame : frames )
		{
			final String fileName = folder.getName() + '_' + frame + ".ply";
			final File targetFile = new File( folder, fileName );
			final List< Mesh > meshes = new ArrayList<>();
			for ( final Spot spot : spots.iterable( frame, true ) )
			{
				if ( spot instanceof SpotMesh )
				{
					final SpotMesh sm = ( SpotMesh ) spot;
					meshes.add( TranslateMesh.translate( sm.getMesh(), spot ) );
				}
			}
			logger.log( " - Found " + meshes.size() + " meshes in frame " + frame + "." );
			final Mesh merged = Meshes.merge( meshes );
			final BufferMesh mesh = new BufferMesh( merged.vertices().size(), merged.triangles().size() );
			Meshes.calculateNormals( merged, mesh );
			try
			{
				PLYMeshIO.save( mesh, targetFile.getAbsolutePath() );
			}
			catch ( final IOException e )
			{
				logger.error( "\nProblem writing to " + targetFile + '\n' + e.getMessage() + '\n' );
				e.printStackTrace();
				continue;
			}
			logger.log( " Saved.\n" );
		}
		logger.log( "Done. Meshes saved to folder " + folder + '\n' );
	}

	@Plugin( type = TrackMateActionFactory.class, visible = true )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public ImageIcon getIcon()
		{
			return ORANGE_ASTERISK_ICON;
		}

		@Override
		public TrackMateAction create()
		{
			return new MeshSeriesExporter();
		}
	}
}
