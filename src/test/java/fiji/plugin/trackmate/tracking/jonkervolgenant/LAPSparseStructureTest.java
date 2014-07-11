package fiji.plugin.trackmate.tracking.jonkervolgenant;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;


public class LAPSparseStructureTest
{

	private LAPSparseStructure< String, String > sparse;

	@Before
	public void setUp()
	{
		final List< String > sources = Arrays.asList( new String[] { "John", "Paul", "Ringo", "George", "Paul", "Paul", "John", "Ringo", "George" } );
		final List< String > targets = Arrays.asList( new String[] { "Guitar", "Bass", "Drum", "Guitar", "Keyboard", "Voice", "Voice", "Voice", "Voice" } );
		final List< Double > costs = Arrays.asList( new Double[] { 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.5, 3.0, 4.0 } );
		final LAPSparseStructure< String, String > sparse = new LAPSparseStructure< String, String >( sources, targets, costs, null );
		this.sparse = sparse;
	}

	@Test
	public void testSzudzikPairing()
	{
		final Random ran = new Random();
		final int nTest = 1000;
		for ( int i = 0; i < nTest; i++ )
		{
			final int i1 = ran.nextInt( Integer.MAX_VALUE );
			final int i2 = ran.nextInt( Integer.MAX_VALUE );
			final long paired = JVSUtils.szudzikPair( i1, i2 );
			final int[] unpaired = JVSUtils.szudzikUnpair( paired );
			assertArrayEquals( "Expected " + i1 + " and " + i2 + ", but got " + unpaired[ 0 ] + " and " + unpaired[ 1 ] + ".", new int[] { i1, i2 }, unpaired );
//			System.out.println( "" + i1 + "\t&\t" + i2 + "\t→\t" + paired + "\t→\t" + unpaired[ 0 ] + "\t&\t" + unpaired[ 1 ] + "." );
		}
	}

	@Test( expected = IllegalArgumentException.class )
	public void testBadArgument()
	{
		final List< String > sources = Arrays.asList( new String[] { "John", "Paul", "Ringo", "George", "Paul", "Paul", "John", "Ringo", "George", "George" } );
		final List< String > targets = Arrays.asList( new String[] { "Guitar", "Bass", "Drum", "Guitar", "Keyboard", "Voice", "Voice", "Voice", "Voice", "Voice" } );
		final List< Double > costs = Arrays.asList( new Double[] { 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.5, 3.0, 4.0, 9.0 } );
		new LAPSparseStructure< String, String >( sources, targets, costs, null );
	}

	@Test( expected = IllegalArgumentException.class )
	public void testBadArgumentSize()
	{
		final List< String > sources = Arrays.asList( new String[] { "John", "Paul", "Ringo", "George", "Paul", "Paul", "John", "Ringo", } );
		final List< String > targets = Arrays.asList( new String[] { "Guitar", "Bass", "Drum", "Guitar", "Keyboard", "Voice", "Voice", "Voice", "Voice" } );
		final List< Double > costs = Arrays.asList( new Double[] { 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.5, 3.0, 4.0, 9.0 } );
		new LAPSparseStructure< String, String >( sources, targets, costs, null );
	}

	@Test( expected = IllegalArgumentException.class )
	public void testBadArgumentSizeCost()
	{
		final List< String > sources = Arrays.asList( new String[] { "John", "Paul", "Ringo", "George", "Paul", "Paul", "John", "Ringo", "George" } );
		final List< String > targets = Arrays.asList( new String[] { "Guitar", "Bass", "Drums", "Guitar", "Keyboard", "Voice", "Voice", "Voice", "Voice" } );
		final List< Double > costs = Arrays.asList( new Double[] { 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 1.5, 3.0 } );
		new LAPSparseStructure< String, String >( sources, targets, costs, null );
	}

	@Test
	public void testRowIterator()
	{
		final Collection< Double > expectedValues = Arrays.asList( new Double[] { null, 1.0, null, 2.0, 2.0 } );
		final Iterator< Double > expectedIt = expectedValues.iterator();
		final int row = sparse.rowForSource( "Paul" );
		final Iterator< Double > rowIterator = sparse.rowIterator( row );
		while ( rowIterator.hasNext() )
		{
			final Double actual = rowIterator.next();
			final Double expected = expectedIt.next();
			assertEquals( expected, actual );
		}
	}

	@Test
	public void testColumnIterator()
	{
		final Collection< Double > expectedValues = Arrays.asList( new Double[] { 1.5, 2.0, 3.0, 4.0 } );
		final Iterator< Double > expectedIt = expectedValues.iterator();
		final int col = sparse.colForTarget( "Voice" );
		final Iterator< Double > colIterator = sparse.colIterator( col );
		while ( colIterator.hasNext() )
		{
			final Double actual = colIterator.next();
			final Double expected = expectedIt.next();
			assertEquals( expected, actual );
		}
	}

	@Test
	public void testGet()
	{
		final List< String > names = Arrays.asList( new String[] { "John", "Paul", "Ringo", "Georges" } );
		final List< String > instruments = Arrays.asList( new String[] { "Guitar", "Bass", "Drums", "Keyboard", "Voice" } );
		final List< Double > expectedVals = Arrays.asList( new Double[] {
				1.0, null, null, null, 1.5,
				null, 1.0, null, 2.0, 2.0,
				null, null, 1.0, null, 3.0,
				1.0, null, null, null, 4.0
		} );

		final Iterator<Double> itExpected = expectedVals.iterator();
		for ( int r = 0; r < names.size(); r++ )
		{
			for ( int c = 0; c < instruments.size(); c++ )
			{
				final Double expected = itExpected.next();
				final Double actual = sparse.get( r, c );
				assertEquals( "Wrong value for row = " + r + ", c = " + c, expected, actual );
			}

		}
	}

}
