/*
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

package fiji.plugin.trackmate.tracking.kdtree;

import net.imglib2.KDTree;
import net.imglib2.KDTreeNode;
import net.imglib2.RealLocalizable;
import net.imglib2.Sampler;
import net.imglib2.neighborsearch.NearestNeighborSearch;

public class NearestNeighborFlagSearchOnKDTree< T > implements NearestNeighborSearch< FlagNode< T > >
{

	protected KDTree< FlagNode< T > > tree;

	protected final int n;

	protected final double[] pos;

	protected KDTreeNode< FlagNode< T > > bestPoint;

	protected double bestSquDistance;

	public NearestNeighborFlagSearchOnKDTree( final KDTree< FlagNode< T > > tree )
	{
		n = tree.numDimensions();
		pos = new double[ n ];
		this.tree = tree;
	}

	@Override
	public int numDimensions()
	{
		return n;
	}

	@Override
	public void search( final RealLocalizable p )
	{
		p.localize( pos );
		bestSquDistance = Double.MAX_VALUE;
		searchNode( tree.getRoot() );
	}

	protected void searchNode( final KDTreeNode< FlagNode< T > > current )
	{
		// consider the current node
		final double distance = current.squDistanceTo( pos );
		final boolean visited = current.get().isVisited();
		if ( distance < bestSquDistance && !visited )
		{
			bestSquDistance = distance;
			bestPoint = current;
		}

		final double axisDiff = pos[ current.getSplitDimension() ] - current.getSplitCoordinate();
		final double axisSquDistance = axisDiff * axisDiff;
		final boolean leftIsNearBranch = axisDiff < 0;

		// search the near branch
		final KDTreeNode< FlagNode< T > > nearChild = leftIsNearBranch ? current.left : current.right;
		final KDTreeNode< FlagNode< T > > awayChild = leftIsNearBranch ? current.right : current.left;
		if ( nearChild != null )
			searchNode( nearChild );

		// search the away branch - maybe
		if ( ( axisSquDistance <= bestSquDistance ) && ( awayChild != null ) )
			searchNode( awayChild );
	}

	@Override
	public Sampler< fiji.plugin.trackmate.tracking.kdtree.FlagNode< T > > getSampler()
	{
		return bestPoint;
	}

	@Override
	public RealLocalizable getPosition()
	{
		return bestPoint;
	}

	@Override
	public double getSquareDistance()
	{
		return bestSquDistance;
	}

	@Override
	public double getDistance()
	{
		return Math.sqrt( bestSquDistance );
	}

	@Override
	public NearestNeighborFlagSearchOnKDTree< T > copy()
	{
		final NearestNeighborFlagSearchOnKDTree< T > copy = new NearestNeighborFlagSearchOnKDTree<>( tree );
		System.arraycopy( pos, 0, copy.pos, 0, pos.length );
		copy.bestPoint = bestPoint;
		copy.bestSquDistance = bestSquDistance;
		return copy;
	}
}
