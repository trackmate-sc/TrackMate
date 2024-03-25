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
package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.makeFeatureKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * A factory for {@link SpotContrastAndSNRAnalyzer}s. Because the analyzers of
 * this factory depends on some features defined in
 * {@link SpotIntensityMultiCAnalyzer}s, we use a lower priority, so that
 * computation are done after the aforementioned analyzer are done.
 *
 * @author Jean- Yves Tinevez
 */
@Plugin( type = SpotAnalyzerFactory.class, priority = Priority.LOW )
public class SpotContrastAndSNRAnalyzerFactory< T extends RealType< T > & NativeType< T >> implements SpotAnalyzerFactory< T >
{

	public static final String CONTRAST = "CONTRAST_CH";
	public static final String SNR = "SNR_CH";

	private static final String CONTRAST_NAME = "Contrast ch";
	private static final String SNR_NAME = "Signal/Noise ratio ch";
	private static final String CONTRAST_SHORTNAME = "Ctrst ch";
	private static final String SNR_SHORTNAME = "SNR ch";
	private static final List< String > FEATURES = Arrays.asList( new String[] {
			CONTRAST, SNR } );
	private static final List< String > FEATURE_SHORTNAMES = Arrays.asList( new String[] {
			CONTRAST_SHORTNAME, SNR_SHORTNAME } );
	private static final List< String > FEATURE_NAMES = Arrays.asList( new String[] {
			CONTRAST_NAME, SNR_NAME } );

	public static final String KEY = "Spot contrast and SNR";

	private int nChannels = 1;


	/*
	 * METHODS
	 */


	@Override
	public SpotAnalyzer< T > getAnalyzer( final ImgPlus< T > img, final int frame, final int channel )
	{
		final ImgPlus< T > imgTC = TMUtils.hyperSlice( img, channel, frame );
		return new SpotContrastAndSNRAnalyzer<>( imgTC, channel );
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public List< String > getFeatures()
	{
		final List< String > features = new ArrayList<>( nChannels * FEATURES.size() );
		for ( int c = 0; c < nChannels; c++ )
			for ( final String feature : FEATURES )
				features.add( makeFeatureKey( feature, c ) );

		return features;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		final Map< String, String > names = new LinkedHashMap<>( nChannels * FEATURES.size() );
		for ( int c = 0; c < nChannels; c++ )
			for ( int i = 0; i < FEATURES.size(); i++ )
			{
				final String feature = FEATURES.get( i );
				final String shortName = FEATURE_SHORTNAMES.get( i );
				names.put( makeFeatureKey( feature, c ), makeFeatureKey( shortName, c ) );
			}

		return names;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		final Map< String, String > names = new LinkedHashMap<>( nChannels * FEATURES.size() );
		for ( int c = 0; c < nChannels; c++ )
			for ( int i = 0; i < FEATURES.size(); i++ )
			{
				final String feature = FEATURES.get( i );
				final String shortName = FEATURE_NAMES.get( i );
				names.put( makeFeatureKey( feature, c ), makeFeatureKey( shortName, c ) );
			}

		return names;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		final List< String > features = getFeatures();
		final Map< String, Dimension > dimensions = new LinkedHashMap<>( features.size() );
		for ( final String feature : features )
			dimensions.put( feature, Dimension.NONE );

		return dimensions;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		final List< String > features = getFeatures();
		final Map< String, Boolean > isints = new LinkedHashMap<>( features.size() );
		for ( final String feature : features )
			isints.put( feature, Boolean.FALSE );

		return isints;
	}


	@Override
	public String getInfoText()
	{
		return null;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return KEY;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}

	@Override
	public void setNChannels( final int nChannels )
	{
		this.nChannels = nChannels;
	}
}
