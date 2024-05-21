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
import static fiji.plugin.trackmate.io.IOUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeTargetChannel;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkOptionalParameter;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.detector.MaskDetectorConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class )
public class MaskDetectorFactory< T extends RealType< T > & NativeType< T > > extends ThresholdDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "MASK_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "Mask detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector creates spots from a black and white mask."
			+ "<p>"
			+ "More precisely, all the pixels in the designated channel that have "
			+ "a value strictly larger than 0 are "
			+ "considered as part of the foreground, "
			+ "and used to build connected regions. In 2D, spots are created with "
			+ "the (possibly simplified) contour of the region. In 3D, a mesh is "
			+ "created for each region."
			+ "<p>"
			+ "The spot quality stores the object area or volume in pixels."
			+ "</html>";

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
	public SpotDetector< T > getDetector( final Interval interval, final int frame )
	{
		final boolean simplifyContours = ( Boolean ) settings.get( KEY_SIMPLIFY_CONTOURS );
		final double smoothingScale = ( Double ) settings.get( KEY_SMOOTHING_SCALE );
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ) - 1;
		final RandomAccessible< T > imFrame = DetectionUtils.prepareFrameImg( img, channel, frame );
		final RandomAccessible< T > mask = mask( imFrame );

		final MaskDetector< T > detector = new MaskDetector<>(
				mask,
				interval,
				calibration,
				simplifyContours,
				smoothingScale );

		detector.setNumThreads( 1 );
		return detector;
	}

	/**
	 * Return a view of the input image where all pixels with values strictly
	 * larger than 0 are set to 1, and set to 0 otherwise.
	 *
	 * @param input
	 *            the image to wrap.
	 * @return a view of the image.
	 */
	protected RandomAccessible< T > mask( final RandomAccessible< T > input )
	{
		final Converter< T, T > c = new Converter< T, T >()
		{
			@Override
			public void convert( final T input, final T output )
			{
				output.setReal( input.getRealDouble() > 0. ? 1. : 0. );
			}
		};
		return Converters.convert( input, c, img.firstElement().createVariable() );
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
		ok = ok & checkParameter( lSettings, KEY_SIMPLIFY_CONTOURS, Boolean.class, errorHolder );
		ok = ok & checkOptionalParameter( lSettings, KEY_SMOOTHING_SCALE, Double.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_SIMPLIFY_CONTOURS );
		final List< String > optionalKeys = new ArrayList<>();
		optionalKeys.add( KEY_SMOOTHING_SCALE );
		ok = ok & checkMapKeys( lSettings, mandatoryKeys, optionalKeys, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean marshall( final Map< String, Object > lSettings, final Element element )
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = writeTargetChannel( lSettings, element, errorHolder )
				&& writeAttribute( lSettings, element, KEY_SIMPLIFY_CONTOURS, Boolean.class, errorHolder )
				&& writeAttribute( lSettings, element, KEY_SMOOTHING_SCALE, Double.class, errorHolder );

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
		ok = ok & readIntegerAttribute( element, lSettings, KEY_TARGET_CHANNEL, errorHolder );
		ok = ok & readBooleanAttribute( element, lSettings, KEY_SIMPLIFY_CONTOURS, errorHolder );
		ok = ok & readDoubleAttribute( element, lSettings, KEY_SMOOTHING_SCALE, errorHolder );
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
		return new MaskDetectorConfigurationPanel( lSettings, model );
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
		final Map< String, Object > lSettings = new HashMap<>();
		lSettings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		lSettings.put( KEY_SIMPLIFY_CONTOURS, true );
		lSettings.put( KEY_SMOOTHING_SCALE, -1. );
		return lSettings;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public MaskDetectorFactory< T > copy()
	{
		return new MaskDetectorFactory<>();
	}
}
