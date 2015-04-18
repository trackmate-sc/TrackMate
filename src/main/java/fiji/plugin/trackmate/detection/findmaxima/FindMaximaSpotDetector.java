package fiji.plugin.trackmate.detection.findmaxima;

import ij.IJ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.MaximumCheck;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotDetector;

public class FindMaximaSpotDetector<T extends RealType<T> & NativeType<T>>
		implements SpotDetector<T>, MultiThreaded {

	private static final String BASE_ERROR_MESSAGE = "[FindMaximaSpotDetector] ";

	private static final double DEFAULT_RADIUS = 2.5;

	private final RandomAccessible<T> img;

	private final Interval interval;

	private final double[] calibration;

	private double threshold;

	private int numThreads;

	private long processingTime;

	private String errorMessage;

	private List<Spot> spots;

	private final double radius;
	RandomAccess<T> ra;

	/*
	 * Image Boundaries
	 */
	private int width;
	private int height;
	private int depth;

	/*
	 * CONSTRUCTORS
	 */

	public FindMaximaSpotDetector(final RandomAccessible<T> img,
			final Interval interval, final double[] calibration,
			final double threshold) {
		this.img = img;
		this.interval = DetectionUtils.squeeze(interval);
		this.calibration = calibration;
		this.threshold = threshold;
		this.radius = DEFAULT_RADIUS;
		setNumThreads();

	}

	/*
	 * METHODS
	 */

	@Override
	public boolean checkInput() {
		if (null == img) {
			errorMessage = BASE_ERROR_MESSAGE + "Image is null.";
			return false;
		}
		return true;
	}

	long T; // Tolerance
	ArrayList<MyPoint> sortedPeaks; // List of sortedPeaks (descending by
									// intensity)

	@Override
	public boolean process() {
		final long start = System.currentTimeMillis();

		/*
		 * Find maxima with plain ImgLib2 algos. Emulate the MaximaFinder
		 * plugin, with no maxima on edges.
		 */

		final ImgFactory<FloatType> factory = Util.getArrayOrCellImgFactory(
				interval, new FloatType());
		final Img<FloatType> source = DetectionUtils.copyToFloatImg(img,
				interval, factory);
		ra = img.randomAccess();
		width = (int) source.dimension(0);
		height = (int) source.dimension(1);
		if (source.numDimensions() > 1) {
			depth = (int) source.dimension(2);
		} else {
			depth = 0;
		}
		/*
		 * Find global min;
		 */
		double globalmin=getGlobalMin();
		T = (long) threshold;
		//All pixels with values smaller as the globalmin+tolerance+2 should be suppressed. Threshold plays the role of tolerance.
		final FloatType minPeakVal = new FloatType((float)(globalmin+threshold+2));
		minPeakVal.setReal(globalmin+threshold+2);

		final MaximumCheck<FloatType> check = new LocalExtrema.MaximumCheck<FloatType>(
				minPeakVal);
		final ExecutorService service = Executors
				.newFixedThreadPool(numThreads);
		final ArrayList<Point> peaksHelp = LocalExtrema.findLocalExtrema(
				source, check, service);
		
		ArrayList<MyPoint> peaks = new ArrayList<MyPoint>();
		for (Point p : peaksHelp) {
			MyPoint pTemp = null;
			if (p.numDimensions() == 1) {
				pTemp = new MyPoint(p.getLongPosition(0), 0,
						0);
			}
			else if (p.numDimensions() == 2) {
				pTemp = new MyPoint(p.getLongPosition(0), p.getLongPosition(1),
						0);
			} else if (p.numDimensions() == 3) {
				pTemp = new MyPoint(p.getLongPosition(0), p.getLongPosition(1),
						p.getLongPosition(2));
			}
			peaks.add(pTemp);
		}
		service.shutdown();

		/*
		 * Sort Peaks
		 */
		Collections.sort(peaks, Collections
				.reverseOrder(new FindMaximaSpotComparator<T>(img
						.randomAccess())));
		sortedPeaks = peaks;

		/*
		 * Analyse peaks
		 */

		
		int[][][] m = null; // Map to mark the visited positions [x][y][z]
		if (source.numDimensions() == 1) {
			m = new int[(int) source.dimension(0)][1][1];
		}
		else if (source.numDimensions() == 2) {
			m = new int[(int) source.dimension(0)][(int) source.dimension(1)][1];
		}
		else if (source.numDimensions() == 3) {
			m = new int[(int) source.dimension(0)][(int) source.dimension(1)][(int) source
					.dimension(2)];
		}
		ArrayList<Integer> removePeaks = new ArrayList<Integer>();
		for (int i = 0; i < sortedPeaks.size(); i++) {

			
			MyPoint p = sortedPeaks.get(i);
			ArrayList<Point> l = new ArrayList<Point>(); // Position fürs
															// geometrische
															// Mittel

			ra.setPosition(p);
			FloodFillRemover(p, ra.get().getRealDouble(), i,
					(int) T, l, m);
	
		
			if (l.size() > 1) {
				// Replace p with the mean position of l
				int meanX = 0;
				int meanY = 0;
				int meanZ = 0;
				for (Point lp : l) {
					meanX += lp.getIntPosition(0);
					if(img.numDimensions()>1){
						meanY += lp.getIntPosition(1);
						if (img.numDimensions() > 2) {
							meanZ += lp.getIntPosition(2);
						}
					}
				}
				meanX = meanX / l.size();
				meanY = meanY / l.size();
				meanZ = meanZ / l.size();
				MyPoint newP = new MyPoint(meanX, meanY, meanZ);
				boolean isInsideItsRegion = m[meanX][meanY][meanZ]==(i+1);
				boolean isLocalMax = isLocalMaxima(newP);
				if(isInsideItsRegion && isLocalMax){
					sortedPeaks.set(i, newP);
				}else{
					//If the new position is not a local max or it is not inside the labeled region.
					removePeaks.add(i);
				}
			}
		}
		Collections.sort(removePeaks,Collections.reverseOrder());
		for (Integer i : removePeaks) {
			sortedPeaks.remove(i);
		}

		/*
		 * Convert to spots.
		 */

		spots = new ArrayList<Spot>(sortedPeaks.size());

		if (source.numDimensions() > 2) { // 3D
			for (int i = 0; i < sortedPeaks.size(); i++) {
				final Point peak = sortedPeaks.get(i);
				ra.setPosition(peak);
				final double quality = ra.get().getRealDouble();
				final double x = peak.getDoublePosition(0) * calibration[0];
				final double y = peak.getDoublePosition(1) * calibration[1];
				final double z = peak.getDoublePosition(2) * calibration[2];
				final Spot spot = new Spot(x, y, z, 2*calibration[0], quality);
				spots.add(spot);
			}
		} else if (source.numDimensions() > 1) { // 2D
			final double z = 0;
			for (int i = 0; i < sortedPeaks.size(); i++) {
				final Point peak = sortedPeaks.get(i);
				ra.setPosition(peak);
				final double quality = ra.get().getRealDouble();
				IJ.log("xpos: " +peak.getDoublePosition(0)+ " xcal: " + calibration[0]);
				final double x = peak.getDoublePosition(0) * calibration[0];
				final double y = peak.getDoublePosition(1) * calibration[1];
				//radius.get(i)
				final Spot spot = new Spot(x, y, z, 2*calibration[0], quality);
				spots.add(spot);
			}
		} else { // 1D
			final double z = 0;
			final double y = 0;
			for (int i = 0; i < sortedPeaks.size(); i++) {
				final Point peak = sortedPeaks.get(i);
				ra.setPosition(peak);
				final double quality = ra.get().getRealDouble();
				final double x = peak.getDoublePosition(0) * calibration[0];
				final Spot spot = new Spot(x, y, z, 2*calibration[0], quality);
				spots.add(spot);
			}
		}

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;

		return true;
	}

	/**
	 * Iterative implementation of a flood fill algorithm. Builds a region
	 * around a start peak upto a intensity tolerance t. If another peak is
	 * inside the tolerance, it will be removed from the sortedPeaks ArrayList.
	 * If there are points inside the region with the same intensity as the
	 * peak, the mean position will be calculated
	 * 
	 * @param p
	 *            Peak
	 * @param peakIntensity
	 *            The intensity of the peak
	 * @param peakIndex
	 *            The index in the sortedPeaks ArrayList
	 * @param tolerance
	 *            The difference in intensity between a point and the peak have
	 *            to the smaller than the tolerance value
	 * @param l   Points with the same intensity as the peak will be added to
	 *            that list.
	 * @param m   A map where all visited pixel are marked.
	 *
	 * @return Maximum distance from the peak to a point in the region (not used at the moment)
	 */
	public double FloodFillRemover(MyPoint peak, double peakIntensity,
			int peakIndex, int tolerance, ArrayList<Point> l, int[][][] m) {
		//TODO: REMOVE PLATEAUS MAXIMA!
		Stack<MyPoint> s = new Stack<MyPoint>();
		
		s.push(peak);
		m[peak.getIntPosition(0)][peak.getIntPosition(1)][peak.getIntPosition(2)] = peakIndex+1; //mark the peak as visited with its peak index (+1, becaue 0 means unmarked)
		double maxDistance = Double.MIN_VALUE;
	
		while (!s.isEmpty()) {
			MyPoint p = s.pop();
			ra.setPosition(p);

			double diff = peakIntensity - ra.get().getRealDouble();
	
			if (diff >= 0 && diff <= tolerance) {

				if (distance(p, sortedPeaks.get(peakIndex)) > maxDistance) {
					maxDistance = distance(p, sortedPeaks.get(peakIndex));
				}
				
				if (Math.abs(diff) < 0.0001) {
					// If the intensity is equal to the peak intensity, use it
					// to calculate the mean at a later point.
					l.add(p);
					
				}

				if (sortedPeaks.contains(p)
						&& sortedPeaks.indexOf(p) > peakIndex) {
					// When the visited point is a peak, remove it from the
					// sortedPeaks list.
					sortedPeaks.remove(p);
				}

				if (img.numDimensions() > 2) { //3D
					int x = p.getIntPosition(0);
					int y = p.getIntPosition(1);
					int z = p.getIntPosition(2);
					// for each pixel in the 3d neighborhood:
					for (int dx = -1; dx <= 1; dx++) {
						for (int dy = -1; dy <= 1; dy++) {
							for (int dz = -1; dz <= 1; dz++) {
								if (dx != 0 || dy != 0 || dz != 0) {
									MyPoint pNext = new MyPoint(x + dx, y + dy,
											z + dz);
									if (isInsideBoundaries(x + dx, y + dy, z+ dz)
											&& m[pNext.getIntPosition(0)][pNext
													.getIntPosition(1)][pNext
													.getIntPosition(2)] == 0) {
										// If inside the image boundaries and the next position is unmarked
										s.push(pNext);
										m[pNext.getIntPosition(0)][pNext.getIntPosition(1)][pNext.getIntPosition(2)] = peakIndex+1; //Mark as visited
										double maxCand = distance(pNext, p) ;
										if (maxCand > maxDistance) {
											maxDistance = maxCand;
										}
									}
								}
							}
						}
					}

				} else if (img.numDimensions() > 1) { //2D

					long x = p.getIntPosition(0);
					long y = p.getIntPosition(1);

					long z = 0;
					// for each pixel in the 2d neighborhood:
					for (int dx = -1; dx <= 1; dx++) {
						for (int dy = -1; dy <= 1; dy++) {
							if (dx != 0 || dy != 0) {
								MyPoint pNext = new MyPoint(x + dx, y + dy, z);
								if (isInsideBoundaries(x + dx, y + dy, z)
										&& m[pNext.getIntPosition(0)][pNext
												.getIntPosition(1)][pNext
												.getIntPosition(2)] == 0) {
									// If inside the image boundaries and the next position is unmarked
									s.push(pNext);
									m[pNext.getIntPosition(0)][pNext.getIntPosition(1)][pNext.getIntPosition(2)] = peakIndex+1; //Mark as visited
									double maxCand = distance(pNext, p) ;
									if (maxCand > maxDistance) {
										maxDistance = maxCand;
									}
								}
							}
						}
					}
				}
				else if (img.numDimensions() > 0) { //1D

					long x = p.getIntPosition(0);
					long y = 0;

					long z = 0;
					// for each pixel in the 1d neighborhood:
					for (int dx = -1; dx <= 1; dx++) {
							if (dx != 0) {
								MyPoint pNext = new MyPoint(x + dx, y, z);
								if (isInsideBoundaries(x + dx, y, z)
										&& m[pNext.getIntPosition(0)][pNext
												.getIntPosition(1)][pNext
												.getIntPosition(2)] == 0) {
									// If inside the image boundaries and the next position is unmarked
									s.push(pNext);
									m[pNext.getIntPosition(0)][pNext.getIntPosition(1)][pNext.getIntPosition(2)] = peakIndex+1; //Mark as visited
									double maxCand = distance(pNext, p) ;
									if (maxCand > maxDistance) {
										maxDistance = maxCand;
									}
								}
							}
					}
				}
			}
		}
		return maxDistance;
	}
	
	private double getGlobalMin(){
		double min = Double.MAX_VALUE;
		if(ra.numDimensions()>2){
			for(int x = 0; x < width; x++){
				for(int y = 0; y < height; y++){
					for(int z = 0; z < depth; z++){
						ra.setPosition(new Point(x,y,z));
						double v = ra.get().getRealDouble();
						if(v<min){
							min = v;
						}
					}
				}
			}
		}
		else if(ra.numDimensions()>1){ //2D;
			for(int x = 0; x < width; x++){
				for(int y = 0; y < height; y++){
					ra.setPosition(new Point(x,y,0));
					double v = ra.get().getRealDouble();
					if(v<min){
						min = v;
					}
				}
			}
		}
		else if(ra.numDimensions()>0){ //1D;
			for(int x = 0; x < width; x++){
				ra.setPosition(new Point(x,0,0));
				double v = ra.get().getRealDouble();
				if(v<min){
					min = v;
				}
			}
		}
		return min;
	}
	
	/**
	 * Checks if the point p is a local maximum. All pixels in local neighborhood have to be smaller or equal to p.
	 */
	private boolean isLocalMaxima(MyPoint p){

		ra.setPosition(p);
		double v = ra.get().getRealDouble();
		if (img.numDimensions() > 2) {
			int x = p.getIntPosition(0);
			int y = p.getIntPosition(1);
			int z = p.getIntPosition(2);
			// for each pixel in the 3d neighborhood:
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if ( (dx != 0 || dy != 0 || dz != 0) && isInsideBoundaries(x+dx, y+dy, z+dz) ) {
							MyPoint pNext = new MyPoint(x + dx, y + dy,
									z + dz);
							ra.setPosition(pNext);
							double v1 = ra.get().getRealDouble();
							if(v1>v){
								return false;
							}
						}
					}
				}
			}
		}
		else if (img.numDimensions() > 1) {
			int x = p.getIntPosition(0);
			int y = p.getIntPosition(1);
			int z = 0;
			// for each pixel in the 3d neighborhood:
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if ((dx != 0 || dy != 0) && isInsideBoundaries(x+dx, y+dy, z)) {
						MyPoint pNext = new MyPoint(x + dx, y + dy,
								z);
						
						ra.setPosition(pNext);
						double v1 = ra.get().getRealDouble();
						if(v1>v){
							return false;
						}
					}
				}
			}
		}
		else if (img.numDimensions() > 0) {
			int x = p.getIntPosition(0);
			int y = 0;
			int z = 0;
			// for each pixel in the 3d neighborhood:
			for (int dx = -1; dx <= 1; dx++) {
					if (dx != 0 && isInsideBoundaries(x+dx, y, z)) {
						MyPoint pNext = new MyPoint(x + dx, y,
								z);
						ra.setPosition(pNext);
						double v1 = ra.get().getRealDouble();
						if(v1>v){
							return false;
						}
					}
			}
		}
		
		
		return true;
	}
	/**
	 * 
	 * @return distance between point p und q
	 */
	private double distance(MyPoint p, MyPoint q) {
		double dist = 0;
		for (int i = 0; i < p.numDimensions(); i++) {
			long d = p.getLongPosition(i) - q.getLongPosition(i);
			dist += d * d;
		}
		dist = Math.sqrt(dist);
		return dist;
	}

	/**
	 * Checks if the the coordinates [x y z] are inside the the image boundaries.
	 */
	private boolean isInsideBoundaries(long x, long y, long z) {
		boolean isInside = true;
		if (x < 0 | y < 0 | z < 0) {
			isInside = false;
		}
		if (x >= width | y >= height | z >= depth) {
			isInside = false;
		}
		return isInside;
	}

	@Override
	public List<Spot> getResult() {
		return spots;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public long getProcessingTime() {
		return processingTime;
	}

	@Override
	public void setNumThreads() {
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads(final int numThreads) {
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
	}

}
