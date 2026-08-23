package fiji.plugin.trackmate.io.geff.imglib2.heuristics;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class tries to identify the property representing the radius of a
 * sphere or circle among a list of candidate property names using some
 * heuristics.
 */
public class RadiusIdentifierHeuristic
{

	/*
	 * In case we have common properties that do not work well with the
	 * heuristics in the future. So far I could only think of the following.
	 */
	private static final List< String > FALSE_POSITIVES = List.of( "ratio", "rate", "color", "row" );

	/*
	 * Names that are used in the GEFF specs and standard software.
	 */
	private static final List< String > GEFF_SPECIAL = List.of( "sphere", "radius", "r" );

	/**
	 * Given a list of property names, returns the one most likely to represent
	 * the radius. Returns null if no suitable candidate is found or if the best
	 * candidate's score is below a reasonable threshold.
	 *
	 * @param names
	 *            a list of candidate property names.
	 * @return the best-matching name, or null.
	 */
	public static String identify( final Iterable< String > names )
	{
		if ( names == null )
			return null;

		String bestCandidate = null;
		double bestScore = Double.NEGATIVE_INFINITY;

		for ( final String name : names )
		{
			final double s = score( name );
			if ( s > bestScore )
			{
				bestScore = s;
				bestCandidate = name;
			}
		}

		// Fail rather than returning a low-scoring candidate.
		final double threshold = 15.0;
		return ( bestScore >= threshold ) ? bestCandidate : null;
	}

	/**
	 * This method implements the core scoring for the radius heuristic.
	 * It tests for matches on "radius", "rad", "r", geometric fallbacks,
	 * and filters out common non-spatial false positives containing "rad".
	 * 
	 * @param name
	 *            the name to score
	 * @return a score, higher is better
	 */
	private static double score( final String name )
	{
		if ( name == null || name.isEmpty() )
			return 0;

		final String lower = name.toLowerCase();

		// Hard filter on false positives.
		for ( final String fp : FALSE_POSITIVES )
		{
			if ( lower.contains( fp ) )
				return Double.NEGATIVE_INFINITY;
		}

		double s = 0;

		// Exact matches
		if ( GEFF_SPECIAL.contains( lower ) )
			s += 100;
		if ( lower.equals( "rad" ) )
			s += 80;

		// Standalone token match (split on non-alphanumeric separators)
		final String[] tokens = lower.split( "[^a-z0-9]+" );
		for ( final String t : tokens )
		{
			if ( t.equals( "radius" ) )
				s += 75;
			else if ( t.equals( "rad" ) )
				s += 60;
			else if ( t.equals( "r" ) )
				s += 50;
			else if ( t.equals( "diameter" ) || t.equals( "dia" ) )
				s += 30; // Diameter is a good fallback proxy
			else if ( t.equals( "size" ) )
				s += 15;
		}

		// Starts/ends with patterns (e.g., sphereRadius, radiusX)
		if ( lower.startsWith( "radius" ) || lower.endsWith( "radius" ) )
			s += 50;
		if ( lower.startsWith( "rad" ) || lower.endsWith( "rad" ) )
			s += 40;

		// Loose containment
		if ( lower.contains( "radius" ) )
			s += 20;
		if ( lower.contains( "rad" ) )
			s += 15;

		// Numeric index patterns (e.g., r0, r1, r_1)
		final Matcher m = Pattern.compile( "^r_?\\d+$" ).matcher( lower );
		if ( m.find() )
			s += 65;

		// Fallback compound word containment
		if ( lower.contains( "sphere" ) || lower.contains( "circle" ) )
		{
			s += 10;
			if ( lower.contains( "size" ) || lower.contains( "width" ) || lower.contains( "dim" ) )
				s += 15;
		}

		return s;
	}

	public static void main( final String[] args )
	{
		System.out.println( "Best match: " + identify( Arrays.asList( "id", "name", "color", "radius", "height" ) ) );
		System.out.println( "Best match: " + identify( Arrays.asList( "x", "y", "r" ) ) );
		System.out.println( "Best match: " + identify( Arrays.asList( "sphereRadius", "sphereColor", "id" ) ) );
		System.out.println( "Best match: " + identify( Arrays.asList( "outer_rad", "inner_gradient", "radio_frequency" ) ) );
		System.out.println( "Best match: " + identify( Arrays.asList( "circleSize", "some_other_field" ) ) );
		System.out.println( "Best match: " + identify( Arrays.asList( "r0", "r1", "height" ) ) );
		System.out.println( "Best match: " + identify( Arrays.asList( "sphere", "x", "intensity" ) ) );
	}
}