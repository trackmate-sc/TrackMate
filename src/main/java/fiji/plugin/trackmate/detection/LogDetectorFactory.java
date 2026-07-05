/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;

import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.detection.LogDetectorFactory.LogDetectorConfig;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.config.HasInteractivePreview;
import fiji.plugin.trackmate.util.config.TrackMateConfigurator;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class )
public class LogDetectorFactory< T extends RealType< T > & NativeType< T > > implements SpotDetectorFactory< T >, SpotDetectorConfigFactory< T, LogDetectorConfig >
{

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "LOG_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "LoG detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector applies a LoG (Laplacian of Gaussian) filter <br>"
			+ "to the image, with a sigma suited to the blob estimated size. <br>"
			+ "Calculations are made in the Fourier space. The maxima in the <br>"
			+ "filtered image are searched for, and maxima too close from each <br>"
			+ "other are suppressed. A quadratic fitting scheme allows to do <br>"
			+ "sub-pixel localization. "
			+ "</html>";

	public static final ImageIcon ICON = new ImageIcon( Icons.class.getResource( "images/LoG-icon-64px.png" ) );


	@Override
	public SpotDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval, final int frame )
	{
		final double radius = ( Double ) settings.get( KEY_RADIUS );
		final double threshold = ( Double ) settings.get( KEY_THRESHOLD );
		final boolean doMedian = ( Boolean ) settings.get( KEY_DO_MEDIAN_FILTERING );
		final boolean doSubpixel = ( Boolean ) settings.get( KEY_DO_SUBPIXEL_LOCALIZATION );
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ) - 1;
		final RandomAccessible< T > imFrame = DetectionUtils.prepareFrameImg( img, channel, frame );

		final LogDetector< T > detector = new LogDetector<>( imFrame, interval, calibration, radius, threshold, doSubpixel, doMedian );
		detector.setNumThreads( 1 );
		return detector;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
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
	public ImageIcon getIcon()
	{
		return ICON;
	}

	@Override
	public LogDetectorConfig createConfig( final ImagePlus imp )
	{
		final int nChannels = ( imp == null ) ? 1 : imp.getNChannels( );
		final String units = ( imp == null ) ? "no image" : imp.getCalibration().getUnit();
		return new LogDetectorConfig( nChannels, units  );
	}

	/**
	 * Specifies what are the parameters of the {@link LogDetector} and
	 * DogDetector.
	 *
	 * @author Jean-Yves Tinevez
	 */
	public static class LogDetectorConfig extends TrackMateConfigurator implements HasInteractivePreview
	{

		public LogDetectorConfig( final int nChannels, final String units )
		{
			super( NAME, INFO_TEXT );
			addTargetChannel( nChannels );
			addDiameter( units );
			addThreshold();
			addMedianFiltering();
			addSubpixelLocalization();
		}

		@Override
		public String getPreviewArgumentKey()
		{
			return DetectorKeys.KEY_THRESHOLD;
		}
	}
}
