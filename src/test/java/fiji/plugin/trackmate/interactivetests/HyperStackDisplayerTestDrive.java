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
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackDisplayMode;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.ImagePlus;

public class HyperStackDisplayerTestDrive
{

	public static void main( final String[] args )
	{

		ij.ImageJ.main( args );

		final File file = new File( AppUtils.getBaseDirectory( TrackMate.class ), "samples/FakeTracks.xml" );
		final TmXmlReader reader = new TmXmlReader( file );

		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		final Settings settings = reader.readSettings( imp );

		final DisplaySettings ds = DisplaySettings.defaultStyle().copy();
		ds.setSpotShowName( true );
		ds.setTrackDisplayMode( TrackDisplayMode.LOCAL_BACKWARD );

		new ModelFeatureUpdater( model, settings );
		final SelectionModel selectionModel = new SelectionModel( model );
		final HyperStackDisplayer displayer = new HyperStackDisplayer( model, selectionModel, imp, ds );
		displayer.render();

		final TrackScheme trackScheme = new TrackScheme( model, selectionModel, ds );
		trackScheme.render();
	}
}
