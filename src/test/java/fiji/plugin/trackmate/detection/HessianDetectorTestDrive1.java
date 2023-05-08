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

import java.util.List;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class HessianDetectorTestDrive1
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args )
	{
		ImageJ.main( args );
		GuiUtils.setSystemLookAndFeel();

//			final ImagePlus imp = IJ.openImage( "samples/Celegans-5pc-17timepoints-t13.tif" );
		final ImagePlus imp = IJ.openImage( "samples/TSabateCell.tif" );
		imp.show();

		final ImgPlus< T > input = TMUtils.rawWraps( imp );
		final double[] calibration = TMUtils.getSpatialCalibration( imp );
		final double radiusXY = 0.6 / 2.; // um;
		final double radiusZ = 1.6 / 2.;
		final double threshold = 0.2;
		final boolean normalize = true; // scale quality to 0 - 1.

		final HessianDetector< T > detector = new HessianDetector<>(
				Views.extendMirrorDouble( input ),
				input,
				calibration,
				radiusXY,
				radiusZ,
				threshold,
				normalize,
				true );
		if ( !detector.checkInput() || !detector.process() )
		{
			System.err.println( "Processing failed:\n" + detector.getErrorMessage() );
			return;
		}

		final List< Spot > spots = detector.getResult();
		spots.sort( Spot.featureComparator( Spot.QUALITY ) );
		spots.forEach( s -> System.out.println( String.format( " - %3d: Q = %7.3f, L = %s",
				s.ID(),
				s.getFeature( Spot.QUALITY ),
				Util.printCoordinates( s ) ) ) );

		final SpotCollection sc = new SpotCollection();
		sc.put( 0, spots );
		sc.setVisible( true );
		final Model model = new Model();
		model.setPhysicalUnits( imp.getCalibration().getUnit(), imp.getCalibration().getTimeUnit() );
		model.setSpots( sc, false );
		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		final String feature = Spot.QUALITY;
		ds.setSpotColorBy( TrackMateObject.SPOTS, feature );
		final double[] mm = FeatureUtils.autoMinMax( model, TrackMateObject.SPOTS, feature );
		ds.setSpotMinMax( mm[ 0 ], mm[ 1 ] );
		final HyperStackDisplayer displayer = new HyperStackDisplayer( model, selectionModel, imp, ds );
		displayer.render();
		displayer.refresh();
	}
}
