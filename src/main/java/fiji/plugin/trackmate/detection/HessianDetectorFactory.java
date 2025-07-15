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

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_NORMALIZE;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_RADIUS_Z;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_NORMALIZE;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS_Z;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;

import java.util.Map;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.detector.HessianDetectorConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

@Plugin( type = SpotDetectorFactory.class )
public class HessianDetectorFactory< T extends RealType< T > & NativeType< T > > extends LogDetectorFactory< T >
{

	public static final String DETECTOR_KEY = "HESSIAN_DETECTOR";

	public static final String NAME = "Hessian detector";

	public static final String INFO_TEXT = "<html>"
			+ "This detector is based on computing the determinant of the <br>"
			+ "Hessian matrix of the image to detector bright blobs.  "
			+ "<p>"
			+ "It can be configured with a different spots size in XY and Z. <br>"
			+ "It can also return a normalized quality value, scaled from 0 <br>"
			+ "to 1 for the spots of each time-point."
			+ "<p>"
			+ "As discussed in Mikolajczyk et al.(2005), this detector has <br>"
			+ "a better edge response elimination than the LoG detector and is <br>"
			+ "suitable for detect spots in images with many strong edges."
			+ "</html>";

	@Override
	public SpotDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval, final int frame )
	{
		final double radiusXY = ( Double ) settings.get( KEY_RADIUS );
		final double radiusZ = ( Double ) settings.get( KEY_RADIUS_Z );
		final double thresholdQuality = ( Double ) settings.get( KEY_THRESHOLD );
		final boolean doSubpixel = ( Boolean ) settings.get( KEY_DO_SUBPIXEL_LOCALIZATION );
		final boolean normalize = ( Boolean ) settings.get( KEY_NORMALIZE );
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ) - 1;
		final RandomAccessibleInterval< T > imFrame = DetectionUtils.prepareFrameImg( img, channel, frame );

		final HessianDetector< T > detector = new HessianDetector<>(
				Views.extendMirrorDouble( imFrame ),
				interval,
				calibration,
				radiusXY,
				radiusZ,
				thresholdQuality,
				normalize,
				doSubpixel );
		detector.setNumThreads( 1 );
		return detector;
	}

	@Override
	public boolean forbidMultithreading()
	{
		return true;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings lSettings, final Model model )
	{
		return new HessianDetectorConfigurationPanel( lSettings, model, INFO_TEXT, NAME );
	}

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
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > lSettings = super.getDefaultSettings();
		lSettings.remove( KEY_DO_MEDIAN_FILTERING );
		lSettings.put( KEY_RADIUS_Z, DEFAULT_RADIUS_Z );
		lSettings.put( KEY_NORMALIZE, DEFAULT_NORMALIZE );
		return lSettings;
	}
}
