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

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.detector.ThresholdDetectorConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class )
public class ThresholdDetectorFactory< T extends RealType< T > & NativeType< T >> implements SpotDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "THRESHOLD_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Thresholding detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector creates spots by thresholding a grayscale image."
			+ "<p>"
			+ "Pixels in the designated channel that have "
			+ "a value larger than the threshold are considered as part of the foreground, "
			+ "and used to build connected regions. In 2D, spots are created with "
			+ "the (possibly simplified) contour of the region. In 3D, a mesh is "
			+ "created for each region."
			+ "<p>"
			+ "The spot quality stores the object area or volume in pixels."
			+ "</html>";

	public static final String URL_DOC = "https://imagej.net/plugins/trackmate/detectors/trackmate-thresholding-detector";

	public static final ImageIcon ICON = new ImageIcon( Icons.class.getResource( "images/LabelImageDetector-icon-64px.png" ) );

	public static final String KEY_SIMPLIFY_CONTOURS = "SIMPLIFY_CONTOURS";

	/**
	 * If strictly larger than 0, the mask will be smoothed before creating the
	 * mesh, resulting in smoother meshes. The scale value sets the (Gaussian)
	 * filter radius and is specified in physical units. If 0 or lower than 0,
	 * no smoothing is applied.
	 */
	public static final String KEY_SMOOTHING_SCALE = "SMOOTHING_SCALE";

	public static final String KEY_INTENSITY_THRESHOLD = "INTENSITY_THRESHOLD";

	/*
	 * METHODS
	 */

	@Override
	public SpotDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval, final int frame )
	{
		final double intensityThreshold = ( Double ) settings.get( KEY_INTENSITY_THRESHOLD );
		final boolean simplifyContours = ( Boolean ) settings.get( KEY_SIMPLIFY_CONTOURS );
		final double smoothingScale = ( Double ) settings.get( KEY_SMOOTHING_SCALE );
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ) - 1;
		final RandomAccessible< T > imFrame = DetectionUtils.prepareFrameImg( img, channel, frame );

		final ThresholdDetector< T > detector = new ThresholdDetector<>(
				imFrame,
				interval,
				calibration,
				intensityThreshold,
				simplifyContours,
				smoothingScale );
		detector.setNumThreads( 1 );
		return detector;
	}

	@Override
	public boolean has2Dsegmentation()
	{
		return true;
	}

	@Override
	public boolean has3Dsegmentation()
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
		return new ThresholdDetectorConfigurationPanel( lSettings, model );
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
	public ImageIcon getIcon()
	{
		return ICON;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > lSettings = new HashMap<>();
		lSettings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		lSettings.put( KEY_INTENSITY_THRESHOLD, 0. );
		lSettings.put( KEY_SIMPLIFY_CONTOURS, true );
		lSettings.put( KEY_SMOOTHING_SCALE, -1. );
		return lSettings;
	}
}
