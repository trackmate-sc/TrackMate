package fiji.plugin.trackmate.tracking.overlap;

import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.BASE_ERROR_MESSAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.scijava.Cancelable;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.tracking.SpotTracker;
import math.geom2d.AffineTransform2D;
import math.geom2d.Point2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Polygons2D;
import math.geom2d.polygon.Rectangle2D;
import math.geom2d.polygon.SimplePolygon2D;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;

public class OverlapTracker extends MultiThreadedBenchmarkAlgorithm implements SpotTracker, Cancelable
{

	public static enum IoUCalculation
	{
		FAST( "Fast", "IoU is calculated using the bounding box of the spot." ),
		PRECISE( "Precise", "IoU is calculated over the shape of the spot ROI." );

		private final String str;

		private final String infoText;

		private IoUCalculation( final String str, final String infoText )
		{
			this.str = str;
			this.infoText = infoText;
		}

		public String getInfoText()
		{
			return infoText;
		}

		@Override
		public String toString()
		{
			return str;
		}
	}

	private SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	private Logger logger = Logger.VOID_LOGGER;

	private final SpotCollection spots;

	private final double enlargeFactor;

	private final IoUCalculation method;

	private final double minIoU;

	private boolean isCanceled;

	private String cancelReason;

	/*
	 * CONSTRUCTOR
	 */

	public OverlapTracker( final SpotCollection spots, final IoUCalculation method, final double minIoU, final double enlargeFactor )
	{
		this.spots = spots;
		this.method = method;
		this.minIoU = minIoU;
		this.enlargeFactor = enlargeFactor;
	}

	/*
	 * METHODS
	 */

	@Override
	public SimpleWeightedGraph< Spot, DefaultWeightedEdge > getResult()
	{
		return graph;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		isCanceled = false;
		cancelReason = null;

		/*
		 * Check input now.
		 */

		// Check that the objects list itself isn't null
		if ( null == spots )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is null.";
			return false;
		}

		// Check that the objects list contains inner collections.
		if ( spots.keySet().isEmpty() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is empty.";
			return false;
		}

		if ( enlargeFactor <= 0 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The enlargement factor must be strictly positive, was " + enlargeFactor;
			return false;
		}

		// Check that at least one inner collection contains an object.
		boolean empty = true;
		for ( final int frame : spots.keySet() )
		{
			if ( spots.getNSpots( frame, true ) > 0 )
			{
				empty = false;
				break;
			}
		}
		if ( empty )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is empty.";
			return false;
		}

		/*
		 * Process.
		 */

		final long start = System.currentTimeMillis();

		// Instantiate graph
		graph = new SimpleWeightedGraph<>( DefaultWeightedEdge.class );

		// Flag if we are doing ok.
		final AtomicBoolean ok = new AtomicBoolean( true );

		// Prepare frame pairs in order, not necessarily separated by 1.
		final Iterator< Integer > frameIterator = spots.keySet().iterator();

		// First frame.
		final int sourceFrame = frameIterator.next();
		Map< Spot, Polygon2D > sourceGeometries = createGeometry( spots.iterable( sourceFrame, true ), method, enlargeFactor );

		logger.setStatus( "Frame to frame linking..." );
		int progress = 0;
		while ( frameIterator.hasNext() )
		{
			if ( !ok.get() || isCanceled() )
				break;

			final int targetFrame = frameIterator.next();
			final Map< Spot, Polygon2D > targetGeometries = createGeometry( spots.iterable( targetFrame, true ), method, enlargeFactor );

			if ( sourceGeometries.isEmpty() || targetGeometries.isEmpty() )
				continue;

			final ExecutorService executors = Executors.newFixedThreadPool( numThreads );
			final List< Future< IoULink > > futures = new ArrayList<>();

			// Submit work.
			for ( final Spot target : targetGeometries.keySet() )
			{
				final Polygon2D targetPoly = targetGeometries.get( target );
				futures.add( executors.submit( new FindBestSourceTask( target, targetPoly, sourceGeometries, minIoU ) ) );
			}

			// Get results.
			for ( final Future< IoULink > future : futures )
			{
				if ( !ok.get() || isCanceled() )
					break;

				try
				{
					final IoULink link = future.get();
					if ( link.source == null )
						continue;

					graph.addVertex( link.source );
					graph.addVertex( link.target );
					final DefaultWeightedEdge edge = graph.addEdge( link.source, link.target );
					graph.setEdgeWeight( edge, 1. - link.iou );

				}
				catch ( InterruptedException | ExecutionException e )
				{
					errorMessage = e.getMessage();
					ok.set( false );
				}
			}
			executors.shutdown();

			sourceGeometries = targetGeometries;
			logger.setProgress( ( double ) progress++ / spots.keySet().size() );
		}

		logger.setProgress( 1d );
		logger.setStatus( "" );

		final long end = System.currentTimeMillis();
		processingTime = end - start;

		return ok.get();
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	protected boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		final boolean ok = true;
		return ok;
	}

	private static Map< Spot, Polygon2D > createGeometry( final Iterable< Spot > spots, final IoUCalculation method, final double scale )
	{
		final Map< Spot, Polygon2D > geometries = new HashMap<>();
		switch ( method )
		{
		case FAST:
			for ( final Spot spot : spots )
				geometries.put( spot, toBoundingBox( spot, scale ) );
			break;

		case PRECISE:
			for ( final Spot spot : spots )
				geometries.put( spot, toPolygon( spot, scale ) );
			break;

		default:
			throw new IllegalArgumentException( "Do not know how to compute IoU for method " + method );
		}
		return Collections.unmodifiableMap( geometries );
	}

	private static SimplePolygon2D toPolygon( final Spot spot, final double scale )
	{
		final double xc = spot.getDoublePosition( 0 );
		final double yc = spot.getDoublePosition( 1 );
		final SpotRoi roi = spot.getRoi();
		final SimplePolygon2D poly;
		if ( roi == null )
		{
			final double radius = spot.getFeature( Spot.RADIUS ).doubleValue();
			poly = new SimplePolygon2D( new Circle2D( xc, yc, radius ).asPolyline( 32 ) );
		}
		else
		{
			final double[] xcoords = roi.toPolygonX( 1., 0., xc, 1. );
			final double[] ycoords = roi.toPolygonY( 1., 0., yc, 1. );
			poly =  new SimplePolygon2D( xcoords, ycoords );
		}
		return poly.transform( AffineTransform2D.createScaling( new Point2D( xc, yc ), scale, scale ) );
	}

	private static Rectangle2D toBoundingBox( final Spot spot, final double scale )
	{
		final double xc = spot.getDoublePosition( 0 );
		final double yc = spot.getDoublePosition( 1 );
		final SpotRoi roi = spot.getRoi();
		if ( roi == null )
		{
			final double radius = spot.getFeature( Spot.RADIUS ).doubleValue() * scale;
			return new Rectangle2D( xc - radius, yc - radius, 2 * radius, 2 * radius );
		}
		else
		{
			final double minX = Arrays.stream( roi.x ).min().getAsDouble() * scale;
			final double maxX = Arrays.stream( roi.x ).max().getAsDouble() * scale;
			final double minY = Arrays.stream( roi.y ).min().getAsDouble() * scale;
			final double maxY = Arrays.stream( roi.y ).max().getAsDouble() * scale;
			return new Rectangle2D( xc + minX, yc + minY, maxX - minX, maxY - minY );
		}
	}

	private static final class FindBestSourceTask implements Callable< IoULink >
	{

		private final Spot target;

		private final Polygon2D targetPoly;

		private final Map< Spot, Polygon2D > sourceGeometries;

		private final double minIoU;


		public FindBestSourceTask( final Spot target, final Polygon2D targetPoly, final Map< Spot, Polygon2D > sourceGeometries, final double minIoU )
		{
			this.target = target;
			this.targetPoly = targetPoly;
			this.sourceGeometries = sourceGeometries;
			this.minIoU = minIoU;
		}

		@Override
		public IoULink call() throws Exception
		{
			final double targetArea = Math.abs( targetPoly.area() );
			double maxIoU = minIoU;
			Spot bestSpot = null;
			for ( final Spot spot : sourceGeometries.keySet() )
			{
				final Polygon2D sourcePoly = sourceGeometries.get( spot );
				final double intersection = Math.abs( Polygons2D.intersection( targetPoly, sourcePoly ).area() );
				if ( intersection == 0. )
					continue;

				final double union = Math.abs( sourcePoly.area() ) + targetArea - intersection;
				final double iou = intersection / union;
				if ( iou > maxIoU )
				{
					maxIoU = iou;
					bestSpot = spot;
				}
			}
			return new IoULink( bestSpot, target, maxIoU );
		}
	}

	private static final class IoULink
	{
		public final Spot source;

		public final Spot target;

		public final double iou;

		public IoULink( final Spot source, final Spot target, final double iou )
		{
			this.source = source;
			this.target = target;
			this.iou = iou;
		}
	}

	// --- org.scijava.Cancelable methods ---

	@Override
	public boolean isCanceled()
	{
		return isCanceled;
	}

	@Override
	public void cancel( final String reason )
	{
		isCanceled = true;
		cancelReason = reason;
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}
}
