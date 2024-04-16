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
package fiji.plugin.trackmate.tracking.sparselap;

import fiji.plugin.trackmate.tracking.jaqaman.JaqamanLinker;
import fiji.plugin.trackmate.tracking.jaqaman.costmatrix.DefaultCostMatrixCreator;
import fiji.plugin.trackmate.tracking.jaqaman.costmatrix.DefaultCostMatrixCreator.Assignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class DefaultCostMatrixCreatorExample
{

	public static void main( final String[] args )
	{
		final List< String > names = Arrays.asList( new String[] { "Florian", "Jean-Yves", "Spencer", "Pascal", "Audrey", "Nathalie", "Anne" } );
		final List< String > activities = Arrays.asList( new String[] { "MEMI-OP", "Leading", "Confocals", "Spinning-disks", "Image analysis", "HCA", "Biphoton", "Administration", "Grant writing" } );

		final int nAssgn = 30;

		final long seed = new Random().nextLong();
		final Random ran = new Random( seed );
		System.out.println( "Random seed used: " + seed );
		// Ensure we do not have duplicate assignments.
		final HashSet< Assignment > assgns = new HashSet<>();
		for ( int i = 0; i < nAssgn; i++ )
		{
			final int row = ran.nextInt( names.size() );
			final int col = ran.nextInt( activities.size() );
			final double cost = 1 + ran.nextInt( 99 );
			assgns.add( new Assignment( row, col, cost ) );
		}

		final List< String > rows = new ArrayList<>( assgns.size() );
		final List< String > cols = new ArrayList<>( assgns.size() );
		final double[] costs = new double[ assgns.size() ];
		final Iterator< Assignment > it = assgns.iterator();
		for ( int i = 0; i < assgns.size(); i++ )
		{
			final Assignment next = it.next();
			rows.add( names.get( next.getR() ) );
			cols.add( activities.get( next.getC() ) );
			costs[ i ] = next.getCost();
		}

		final DefaultCostMatrixCreator< String, String > creator = new DefaultCostMatrixCreator<>( rows, cols, costs, 1.09, 0.5 );

		final JaqamanLinker< String, String > solver = new JaqamanLinker<>( creator );
		if ( !solver.checkInput() || !solver.process() )
		{
			System.err.println( solver.getErrorMessage() );
			return;
		}

		System.out.println( "For cost matrix:\n" + creator.getResult().toString( creator.getSourceList(), creator.getTargetList() ) );
		System.out.println( '\n' + solver.resultToString() );
	}
}
