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
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.CompositeImage;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.mesh.alg.zslicer.Contour;
import net.imglib2.mesh.alg.zslicer.Slice;
import net.imglib2.mesh.alg.zslicer.ZSlicer;

public class DebugZSlicer
{
	public static void main( final String[] args )
	{
		try
		{
			ImageJ.main( args );

			final String filePath = "samples/CElegans3D-smoothed-mask-orig-t7.xml";
			final TmXmlReader reader = new TmXmlReader( new File( filePath ) );
			if ( !reader.isReadingOk() )
			{
				System.err.println( reader.getErrorMessage() );
				return;
			}

			final ImagePlus imp = reader.readImage();
			imp.show();
			final double[] calibration = TMUtils.getSpatialCalibration( imp );

			final Model model = reader.getModel();
			final SelectionModel selection = new SelectionModel( model );
			final DisplaySettings ds = reader.getDisplaySettings();

			final HyperStackDisplayer view = new HyperStackDisplayer( model, selection, imp, ds );
			view.render();
			imp.setDisplayMode( CompositeImage.GRAYSCALE );

			final Spot spot = model.getSpots().iterable( true ).iterator().next();
			final double z = 21.;

			imp.setZ( ( int ) Math.round( z / calibration[ 2 ] ) + 1 );

			final Slice contours = ZSlicer.slice( ( ( SpotMesh ) spot ).getMesh(), z, calibration[ 2 ] );
			System.out.println( "Found " + contours.size() + " contours." );
			for ( final Contour contour : contours )
				System.out.println( contour );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}

