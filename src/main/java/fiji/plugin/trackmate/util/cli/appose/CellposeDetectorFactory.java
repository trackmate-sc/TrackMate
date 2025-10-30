/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2023 TrackMate developers.
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
public class CellposeDetectorFactory< T extends RealType< T > & NativeType< T > >
		implements SpotGlobalDetectorFactory< T >, SpotDetectorFactoryGenericConfig< T, CellposeAppose >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "APPOSE_CELLPOSE_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Appose Cellpose detector";

	public static final String DOC_CELLPOSE_URL = "https://imagej.net/plugins/trackmate/detectors/trackmate-appose-cellpose";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on cellpose to detect objects."
			+ "<p>"
			+ "The detector relies on Appose to download and run cellpose 3."
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the cellpose paper: <a href=\"https://doi.org/10.1038/s41592-025-02595-5\">Stringer, C., Pachitariu, M. "
			+ "Cellpose3: one-click image restoration for improved cellular segmentation. "
			+ "Nat Methods 22, 592–599 (2025).</a>"
			+ "</html>";

	@Override
	public CellposeAppose getConfigurator( final ImagePlus imp )
	{
		final int nChannels = ( imp == null ) ? 1 : imp.getNChannels();
		final String units = ( imp == null ) ? "no input image" : imp.getCalibration().getUnit();
		final double pixelSize = ( imp == null ) ? 1. : imp.getCalibration().pixelWidth;
		return new CellposeAppose( nChannels, units, pixelSize );
	}

	@Override
	public SpotGlobalDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval )
	{
		// Create the config and loads settings into it.
		final CellposeAppose config = getConfigurator( img );
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
		return true;
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
		return DOC_CELLPOSE_URL;
	}

	@Override
	public ImageIcon getIcon()
	{
		return Icons.TRACKMATE_ICON;
	}
}
