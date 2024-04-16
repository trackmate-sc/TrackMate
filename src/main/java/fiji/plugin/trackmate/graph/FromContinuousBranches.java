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
package fiji.plugin.trackmate.graph;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Spot;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;

public class FromContinuousBranches implements OutputAlgorithm< SimpleWeightedGraph< Spot, DefaultWeightedEdge > >, Benchmark
{

	private static final String BASE_ERROR_MSG = "[FromContinuousBranches] ";

	private long processingTime;

	private final Collection< List< Spot > > branches;

	private final Collection< List< Spot > > links;

	private String errorMessage;

	private SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	public FromContinuousBranches( final Collection< List< Spot > > branches, final Collection< List< Spot > > links )
	{
		this.branches = branches;
		this.links = links;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public boolean checkInput()
	{
		final long start = System.currentTimeMillis();
		if ( null == branches )
		{
			errorMessage = BASE_ERROR_MSG + "branches are null.";
			return false;
		}
		if ( null == links )
		{
			errorMessage = BASE_ERROR_MSG + "links are null.";
			return false;
		}
		for ( final List< Spot > link : links )
		{
			if ( link.size() != 2 )
			{
				errorMessage = BASE_ERROR_MSG + "A link is not made of two spots.";
				return false;
			}
			if ( !checkIfInBranches( link.get( 0 ) ) )
			{
				errorMessage = BASE_ERROR_MSG + "A spot in a link is not present in the branch collection: " + link.get( 0 ) + " in the link " + link.get( 0 ) + "-" + link.get( 1 ) + ".";
				return false;
			}
			if ( !checkIfInBranches( link.get( 1 ) ) )
			{
				errorMessage = BASE_ERROR_MSG + "A spot in a link is not present in the branch collection: " + link.get( 1 ) + " in the link " + link.get( 0 ) + "-" + link.get( 1 ) + ".";
				return false;
			}
		}
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		graph = new SimpleWeightedGraph<>( DefaultWeightedEdge.class );
		for ( final List< Spot > branch : branches )
		{
			for ( final Spot spot : branch )
			{
				graph.addVertex( spot );
			}
		}

		for ( final List< Spot > branch : branches )
		{
			final Iterator< Spot > it = branch.iterator();
			Spot previous = it.next();
			while ( it.hasNext() )
			{
				final Spot spot = it.next();
				graph.addEdge( previous, spot );
				previous = spot;
			}
		}

		for ( final List< Spot > link : links )
		{
			graph.addEdge( link.get( 0 ), link.get( 1 ) );
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public SimpleWeightedGraph< Spot, DefaultWeightedEdge > getResult()
	{
		return graph;
	}

	private final boolean checkIfInBranches( final Spot spot )
	{
		for ( final List< Spot > branch : branches )
		{
			if ( branch.contains( spot ) )
				return true;
		}
		return false;
	}
}
