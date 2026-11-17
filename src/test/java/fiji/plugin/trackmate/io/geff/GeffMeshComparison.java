package fiji.plugin.trackmate.io.geff;

import static fiji.plugin.trackmate.io.geff.TmGeffWriter.MESH_F_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter.MESH_VN_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter.MESH_V_PROP;
import static fiji.plugin.trackmate.io.geff.TmGeffWriter.TRACKMATE_SPOT_ID_PROP;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipFile;

import org.janelia.saalfeldlab.n5.zarr.N5ZarrReader;
import org.litt.geff.io.IoUtils;
import org.litt.geff.props.ElementType;
import org.litt.geff.props.GeffProperties;
import org.litt.geff.props.GeffProperty;
import org.litt.geff.props.Suppliers;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.io.xml.TmXmlReader;
import fiji.plugin.trackmate.io.xml.TmXmlWriter;
import net.imglib2.mesh.Mesh;
import net.imglib2.mesh.Meshes;
import net.imglib2.mesh.impl.nio.BufferMesh;
import net.imglib2.mesh.io.ply.PLYMeshIO;

/**
 * Standalone tool that loads the same session from a GEFF file and a TrackMate
 * XML file (+ its <code>.meshes</code> companion ZIP), and verifies that the
 * spot meshes they contain are identical. I made it because I was getting crazy
 * trying to understand why the meshes where not properly loading from GEFF.
 * <p>
 * It compares the mesh at two levels:
 * <ol>
 * <li>The <b>raw file data</b>: the GEFF mesh property datasets read directly
 * from the Zarr store, against the PLY files stored in the XML companion ZIP.
 * Note that the two formats store the mesh with a different reference: GEFF
 * stores vertices <i>relative to the spot center</i> (the PLY stores them in
 * world coordinates), so raw vertices are compared after adding back the spot
 * position to the GEFF ones.</li>
 * <li>The <b>reloaded models</b>: the {@link SpotMesh} instances built by
 * {@link TmGeffReader} and {@link TmXmlReader}, matched by spot ID.</li>
 * </ol>
 * <p>
 * It writes the dumps to files in a specified output folder, so that they can
 * be inspected manually, and prints a summary to the console.
 * <p>
 * Usage:
 *
 * <pre>
 * java GeffMeshComparison &lt;path-to-geff&gt; &lt;path-to-xml&gt; &lt;output-folder&gt;
 * </pre>
 *
 * With no argument, it runs on the CElegans3D demo files in the
 * <code>samples/</code> folder and writes to a temporary folder.
 *
 * @author Jean-Yves Tinevez
 */
public class GeffMeshComparison extends GeffTestBase
{

	private static final String GEFF_DEFAULT = "samples/test-cube-mesh.geff";

	private static final String XML_DEFAULT = "samples/test-cube-mesh.xml";

	public static void main( final String[] args ) throws Exception
	{
		final Model model = new Model();
		model.getSpots().add( cubeMesh(), 0 );

//		final ImagePlus imp = ViewUtils.makeEmptyImagePlus( 10, 10, 10, 2, new double[] { 1., 1., 1. } );
//		final GuiModel gm = new GuiModel( model, imp );
//		new TrackMateBVV<>( gm ).render();

		final String geffPath = args.length > 0 ? args[ 0 ] : GEFF_DEFAULT;
		final String xmlPath = args.length > 1 ? args[ 1 ] : XML_DEFAULT;
		final String outDir = args.length > 2
				? args[ 2 ]
				: Files.createTempDirectory( "geffmesh" ).toAbsolutePath().toString();
		Files.createDirectories( Paths.get( outDir ) );
		System.out.println( "Output folder: " + outDir );

		/*
		 * Level 0: save to GEFF and XML
		 */

		final TmGeffWriter geffWriter = new TmGeffWriter( geffPath );
		geffWriter.appendModel( model );
		geffWriter.write();

		final TmXmlWriter xmlWriter = new TmXmlWriter( new File( xmlPath ) );
		xmlWriter.appendModel( model );
		xmlWriter.writeToFile();

		/*
		 * Level 1: raw file data.
		 */

		dumpGeffRaw( geffPath, outDir + "/geff-raw.txt" );
		dumpPlyRaw( xmlPath, outDir + "/ply-raw.txt" );
		diff( outDir + "/geff-raw.txt", outDir + "/ply-raw.txt", outDir + "/diff-raw.txt", 1e-4 );

		/*
		 * Level 2: reloaded models.
		 */

		final TmGeffReader geffReader = new TmGeffReader( geffPath );
		final Model geffModel = geffReader.getModel();
		if ( geffModel == null )
		{
			System.err.println( "Could not read GEFF file: " + geffReader.getErrorMessage() );
			return;
		}
		// Test that we can detect a diff in the model.
//		final SpotMesh next = ( SpotMesh ) geffModel.getSpots().iterator( false ).next();
//		final Vertices vertices = next.getMesh().vertices();
//		vertices.set( 0, -3., -3., -3. );

		final TmXmlReader xmlReader = new TmXmlReader( xmlPath );
		final Model xmlModel = xmlReader.getModel();
		if ( xmlModel == null )
		{
			System.err.println( "Could not read XML file: " + xmlReader.getErrorMessage() );
			return;
		}

		try (final PrintWriter wg = new PrintWriter( outDir + "/geff-model.txt", "UTF-8" );
				final PrintWriter wx = new PrintWriter( outDir + "/xml-model.txt", "UTF-8" ))
		{
			dumpModelMeshes( geffModel, "GEFF", wg );
			dumpModelMeshes( xmlModel, "XML", wx );
		}
		diff( outDir + "/geff-model.txt", outDir + "/xml-model.txt", outDir + "/diff-models.txt", 1e-5 );

		System.out.println( "Done. Inspect the files in " + outDir );
	}

	/**
	 * Reads the mesh properties of all the nodes directly from the GEFF Zarr
	 * store, and dumps them as text.
	 */
	private static void dumpGeffRaw( final String geffPath, final String outPath ) throws Exception
	{
		try (final PrintWriter w = new PrintWriter( outPath, "UTF-8" );
				final N5ZarrReader n5 = new N5ZarrReader( geffPath, true ))
		{
			final GeffProperties props = IoUtils.loadProperties( n5, ElementType.NODE );
			final long nNodes = props.numElements();
			w.println( "# nodes: " + nNodes );

			final GeffProperty< ? > tmIds = props.property( TRACKMATE_SPOT_ID_PROP );
			final GeffProperty< ? > meshV = props.property( MESH_V_PROP );
			final GeffProperty< ? > meshN = props.property( MESH_VN_PROP );
			final GeffProperty< ? > meshF = props.property( MESH_F_PROP );
			final GeffProperty< ? > px = props.property( "x" );
			final GeffProperty< ? > py = props.property( "y" );
			final GeffProperty< ? > pz = props.property( "z" );
			for ( long i = 0; i < nNodes; i++ )
			{
				tmIds.elementIndex().set( i );
				if ( !Suppliers.asMaybeIntSupplier( tmIds ).get().isPresent() )
				{
					w.println( "# node " + i + " has no TrackMate spot ID." );
					continue;
				}
				final int tmId = Suppliers.asMaybeIntSupplier( tmIds ).get().get();

				// Spot position, to convert the mesh vertices (stored relative
				// to the spot center) to world coordinates.
				px.elementIndex().set( i );
				py.elementIndex().set( i );
				pz.elementIndex().set( i );
				final double x = Suppliers.asMaybeDoubleSupplier( px ).get().get();
				final double y = Suppliers.asMaybeDoubleSupplier( py ).get().get();
				final double z = Suppliers.asMaybeDoubleSupplier( pz ).get().get();

				meshV.elementIndex().set( i );
				final float[] v = Suppliers.asFloatArraySupplier( meshV ).get();
				meshN.elementIndex().set( i );
				final float[] nv = Suppliers.asFloatArraySupplier( meshN ).get();
				meshF.elementIndex().set( i );
				final long[] f = Suppliers.asLongArraySupplier( meshF ).get();

				w.println( "# spot trackmate_id=" + tmId + " pos=(" + x + "," + y + "," + z + ") verts=" + ( v.length / 3 ) + " tris=" + ( f.length / 3 ) );
				// Vertices are dumped in WORLD coordinates (stored value + spot
				// position), so that they can be compared directly with the PLY
				// files of the XML companion ZIP.
				for ( int k = 0; k < v.length / 3; k++ )
					w.printf( "V %d %.9g %.9g %.9g%n", k, v[ 3 * k ] + x, v[ 3 * k + 1 ] + y, v[ 3 * k + 2 ] + z );
				for ( int k = 0; k < nv.length / 3; k++ )
					w.printf( "N %d %.9g %.9g %.9g%n", k, nv[ 3 * k ], nv[ 3 * k + 1 ], nv[ 3 * k + 2 ] );
				for ( int k = 0; k < f.length / 3; k++ )
					w.printf( "F %d %d %d %d%n", k, f[ 3 * k ], f[ 3 * k + 1 ], f[ 3 * k + 2 ] );
			}
		}
	}

	/**
	 * Reads all the PLY files stored in the XML companion meshes ZIP, and dumps
	 * their content as text. The PLY stores vertex positions in world
	 * coordinates.
	 */
	private static void dumpPlyRaw( final String xmlPath, final String outPath ) throws Exception
	{
		try (final PrintWriter w = new PrintWriter( outPath, "UTF-8" );
				final ZipFile zip = new ZipFile( xmlPath + ".meshes" ))
		{
			final var entries = zip.entries();
			while ( entries.hasMoreElements() )
			{
				final var entry = entries.nextElement();
				if ( !entry.getName().endsWith( ".ply" ) )
					continue;
				final Mesh m = PLYMeshIO.open( zip.getInputStream( entry ) );
				w.println( "# ply entry: " + entry.getName() + " verts=" + m.vertices().sizel() + " tris=" + m.triangles().sizel() );
				for ( long i = 0; i < m.vertices().sizel(); i++ )
					w.printf( "V %d %.9g %.9g %.9g%n", i, m.vertices().xf( i ), m.vertices().yf( i ), m.vertices().zf( i ) );
				for ( long i = 0; i < m.vertices().sizel(); i++ )
					w.printf( "N %d %.9g %.9g %.9g%n", i, m.vertices().nxf( i ), m.vertices().nyf( i ), m.vertices().nzf( i ) );
				for ( long i = 0; i < m.triangles().sizel(); i++ )
					w.printf( "F %d %d %d %d%n", i, m.triangles().vertex0( i ), m.triangles().vertex1( i ), m.triangles().vertex2( i ) );
			}
		}
	}

	private static void dumpModelMeshes( final Model model, final String label, final PrintWriter w )
	{
		final TreeMap< Integer, SpotMesh > meshes = new TreeMap<>();
		for ( final Spot s : model.getSpots().iterable( false ) )
			if ( s instanceof SpotMesh )
				meshes.put( Integer.valueOf( s.ID() ), ( SpotMesh ) s );

		w.println( "# " + label + " model: " + model.getSpots().iterable( false ).spliterator().estimateSize()
				+ " spots, " + meshes.size() + " SpotMesh" );
		for ( final var e : meshes.entrySet() )
		{
			final SpotMesh sm = e.getValue();
			final Spot spot = sm;
			w.println( "# spot ID=" + e.getKey() + " pos=(" + spot.getDoublePosition( 0 ) + "," + spot.getDoublePosition( 1 )
					+ "," + spot.getDoublePosition( 2 ) + ") RADIUS=" + spot.getFeature( "RADIUS" ) );
			dumpMesh( sm.getMesh(), w, label + " spot " + e.getKey() );
		}
	}

	private static void dumpMesh( final Mesh m, final PrintWriter w, final String label )
	{
		w.println( "# " + label + " vertices: " + m.vertices().sizel() + " triangles: " + m.triangles().sizel() );
		for ( long i = 0; i < m.vertices().sizel(); i++ )
			w.printf( "V %d %.9g %.9g %.9g%n", i, m.vertices().xf( i ), m.vertices().yf( i ), m.vertices().zf( i ) );
		for ( long i = 0; i < m.vertices().sizel(); i++ )
			w.printf( "N %d %.9g %.9g %.9g%n", i, m.vertices().nxf( i ), m.vertices().nyf( i ), m.vertices().nzf( i ) );
		for ( long i = 0; i < m.triangles().sizel(); i++ )
			w.printf( "F %d %d %d %d%n", i, m.triangles().vertex0( i ), m.triangles().vertex1( i ), m.triangles().vertex2( i ) );
	}

	/**
	 * Line-based diff of two dumps. Lines starting with a letter (V, N, F) are
	 * matched by their index, so that header or meta-line differences do not
	 * shift the comparison. Prints a summary to the diff file and the console.
	 */
	private static void diff( final String aPath, final String bPath, final String outPath, final double vTolerance ) throws Exception
	{
		final List< String > a = Files.readAllLines( Paths.get( aPath ) );
		final List< String > b = Files.readAllLines( Paths.get( bPath ) );

		// Index the lines by kind and index: e.g. "V:3" -> "V 3 0.1 0.2 0.3".
		final TreeMap< String, String > mapA = index( a );
		final TreeMap< String, String > mapB = index( b );

		try (final PrintWriter w = new PrintWriter( outPath, "UTF-8" ))
		{
			if ( !mapA.keySet().equals( mapB.keySet() ) )
			{
				final TreeMap< String, String > onlyA = new TreeMap<>( mapA );
				onlyA.keySet().removeAll( mapB.keySet() );
				final TreeMap< String, String > onlyB = new TreeMap<>( mapB );
				onlyB.keySet().removeAll( mapA.keySet() );
				w.println( "STRUCTURAL DIFFERENCE:" );
				for ( final String k : onlyA.keySet() )
					w.println( "  only in " + aPath + ": " + mapA.get( k ) );
				for ( final String k : onlyB.keySet() )
					w.println( "  only in " + bPath + ": " + mapB.get( k ) );
				System.out.println( outPath + ": STRUCTURAL DIFFERENCE — see file." );
				return;
			}

			int nDiffs = 0;
			double maxVdist = 0.;
			for ( final String k : mapA.keySet() )
			{
				final String la = mapA.get( k ), lb = mapB.get( k );
				if ( la.equals( lb ) )
					continue;
				if ( k.startsWith( "V:" ) )
				{
					final double d = vertexDistance( la, lb );
					maxVdist = Math.max( maxVdist, d );
					if ( d > vTolerance )
					{
						w.println( "V DIFF (dist=" + d + ") " + la + " || " + lb );
						nDiffs++;
					}
				}
				else
				{
					w.println( "DIFF " + la + " || " + lb );
					nDiffs++;
				}
			}
			if ( nDiffs == 0 )
			{
				w.println( "IDENTICAL (" + mapA.size() + " elements; vertices equal within 1e-5" + ")" );
				System.out.println( outPath + ": IDENTICAL — " + mapA.size() + " elements." );
			}
			else
			{
				w.println( "TOTAL DIFFS: " + nDiffs + " / " + mapA.size() + " (max vertex distance " + maxVdist + ")" );
				System.out.println( outPath + ": " + nDiffs + " DIFFS of " + mapA.size() + " elements — see file." );
			}
		}
	}

	/**
	 * Index the dump lines: "V 12 0.1 ..." -> key "V:12", etc. Meta lines
	 * (starting with '#') are skipped: they carry the file-specific labels and
	 * are not part of the mesh data.
	 */
	private static TreeMap< String, String > index( final List< String > lines )
	{
		final TreeMap< String, String > map = new TreeMap<>();
		for ( final String line : lines )
		{
			if ( line.startsWith( "#" ) )
				continue;
			final int sp = line.indexOf( ' ' );
			if ( sp < 0 )
				continue;
			final String kind = line.substring( 0, sp );
			final int sp2 = line.indexOf( ' ', sp + 1 );
			final String idx = sp2 < 0 ? "" : line.substring( sp + 1, sp2 );
			map.put( kind + ":" + idx, line );
		}
		return map;
	}

	private static double vertexDistance( final String a, final String b )
	{
		final String[] pa = a.split( "\\s+" ), pb = b.split( "\\s+" );
		final double dx = Double.parseDouble( pa[ 2 ] ) - Double.parseDouble( pb[ 2 ] );
		final double dy = Double.parseDouble( pa[ 3 ] ) - Double.parseDouble( pb[ 3 ] );
		final double dz = Double.parseDouble( pa[ 4 ] ) - Double.parseDouble( pb[ 4 ] );
		return Math.sqrt( dx * dx + dy * dy + dz * dz );
	}

	private static SpotMesh cubeMesh()
	{
		final BufferMesh mesh = new BufferMesh( 8, 12 );

		final long v0 = mesh.vertices().add( -1, -1, -1 );
		final long v1 = mesh.vertices().add( 1, -1, -1 );
		final long v2 = mesh.vertices().add( 1, 1, -1 );
		final long v3 = mesh.vertices().add( -1, 1, -1 );
		final long v4 = mesh.vertices().add( -1, -1, 1 );
		final long v5 = mesh.vertices().add( 1, -1, 1 );
		final long v6 = mesh.vertices().add( 1, 1, 1 );
		final long v7 = mesh.vertices().add( -1, 1, 1 );

		mesh.triangles().add( v4, v5, v6 );
		mesh.triangles().add( v4, v6, v7 );
		mesh.triangles().add( v1, v0, v3 );
		mesh.triangles().add( v1, v3, v2 );
		mesh.triangles().add( v7, v6, v2 );
		mesh.triangles().add( v7, v2, v3 );
		mesh.triangles().add( v0, v1, v5 );
		mesh.triangles().add( v0, v5, v4 );
		mesh.triangles().add( v5, v1, v2 );
		mesh.triangles().add( v5, v2, v6 );
		mesh.triangles().add( v0, v4, v7 );
		mesh.triangles().add( v0, v7, v3 );

		Meshes.translate( mesh, new double[] { 2., 2., 2. } );

		final SpotMesh spot = new SpotMesh( mesh, -1. );
		return spot;
	}

}
