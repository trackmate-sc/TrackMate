package fiji.plugin.trackmate.tracking.jonkervolgenant;

import net.imglib2.algorithm.OutputAlgorithm;

/**
 * Ported from Lee code.
 */
public class JonkerVolgenantSolver implements OutputAlgorithm< int[][] >
{

	private static final String BASE_ERROR_MESSAGE = "[JonkerVolgenant] ";

	/**
	 * The index of the source object, so that linking object <code>i[n]</code>
	 * with object <code>j[n]</code> has a cost <code>c[n]</code>.
	 */
	private final int[] i;

	/**
	 * The index of the target object, so that linking object <code>i[n]</code>
	 * with object <code>j[n]</code> has a cost <code>c[n]</code>.
	 */
	private final int[] j;

	/**
	 * The cost of for linking object <code>i[n]</code> with object
	 * <code>j[n]</code>.
	 */
	private final double[] c;

	private String errorMessage;

	public JonkerVolgenantSolver( final int[] sourceIndex, final int[] targetIndex, final double[] costs )
	{
		this.i = sourceIndex;
		this.j = targetIndex;
		this.c = costs;
	}

	@Override
	public boolean checkInput()
	{
		if ( i.length != j.length )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Source and target index arrays must have the same length.";
			return false;
		}
		if ( c.length != i.length )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Cost array and index arrays must have the same length.";
			return false;
		}
		for ( final double cost : c )
		{
			if ( cost < 0d )
			{
				errorMessage = BASE_ERROR_MESSAGE + "All costs must be positive or 0.";
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean process()
	{
		// Check all targets have at least a source
		final int[] jcount = bincount( j );
		for ( final int jc : jcount )
		{
			if ( jc == 0 )
			{
				errorMessage = BASE_ERROR_MESSAGE + "Target object with index " + jc + " does not have a source.";
				return false;
			}
		}

		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public int[][] getResult()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * UTIL METHODS
	 */

	/**
	 * Returns the number of occurrences of each value in array of non-negative
	 * ints.
	 */
	private static final int[] bincount( final int[] vals )
	{
		// Find the max in i.
		int max = vals[ 0 ];
		for ( int k = 1; k < vals.length; k++ )
		{
			if ( vals[ k ] > max )
			{
				max = vals[ k ];
			}
		}

		final int[] bc = new int[ max ];
		for ( int k = 0; k < vals.length; k++ )
		{
			bc[ vals[ k ] ]++;
		}
		return bc;
	}
}
