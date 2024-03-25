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

import static fiji.plugin.trackmate.gui.Icons.VECTOR_ICON;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.TMUtils;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ComputeDistanceToRoiAction extends AbstractTMAction
{

	private static final String INFO_TEXT = "<html>"
			+ "Computes the distance from each spot to the closest ROI in the current frame."
			+ "<p>"
			+ "The distances are stored in a new spot feature and the distances are "
			+ "reported using physical distance units. "
			+ "<p>"
			+ "The ROIs are taken from the Roi Manager window currently opened. All the "
			+ "ROIs from the current time-point are considered, so the frame position of the "
			+ "ROIs must be properly set. If the frame position of a ROI is not set, it will "
			+ "be considered as being present for <i>all</i> time-points. "
			+ "The Z position of the ROIs is "
			+ "not taken into account. That is: the distances are calculated as if all "
			+ "the ROIs would be in the Z plane of the spot. "
			+ "</html>";

	private static final String NAME = "Compute distance to ROIs";

	private static final String KEY = "COMPUTE_DIST_TO_ROI";

	@Override
	public void execute(
			final TrackMate trackmate,
			final SelectionModel selectionModel,
			final DisplaySettings displaySettings,
			final Frame parent )
	{
		logger.log( "Computing distance from visible spots to closest ROI.\n" );
		// Get Roi Manager.
		final RoiManager rm = RoiManager.getInstance();
		if ( rm == null )
		{
			logger.error( "The Roi Manager window is not opened. Aborting.\n" );
			return;
		}
		// Get spatial calibration.
		final double[] calibration;
		if ( trackmate.getSettings() != null )
		{
			final Settings settings = trackmate.getSettings();
			if ( settings.imp != null )
			{
				calibration = TMUtils.getSpatialCalibration( settings.imp );
				logger.log( "Using pixel size from the target image.\n" );
			}
			else
			{
				calibration = new double[] { settings.dx, settings.dy, settings.dz };
				logger.log( "Using pixel size stored in the settings.\n" );
			}
		}
		else
		{
			calibration = new double[] { 1., 1., 1. };
			logger.log( "No image nor settings found. Using pixel distance.\n" );
		}

		// Compute
		final Model model = trackmate.getModel();
		computeDistance( model, rm, calibration );
		logger.log( "Done.\n" );
	}

	public static void computeDistance( final Model model, final RoiManager rm, final double[] calibration )
	{
		final Roi[] rois = rm.getRoisAsArray();
		computeDistance( model, rois, calibration );
	}

	public static void computeDistance( final Model model, final Roi[] rois, final double[] calibration )
	{
		/*
		 * Declare feature.
		 */
		final FeatureModel fm = model.getFeatureModel();
		fm.declareSpotFeatures(
				DistanceToRoiFeature.FEATURES,
				DistanceToRoiFeature.FEATURE_NAMES,
				DistanceToRoiFeature.FEATURE_SHORT_NAMES,
				DistanceToRoiFeature.FEATURE_DIMENSIONS,
				DistanceToRoiFeature.IS_INT );
		/*
		 * Compute.
		 */
		final SpotCollection spots = model.getSpots();
		final NavigableSet< Integer > timepoints = spots.keySet();
		for ( final Integer tp : timepoints )
		{
			final int frame = tp.intValue();

			// Filter ROIs by frame.
			final List< Roi > list = new ArrayList<>();
			for ( final Roi roi : rois )
			{
				// In IJ positions are 1-indexed.
				if ( roi.getTPosition() == 0 || roi.getTPosition() == frame + 1 )
					list.add( roi );
			}

			for ( final Spot spot : spots.iterable( frame, true ) )
			{
				final double d = distance( spot, list, calibration );
				spot.putFeature( DistanceToRoiFeature.FEATURE, Double.valueOf( d ) );
			}
		}

		// Notify.
		model.notifyFeaturesComputed();
	}

	public static double distance( final Spot spot, final List< Roi > list, final double[] calibration )
	{
		double distance = Double.POSITIVE_INFINITY;
		for ( final Roi roi : list )
		{
			final double d = distance( spot, roi, calibration );
			if ( d < distance )
				distance = d;
		}
		return distance;
	}

	public static double distance( final Spot spot, final Roi roi, final double[] calibration )
	{
		double min2 = Double.POSITIVE_INFINITY;
		// Brute force, but generic.
		final FloatPolygon polygon = roi.getInterpolatedPolygon();
		for ( int i = 0; i < polygon.npoints; i++ )
		{
			final double x = polygon.xpoints[ i ] * calibration[ 0 ];
			final double dx = spot.getDoublePosition( 0 ) - x;
			final double y = polygon.ypoints[ i ] * calibration[ 1 ];
			final double dy = spot.getDoublePosition( 1 ) - y;
			final double dd = dx * dx + dy * dy;
			if ( dd < min2 )
				min2 = dd;
		}
		return Math.sqrt( min2 );
	}

	@Plugin( type = TrackMateActionFactory.class )
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
			return VECTOR_ICON;
		}

		@Override
		public TrackMateAction create()
		{
			return new ComputeDistanceToRoiAction();
		}
	}

	/*
	 * Spot feature.
	 */

//	@Plugin( type = SpotAnalyzerFactory.class )
	public static class DistanceToRoiFeature< T extends RealType< T > & NativeType< T > > implements SpotAnalyzerFactory< T >
	{

		public static final String FEATURE = "DISTANCE_TO_ROI";

		public static final String NAME = "Distance to ROI";

		static final List< String > FEATURES = Collections.singletonList( FEATURE );

		static final Map< String, String > FEATURE_SHORT_NAMES = Collections.singletonMap( FEATURE, "Dist to ROI" );

		static final Map< String, String > FEATURE_NAMES = Collections.singletonMap( FEATURE, "Distance to ROI" );

		static final Map< String, Dimension > FEATURE_DIMENSIONS = Collections.singletonMap( FEATURE, Dimension.LENGTH );

		static final Map< String, Boolean > IS_INT = Collections.singletonMap( FEATURE, Boolean.FALSE );

		static final String INFO_TEXT = "<html>"
				+ "A dummy analyzer for the feature that stores "
				+ "the distance from a spot to a ROI.</html>";

		@Override
		public String getKey()
		{
			return NAME;
		}

		@Override
		public List< String > getFeatures()
		{
			return FEATURES;
		}

		@Override
		public Map< String, String > getFeatureShortNames()
		{
			return FEATURE_SHORT_NAMES;
		}

		@Override
		public Map< String, String > getFeatureNames()
		{
			return FEATURE_NAMES;
		}

		@Override
		public Map< String, Dimension > getFeatureDimensions()
		{
			return FEATURE_DIMENSIONS;
		}

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public Map< String, Boolean > getIsIntFeature()
		{
			return IS_INT;
		}

		@Override
		public boolean isManualFeature()
		{
			return true;
		}

		@Override
		public ImageIcon getIcon()
		{
			return null;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public SpotAnalyzer< T > getAnalyzer( final ImgPlus< T > img, final int frame, final int channel )
		{
			return SpotAnalyzer.dummyAnalyzer();
		}
	}
}
