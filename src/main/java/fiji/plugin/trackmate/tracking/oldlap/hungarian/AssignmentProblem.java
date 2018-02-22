package fiji.plugin.trackmate.tracking.oldlap.hungarian;

/**
 * An implementation of this: http://en.wikipedia.org/wiki/Hungarian_algorithm
 * <p>
 * Based loosely on Guy Robinson's description of the algorithm here: <a
 * href=http://www.netlib.org/utk/lsi/pcwLSI/text/node222.html>The Sequential
 * Algorithm</a>.
 * <p>
 * In short, it finds the cheapest assignment pairs given a cost matrix.
 * <p>
 * Copyright 2007 Gary Baker (GPL v3)
 * 
 * @author Gary Baker
 */
public class AssignmentProblem
{

	private final double[][] costMatrix;

	public AssignmentProblem( final double[][] aCostMatrix )
	{
		costMatrix = aCostMatrix;
	}

	private double[][] copyOfMatrix()
	{
		// make a copy of the passed array
		final double[][] retval = new double[ costMatrix.length ][];
		for ( int i = 0; i < costMatrix.length; i++ )
		{
			retval[ i ] = new double[ costMatrix[ i ].length ];
			System.arraycopy( costMatrix[ i ], 0, retval[ i ], 0, costMatrix[ i ].length );
		}
		return retval;
	}

	public int[][] solve( final AssignmentAlgorithm anAlgorithm )
	{
		final double[][] lCostMatrix = copyOfMatrix();
		return anAlgorithm.computeAssignments( lCostMatrix );
	}
}
