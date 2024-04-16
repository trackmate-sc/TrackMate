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
package fiji.plugin.trackmate.detection;

import fiji.plugin.trackmate.TrackMatePlugIn;
import fiji.plugin.trackmate.gui.GuiUtils;
import ij.ImageJ;
import ij.plugin.frame.RoiManager;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class HessianDetectorTestDrive2
{
	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args )
	{
		ImageJ.main( args );
		GuiUtils.setSystemLookAndFeel();
		final RoiManager roiManager = RoiManager.getRoiManager();
		roiManager.runCommand( "Open", "samples/20220131-1435_Lv4TetOinCuO-C4_t-000-106_p005.ome_ALN_MarginsCropped-rois2.zip" );
		new TrackMatePlugIn().run( "samples/20220131-1435_Lv4TetOinCuO-C4_t-000-106_p005.ome_ALN_MarginsCropped-2.tif" );
	}
}
