package fiji.plugin.trackmate.io.geff.imglib2.heuristics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class tries to identify the X, Y, Z axes among 1, 2 or 3 axis names
 * using some heuristics, based on what I have seen in the field.
 */
public class AxisIdentifierHeuristic
{

	/*
	 * Some common names from other fields.
	 */
	private static final Map< Character, List< String > > SYNONYMS = new HashMap<>();
	static
	{
		SYNONYMS.put( 'x', Arrays.asList( "width", "east", "lon", "longitude", "long", "right", "horizontal" ) );
		SYNONYMS.put( 'y', Arrays.asList( "height", "north", "lat", "latitude", "vertical" ) );
		SYNONYMS.put( 'z', Arrays.asList( "depth", "altitude", "alt", "elevation", "up", "forward" ) );
	}

	private static final char[] AXES = { 'x', 'y', 'z' };

	/**
	 * Given a list of 1, 2 or 3 axis-name candidates, returns a map from axis
	 * letter ('x','y','z') to the original string that best matches it. Only as
	 * many axes as there are names will be present in the returned map.
	 *
	 * @param names
	 *            a list of 1, 2 or 3 candidate axis names.
	 * @return a map from axis letter to the best-matching name.
	 */
	public static Map< Character, String > identify( final List< String > names )
	{
		final int n = names.size();
		if ( n < 1 || n > 3 )
			throw new IllegalArgumentException( "Expected 1, 2 or 3 names, got " + n );

		// Score each possible combination.
		final double[][] score = new double[ n ][ 3 ];
		for ( int i = 0; i < n; i++ )
		{
			for ( int j = 0; j < 3; j++ )
				score[ i ][ j ] = score( names.get( i ), AXES[ j ] );
		}

		// Brute force search. C'est pas la mer à boire, n <= 3.
		int[] bestAssignment = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for ( final int[] assignment : axisAssignments( n ) )
		{
			double total = 0;
			for ( int i = 0; i < n; i++ )
				total += score[ i ][ assignment[ i ] ];
			if ( total > bestScore )
			{
				bestScore = total;
				bestAssignment = assignment;
			}
		}

		final Map< Character, String > result = new TreeMap<>();
		for ( int i = 0; i < n; i++ )
			result.put( AXES[ bestAssignment[ i ] ], names.get( i ) );
		return result;
	}

	/**
	 * This method implements the core scoring for the heuristic. It's not
	 * rocket science: we test for the presence of x,y,z letters in the name,
	 * for 0, 1, 2, 3 etc or for names that are known synonyms of the axes. The
	 * scoring is arbitrary. One thing I struggled with is are axis names with a
	 * 'x' 'y' 'z' INSIDE the name, while most of the time the letter is at the
	 * start or end of the name.
	 * 
	 * @param name
	 *            the name to score
	 * @param axis
	 *            the axis to score against, one of 'x', 'y', 'z'
	 * @return a score, higher is better
	 */
	private static double score( final String name, final char axis )
	{
		if ( name == null || name.isEmpty() )
			return 0;
		final String lower = name.toLowerCase();
		double s = 0;
		final String letter = String.valueOf( axis );

		// exact match
		if ( lower.equals( letter ) )
			s += 100;

		// standalone token match (split on non-alphanumeric separators)
		final String[] tokens = lower.split( "[^a-z0-9]+" );
		for ( final String t : tokens )
		{
			if ( t.equals( letter ) )
				s += 80;
		}

		// e.g. posX, Xpos, ...
		if ( lower.matches( ".*[^a-z]" + letter + "([^a-z].*)?$" ) || lower.matches( "^" + letter + "[^a-z].*" ) )
			s += 60;

		// starts / ends with the raw letter
		if ( lower.startsWith( letter ) )
			s += 50;
		if ( lower.endsWith( letter ) )
			s += 50;

		// loose containment (weak signal, avoid double counting too much)
		if ( lower.contains( letter ) )
			s += 20;

		// domain synonyms
		for ( final String syn : SYNONYMS.get( axis ) )
		{
			if ( lower.contains( syn ) )
				s += 70;
		}

		// numeric index patterns (0-based and 1-based)
		final Matcher m = Pattern.compile( "\\d+" ).matcher( lower );
		if ( m.find() )
		{
			final int num = Integer.parseInt( m.group() );
			final int idx = axis - 'x'; // x=0, y=1, z=2
			if ( num == idx )
				s += 40; // 0-based
			if ( num == idx + 1 )
				s += 35; // 1-based
		}

		return s;
	}

	/**
	 * Generates all ordered assignments of {@code n} distinct axis indices
	 * (chosen from {0,1,2}, corresponding to {@link #AXES}) to {@code n} names.
	 * This is equivalent to all permutations of size {@code n} taken from the 3
	 * possible axes.
	 *
	 * @param n
	 *            the number of names to assign axes to (1, 2 or 3).
	 * @return the list of assignments, each an int[] of length n with values in
	 *         {0,1,2}, all distinct.
	 */
	private static List< int[] > axisAssignments( final int n )
	{
		final List< int[] > result = new ArrayList<>();
		final boolean[] used = new boolean[ 3 ];
		final int[] current = new int[ n ];
		generateAssignments( n, used, current, 0, result );
		return result;
	}

	private static void generateAssignments( final int n, final boolean[] used, final int[] current, final int pos, final List< int[] > result )
	{
		if ( pos == n )
		{
			result.add( current.clone() );
			return;
		}
		for ( int idx = 0; idx < 3; idx++ )
		{
			if ( !used[ idx ] )
			{
				used[ idx ] = true;
				current[ pos ] = idx;
				generateAssignments( n, used, current, pos + 1, result );
				used[ idx ] = false;
			}
		}
	}

	public static void main( final String[] args )
	{
		System.out.println( identify( Arrays.asList( "POSITION_X", "POSITION_Y", "POSITION_Z" ) ) );
		System.out.println( identify( Arrays.asList( "cell_y", "cell_x", "cell_z" ) ) );
		System.out.println( identify( Arrays.asList( "posX", "posY", "posZ" ) ) );
		System.out.println( identify( Arrays.asList( "0", "1", "2" ) ) );
		System.out.println( identify( Arrays.asList( "3", "1", "2" ) ) );
		System.out.println( identify( Arrays.asList( "z", "y", "posX" ) ) );
		System.out.println( identify( Arrays.asList( "width", "height", "depth" ) ) );
		System.out.println( identify( Arrays.asList( "Longitude", "Latitude", "Altitude" ) ) );
		System.out.println( identify( Arrays.asList( "coord_2", "coord_0", "coord_1" ) ) );
		System.out.println( identify( Arrays.asList( "east", "north", "up" ) ) );

		// Less than 3 names
		System.out.println( identify( Arrays.asList( "POSITION_X", "POSITION_Y" ) ) );
		System.out.println( identify( Arrays.asList( "posY" ) ) );
	}
}
