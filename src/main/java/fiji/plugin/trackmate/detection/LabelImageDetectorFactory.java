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

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;

import java.util.HashMap;
import java.util.Map;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.detector.LabelImageDetectorConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class )
public class LabelImageDetectorFactory< T extends RealType< T > & NativeType< T >> implements SpotDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "LABEL_IMAGE_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Label image detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector creates spots by importing regions from a label image."
			+ "<p>"
			+ "A label image is an image where the pixel values are integers. Each object "
			+ "in a label image is represented by a single common pixel value (the label) "
			+ "that is unique to the object."
			+ "<p>"
			+ "This detector reads such an image and create spots from each object. "
			+ "In 2D the contour of a label is imported. In 3D, spherical spots "
			+ "of the same volume that the label are created."
			+ "<p>"
			+ "The spot quality stores the object area or volume in pixels."
			+ "</html>";

	public static final String URL_DOC = "https://imagej.net/plugins/trackmate/detectors/trackmate-label-image-detector";

	/*
	 * METHODS
	 */

	@Override
	public SpotDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval, final int frame )
	{
		final boolean simplifyContours = ( Boolean ) settings.get( KEY_SIMPLIFY_CONTOURS );
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ) - 1;
		final RandomAccessible< T > imFrame = DetectionUtils.prepareFrameImg( img, channel, frame );

		final LabelImageDetector< T > detector = new LabelImageDetector<>(
				imFrame,
				interval,
				calibration,
				simplifyContours );
		return detector;
	}

	@Override
	public boolean has2Dsegmentation()
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
		return new LabelImageDetectorConfigurationPanel( lSettings, model );
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
	public String getUrl()
	{
		return URL_DOC;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > lSettings = new HashMap<>();
		lSettings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		lSettings.put( KEY_SIMPLIFY_CONTOURS, true );
		return lSettings;
	}
}
