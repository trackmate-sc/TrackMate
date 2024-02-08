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

import fiji.plugin.trackmate.TrackMatePlugIn;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class Demo3DMeshTrackMate
{

	public static void main( final String[] args )
	{
		try
		{

			ImageJ.main( args );
//			final String filePath = "samples/CElegans3D-smoothed-mask-orig-t7.tif";
			final String filePath = "samples/Celegans-5pc-17timepoints.tif";
			final ImagePlus imp = IJ.openImage( filePath );
			imp.show();

			new TrackMatePlugIn().run( null );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
