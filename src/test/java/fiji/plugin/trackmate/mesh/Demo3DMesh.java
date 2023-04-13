package fiji.plugin.trackmate.mesh;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.itextpdf.text.pdf.codec.Base64;

import fiji.plugin.trackmate.detection.MaskUtils;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.Triangles;
import net.imagej.mesh.Vertices;
import net.imagej.mesh.io.stl.STLMeshIO;
import net.imagej.mesh.nio.BufferMesh;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class Demo3DMesh
{
	public static < T extends RealType< T > & NumericType< T > > void main( final String[] args ) throws IOException
	{
		final String filePath = "samples/mesh/CElegansMask3D.tif";

		ImageJ.main( args );
		final ImagePlus imp = IJ.openImage( filePath );
		imp.show();

		// To ImgLib2 boolean.

		// First channel is the mask.
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final ImgPlus< T > c1 = ImgPlusViews.hyperSlice( img, img.dimensionIndex( Axes.CHANNEL ), 0 );

		// Take the first time-point
		final ImgPlus< T > t1 = ImgPlusViews.hyperSlice( c1, c1.dimensionIndex( Axes.TIME ), 0 );

		// Make it to boolean.
		final RandomAccessibleInterval< BitType > mask = RealTypeConverters.convert( t1, new BitType() );

		// Convert it to labeling.
		final ImgLabeling< Integer, IntType > labeling = MaskUtils.toLabeling( mask, mask, 0.5, 1 );
		ImageJFunctions.show( labeling.getSource(), "labeling" );

		// Iterate through all components.
		final LabelRegions< Integer > regions = new LabelRegions< Integer >( labeling );
		final double[] cal = TMUtils.getSpatialCalibration( img );

		// Parse regions to create polygons on boundaries.
		final Iterator< LabelRegion< Integer > > iterator = regions.iterator();
		int j = 0;
		while ( iterator.hasNext() )
		{
			final LabelRegion< Integer > region = iterator.next();
			final IntervalView< BoolType > box = Views.zeroMin( region );

			// To mesh.
			final Mesh mesh = Meshes.marchingCubes( box );

			// Scale and offset with physical coordinates.
			final double[] origin = region.minAsDoubleArray();
			scale( mesh.vertices(), cal, origin );

			// Simplify.
			final Mesh simplified = Meshes.simplify( mesh, 0.25f, 10f );

			/*
			 * IO.
			 */
			testIO( simplified, ++j );

			/*
			 * Display.
			 */

			// Intersection with a XY plane at a fixed Z position.
			final int zslice = 20; // plan
			final double z = ( zslice ) * cal[ 2 ]; // um
			final Triangles triangles = simplified.triangles();
			final Vertices vertices = simplified.vertices();
			final List< double[] > polygon = new ArrayList<>();
			for ( long i = 0; i < triangles.size(); i++ )
			{
				final long v0 = triangles.vertex0( i );
				final double z0 = vertices.z( v0 );
				final long v1 = triangles.vertex1( i );
				final double z1 = vertices.z( v1 );
				final long v2 = triangles.vertex2( i );
				final double z2 = vertices.z( v2 );

				if ( ( z0 <= z && z1 > z && z2 > z ) ||
						( z1 <= z && z2 > z && z0 > z ) ||
						( z2 <= z && z0 > z && z1 > z ) ||
						( z0 >= z && z1 < z && z2 < z ) ||
						( z1 >= z && z2 < z && z0 < z ) ||
						( z2 >= z && z0 < z && z1 < z ) )
				{
					final double[] i1 = intersect( vertices, v0, v1, z );
					final double[] i2 = intersect( vertices, v1, v2, z );
					final double[] i3 = intersect( vertices, v2, v0, z );
					polygon.add( i1 );
					polygon.add( i2 );
					polygon.add( i3 );
				}
			}

			// Create a ROI to display.
			final Set< Point2D > set = new HashSet<>();
			for ( int i = 0; i < polygon.size(); i++ )
			{
				final double[] point = polygon.get( i );
				set.add( new Point2D.Double( point[ 0 ] / cal[ 1 ], point[ 1 ] / cal[ 1 ] ) );
			}
			final List< Point2D > list = new ArrayList<>( set );
			final double mx = list.stream().mapToDouble( p -> p.getX() ).average().getAsDouble();
			final double my = list.stream().mapToDouble( p -> p.getY() ).average().getAsDouble();
			list.sort( new Comparator< Point2D >()
			{

				@Override
				public int compare( final Point2D o1, final Point2D o2 )
				{
					final double angle1 = Math.atan2( o1.getY() - my, o1.getX() - mx );
					final double angle2 = Math.atan2( o2.getY() - my, o2.getX() - mx );
					return Double.compare( angle1, angle2 );
				}
			} );
			final float[] xRoi = new float[ list.size() ];
			final float[] yRoi = new float[ list.size() ];
			for ( int i = 0; i < list.size(); i++ )
			{
				xRoi[ i ] = ( float ) list.get( i ).getX();
				yRoi[ i ] = ( float ) list.get( i ).getY();
			}
			final PolygonRoi roi = new PolygonRoi( xRoi, yRoi, PolygonRoi.POLYGON );
			Overlay overlay = imp.getOverlay();
			if ( overlay == null )
			{
				overlay = new Overlay();
				imp.setOverlay( overlay );
			}
			overlay.add( roi );
		}
		System.out.println( "Done." );

	}

	private static double[] intersect( final Vertices vertices, final long v1, final long v2, final double z )
	{
		final double x1 = vertices.x( v1 );
		final double y1 = vertices.y( v1 );
		final double z1 = vertices.z( v1 );
		final double x2 = vertices.x( v2 );
		final double y2 = vertices.y( v2 );
		final double z2 = vertices.z( v2 );

		final double t;
		if ( z1 == z2 )
			t = 0.5;
		else
			t = ( z - z1 ) / ( z2 - z1 );
		final double x = x1 + t * ( x2 - x1 );
		final double y = y1 + t * ( y2 - y1 );
		return new double[] { x, y, z };
	}

	private static void testIO( final Mesh simplified, final int j )
	{
		final STLMeshIO meshIO = new STLMeshIO();

		// Encode to string.
		final String str = Base64.encodeBytes( meshIO.write( simplified ) );

		// Decode to mesh.
		final int nVertices = ( int ) simplified.vertices().size();
		final int nTriangles = ( int ) simplified.triangles().size();
		// We need to know N in advance. Save it in the XML?
		final Mesh decoded = new BufferMesh( nVertices, nTriangles );
		meshIO.read( decoded, Base64.decode( str ) );

		// Serialize to disk.
		try
		{
			meshIO.save( decoded, String.format( "samples/mesh/CElegansMask3D_%02d.stl", j ) );
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
	}

	private static void scale( final Vertices vertices, final double[] scale, final double[] origin )
	{
		final long nv = vertices.size();
		for ( long i = 0; i < nv; i++ )
		{
			final double x = ( origin[ 0 ] + vertices.x( i ) ) * scale[ 0 ];
			final double y = ( origin[ 1 ] + vertices.y( i ) ) * scale[ 1 ];
			final double z = ( origin[ 2 ] + vertices.z( i ) ) * scale[ 2 ];
			vertices.set( i, x, y, z );
		}
	}

}
