package fiji.plugin.trackmate.tracking.sparselap.linker;

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

}
