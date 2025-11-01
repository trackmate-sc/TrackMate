/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2025 TrackMate developers.
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
package fiji.plugin.trackmate.util.cli.appose;

import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryGenericConfig;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.detection.SpotGlobalDetectorFactory;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.HIGH )
public class StarDistDetectorFactory< T extends RealType< T > & NativeType< T > >
		implements SpotGlobalDetectorFactory< T >, SpotDetectorFactoryGenericConfig< T, StarDistAppose >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "APPOSE_STARDIST_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Appose StarDist 3D detector";

	public static final String DOC_STARDIST_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-appose-stardist";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on StarDist 3D to detect nuclei in 3D volumes."
			+ "<p>"
			+ "The detector relies on Appose to download and run StarDist with TensorFlow."
			+ "<p>"
			+ "StarDist is designed for star-convex object detection and works particularly well "
			+ "for nuclei segmentation in fluorescence microscopy."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the StarDist papers: "
			+ "<ul>"
			+ "<li><a href=\"https://doi.org/10.1007/978-3-030-00934-2_30\">Uwe Schmidt, Martin Weigert, Coleman Broaddus, and Gene Myers. "
			+ "Cell Detection with Star-convex Polygons. MICCAI 2018.</a></li>"
			+ "<li><a href=\"https://doi.org/10.1109/WACV45572.2020.9093435\">Martin Weigert, Uwe Schmidt, Robert Haase, Ko Sugawara, and Gene Myers. "
			+ "Star-convex Polyhedra for 3D Object Detection and Segmentation in Microscopy. WACV 2020.</a></li>"
			+ "</ul>"
			+ "</html>";

	@Override
	public StarDistAppose getConfigurator( final ImagePlus imp )
	{
		final int nChannels = ( imp == null ) ? 1 : imp.getNChannels();
		final String units = ( imp == null ) ? "no input image" : imp.getCalibration().getUnit();
		final double pixelSize = ( imp == null ) ? 1. : imp.getCalibration().pixelWidth;
		final double pixelDepth = ( imp == null ) ? 1. : imp.getCalibration().pixelDepth;
		return new StarDistAppose( nChannels, units, pixelSize, pixelDepth );
	}

	@Override
	public SpotGlobalDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval )
	{
		// Create the config and loads settings into it.
		final StarDistAppose config = getConfigurator( img );
		TrackMateSettingsBuilder.fromTrackMateSettings( settings, config );

		// Other parameters.
		final boolean simplify = config.simplifyContour().getValue();
		final double smoothingScale = config.smoothingScale().getValue();
		// Create the detector.
		return new ApposeDetector< T >( config, img, interval, simplify, smoothingScale );
	}

	@Override
	public boolean has2Dsegmentation()
	{
		return false;
	}

	@Override
	public boolean forbidMultithreading()
	{
		return true;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getUrl()
	{
		return DOC_STARDIST_URL;
	}

	@Override
	public ImageIcon getIcon()
	{
		return Icons.TRACKMATE_ICON;
	}
}
