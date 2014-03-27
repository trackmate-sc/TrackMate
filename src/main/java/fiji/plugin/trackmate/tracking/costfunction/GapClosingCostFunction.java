package fiji.plugin.trackmate.tracking.costfunction;

import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.multithreading.SimpleMultiThreading;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import Jama.Matrix;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.TrackableObject;
import fiji.plugin.trackmate.tracking.trackers.LAPTracker;

/**
 * <p>Gap closing cost function used with {@link LAPTracker}.
 * 
 * <p>The <b>cost function</b> is determined by the default equation in the
 * TrackMate trackmate, see below.
 * <p>  
 *  It slightly differs from the Jaqaman article, see equation (4) in the paper.
 * <p>
 * The <b>thresholds</b> used are:
 * <ul>
 * <li>Must be within a certain number of frames.</li>
 * <li>Must be within a certain distance.</li>
 * </ul>
 * 
 * @see LAPUtils#computeLinkingCostFor(Spot, Spot, double, double, Map)
 * @author Nicholas Perry
 * @author Jean-Yves Tinevez
 */
public class GapClosingCostFunction<T extends TrackableObject> extends MultiThreadedBenchmarkAlgorithm implements OutputAlgorithm<Matrix> {

	/** If false, gap closing will be prohibited. */
	protected final boolean allowed;
	/** The distance cutoff: no gap is closed if the two spots are further than this distance. */
	protected final double maxDist;
	/** The max frame gap above which gap to close are not sought. */
	protected final int frameCutoff;
	/** The value to use to block an assignment in the cost matrix. */
	protected final double blockingValue;
	/** Feature penalties. */
	protected final Map<String, Double> featurePenalties;
	protected final List<SortedSet<T>> trackSegments;
	protected Matrix m;
	protected CostCalculator<T> costCalculator;


	@SuppressWarnings("unchecked")
	public GapClosingCostFunction(final CostCalculator<T> costCalculator, final Map<String, Object> settings, final List<SortedSet<T>> trackSegments) {
		this.frameCutoff 		= (Integer) settings.get(KEY_GAP_CLOSING_MAX_FRAME_GAP);
		this.maxDist 			= (Double) settings.get(KEY_GAP_CLOSING_MAX_DISTANCE);
		this.blockingValue		= (Double) settings.get(KEY_BLOCKING_VALUE);
		this.featurePenalties	= (Map<String, Double>) settings.get(KEY_GAP_CLOSING_FEATURE_PENALTIES);
		this.allowed 			= (Boolean) settings.get(KEY_ALLOW_GAP_CLOSING);
		this.trackSegments 	= trackSegments;
		this.costCalculator = costCalculator;
	}

	@Override
	public boolean process() {

		final long start = System.currentTimeMillis();
		final int n = trackSegments.size();

		// If we do not allow to make gap-closing, simply fill the matrix with blocking values.
		if (!allowed) {
			m = new Matrix(n, n, blockingValue);
		} else {

			m = new Matrix(n, n);

			// Prepare threads
			final Thread[] threads = SimpleMultiThreading.newThreads(numThreads);

			// Prepare the thread array
			final AtomicInteger ai = new AtomicInteger(0);
			for (int ithread = 0; ithread < threads.length; ithread++) {

				threads[ithread] = new Thread("LAPTracker gap closing cost thread "+(1+ithread)+"/"+threads.length) {  

					@Override
					public void run() {

						for (int i = ai.getAndIncrement(); i < n; i = ai.getAndIncrement()) {

							final SortedSet<T> seg1 = trackSegments.get(i);
							final T end = seg1.last();				// get last Spot of seg1
							final int endFrame = end.frame(); // we want at least tstart > tend

							// Set the gap closing scores for each segment start and end pair
							for (int j = 0; j < n; j++) {

								// If i and j are the same track segment, block it
								if (i == j) {
									m.set(i, j, blockingValue);
									continue;
								}

								final SortedSet<T> seg2 = trackSegments.get(j);
								final T start = seg2.first();			// get first Spot of seg2
								final int startFrame = start.frame();

								// Frame cutoff. A value of 1 means a gap of 1 frame. If the end spot 
								// is in frame 10, the start spot in frame 12, and if the max gap is 1
								// then we should sought to bridge this gap (12 to 10 is a gap of 1 frame).
								if (startFrame - endFrame > (frameCutoff+1) || endFrame >= startFrame) {
									m.set(i, j, blockingValue);
									continue;
								}

								final double cost = costCalculator.computeLinkingCostFor(end, start, maxDist, blockingValue, featurePenalties);
								m.set(i, j, cost);
							}
						}
					}
				};
			}

			SimpleMultiThreading.startAndJoin(threads);
		}
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public Matrix getResult() {
		return m;
	}
}
