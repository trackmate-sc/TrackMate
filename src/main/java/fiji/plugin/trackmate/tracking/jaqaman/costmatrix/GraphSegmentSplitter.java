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
package fiji.plugin.trackmate.tracking.jaqaman.costmatrix;

import fiji.plugin.trackmate.Spot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;

public class GraphSegmentSplitter
{
	private final List< Spot > segmentStarts;

	private final List< Spot > segmentEnds;

	private final List< List< Spot > > segmentMiddles;

	public GraphSegmentSplitter( final Graph< Spot, DefaultWeightedEdge > graph, final boolean findMiddlePoints )
	{
		final ConnectivityInspector< Spot, DefaultWeightedEdge > connectivity = new ConnectivityInspector<>( graph );
		final List< Set< Spot > > connectedSets = connectivity.connectedSets();
		final Comparator< Spot > framecomparator = Spot.frameComparator;

		segmentStarts = new ArrayList<>( connectedSets.size() );
		segmentEnds = new ArrayList<>( connectedSets.size() );
		if ( findMiddlePoints )
		{
			segmentMiddles = new ArrayList<>( connectedSets.size() );
		}
		else
		{
			segmentMiddles = Collections.emptyList();
		}

		for ( final Set< Spot > set : connectedSets )
		{
			if ( set.size() < 2 )
			{
				continue;
			}

			final List< Spot > list = new ArrayList<>( set );
			Collections.sort( list, framecomparator );

			segmentEnds.add( list.remove( list.size() - 1 ) );
			segmentStarts.add( list.remove( 0 ) );
			if ( findMiddlePoints )
			{
				segmentMiddles.add( list );
			}
		}
	}

	public List< Spot > getSegmentEnds()
	{
		return segmentEnds;
	}

	public List< List< Spot > > getSegmentMiddles()
	{
		return segmentMiddles;
	}

	public List< Spot > getSegmentStarts()
	{
		return segmentStarts;
	}

}
