package fiji.plugin.trackmate.io.geff;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.IOUtils;
import net.imglib2.mesh.impl.nio.BufferMesh;

/**
 * Writes a small TrackMate model made of 3D mesh spots to a GEFF file, reads
 * it back, and checks that the meshes round-trip. Not a JUnit test; run it
 * with a main method.
 */
public class MeshGeffRoundTrip
{

	private static final double EPS = 1e-5;

	public static void main( final String[] args ) throws IOException
	{
		final String path = "samples/test-mesh-roundtrip.geff";
		new File( path ).getParentFile().mkdirs();
		IOUtils.deleteRecursively( Path.of( path ) );

		// 3 tetrahedron meshes, of different sizes and positions, plus 1
		// plain spot.
		final double[][] centers = { { 10, 20, 30 }, { 40, 50, 60 }, { 70, 80, 90 } };
		final double[] sizes = { 5., 8., 12. };
		final Model model = new Model();
		model.beginUpdate();
		try
		{
			final Spot[] spots = new Spot[ 4 ];
			for ( int i = 0; i < centers.length; i++ )
			{
				final BufferMesh mesh = tetrahedron( centers[ i ], sizes[ i ] );
				final SpotMesh spot = new SpotMesh( mesh, 1. + i, "m" + i );
				spot.putFeature( Spot.FRAME, 0. );
				spot.putFeature( Spot.POSITION_T, 0. );
				spots[ i ] = spot;
				model.getSpots().add( spot, 0 );
			}
			spots[ 3 ] = new SpotBase( 100, 110, 120, 1.5, 2., "plain" );
			spots[ 3 ].putFeature( Spot.FRAME, 0. );
			spots[ 3 ].putFeature( Spot.POSITION_T, 0. );
			model.getSpots().add( spots[ 3 ], 0 );
			model.addEdge( spots[ 0 ], spots[ 1 ], 1. );
			model.addEdge( spots[ 1 ], spots[ 2 ], 1. );
		}
		finally
		{
			model.endUpdate();
		}

		// The writer reads track and edge features, which need the analyzers
		// to run.
		final Settings settings = new Settings();
		settings.addAllAnalyzers();
		final TrackMate trackmate = new TrackMate( model, settings );
		trackmate.computeSpotFeatures( true );
		trackmate.computeEdgeFeatures( true );
		trackmate.computeTrackFeatures( true );

		System.out.println( "Writing " + path );
		final TmGeffWriter writer = new TmGeffWriter( path );
		writer.appendModel( model );
		writer.write();

		System.out.println( "Reading back." );
		final TmGeffReader reader = new TmGeffReader( path );
		final Model model2 = reader.getModel();
		if ( !reader.isReadingOk() )
			throw new IOException( "Reading failed: " + reader.getErrorMessage() );

		int nChecked = 0;
		for ( final Spot spot2 : model2.getSpots().iterable( 0, false ) )
		{
			final String name = spot2.getName();
			final Spot spot1 = byName( model, name );
			if ( spot1 == null )
				throw new AssertionError( "Spot " + name + " not found in original model." );
			if ( spot1 instanceof SpotMesh != spot2 instanceof SpotMesh )
				throw new AssertionError( "Mesh-ness mismatch for spot " + name );

			if ( spot1 instanceof SpotMesh )
			{
				checkMesh( ( SpotMesh ) spot1, ( SpotMesh ) spot2, name );
				nChecked++;
			}
		}
		if ( nChecked != centers.length )
			throw new AssertionError( "Expected " + centers.length + " mesh spots, checked " + nChecked );
		System.out.println( "OK: " + nChecked + " mesh spots round-tripped losslessly." );
	}

	private static void checkMesh( final SpotMesh s1, final SpotMesh s2, final String name )
	{
		final BufferMesh m1 = s1.getMesh();
		final BufferMesh m2 = s2.getMesh();

		if ( m1.vertices().size() != m2.vertices().size() )
			throw new AssertionError( name + ": vertex count mismatch: " + m1.vertices().size() + " vs " + m2.vertices().size() );
		if ( m1.triangles().size() != m2.triangles().size() )
			throw new AssertionError( name + ": triangle count mismatch: " + m1.triangles().size() + " vs " + m2.triangles().size() );

		// Vertices: sort lexicographically, compare (the vertex order in the
		// file might differ from the original one, but the set must be the
		// same).
		final List< double[] > v1 = sorted( m1 );
		final List< double[] > v2 = sorted( m2 );
		for ( int i = 0; i < v1.size(); i++ )
		{
			final double[] a = v1.get( i );
			final double[] b = v2.get( i );
			for ( int d = 0; d < 3; d++ )
				if ( Math.abs( a[ d ] - b[ d ] ) > EPS )
					throw new AssertionError( name + ": vertex " + i + " mismatch: "
							+ Arrays.toString( a ) + " vs " + Arrays.toString( b ) );
		}

		// Triangles: collect the index triplets remapped to the lex-sorted
		// vertex ranks, each triplet itself sorted, so the comparison is
		// insensitive to the vertex order in the file.
		final List< int[] > t1 = sortedTriangles( m1, v1 );
		final List< int[] > t2 = sortedTriangles( m2, v2 );
		final Comparator< int[] > c = ( x, y ) -> Arrays.compare( x, y );
		t1.sort( c );
		t2.sort( c );
		for ( int i = 0; i < t1.size(); i++ )
			if ( !Arrays.equals( t1.get( i ), t2.get( i ) ) )
				throw new AssertionError( name + ": triangle " + i + " mismatch: "
						+ Arrays.toString( t1.get( i ) ) + " vs " + Arrays.toString( t2.get( i ) ) );
	}

	private static List< double[] > sorted( final BufferMesh m )
	{
		final List< double[] > out = new ArrayList<>();
		for ( int i = 0; i < m.vertices().sizel(); i++ )
			out.add( new double[] { m.vertices().xf( i ), m.vertices().yf( i ), m.vertices().zf( i ) } );
		out.sort( ( a, b ) -> Arrays.compare( a, b ) );
		return out;
	}

	/**
	 * Returns the triangles of the mesh, as index triplets into the
	 * lex-sorted vertex list, each triplet itself sorted.
	 */
	private static List< int[] > sortedTriangles( final BufferMesh m, final List< double[] > sortedVerts )
	{
		final Map< List< Double >, Integer > rank = new HashMap<>();
		for ( int i = 0; i < sortedVerts.size(); i++ )
			rank.put( asList( sortedVerts.get( i ) ), Integer.valueOf( i ) );

		final List< int[] > out = new ArrayList<>();
		for ( int i = 0; i < m.triangles().sizel(); i++ )
		{
			final int[] tri = new int[ 3 ];
			tri[ 0 ] = rank.get( asList( new double[] {
					m.vertices().xf( m.triangles().vertex0( i ) ),
					m.vertices().yf( m.triangles().vertex0( i ) ),
					m.vertices().zf( m.triangles().vertex0( i ) ) } ) ).intValue();
			tri[ 1 ] = rank.get( asList( new double[] {
					m.vertices().xf( m.triangles().vertex1( i ) ),
					m.vertices().yf( m.triangles().vertex1( i ) ),
					m.vertices().zf( m.triangles().vertex1( i ) ) } ) ).intValue();
			tri[ 2 ] = rank.get( asList( new double[] {
					m.vertices().xf( m.triangles().vertex2( i ) ),
					m.vertices().yf( m.triangles().vertex2( i ) ),
					m.vertices().zf( m.triangles().vertex2( i ) ) } ) ).intValue();
			Arrays.sort( tri );
			out.add( tri );
		}
		return out;
	}

	private static List< Double > asList( final double[] a )
	{
		return Arrays.asList( a[ 0 ], a[ 1 ], a[ 2 ] );
	}

	private static Spot byName( final Model model, final String name )
	{
		for ( final Spot s : model.getSpots().iterable( 0, false ) )
			if ( s.getName().equals( name ) )
				return s;
		return null;
	}

	private static BufferMesh tetrahedron( final double[] c, final double s )
	{
		final float fx = ( float ) c[ 0 ], fy = ( float ) c[ 1 ], fz = ( float ) c[ 2 ], fs = ( float ) s;
		final BufferMesh mesh = new BufferMesh( 4, 4 );
		mesh.vertices().addf( fx + fs, fy + fs, fz + fs, 0, 0, 0, 0, 0 );
		mesh.vertices().addf( fx - fs, fy - fs, fz + fs, 0, 0, 0, 0, 0 );
		mesh.vertices().addf( fx - fs, fy + fs, fz - fs, 0, 0, 0, 0, 0 );
		mesh.vertices().addf( fx + fs, fy - fs, fz - fs, 0, 0, 0, 0, 0 );
		mesh.triangles().addf( 0, 1, 2, 0, 0, 0 );
		mesh.triangles().addf( 0, 3, 1, 0, 0, 0 );
		mesh.triangles().addf( 0, 2, 3, 0, 0, 0 );
		mesh.triangles().addf( 1, 3, 2, 0, 0, 0 );
		return mesh;
	}
}
