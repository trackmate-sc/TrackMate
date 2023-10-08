package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.mesh.Mesh;
import net.imglib2.mesh.MeshStats;
import net.imglib2.mesh.Meshes;
import net.imglib2.mesh.alg.MeshConnectedComponents;
import net.imglib2.mesh.impl.nio.BufferMesh;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Utility classes to create 3D {@link fiji.plugin.trackmate.SpotMesh}es from
 * single time-point, single channel images.
 * 
 * @author Jean-Yves Tinevez, 2023
 */
public class SpotMeshUtils
{

	/** Number of triangles below which not to simplify a mesh. */
	private static final int MIN_N_TRIANGLES = 100;

	/** Quadratic mesh decimation aggressiveness for simplification. */
	private static final float SIMPLIFY_AGGRESSIVENESS = 10f;

	/** Minimal volume, in pixels, below which we discard meshes. */
	private static final double MIN_MESH_PIXEL_VOLUME = 15.;

	/**
	 * Precision for the vertex duplicate removal step. A value of 2 means that
	 * the vertices with coordinates (in pixel units) equal up to the second
	 * decimal will be considered duplicates and merged.
	 */
	private static final int VERTEX_DUPLICATE_REMOVAL_PRECISION = 2;

	/**
	 * Creates spots <b>with meshes</b> from a <b>3D</b> grayscale image. The
	 * quality value is read from a secondary image, by taking the max value in
	 * each object, or the volume if the quality image is <code>null</code>.
	 * <p>
	 * The grayscale marching-cube algorithm is used to create one big mesh from
	 * the source image. It is then split in connected-components to create
	 * single spot objects. However, to deal with possible holes in objects,
	 * meshes are possibly re-merged based on full inclusion of their bounding
	 * box. For instance, a hollow sphere would be represented by two
	 * connected-components, yielding two meshes. But because the small one is
	 * included in the big one, they are merged in this method.
	 *
	 * @param <T>
	 *            the type of the source image. Must be real, scalar.
	 * @param <S>
	 *            the type of the quality image. Must be real, scalar.
	 * @param input
	 *            the source image, must be zero-min and 3D.
	 * @param interval
	 *            the interval in which to segment spots.
	 * @param calibration
	 *            the physical calibration.
	 * @param threshold
	 *            the threshold to apply to the input image.
	 * @param simplify
	 *            if <code>true</code> the meshes will be post-processed to be
	 *            smoother and contain less points.
	 * @param qualityImage
	 *            the image in which to read the quality value.
	 * @return a list of spots, with meshes.
	 */
	public static < T extends RealType< T >, S extends RealType< S > > List< Spot > from3DThresholdWithROI(
			final RandomAccessible< T > input,
			final Interval interval,
			final double[] calibration,
			final double threshold,
			final boolean simplify,
			final RandomAccessibleInterval< S > qualityImage )
	{
		if ( input.numDimensions() != 3 )
			throw new IllegalArgumentException( "Can only process 3D images with this method, but got " + input.numDimensions() + "D." );

		// Crop.
		final RandomAccessibleInterval< T > crop = Views.interval( input, interval );
		final RandomAccessibleInterval< T > in = Views.zeroMin( crop );

		// Get big mesh.
		final Mesh mc = Meshes.marchingCubes( in, threshold );
		final Mesh bigMesh = Meshes.removeDuplicateVertices( mc, VERTEX_DUPLICATE_REMOVAL_PRECISION );

		// Split into connected components.
		final List< Mesh > meshes = new ArrayList<>();
		final List< RealInterval > boundingBoxes = new ArrayList<>();
		for ( final BufferMesh m : MeshConnectedComponents.iterable( bigMesh ) )
		{
			meshes.add( m );
			boundingBoxes.add( Meshes.boundingBox( m ) );
		}

		// Merge if bb is included in one another.
		final List< Mesh > out = new ArrayList<>();
		MESH_I: for ( int i = 0; i < meshes.size(); i++ )
		{
			final RealInterval bbi = boundingBoxes.get( i );
			final Mesh meshi = meshes.get( i );

			/*
			 * FIXME revise this. Improper and incorrect.
			 */

			// Can we put it inside another?
			for ( int j = i + 1; j < meshes.size(); j++ )
			{
				final RealInterval bbj = boundingBoxes.get( j );
				if ( Intervals.contains( bbj, bbi ) )
				{
					final Mesh meshj = meshes.get( j );

					// Merge the ith into the jth.
					final Mesh merged = Meshes.merge( Arrays.asList( meshi, meshj ) );
					meshes.set( j, merged );
					continue MESH_I;
				}
			}

			// We could not, retain it for later.
			out.add( meshi );
		}

		// Create spot from merged meshes.
		final List< Spot > spots = new ArrayList<>( out.size() );
		final double[] origin = interval.minAsDoubleArray();
		for ( final Mesh mesh : out )
		{
			final SpotMesh spot = meshToSpotMesh(
					mesh,
					simplify,
					calibration,
					qualityImage,
					origin );
			if ( spot != null )
				spots.add( spot );
		}
		return spots;
	}

	/**
	 * Creates spots <b>with meshes</b> from a <b>3D</b> label image. The
	 * quality value is read from a secondary image, by taking the max value in
	 * each ROI.
	 *
	 * @param <R>
	 *            the type that backs-up the labeling.
	 * @param <S>
	 *            the type of the quality image. Must be real, scalar.
	 * @param labeling
	 *            the labeling, must be zero-min and 3D.
	 * @param interval
	 *            the interval, used to reposition the spots from the zero-min
	 *            labeling to the proper coordinates.
	 * @param calibration
	 *            the physical calibration.
	 * @param simplify
	 *            if <code>true</code> the meshes will be post-processed to be
	 *            smoother and contain less points.
	 * @param qualityImage
	 *            the image in which to read the quality value.
	 * @return a list of spots, with meshes.
	 */
	public static < R extends IntegerType< R >, S extends RealType< S > > List< Spot > from3DLabelingWithROI(
			final ImgLabeling< Integer, R > labeling,
			final Interval interval,
			final double[] calibration,
			final boolean simplify,
			final RandomAccessibleInterval< S > qualityImage )
	{
		if ( labeling.numDimensions() != 3 )
			throw new IllegalArgumentException( "Can only process 3D images with this method, but got " + labeling.numDimensions() + "D." );

		// Parse regions to create meshes on label.
		final LabelRegions< Integer > regions = new LabelRegions< Integer >( labeling );
		final Iterator< LabelRegion< Integer > > iterator = regions.iterator();
		final List< Spot > spots = new ArrayList<>( regions.getExistingLabels().size() );
		while ( iterator.hasNext() )
		{
			final LabelRegion< Integer > region = iterator.next();
			final Spot spot = regionToSpotMesh(
					region,
					simplify,
					calibration,
					qualityImage,
					interval.minAsDoubleArray() );
			if ( spot == null )
				continue;

			spots.add( spot );
		}
		return spots;
	}

	/**
	 * Returns a new {@link Spot} with a {@link SpotMesh} as shape, built from
	 * the specified bit-mask.
	 *
	 * @param <S>
	 *            the type of pixels in the quality image.
	 * @param region
	 *            the bit-mask to build the mesh from.
	 * @param simplify
	 *            if <code>true</code> the mesh will be simplified.
	 * @param calibration
	 *            the pixel size array, used to scale the mesh to physical
	 *            coordinates.
	 * @param qualityImage
	 *            an image from which to read the quality value. If not
	 *            <code>null</code>, the quality of the spot will be the max
	 *            value of this image inside the mesh. If <code>null</code>, the
	 *            quality will be the mesh volume.
	 * @param minInterval
	 *            the origin in image coordinates of the ROI used for detection.
	 *
	 * @return a new spot.
	 */
	private static < S extends RealType< S > > Spot regionToSpotMesh(
			final RandomAccessibleInterval< BoolType > region,
			final boolean simplify,
			final double[] calibration,
			final RandomAccessibleInterval< S > qualityImage,
			final double[] minInterval )
	{
		// To mesh.
		final IntervalView< BoolType > box = Views.zeroMin( region );
		final Mesh mesh = Meshes.marchingCubes( box, 0.5 );
		final Mesh cleaned = Meshes.removeDuplicateVertices( mesh, VERTEX_DUPLICATE_REMOVAL_PRECISION );
		// Shift coords.
		final double[] origin = region.minAsDoubleArray();
		for ( int d = 0; d < 3; d++ )
			origin[ d ] += minInterval[ d ];
		// To spot.
		return meshToSpotMesh(
				cleaned,
				simplify,
				calibration,
				qualityImage,
				origin );
	}

	/**
	 * Creates a {@link SpotMesh} from a {@link Mesh}.
	 * 
	 * @param <S>
	 *            the type of the quality image.
	 * @param mesh
	 *            the mesh to create a spot from.
	 * @param simplify
	 *            whether the simplify the mesh.
	 * @param calibration
	 *            the pixel size array, to map pixel coords to physical coords.
	 * @param qualityImage
	 *            the quality image. If not <code>null</code> the quality he
	 *            quality of the spot will be the max value of this image inside
	 *            the mesh. If <code>null</code>, the quality will be the mesh
	 *            volume.
	 * @param origin
	 *            the origin of the interval the mesh was created on. This is
	 *            used to put back the mesh coordinates with respect to the
	 *            initial source image (same referential that for the quality
	 *            image).
	 * @return
	 */
	public static < S extends RealType< S > > SpotMesh meshToSpotMesh(
			final Mesh mesh,
			final boolean simplify,
			final double[] calibration,
			final RandomAccessibleInterval< S > qualityImage,
			final double[] origin )
	{
		final Mesh simplified;
		if ( simplify )
		{
			// Dont't go below a certain number of triangles.
			final int nTriangles = mesh.triangles().size();
			if ( nTriangles < MIN_N_TRIANGLES )
			{
				simplified = mesh;
			}
			else
			{
				// Crude heuristics.
				final float targetRatio;
				if ( nTriangles < 2 * MIN_N_TRIANGLES )
					targetRatio = 0.5f;
				else if ( nTriangles < 10_000 )
					targetRatio = 0.2f;
				else if ( nTriangles < 1_000_000 )
					targetRatio = 0.1f;
				else
					targetRatio = 0.05f;
				simplified = Meshes.simplify( mesh, targetRatio, SIMPLIFY_AGGRESSIVENESS );
			}
		}
		else
		{
			simplified = mesh;
		}
		// Remove meshes that are too small
		final double volumeThreshold = MIN_MESH_PIXEL_VOLUME * calibration[ 0 ] * calibration[ 1 ] * calibration[ 2 ];
		if ( MeshStats.volume( simplified ) < volumeThreshold )
			return null;

		// Translate back to interval coords & scale to physical coords.
		Meshes.translateScale( simplified, calibration, origin );

		// Make spot with default quality.
		final SpotMesh spot = new SpotMesh( simplified, 0. );

		// Measure quality.
		final double quality;
		if ( null == qualityImage )
		{
			quality = MeshStats.volume( simplified );
		}
		else
		{
			final IterableInterval< S > iterable = spot.iterable( qualityImage, calibration );
			double max = Double.NEGATIVE_INFINITY;
			for ( final S s : iterable )
			{
				final double val = s.getRealDouble();
				if ( val > max )
					max = val;
			}
			quality = max;
		}
		spot.putFeature( Spot.QUALITY, Double.valueOf( quality ) );
		return spot;
	}
}
