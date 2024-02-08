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
public class Spot3DFitEllipsoidAnalyzerFactory< T extends RealType< T > & NativeType< T > > implements Spot3DMorphologyAnalyzerFactory< T >
{

	public static final String KEY = "Spot fit 3D ellipsoid";
	
	public static final String X0 = "ELLIPSOID_X0";
	public static final String Y0 = "ELLIPSOID_Y0";
	public static final String Z0 = "ELLIPSOID_Z0";
	public static final String MAJOR = "ELLIPSOID_MAJOR_LENGTH";
	public static final String MEDIAN = "ELLIPSOID_MEDIAN_LENGTH";
	public static final String MINOR = "ELLIPSOID_MINOR_LENGTH";
	public static final String MAJOR_PHI = "ELLIPSOID_MAJOR_PHI";
	public static final String MAJOR_THETA = "ELLIPSOID_MAJOR_THETA";
	public static final String MEDIAN_PHI = "ELLIPSOID_MEDIAN_PHI";
	public static final String MEDIAN_THETA = "ELLIPSOID_MEDIAN_THETA";
	public static final String MINOR_PHI = "ELLIPSOID_MINOR_PHI";
	public static final String MINOR_THETA = "ELLIPSOID_MINOR_THETA";
	public static final String ASPECTRATIO = "ELLIPSOID_ASPECTRATIO";
	public static final String ELLIPSOID_SHAPE = "ELLIPSOID_SHAPE";
	
	/** Denotes an ellipsoid with no particular shape. */
	public static final int SHAPE_ELLIPSOID = 0;

	/**
	 * Denotes an ellipsoid with the oblate shape. The two largest radii are
	 * roughly equal. Resembles a lentil.
	 */
	public static final int SHAPE_OBLATE = 1;

	/**
	 * Denotes an ellipsoid with the prolate shape. The two smallest radii are
	 * roughly equal. Resembles a rugby balloon.
	 */
	public static final int SHAPE_PROLATE = 2;

	/**
	 * Denotes an ellipsoid with the spherical shape. The three radii are
	 * roughly equal. Resembles a sphere
	 */
	public static final int SHAPE_SPHERE = 3;

	/**
	 * Tolerance in percentage on the radii values to be considered roughly
	 * equals.
	 * 
	 * @see #SHAPE_ELLIPSOID
	 * @see #SHAPE_OBLATE
	 * @see #SHAPE_PROLATE
	 * @see #SHAPE_SPHERE
	 */
	public static final double SHAPE_CLASS_TOLERANCE = 0.1;

	private static final List< String > FEATURES = Arrays.asList( new String[] {
			X0, Y0, Z0,
			MAJOR, MEDIAN, MINOR,
			MAJOR_PHI, MAJOR_THETA,
			MEDIAN_PHI, MEDIAN_THETA,
			MINOR_PHI, MINOR_THETA,
			ASPECTRATIO,
			ELLIPSOID_SHAPE } );
	private static final Map< String, String > FEATURE_SHORTNAMES = new HashMap< >();
	private static final Map< String, String > FEATURE_NAMES = new HashMap< >();
	private static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >();
	private static final Map< String, Boolean > FEATURE_ISINTS = new HashMap< >();
	static
	{
		FEATURE_SHORTNAMES.put( X0, "El. x0" );
		FEATURE_SHORTNAMES.put( Y0, "El. y0" );
		FEATURE_SHORTNAMES.put( Z0, "El. z0" );
		FEATURE_SHORTNAMES.put( MAJOR, "El. long axis" );
		FEATURE_SHORTNAMES.put( MEDIAN, "El. med. axis" );
		FEATURE_SHORTNAMES.put( MINOR, "El. sh. axis" );
		FEATURE_SHORTNAMES.put( MAJOR_PHI, "El. l.a. phi" );
		FEATURE_SHORTNAMES.put( MEDIAN_PHI, "El. m.a. phi" );
		FEATURE_SHORTNAMES.put( MINOR_PHI, "El. s.a. phi" );
		FEATURE_SHORTNAMES.put( MAJOR_THETA, "El. l.a. theta" );
		FEATURE_SHORTNAMES.put( MEDIAN_THETA, "El. m.a. theta" );
		FEATURE_SHORTNAMES.put( MINOR_THETA, "El. s.a. theta" );
		FEATURE_SHORTNAMES.put( ASPECTRATIO, "El. a.r." );
		FEATURE_SHORTNAMES.put( ELLIPSOID_SHAPE, "El. shape" );

		FEATURE_NAMES.put( X0, "Ellipsoid center x0" );
		FEATURE_NAMES.put( Y0, "Ellipsoid center y0" );
		FEATURE_NAMES.put( Z0, "Ellipsoid center z0" );
		FEATURE_NAMES.put( MAJOR, "Ellipsoid long axis" );
		FEATURE_NAMES.put( MEDIAN, "Ellipsoid long axis" );
		FEATURE_NAMES.put( MINOR, "Ellipsoid short axis" );
		FEATURE_NAMES.put( MAJOR_PHI, "Ellipsoid long axis phi" );
		FEATURE_NAMES.put( MEDIAN_PHI, "Ellipsoid long axis. phi" );
		FEATURE_NAMES.put( MINOR_PHI, "Ellipsoid short axis phi" );
		FEATURE_NAMES.put( MAJOR_THETA, "Ellipsoid long axis theta" );
		FEATURE_NAMES.put( MEDIAN_THETA, "Ellipsoid long axis theta" );
		FEATURE_NAMES.put( MINOR_THETA, "Ellipsoid short axis theta" );
		FEATURE_NAMES.put( ASPECTRATIO, "Ellipsoid aspect ratio" );
		FEATURE_NAMES.put( ELLIPSOID_SHAPE, "Ellipsoid shape class" );

		FEATURE_DIMENSIONS.put( X0, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( Y0, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( Z0, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( MAJOR, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( MEDIAN, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( MINOR, Dimension.LENGTH );
		FEATURE_DIMENSIONS.put( MAJOR_PHI, Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( MAJOR_THETA, Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( MEDIAN_PHI, Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( MEDIAN_THETA, Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( MINOR_PHI, Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( MINOR_THETA, Dimension.ANGLE );
		FEATURE_DIMENSIONS.put( ASPECTRATIO, Dimension.NONE );
		FEATURE_DIMENSIONS.put( ELLIPSOID_SHAPE, Dimension.NONE );

		FEATURE_ISINTS.put( X0, Boolean.FALSE );
		FEATURE_ISINTS.put( Y0, Boolean.FALSE );
		FEATURE_ISINTS.put( Z0, Boolean.FALSE );
		FEATURE_ISINTS.put( MAJOR, Boolean.FALSE );
		FEATURE_ISINTS.put( MEDIAN, Boolean.FALSE );
		FEATURE_ISINTS.put( MINOR, Boolean.FALSE );
		FEATURE_ISINTS.put( MAJOR_PHI, Boolean.FALSE );
		FEATURE_ISINTS.put( MAJOR_THETA, Boolean.FALSE );
		FEATURE_ISINTS.put( MEDIAN_PHI, Boolean.FALSE );
		FEATURE_ISINTS.put( MEDIAN_THETA, Boolean.FALSE );
		FEATURE_ISINTS.put( MINOR_PHI, Boolean.FALSE );
		FEATURE_ISINTS.put( MINOR_THETA, Boolean.FALSE );
		FEATURE_ISINTS.put( ASPECTRATIO, Boolean.FALSE );
		FEATURE_ISINTS.put( ELLIPSOID_SHAPE, Boolean.TRUE );
	}


	@Override
	public SpotAnalyzer< T > getAnalyzer( final ImgPlus< T > img, final int frame, final int channel )
	{
		// Don't run more than once.
		if ( channel != 0 )
			return SpotAnalyzer.dummyAnalyzer();

		return new Spot3DFitEllipsoidAnalyzer<>( !DetectionUtils.is2D( img ) );
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
