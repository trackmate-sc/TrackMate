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
import static fiji.plugin.trackmate.io.IOUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeDoSubPixel;
import static fiji.plugin.trackmate.io.IOUtils.writeRadius;
import static fiji.plugin.trackmate.io.IOUtils.writeTargetChannel;
import static fiji.plugin.trackmate.io.IOUtils.writeThreshold;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.detector.HessianDetectorConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
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
	public SpotDetector< T > getDetector( final Interval interval, final int frame )
	{
		final double radiusXY = ( Double ) settings.get( KEY_RADIUS );
		final double radiusZ = ( Double ) settings.get( KEY_RADIUS_Z );
		final double thresholdQuality = ( Double ) settings.get( KEY_THRESHOLD );
		final boolean doSubpixel = ( Boolean ) settings.get( KEY_DO_SUBPIXEL_LOCALIZATION );
		final boolean normalize = ( Boolean ) settings.get( KEY_NORMALIZE );

		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final RandomAccessibleInterval< T > imFrame = prepareFrameImg( frame );

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
	public boolean checkSettings( final Map< String, Object > lSettings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( lSettings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_RADIUS, Double.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_RADIUS_Z, Double.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_DO_SUBPIXEL_LOCALIZATION, Boolean.class, errorHolder );
		ok = ok & checkParameter( lSettings, KEY_NORMALIZE, Boolean.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_RADIUS );
		mandatoryKeys.add( KEY_RADIUS_Z );
		mandatoryKeys.add( KEY_THRESHOLD );
		mandatoryKeys.add( KEY_DO_SUBPIXEL_LOCALIZATION );
		mandatoryKeys.add( KEY_NORMALIZE );
		ok = ok & checkMapKeys( lSettings, mandatoryKeys, null, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();
		return ok;
	}

	@Override
	public boolean marshall( final Map< String, Object > lSettings, final Element element )
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = writeTargetChannel( lSettings, element, errorHolder )
				&& writeRadius( lSettings, element, errorHolder )
				&& writeAttribute( lSettings, element, KEY_RADIUS_Z, Double.class, errorHolder )
				&& writeThreshold( lSettings, element, errorHolder )
				&& writeAttribute( lSettings, element, KEY_NORMALIZE, Boolean.class, errorHolder )
				&& writeDoSubPixel( lSettings, element, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();
		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > lSettings )
	{
		lSettings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readDoubleAttribute( element, lSettings, KEY_RADIUS, errorHolder );
		ok = ok & readDoubleAttribute( element, lSettings, KEY_RADIUS_Z, errorHolder );
		ok = ok & readDoubleAttribute( element, lSettings, KEY_THRESHOLD, errorHolder );
		ok = ok & readBooleanAttribute( element, lSettings, KEY_DO_SUBPIXEL_LOCALIZATION, errorHolder );
		ok = ok & readBooleanAttribute( element, lSettings, KEY_NORMALIZE, errorHolder );
		ok = ok & readIntegerAttribute( element, lSettings, KEY_TARGET_CHANNEL, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( lSettings );
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

	@Override
	public HessianDetectorFactory< T > copy()
	{
		return new HessianDetectorFactory< >();
	}
}
