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
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import net.imagej.mesh.Mesh;
import net.imglib2.RandomAccess;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.iterator.LocalizingIntervalIterator;
import net.imglib2.type.numeric.RealType;

public class DemoPixelIteration
{

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

			final TDoubleArrayList xs = new TDoubleArrayList();
			final double[] coords = new double[ 3 ];
			while ( it.hasNext() )
			{
				it.fwd();
				ra.setPosition( it );

				// Get all the X position where triangles cross the line.
				final double y = it.getIntPosition( 1 ) * cal[ 1 ];
				final double z = it.getIntPosition( 2 ) * cal[ 2 ];
				getXIntersectingCoords( y, z, xs, coords );

				// No intersection?
				if ( xs.isEmpty() )
					continue;

				xs.sort();
				final int xsSize = xs.size();

				final double firstIntersection = xs.min();
				final double lastIntersection = xs.max();
				for ( long ix = minX; ix <= maxX; ix++ )
				{
					final double x = ix * cal[ 0 ];
					final boolean inside;
					if ( x < firstIntersection || x > lastIntersection )
					{
						inside = false;
					}
					else
					{
						final int i = xs.binarySearch( x, 0, xsSize );
						if ( i < 0 )
						{
							final int ip = -( i + 1 );

							// Below the first intersection or beyond the last.
							if ( ip == 0 || ip == xs.size() )
							{
								inside = false;
							}
							else
							{
								// Between two intersections.
								inside = ( ip % 2 ) != 0;
							}
						}
						else
						{
							// On an intersection. We accept.
							inside = true;
						}
					}
					if ( inside )
					{
						ra.setPosition( ix, 0 );
						ra.get().setReal( 500 );
					}
				}
			}
		}

		private int removeDuplicate( final TDoubleArrayList ts )
		{
			// Sort it.
			ts.sort();

			if ( ts.size() < 2 )
				return ts.size();

			int j = 0;
			for ( int i = 0; i < ts.size() - 1; i++ )
			{
				if ( ts.get( i ) != ts.get( i + 1 ) )
				{
					ts.set( j++, ts.get( i ) );
				}
			}

			ts.set( j++, ts.get( ts.size() - 1 ) );
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
		 * @param ts
		 *            a holder for the resulting intersections X coordinate.
		 * @param intersection
		 *            a holder for intersection coordinates, messed with
		 *            internally.
		 */
		private void getXIntersectingCoords( final double y, final double z, final TDoubleArrayList ts, final double[] intersection )
		{
			ts.resetQuick();
			for ( long id = 0; id < mesh.triangles().size(); id++ )
				if ( mollerTrumbore.rayIntersectsTriangle( id, 0, y, z, 1., 0, 0, intersection ) )
					ts.add( intersection[ 0 ] );
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
					true );

			final TrackMate trackmate = new TrackMate( settings );
			trackmate.setNumThreads( 4 );
			trackmate.execDetection();

			final Model model = trackmate.getModel();
			final SpotCollection spots = model.getSpots();
			spots.setVisible( true );

			final ImagePlus out = NewImage.createShortImage( "OUT", imp.getWidth(), imp.getHeight(), imp.getNSlices(), NewImage.FILL_BLACK );
			out.show();

			final double[] cal = TMUtils.getSpatialCalibration( imp );
			for ( final Spot spot : model.getSpots().iterable( true ) )
			{
				final MeshIterator it = new MeshIterator( spot, cal );
				it.iterate( ( RandomAccess< T > ) ImageJFunctions.wrap( out ).randomAccess() );
				it.iterate( ( RandomAccess< T > ) ImageJFunctions.wrap( imp ).randomAccess() );
				break;
			}

			imp.show();
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
