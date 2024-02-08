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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.detection.DetectionUtils;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = Spot3DMorphologyAnalyzerFactory.class )
public class Spot3DShapeAnalyzerFactory< T extends RealType< T > & NativeType< T > > implements Spot3DMorphologyAnalyzerFactory< T >
{

	public static final String KEY = "Spot 3D shape descriptors";

	public static final String VOLUME = "VOLUME";
	public static final String SURFACE_AREA = "SURFACE_AREA";
	public static final String SPHERICITY = "SPHERICITY";
	public static final String SOLIDITY = "SOLIDITY";
	public static final String CONVEXITY = "CONVEXITY";

	private static final List< String > FEATURES = Arrays.asList( new String[] {
			VOLUME, SURFACE_AREA, SPHERICITY, SOLIDITY, CONVEXITY } );
	private static final Map< String, String > FEATURE_SHORTNAMES = new HashMap< >();
	private static final Map< String, String > FEATURE_NAMES = new HashMap< >();
	private static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >();
	private static final Map< String, Boolean > FEATURE_ISINTS = new HashMap< >();
	static
	{
		FEATURE_SHORTNAMES.put( VOLUME, "Volume" );
		FEATURE_SHORTNAMES.put( SURFACE_AREA, "Surf. area" );
		FEATURE_SHORTNAMES.put( SPHERICITY, "Sphericity" );
		FEATURE_SHORTNAMES.put( CONVEXITY, "Conv." );
		FEATURE_SHORTNAMES.put( SOLIDITY, "Solidity" );

		FEATURE_NAMES.put( VOLUME, "Volume" );
		FEATURE_NAMES.put( SURFACE_AREA, "Surface area" );
		FEATURE_NAMES.put( SPHERICITY, "Sphericity" );
		FEATURE_NAMES.put( CONVEXITY, "Convexity" );
		FEATURE_NAMES.put( SOLIDITY, "Solidity" );

		FEATURE_DIMENSIONS.put( SURFACE_AREA, Dimension.AREA );
		FEATURE_DIMENSIONS.put( VOLUME, Dimension.VOLUME );
		FEATURE_DIMENSIONS.put( SPHERICITY, Dimension.NONE );
		FEATURE_DIMENSIONS.put( CONVEXITY, Dimension.NONE );
		FEATURE_DIMENSIONS.put( SOLIDITY, Dimension.NONE );

		FEATURE_ISINTS.put( VOLUME, Boolean.FALSE );
		FEATURE_ISINTS.put( SURFACE_AREA, Boolean.FALSE );
		FEATURE_ISINTS.put( SPHERICITY, Boolean.FALSE );
		FEATURE_ISINTS.put( CONVEXITY, Boolean.FALSE );
		FEATURE_ISINTS.put( SOLIDITY, Boolean.FALSE );
	}

	@Override
	public SpotAnalyzer< T > getAnalyzer( final ImgPlus< T > img, final int frame, final int channel )
	{
		// Don't run more than once.
		if ( channel != 0 )
			return SpotAnalyzer.dummyAnalyzer();

		return new Spot3DShapeAnalyzer<>( !DetectionUtils.is2D( img ) );
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORTNAMES;
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
	public Map< String, Boolean > getIsIntFeature()
	{
		return FEATURE_ISINTS;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
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
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return KEY;
	}
}
