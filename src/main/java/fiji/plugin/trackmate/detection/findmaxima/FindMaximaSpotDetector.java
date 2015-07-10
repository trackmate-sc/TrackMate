package fiji.plugin.trackmate.detection.findmaxima;

import ij.IJ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.scijava.Context;
import org.scijava.plugin.Parameter;

import net.imagej.ops.OpService;
import net.imagej.ops.convolve.*;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.localextrema.LocalExtrema;
import net.imglib2.algorithm.localextrema.LocalExtrema.MaximumCheck;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotDetector;
import net.imagej.ImageJ;

public class FindMaximaSpotDetector<T extends RealType<T> & NativeType<T>>
		implements SpotDetector<T>, MultiThreaded {

	@Parameter
    private OpService ops;
    
	private static final String BASE_ERROR_MESSAGE = "[FindMaximaSpotDetector] ";

	private static final double DEFAULT_RADIUS = 2.5;

	private final RandomAccessible<T> img;

	private final Interval interval;

	private final double[] calibration;

	private double tolerance;

	private int numThreads;

	private long processingTime;

	private String errorMessage;

	private List<Spot> spots;
	private int[][][] m; //Map for labeling visited pixels
	
	

	private final double radius;
	RandomAccess<T> sourceRa;

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
		this.tolerance = threshold;
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
	
	public Img<FloatType> createSecondDerivativeImage(final Img<FloatType> source){
		int[] kernelSize = new int[] {3,3};
		Img<FloatType> kernel = new ArrayImgFactory<FloatType>().create(kernelSize, new FloatType());
		RandomAccess<FloatType> kernelRa = kernel.randomAccess();
		//long[] borderSize = new long[] {1, 1};
		//int[] k = new int[]{1, -2, 1,2, -4, 2,1, -2, 1}; //Second derivative filter
		int[] k = new int[]{0, 1, 0,1, -4, 1,0, 1, 0}; //Laplace
		for(int i = 0; i < 3; i++){
			for(int j = 0; j < 3; j++){
				kernelRa.setPosition(new Point(i,j));
				kernelRa.get().set(k[i*3+j]);
			}
		}
	    final Context context = (Context) IJ.runPlugIn("org.scijava.Context", "");
		final ImageJ ij = new ImageJ(context);

		Img<FloatType> out = new ArrayImgFactory<FloatType>().create(source,new FloatType());
		OutOfBoundsFactory<FloatType, RandomAccessibleInterval<FloatType>> obf = new OutOfBoundsConstantValueFactory<FloatType, RandomAccessibleInterval<FloatType>>(Util.getTypeFromInterval(source).createVariable());
		// extend the input
		RandomAccessibleInterval<FloatType> extendedIn = Views.interval(Views.extend(source, obf), source);
		// extend the output
		RandomAccessibleInterval<FloatType> extendedOut = Views.interval(Views.extend(out, obf), out);
		// convolve
		ij.op().run(ConvolveNaive.class, extendedOut, extendedIn, kernel);
		// show the image in a window
		out = DetectionUtils.applyMedianFilter(out);
		//ij.ui().show("convolved", out);
		return out;
	}
	
	/*
	 * Find Radius Methods:
	 * - getRadius
	 * - isLocalMax
	 */
	private int getRadius(Point p, Img<FloatType> secondDerivative)
	{
		int maxrad=25;
		/*
		 * Find the first maxima along the x-direction in second derivative image
		 */
		RandomAccess<FloatType> ra = secondDerivative.randomAccess();
		int startx = p.getIntPosition(0);
		int starty = p.getIntPosition(1);
		int rad=2;
		for(int i = startx; i < startx+maxrad;i++){
			double[] v = new double[3];
			int[] pos = {i-1,starty};
			ra.setPosition(pos);
			v[0] = ra.get().getRealDouble();
			pos = new int[]{i,starty};
			ra.setPosition(pos);
			v[1] = ra.get().getRealDouble();
			pos = new int[]{i+1,starty};
			ra.setPosition(pos);
			v[2] = ra.get().getRealDouble();
			if(isLocalMax(v) ){ 
				//LOCAL MAX!
				rad = i - startx;
				break;
			}
			else if(v[0]<v[1] && v[1] ==v[2]){ //Probably local max with plateau  /***\
			  	int xoff = 2;
			  	pos = new int[]{i+xoff,starty};
				ra.setPosition(pos);
			  	double value = ra.get().getRealDouble();
			  	while(value==v[1] && (i+xoff-startx)<maxrad){
			  		xoff++;
			  		pos = new int[]{i+xoff,starty};
			  		ra.setPosition(pos);
				 	value = ra.get().getRealDouble();
			  	}
			  	if(value<v[1]){ //Local max with plateau!!
			  		rad = i - startx;
			  		break;
			  	}
			  	 
			}
			
			
		}
		
		
		/*
		 * Size of the plateau (0 when no plateau)
		 */
		int offset =0;
		sourceRa.setPosition(new Point(startx,starty));
		double v0=sourceRa.get().getRealDouble();
		for(int x = startx+1; x < startx+rad; x++){
			sourceRa.setPosition(new Point(x,starty));
			if(v0== sourceRa.get().getRealDouble()){
				offset++;
			}else{
				break;
			}
		}
		
		/*
		 * Summed up intensity of the profile and minimum along the profile
		 */
		double sum=0;
		double min =Double.MAX_VALUE;
		for(int x = startx+offset; x < startx+rad; x++){
			sourceRa.setPosition(new Point(x,starty));
			double v = sourceRa.get().getRealDouble();
			if(v<min){
				min = v;
			}
			sum += v;
		}
		sum = sum - (rad-offset)*min;
		
		/*
		 * 96% Threshold
		 */
		double partSum = 0;
		for(int x = startx+offset; x < startx+rad; x++){
			sourceRa.setPosition(new Point(x,starty));
			
			double v = sourceRa.get().getRealDouble()-min;
			partSum += v;
			if(partSum/sum>0.99){
				rad=x-startx;
				break;
			}
			
		}
		if(rad<DEFAULT_RADIUS){
			return (int) DEFAULT_RADIUS;
		}
		return rad;
	}
	
	private boolean isLocalMax(double[] v){
		return (v[1]>v[0] && v[1]>v[2]);
	}

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
		sourceRa = img.randomAccess();
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
		//All pixels with values smaller as the globalmin+tolerance+2 should be suppressed. Threshold plays the role of tolerance.
		final FloatType minPeakVal = new FloatType((float)(globalmin+tolerance+2));
		minPeakVal.setReal(globalmin+tolerance+2);

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
			ArrayList<Point> l = new ArrayList<Point>();  //Positions with same intensity as the peak

			sourceRa.setPosition(p);
			boolean remove = FloodFillRemover(p, sourceRa.get().getRealDouble(), i,
					(int) tolerance, l);
			
			if(remove){
				sortedPeaks.remove(i);
				i--;
			}
			else if (l.size() > 1) {
				//Calculate the geometric center
				double meanX = 0;
				double meanY = 0;
				double meanZ = 0;
				for (Point lp : l) {
					meanX += lp.getDoublePosition(0);
					if(img.numDimensions()>1){
						meanY += lp.getDoublePosition(1);
						if (img.numDimensions() > 2) {
							meanZ += lp.getDoublePosition(2);
						}
					}
				}
				meanX = meanX / l.size();
				meanY = meanY / l.size();
				meanZ = meanZ / l.size();
				double[] newPDouble = {meanX,meanY,meanZ};
				double minDistance = Double.MAX_VALUE;
				MyPoint minPoint = null;
				for(Point lp : l){
					double distance = distance(newPDouble, lp);
					if(distance < minDistance){
						minDistance = distance;
						minPoint = new MyPoint(lp);
					}
				}
				sortedPeaks.set(i, minPoint);
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
		 Img<FloatType> secondDerivative = createSecondDerivativeImage(source);
		if (source.numDimensions() > 2) { // 3D
			for (int i = 0; i < sortedPeaks.size(); i++) {
				final Point peak = sortedPeaks.get(i);
				sourceRa.setPosition(peak);
				final double quality = sourceRa.get().getRealDouble();
				final double x = peak.getDoublePosition(0) * calibration[0];
				final double y = peak.getDoublePosition(1) * calibration[1];
				final double z = peak.getDoublePosition(2) * calibration[2];
				double radius = 2; //getRadius(peak, secondDerivative)*calibration[0]
				final Spot spot = new Spot(x, y, z,radius, quality);
				spots.add(spot);
			}
		} else if (source.numDimensions() > 1) { // 2D
			final double z = 0;
			for (int i = 0; i < sortedPeaks.size(); i++) {
				final Point peak = sortedPeaks.get(i);
				sourceRa.setPosition(peak);
				final double quality = sourceRa.get().getRealDouble();
				final double x = peak.getDoublePosition(0) * calibration[0];
				final double y = peak.getDoublePosition(1) * calibration[1];
				//radius.get(i)
				double radius = 2; //getRadius(peak, secondDerivative)*calibration[0]
				final Spot spot = new Spot(x, y, z, radius, quality);
				spots.add(spot);
				
				
			}
		} else { // 1D
			final double z = 0;
			final double y = 0;
			for (int i = 0; i < sortedPeaks.size(); i++) {
				final Point peak = sortedPeaks.get(i);
				sourceRa.setPosition(peak);
				final double quality = sourceRa.get().getRealDouble();
				final double x = peak.getDoublePosition(0) * calibration[0];
				double radius = 2; //getRadius(peak, secondDerivative)*calibration[0]
				final Spot spot = new Spot(x, y, z, 2, quality);
				spots.add(spot);
			}
		}

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;

		return true;
	}
	
	private double distance(double[] p, Point q){
		double sum = 0;
		for(int i = 0; i < p.length; i++){
			sum += Math.pow(p[i]-q.getDoublePosition(i), 2);
		}
		sum = Math.sqrt(sum);
		return sum;
	}

	/**
	 * Iterative implementation of a flood fill algorithm. Builds a region
	 * around a start peak upto a intensity tolerance t. If the region touches a region of
	 * a another maxima, it will be discarded.
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
	 *
	 * @return If the peak should be discarded
	 */
	private boolean FloodFillRemover(MyPoint peak, double peakIntensity,
			int peakIndex, int tolerance, ArrayList<Point> l) {
		
		Stack<MyPoint> s = new Stack<MyPoint>();
		boolean remove = false;
		m[peak.getIntPosition(0)][peak.getIntPosition(1)][peak.getIntPosition(2)] = peakIndex+1;
		s.push(peak);
	
		while (!s.isEmpty()) {
			MyPoint p = s.pop();
			sourceRa.setPosition(p);
			double diff = peakIntensity - sourceRa.get().getRealDouble();
	
			if (diff >= 0 && diff <= tolerance) {
				
				if (Math.abs(diff) < 0.0001) {
					// If the intensity is equal to the peak intensity, use it
					// to calculate the mean at a later point.
					l.add(p);
					
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
									if (isInsideBoundaries(x + dx, y + dy, z+ dz)){
											// If inside the image boundaries
											remove = remove?true:checkRemoveable(pNext,peakIndex,peakIntensity);
											boolean notVisitedBefore = m[pNext.getIntPosition(0)][pNext.getIntPosition(1)][pNext.getIntPosition(2)] ==0;
											if(notVisitedBefore){
												s.push(pNext);
												m[pNext.getIntPosition(0)][pNext.getIntPosition(1)][pNext.getIntPosition(2)] = peakIndex+1; //Mark as visited
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
								
					
								if (isInsideBoundaries(x + dx, y + dy, z)) {
									// If inside the image boundaries
									remove = remove?true:checkRemoveable(pNext,peakIndex,peakIntensity);
									boolean notVisitedBefore = m[pNext.getIntPosition(0)][pNext.getIntPosition(1)][pNext.getIntPosition(2)] ==0;
									if(notVisitedBefore){
										s.push(pNext);
										m[pNext.getIntPosition(0)][pNext.getIntPosition(1)][pNext.getIntPosition(2)] = peakIndex+1; //Mark as visited
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
								if (isInsideBoundaries(x + dx, y, z)){
										
									
										/*
										 * If once it was decided that the peak has to be removed, keep that decision!
										 */
										remove = remove?true:checkRemoveable(pNext,peakIndex,peakIntensity); 
										
										boolean notVisitedBefore = m[pNext.getIntPosition(0)][pNext.getIntPosition(1)][pNext.getIntPosition(2)] ==0;
										if(notVisitedBefore){
											s.push(pNext);
											m[pNext.getIntPosition(0)][pNext.getIntPosition(1)][pNext.getIntPosition(2)] = peakIndex+1; //Mark as visited
										}

								}
							}
					}
				}
			}
		}
		return remove;
	}
	

	private boolean checkRemoveable(Point pNext, int peakIndex, double peakIntensity){
		sourceRa.setPosition(pNext);
		double dInt = peakIntensity - sourceRa.get().getRealDouble();
		boolean insideTolerance = (dInt >= 0) && dInt<=tolerance;
		int mValue = m[pNext.getIntPosition(0)][pNext.getIntPosition(1)][pNext.getIntPosition(2)];
		boolean visitedByAnotherMaxima = (mValue!=(peakIndex+1))&&(mValue!=0);
		if(insideTolerance && visitedByAnotherMaxima){
			return true;
		}
		return false;
	}
	
	private double getGlobalMin(){
		double min = Double.MAX_VALUE;
		if(sourceRa.numDimensions()>2){
			for(int x = 0; x < width; x++){
				for(int y = 0; y < height; y++){
					for(int z = 0; z < depth; z++){
						sourceRa.setPosition(new Point(x,y,z));
						double v = sourceRa.get().getRealDouble();
						if(v<min){
							min = v;
						}
					}
				}
			}
		}
		else if(sourceRa.numDimensions()>1){ //2D;
			for(int x = 0; x < width; x++){
				for(int y = 0; y < height; y++){
					sourceRa.setPosition(new Point(x,y,0));
					double v = sourceRa.get().getRealDouble();
					if(v<min){
						min = v;
					}
				}
			}
		}
		else if(sourceRa.numDimensions()>0){ //1D;
			for(int x = 0; x < width; x++){
				sourceRa.setPosition(new Point(x,0,0));
				double v = sourceRa.get().getRealDouble();
				if(v<min){
					min = v;
				}
			}
		}
		return min;
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
