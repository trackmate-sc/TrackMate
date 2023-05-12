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

import java.awt.Frame;
import java.util.Arrays;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

public class IJRoiExporter
{

	private final Logger logger;

	private final RoiManager roiManager;

	private final double dx;

	private final double dy;

	private final double dz;

	private final boolean is2D;

	public IJRoiExporter( final ImagePlus imp, final Logger logger )
	{
		this.dx = imp.getCalibration().pixelWidth;
		this.dy = imp.getCalibration().pixelHeight;
		this.dz = imp.getCalibration().pixelDepth;
		this.is2D = DetectionUtils.is2D( imp );
		this.logger = logger;
		this.roiManager = RoiManager.getRoiManager();
	}

	public void export( final Iterable< Spot > spots )
	{
		final int nspots = count( spots );
		logger.log( "Exporting " + nspots + " spots to ImageJ ROIs.\n" );
		logger.setStatus( "Exporting" );
		int index = 0;
		for ( final Spot spot : spots )
		{
			export( spot );
			logger.setProgress( ( double ) index++ / nspots );
		}
		logger.setProgress( 1. );
		logger.setStatus( "" );
		logger.log( "Done.\n" );
	}

	public void export( final Spot spot )
	{
		final Roi roi;
		if ( spot instanceof SpotRoi )
		{
			final SpotRoi sroi = ( SpotRoi ) spot;
			final double[][] out = sroi.toArray( 0., 0., 1 / dx, 1 / dy );
			final float[] xp = toFloat( out[ 0 ] );
			final float[] yp = toFloat( out[ 1 ] );
			roi = new PolygonRoi( xp, yp, PolygonRoi.POLYGON );
		}
		else
		{
			final double diameter = 2. * spot.getFeature( Spot.RADIUS ).doubleValue() / dx;
			final double xs = spot.getDoublePosition( 0 ) / dx - diameter / 2. + 0.5;
			final double ys = spot.getDoublePosition( 1 ) / dy - diameter / 2. + 0.5;
			roi = new OvalRoi( xs, ys, diameter, diameter );
		}

		final int z = is2D ? 0 : 1 + ( int ) Math.round( spot.getDoublePosition( 2 ) / dz );
		final int frame = 1 + spot.getFeature( Spot.FRAME ).intValue();
		roi.setPosition( 0, z, frame );
		roi.setName( spot.getName() );
		roiManager.addRoi( roi );
	}

	private static final float[] toFloat( final double[] d )
	{
		final float[] f = new float[ d.length ];
		for ( int i = 0; i < f.length; i++ )
			f[ i ] = ( float ) d[ i ];
		return f;
	}

	private static final int count( final Iterable< Spot > spots )
	{
		int count = 0;
		for ( @SuppressWarnings( "unused" )
		final Spot spot : spots )
			count++;
		return count;
	}

	public static class IJRoiExporterAction extends AbstractTMAction
	{

		@Override
		public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
		{
			// Show dialog.
			final GenericDialog dialog = new GenericDialog( "Export spots to IJ ROIs", parent );

			dialog.addMessage( "Export to IJ ROIs for:" );
			final String[] choices = new String[] { "All spots", "Selection", "Tracks of selection" };
			dialog.addRadioButtonGroup( "Dimensionality:", choices, 2, 1, choices[ 1 ] );

			// Show & Read user input
			dialog.showDialog();
			if ( dialog.wasCanceled() )
				return;

			// Execute.
			final Iterable< Spot > spots;
			final int choice = Arrays.asList( choices ).indexOf( dialog.getNextRadioButton() );
			if ( choice == 0 )
				spots = trackmate.getModel().getSpots().iterable( true );
			else if ( choice == 1 )
				spots = selectionModel.getSpotSelection();
			else
			{
				selectionModel.selectTrack(
						selectionModel.getSpotSelection(),
						selectionModel.getEdgeSelection(), 0 );
				spots = selectionModel.getSpotSelection();
			}

			final IJRoiExporter exporter = new IJRoiExporter( trackmate.getSettings().imp, logger );
			exporter.export( spots );
		}
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class IJRoiExporterActionFactory implements TrackMateActionFactory
	{

		private static final String INFO_TEXT = "<html>"
				+ "Export the spots to ImageJ ROIs and store them in the ROI manager. "
				+ "<p>"
				+ "Spots with ROIs are exported as polygon-ROIs. Other spots are exported as "
				+ "an oval spot, located in a single Z-plane at the spot center. "
				+ "</html>";

		private static final String NAME = "Export spots to IJ ROIs";

		private static final String KEY = "EXPORT_TO_ROIS";

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
			return Icons.ORANGE_ASTERISK_ICON;
		}

		@Override
		public TrackMateAction create()
		{
			return new IJRoiExporterAction();
		}
	}
}
