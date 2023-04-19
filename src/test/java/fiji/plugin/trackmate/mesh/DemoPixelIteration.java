package fiji.plugin.trackmate.mesh;

import java.io.IOException;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.MaskDetectorFactory;
import fiji.plugin.trackmate.detection.ThresholdDetectorFactory;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.Triangles;
import net.imagej.mesh.Vertices;
import net.imagej.mesh.io.stl.STLMeshIO;
import net.imagej.mesh.nio.BufferMesh;
import net.imglib2.RandomAccess;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.iterator.LocalizingIntervalIterator;
import net.imglib2.type.numeric.RealType;

public class DemoPixelIteration
{

	private static final boolean DEBUG = false;

	private static final int DEBUG_Z = 16;

	private static final int DEBUG_Y = 143;

	public static class MeshIterator
	{

		private final Mesh mesh;

		private final float[] bb;

		private final MollerTrumbore mollerTrumbore;

		private final double[] cal;

		private final long maxX;

		private final long minX;

		public MeshIterator( final Spot spot, final double[] cal )
		{
			this.cal = cal;
			final SpotMesh sm = spot.getMesh();
			this.mesh = sm.mesh;
			this.bb = sm.boundingBox;
			this.minX = Math.round( bb[ 0 ] / cal[ 0 ] );
			this.maxX = Math.round( bb[ 3 ] / cal[ 0 ] );
			this.mollerTrumbore = new MollerTrumbore( mesh );
		}

		public < T extends RealType< T > > void iterate( final RandomAccess< T > ra )
		{

			final long[] min = new long[ 3 ];
			final long[] max = new long[ 3 ];
			// Iterate only though Y and Z.
			min[ 0 ] = minX;
			max[ 0 ] = minX;
			for ( int d = 1; d < 3; d++ )
			{
				min[ d ] = Math.round( bb[ d ] / cal[ d ] );
				max[ d ] = Math.round( bb[ d + 3 ] / cal[ d ] );
			}
			final LocalizingIntervalIterator it = new LocalizingIntervalIterator( min, max );

			// Array of x position of intersections.
			final TDoubleArrayList xs = new TDoubleArrayList();
			// Array of triangle indices intersecting.
			final TLongArrayList tl = new TLongArrayList();
			// Array of triangle normals projected onto the X line.
			final TDoubleArrayList nxs = new TDoubleArrayList();
			// Array to store the 'inside' score.
			final TIntArrayList insideScore = new TIntArrayList();

			while ( it.hasNext() )
			{
				it.fwd();
				ra.setPosition( it );

				if ( DEBUG )
				{
					if ( it.getIntPosition( 2 ) != DEBUG_Z )
						continue;
					if ( it.getIntPosition( 1 ) != DEBUG_Y )
						continue;
				}

				// Get all the X position where triangles cross the line.
				final double y = it.getIntPosition( 1 ) * cal[ 1 ];
				final double z = it.getIntPosition( 2 ) * cal[ 2 ];
				getXIntersectingCoords( y, z, tl, xs );

				// No intersection?
				if ( xs.isEmpty() )
					continue;

				if ( DEBUG )
				{
					exportMeshSubset( tl );
				}

				// Collect normals projection on the X line.
				getNormalXProjection( tl, nxs );

				// Sort by by X coordinate of intersections.
				final int[] index = SortArrays.quicksort( xs );

				// Sort normal array with the same order.
				SortArrays.reorder( nxs, index );

				if ( DEBUG )
				{
					System.out.println();
					System.out.println( "Before removing duplicates:" );
					System.out.println( "XS: " + xs );
					System.out.println( "NS: " + nxs );
				}

				// Merge duplicates.
				final int maxIndex = removeDuplicate( xs, nxs );

				if ( DEBUG )
				{
					System.out.println( "After removing duplicates:" );
					System.out.println( "XS: " + xs.subList( 0, maxIndex ) );
					System.out.println( "NS: " + nxs.subList( 0, maxIndex ) );
				}

				final TDoubleArrayList outXs = new TDoubleArrayList();
				final TDoubleArrayList outNxs = new TDoubleArrayList();
				// Check we are alternating entering / leaving.
				checkAlternating( xs, nxs, maxIndex, outXs, outNxs );

				if ( DEBUG )
				{
					System.out.println( "After checking alternating:" );
					System.out.println( "XS: " + outXs );
					System.out.println( "NS: " + outNxs );
				}

				// Iterate to build the inside score between each intersection.
				insideScore.resetQuick();
				insideScore.add( 0 );
				for ( int i = 0; i < outNxs.size(); i++ )
				{
					final double n = outNxs.getQuick( i );
					final int prevScore = insideScore.getQuick( i );

					// Weird case: the normal is orthogonal to X. Should not
					// happen because we filtered out triangles parallel to the
					// X axis.
					if ( n == 0. )
					{
						insideScore.add( prevScore );
						continue;
					}
					else
					{
						final int score = prevScore + ( ( n > 0 ) ? -1 : 1 );
						insideScore.add( score );
					}
				}

				for ( long ix = minX; ix <= maxX; ix++ )
				{
					final double x = ix * cal[ 0 ];
					final int i = outXs.binarySearch( x );
					final boolean inside;
					if ( i < 0 )
					{
						final int ip = -( i + 1 );
						inside = insideScore.getQuick( ip ) > 0;
					}
					else
					{
						// On an intersection. We accept.
						inside = true;
					}
					if ( inside )
					{
						ra.setPosition( ix, 0 );
						ra.get().setReal( 500 );
					}
				}
			}
		}

		private void checkAlternating(
				final TDoubleArrayList xs, final TDoubleArrayList nxs, final int maxIndex,
				final TDoubleArrayList outXs, final TDoubleArrayList outNxs )
		{
			outXs.resetQuick();
			outNxs.resetQuick();

			double prevN = nxs.getQuick( 0 );
			final double prevX = xs.getQuick( 0 );

			outXs.add( prevX );
			outNxs.add( prevN );

			// The first one should be an entry (normal neg).
			assert prevN < 0;
			// The last one should be an exit (normal pos).
			assert nxs.getQuick( maxIndex ) > 0;

			for ( int i = 1; i < maxIndex; i++ )
			{
				final double n = nxs.getQuick( i );
				if ( n * prevN < 0. )
				{
					// Sign did change. All good.
					outXs.add( xs.getQuick( i ) );
					outNxs.add( n );
				}
				else
				{
					// Sign did not change! Merge.
					if ( n < 0. )
					{
						// Two consecutive entries.
						// Remove this one, so that the first valid entry stays.
					}
					else
					{
						// Two consecutive exits.
						// Remove the previous one, so that the last exit is
						// this one.
						outXs.removeAt( outXs.size() - 1 );
						outNxs.removeAt( outNxs.size() - 1 );
						// And add this one.
						outXs.add( xs.getQuick( i ) );
						outNxs.add( n );
					}
				}
				prevN = n;
			}
		}

		private void exportMeshSubset( final TLongArrayList tl )
		{
			final Triangles triangles = mesh.triangles();
			final Vertices vertices = mesh.vertices();
			final BufferMesh out = new BufferMesh( tl.size() * 3, tl.size() );
			for ( int i = 0; i < tl.size(); i++ )
			{
				final long id = tl.getQuick( i );

				final long v0 = triangles.vertex0( id );
				final double x0 = vertices.x( v0 );
				final double y0 = vertices.y( v0 );
				final double z0 = vertices.z( v0 );
				final double v0nx = vertices.nx( v0 );
				final double v0ny = vertices.ny( v0 );
				final double v0nz = vertices.nz( v0 );
				final long nv0 = out.vertices().add( x0, y0, z0, v0nx, v0ny, v0nz, 0., 0. );

				final long v1 = triangles.vertex1( id );
				final double x1 = vertices.x( v1 );
				final double y1 = vertices.y( v1 );
				final double z1 = vertices.z( v1 );
				final double v1nx = vertices.nx( v1 );
				final double v1ny = vertices.ny( v1 );
				final double v1nz = vertices.nz( v1 );
				final long nv1 = out.vertices().add( x1, y1, z1, v1nx, v1ny, v1nz, 0., 0. );

				final long v2 = triangles.vertex2( id );
				final double x2 = vertices.x( v2 );
				final double y2 = vertices.y( v2 );
				final double z2 = vertices.z( v2 );
				final double v2nx = vertices.nx( v2 );
				final double v2ny = vertices.ny( v2 );
				final double v2nz = vertices.nz( v2 );
				final long nv2 = out.vertices().add( x2, y2, z2, v2nx, v2ny, v2nz, 0., 0. );

				final double nx = triangles.nx( id );
				final double ny = triangles.ny( id );
				final double nz = triangles.nz( id );

				out.triangles().add( nv0, nv1, nv2, nx, ny, nz );
			}
			Meshes.removeDuplicateVertices( out, 0 );

			System.out.println( out );

			final STLMeshIO io = new STLMeshIO();
			try
			{
				io.save( out, "samples/mesh/io/intersect.stl" );
			}
			catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}

		/**
		 * Remove duplicate positions, for the normals, take the mean of normals
		 * at duplicate positions.
		 *
		 * @param ts
		 * @param nxs
		 * @return the new arrays length.
		 */
		private int removeDuplicate( final TDoubleArrayList ts, final TDoubleArrayList nxs )
		{
			if ( ts.size() < 2 )
				return ts.size();

			int j = 0;
			double accum = 0.;
			int nAccum = 0;
			for ( int i = 0; i < ts.size() - 1; i++ )
			{
				if ( ts.getQuick( i ) != ts.getQuick( i + 1 ) )
				{
					ts.setQuick( j, ts.getQuick( i ) );
					if ( nAccum == 0 )
					{
						nxs.setQuick( j, nxs.getQuick( i ) );
					}
					else
					{
						// Average.
						nxs.setQuick( j, accum / nAccum );
					}
					accum = 0.;
					nAccum = 0;
					j++;
				}
				else
				{
					final double v = nxs.getQuick( i );
					accum += v;
					nAccum++;
				}
			}

			ts.setQuick( j, ts.getQuick( ts.size() - 1 ) );
			if ( nAccum == 0 )
			{
				nxs.setQuick( j, nxs.getQuick( ts.size() - 1 ) );
			}
			else
			{
				nxs.setQuick( j, accum / nAccum );
			}
			j++;
			return j;
		}

		/**
		 * Returns the list of X coordinates where the line parallel to the X
		 * axis and passing through (0,y,z) crosses the triangles of the mesh.
		 * The list is unordered and may have duplicates.
		 *
		 * @param y
		 *            the Y coordinate of the line origin.
		 * @param z
		 *            the Z coordinate of the line origin.
		 * @param tl
		 *            a holder for the triangle indices intersecting.
		 * @param ts
		 *            a holder for the resulting intersections X coordinate.
		 */
		private void getXIntersectingCoords( final double y, final double z,
				final TLongArrayList tl, final TDoubleArrayList ts )
		{
			final double[] intersection = new double[ 3 ];
			tl.resetQuick();
			ts.resetQuick();
			// TODO optimize search of triangles with a data structure.
			for ( long id = 0; id < mesh.triangles().size(); id++ )
				if ( mollerTrumbore.rayIntersectsTriangle( id, 0, y, z, 1., 0, 0, intersection ) )
				{
					tl.add( id );
					ts.add( intersection[ 0 ] );
				}
				else
				{
//					// Second chance: Is this triangle parallel to the X axis
//					// and crossing the line?
//					if (mesh.triangles().nx( id ) == 0.)
//					{
//						final long v0 = mesh.triangles().vertex0( id );
//						final double z0 = mesh.vertices().z( v0 );
//						// Right Z?
//						if ( z0 != z )
//							continue;
//
//						final double y0 = mesh.vertices().y( v0 );
//						final long v1 = mesh.triangles().vertex1( id );
//						final double y1 = mesh.vertices().y( v1 );
//						final long v2 = mesh.triangles().vertex2( id );
//						final double y2 = mesh.vertices().y( v2 );
//						final double minY = Math.min( y0, Math.min( y1, y2 ) );
//						final double maxY = Math.max( y0, Math.max( y1, y2 ) );
//						if ( minY > y )
//							continue;
//						if ( maxY < y )
//							continue;
//
//						final double avg = ( mesh.vertices().x( v0 ) + mesh.vertices().x( v1 )
//								+ mesh.vertices().x( v2 ) ) / 3.;
//
//						tl.add( id );
//						ts.add( avg );
//					}
				}
		}

		private String triangleToString( final long id )
		{
			final StringBuilder str = new StringBuilder( id + ": " );

			final Triangles triangles = mesh.triangles();
			final Vertices vertices = mesh.vertices();
			final long v0 = triangles.vertex0( id );
			final double x0 = vertices.x( v0 );
			final double y0 = vertices.y( v0 );
			final double z0 = vertices.z( v0 );
			str.append( String.format( "(%5.1f, %5.1f, %5.1f) - ", x0, y0, z0 ) );

			final long v1 = triangles.vertex1( id );
			final double x1 = vertices.x( v1 );
			final double y1 = vertices.y( v1 );
			final double z1 = vertices.z( v1 );
			str.append( String.format( "(%5.1f, %5.1f, %5.1f) - ", x1, y1, z1 ) );

			final long v2 = triangles.vertex2( id );
			final double x2 = vertices.x( v2 );
			final double y2 = vertices.y( v2 );
			final double z2 = vertices.z( v2 );
			str.append( String.format( "(%5.1f, %5.1f, %5.1f) - ", x2, y2, z2 ) );

			str.append( String.format( "N = (%4.2f, %4.2f, %4.2f) ",
					triangles.nx( id ), triangles.nz( id ), triangles.nz( id ) ) );

			return str.toString();
		}

		private void getNormalXProjection( final TLongArrayList tl, final TDoubleArrayList nxs )
		{
			nxs.resetQuick();
			final Triangles triangles = mesh.triangles();
			for ( int id = 0; id < tl.size(); id++ )
				nxs.add( triangles.nx( tl.getQuick( id ) ) );
		}
	}

	@SuppressWarnings( "unchecked" )
	public static < T extends RealType< T > > void main( final String[] args )
	{
		try
		{
			ImageJ.main( args );

//			final Mesh mesh = Demo3DMesh.debugMesh( new long[] { 4, 4, 4 }, new long[] { 10, 10, 10 } );
//			final Spot s0 = SpotMesh.createSpot( mesh, 1. );
//			final Model model = new Model();
//			model.beginUpdate();
//			try
//			{
//				model.addSpotTo( s0, 0 );
//			}
//			finally
//			{
//				model.endUpdate();
//			}
//			final ImagePlus imp = NewImage.createByteImage( "cube", 16, 16, 16, NewImage.FILL_BLACK );

			final String imPath = "samples/mesh/CElegansMask3DNoScale-mask-t1.tif";
			final ImagePlus imp = IJ.openImage( imPath );

			final Settings settings = new Settings( imp );
			settings.detectorFactory = new MaskDetectorFactory<>();
			settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
			settings.detectorSettings.put( ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS,
					false );

			final TrackMate trackmate = new TrackMate( settings );
			trackmate.setNumThreads( 4 );
			trackmate.execDetection();

			final Model model = trackmate.getModel();
			final SpotCollection spots = model.getSpots();
			spots.setVisible( true );

			final ImagePlus out = NewImage.createShortImage( "OUT", imp.getWidth(), imp.getHeight(), imp.getNSlices(), NewImage.FILL_BLACK );
			out.show();
			out.setSlice( DEBUG_Z + 1 );
			out.resetDisplayRange();

			final double[] cal = TMUtils.getSpatialCalibration( imp );
			for ( final Spot spot : model.getSpots().iterable( true ) )
			{
				final MeshIterator it = new MeshIterator( spot, cal );
				it.iterate( ( RandomAccess< T > ) ImageJFunctions.wrap( out ).randomAccess() );
				it.iterate( ( RandomAccess< T > ) ImageJFunctions.wrap( imp ).randomAccess() );
			}

			imp.show();
			imp.setSlice( DEBUG_Z + 1 );
			imp.resetDisplayRange();
			final SelectionModel sm = new SelectionModel( model );
			final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
			final HyperStackDisplayer view = new HyperStackDisplayer( model, sm, imp, ds );
			view.render();

		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
