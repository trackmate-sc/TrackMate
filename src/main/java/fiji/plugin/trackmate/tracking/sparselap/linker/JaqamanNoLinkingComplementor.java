package fiji.plugin.trackmate.tracking.sparselap.linker;

import java.util.Arrays;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;
import fiji.plugin.trackmate.tracking.jonkervolgenant.SparseCostMatrix;

/**
 * This algorithm <b>complements</b> the top-left quadrant of a cost matrix with
 * the no-linking alternative costs, so as to generate a full cost matrix
 * according to the LAP framework derived in
 * <code>Jaqaman et al., 2008 Nature Methods</code>.
 * 
 * @author Jean-Yves Tinevez - 2014
 */
public class JaqamanNoLinkingComplementor implements Benchmark, OutputAlgorithm< SparseCostMatrix >
{

	private long processingTime;

	private final SparseCostMatrix tl;

	private SparseCostMatrix scm;

	private String errorMessage;

	private final double alternativeCost;

	public JaqamanNoLinkingComplementor( final SparseCostMatrix topLeft, final double alternativeCost )
	{
		this.tl = topLeft;
		this.alternativeCost = alternativeCost;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		final int nCols = tl.getNCols();
		final int nRows = tl.getNRows();

		/*
		 * Top right
		 */
		final double[] cctr = new double[ nRows ];
		Arrays.fill( cctr, alternativeCost );
		final int[] numbertr = new int[ nRows ];
		Arrays.fill( numbertr, 1 );
		final int[] kktr = new int[ nRows ];
		for ( int i = 0; i < kktr.length; i++ )
		{
			kktr[ i ] = i;
		}
		final SparseCostMatrix tr = new SparseCostMatrix( cctr, kktr, numbertr, nRows );

		/*
		 * Bottom left
		 */
		final double[] ccbl = new double[ nCols ];
		Arrays.fill( ccbl, alternativeCost );
		final int[] numberbl = new int[ nCols ];
		Arrays.fill( numberbl, 1 );
		final int[] kkbl = new int[ nCols ];
		for ( int i = 0; i < kkbl.length; i++ )
		{
			kkbl[ i ] = i;
		}
		final SparseCostMatrix bl = new SparseCostMatrix( ccbl, kkbl, numberbl, nCols );

		/*
		 * Bottom right
		 */
		final SparseCostMatrix br = tl.transpose();
		br.fillWith( alternativeCost );

		/*
		 * Stitch them together
		 */
		scm = ( tl.hcat( tr ) ).vcat( bl.hcat( br ) );

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
	public SparseCostMatrix getResult()
	{
		return scm;
	}

}
