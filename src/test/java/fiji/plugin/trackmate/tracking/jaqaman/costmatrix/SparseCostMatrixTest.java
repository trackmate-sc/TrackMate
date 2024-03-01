package fiji.plugin.trackmate.tracking.jaqaman.costmatrix;
/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SparseCostMatrixTest
{

	@Test
	public void testVcat()
	{
		final int[] kk = new int[] { 0, 1, 2, 3, 4, 5 };
		final double[] cc = new double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6 };
		final int[] number = new int[] { 1, 1, 1, 1, 1, 1 };
		final SparseCostMatrix A = new SparseCostMatrix( cc, kk, number, 6 );
		final SparseCostMatrix C = A.vcat( A );

		assertEquals( "Bad number of rows.", 2 * A.nRows, C.nRows );
		assertEquals( "Bad cardinality.", 2 * A.cardinality, C.cardinality );
	}

	@Test
	public void testHcat()
	{
		final int[] kk = new int[] { 0, 1, 2, 3, 4, 5 };
		final double[] cc = new double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6 };
		final int[] number = new int[] { 1, 1, 1, 1, 1, 1 };
		final SparseCostMatrix A = new SparseCostMatrix( cc, kk, number, 6 );
		final SparseCostMatrix C = A.hcat( A );

		assertEquals( "Bad number of cols.", 2 * A.nCols, C.nCols );
		assertEquals( "Bad cardinality.", 2 * A.cardinality, C.cardinality );
	}

	@Test
	public void testTranspose()
	{
		final int[] kk = new int[] { 0, 3, 2, 1, 3, 3, 0, 3 };
		final double[] cc = new double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8 };
		final int[] number = new int[] { 2, 1, 2, 1, 2 };
		final SparseCostMatrix A = new SparseCostMatrix( cc, kk, number, 4 );
		final SparseCostMatrix C = A.transpose();

		assertEquals( "Bad number of rows.", A.nCols, C.nRows );
		assertEquals( "Bad number of cols.", A.nRows, C.nCols );
		assertEquals( "Bad cardinality.", A.cardinality, C.cardinality );

		for ( int i = 0; i < C.nRows; i++ )
		{
			for ( int j = 0; j < C.nCols; j++ )
			{
				final double actual = C.get( i, j, Double.POSITIVE_INFINITY );
				final double expected = A.get( j, i, Double.POSITIVE_INFINITY );
				assertEquals( "Bad value at row " + i + ", col " + j + ".", expected, actual, Double.MIN_VALUE );
			}
		}
	}

	@Test
    public void test_valid_assignment_cost() {
        // Create a SparseCostMatrix object with sample data
        double[] cc = {1.0, 2.0, 3.0, 4.0};
        int[] kk = {0, 1, 2, 3};
        int[] number = {2, 2};
        int nCols = 4;
        SparseCostMatrix matrix = new SparseCostMatrix(cc, kk, number, nCols);

        // Creating a valid assignment
        int[] rowAssignment = {1, 3};

        // Calculating the total assignment cost
        double totalCost = matrix.totalAssignmentCost(rowAssignment);

        // Assert that if total cost is correct
        assertEquals(6.0, totalCost, 0.0001);
    }

}
