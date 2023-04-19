package fiji.plugin.trackmate.mesh;

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
import net.imagej.mesh.Triangles;
import net.imglib2.RandomAccess;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.iterator.LocalizingIntervalIterator;
import net.imglib2.type.numeric.RealType;

public class DemoPixelIteration
{

	private static final boolean DEBUG = true;

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
					System.out.println( "Normals running sum: " );
					for ( int i = 0; i < nxs.size(); i++ )
						System.out.print( "( " + i + " -> " + nxs.subList( 0, i + 1 ).sum() + "), " );
					System.out.println();
				}

				// Merge duplicates.
				final int maxIndex = removeDuplicate( xs, nxs );

				if ( DEBUG )
				{
					System.out.println( "After removing duplicates:" );
					System.out.println( "XS: " + xs.subList( 0, maxIndex ) );
					System.out.println( "NS: " + nxs.subList( 0, maxIndex ) );
				}

				// DEBUG
//				if ( maxIndex % 2 != 0 )
//				{
//					System.out.println( "XS: " + xs.subList( 0, maxIndex ) );
//					System.out.println( "NS: " + nxs.subList( 0, maxIndex ) );
//				}

				// Iterate to build the inside score between each intersection.
				insideScore.resetQuick();
				insideScore.add( 0 );
				for ( int i = 0; i < maxIndex; i++ )
				{
					final double n = nxs.getQuick( i );
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
					final int i = xs.binarySearch( x, 0, maxIndex );
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

		private int removeDuplicate( final TDoubleArrayList ts, final TDoubleArrayList nxs )
		{
			if ( ts.size() < 2 )
				return ts.size();

			int j = 0;
			double accum = 0.;
			int nAccum = 0;
			int nPos  = 0;
			int nNeg  = 0;
			final double maxN;
			for ( int i = 0; i < ts.size() - 1; i++ )
			{
//				System.out.print( j + " -> " + i );
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
						// Majority.
//						final double vmaj;
//						if ( nPos == nNeg )
//							vmaj = 0.;
//						else if ( nPos > nNeg )
//							vmaj = 1.;
//						else
//							vmaj = -1.;
//						nxs.setQuick( j, vmaj );
					}
					accum = 0.;
					nAccum = 0;
					nPos = 0;
					nNeg = 0;
					j++;
				}
				else
				{
					final double v = nxs.getQuick( i );
					accum += v;
					if ( v > 0 )
						nPos++;
					if ( v < 0 )
						nNeg++;
					nAccum++;
//					System.out.print( ", " + accum );
				}
//				System.out.println();
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
				break;
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
