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
package fiji.plugin.trackmate.features.manual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;

@Plugin( type = EdgeAnalyzer.class )
public class ManualEdgeColorAnalyzer implements EdgeAnalyzer
{

	public static final String FEATURE = "MANUAL_EDGE_COLOR";

	public static final String KEY = "Manual edge color";

	static final List< String > FEATURES = new ArrayList<>( 1 );

	static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap<>( 1 );

	static final Map< String, String > FEATURE_NAMES = new HashMap<>( 1 );

	static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap<>( 1 );

	static final Map< String, Boolean > IS_INT = new HashMap<>( 1 );

	static final String INFO_TEXT = "<html>A dummy analyzer for the feature that stores the color manually assigned to each edge.</html>";

	static final String NAME = KEY;

	static
	{
		FEATURES.add( FEATURE );
		FEATURE_SHORT_NAMES.put( FEATURE, "Edge color" );
		FEATURE_NAMES.put( FEATURE, "Manual edge color" );
		FEATURE_DIMENSIONS.put( FEATURE, Dimension.NONE );
		IS_INT.put( FEATURE, Boolean.TRUE );
	}

	private long processingTime;

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
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
		return IS_INT;
	}

	@Override
	public void setNumThreads()
	{}

	@Override
	public void setNumThreads( final int numThreads )
	{}

	@Override
	public int getNumThreads()
	{
		return 1;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void process( final Collection< DefaultWeightedEdge > edges, final Model model )
	{}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public boolean isManualFeature()
	{
		return true;
	}
}
