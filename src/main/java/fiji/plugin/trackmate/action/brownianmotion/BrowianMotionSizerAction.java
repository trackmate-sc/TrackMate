package fiji.plugin.trackmate.action.brownianmotion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultIntervalXYDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.scijava.ui.swing.widget.SpinnerNumberModelFactory;

import ij.IJ;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.util.gui.GenericDialogPlus;
/**
 * This action provides methods to estimate the size distribution of tracks. 
 * To achieve this, the diffusion coefficient is calculated using a covariance estimator [1].
 * The diffusion coefficient and the corresponding tracklength is saved in two dimensional array which represents
 * the data basis to estimate the size distribution.  There are two options to estimate the size distribution:
 * 
 * 1. Tracklength-weighted histogram: The diffusion coefficient is calculated 
 * 2. Maximum-likelihood histogram: 
 * 
 * @author Thorsten Wagner (twa@posteo.de)
 *
 */
public class BrowianMotionSizerAction implements TrackMateAction {
	
	private final SelectionModel selectionModel;
	private final Model model;
	private Logger logger;
	private double temp = 22+ 273.15; 								// [kelvin]
	private double visk=0.9548 * Math.pow(10, -5);  				// [kg cm^-2 s^-2]
	private double framesPerSecond = 30; 							// [hz]
	TrackMate trackMateInstance;
	private double nmPerPixel = 166;
	private final static double kB = 1.3806488* Math.pow(10, -19); 	// [kg cm^2 s^-2 K^-1]
	String[] methods = {"Weighted Histogram","Maximum Likelihood"};
	final int WEIGHTED_HISTOGRAM = 0;
	final int MAXIMUM_LIKELIHOOD = 1;
	int selectedMethod=0;
	private double data[][];
	
	public BrowianMotionSizerAction(final Model model, final SelectionModel selectionModel ) {
		
		this.model = model;
		this.selectionModel = selectionModel;
	}
	
	@Override
	public void execute(TrackMate trackmate) {
		this.trackMateInstance = trackmate;
		showGui();
		fillDataStructure();
		
		switch (selectedMethod) {
		case WEIGHTED_HISTOGRAM:
			{
				//Convert unit diffusion coefficient from pixel^2 / s to 10^(-10) cm^2 * s^-1
				double conversionFactor = nmPerPixel*nmPerPixel*Math.pow(10, -4); //to 10^-10 cm^2;
				double E10cm2nm = Math.pow(10, 17);
				double toDiameter = (kB*temp/(3*Math.PI*visk))*E10cm2nm;
				double[][] dataC = new double[data.length][2];
			
				int nTracks  = 0;
				int meanTrackLengh = 0;
				for(int i = 0; i < data.length; i++){
					dataC[i][0] = toDiameter/(data[i][0]*conversionFactor);
					dataC[i][1] = data[i][1];
					nTracks++;
					meanTrackLengh += data[i][1];
				}
				meanTrackLengh = meanTrackLengh/nTracks;
				logger.write("Valid tracks: "+  nTracks + "\n");
				logger.write("Mean track length: " + meanTrackLengh + " \n");
				//Plot weighted histogramva
				double[][] hist = getWeightedHistogram(dataC, 5);
				plotHistogram(hist);
			}
			break;
		case MAXIMUM_LIKELIHOOD:
			//Conversion Factor from pixel squared to MSD in [cm^2] - Needed for walker's method
			double convertToCMSquared =  nmPerPixel*nmPerPixel*Math.pow(10, -14)*4*1.0/framesPerSecond; 
			
			//Convert unit diffusion coefficient from pixel^2 / s to 10^(-10) cm^2 * s^-1 and then to diameter in nm
			double conversionFactor = nmPerPixel*nmPerPixel*Math.pow(10, -4); //to 10^-10 cm^2;
			double E10cm2nm = Math.pow(10, 17);
			double toDiameter = (kB*temp/(3*Math.PI*visk))*E10cm2nm;
			
			double[][] dataC = new double[data.length][2];
			int NTracks = 0;
			int meanTrackLengh = 0;
			double maxSize = Double.MIN_VALUE;
			for(int i = 0; i < data.length; i++){
				//Find maximum size
				double dia = toDiameter/(data[i][0]*conversionFactor);
				if(dia>maxSize){
					maxSize = dia;
				}
				
				//Conversion
				dataC[i][0] = (data[i][0]*convertToCMSquared);
				dataC[i][1] = data[i][1];
				NTracks++;
				meanTrackLengh += data[i][1];
			}
			meanTrackLengh = meanTrackLengh/NTracks;
			logger.write("Valid tracks: " + NTracks + " \n");
			logger.write("Mean track length: " + meanTrackLengh + " \n");
			WalkerMethodEstimator walker = new WalkerMethodEstimator(dataC, temp, visk, framesPerSecond, (int)maxSize);
			plotHistogram(walker.estimate());
			break;
		default:
			break;
		}
		

	}
	
	private void fillDataStructure(){
		double drift[] = getDrift();
		
		int N = model.getTrackModel().nTracks(true);
		ArrayList<Double> dcs = new ArrayList<Double>();
		ArrayList<Integer> weights = new ArrayList<Integer>();
		logger.write("Input Tracks: " + N);
		Set<Integer> selectedTracks = model.getTrackModel().trackIDs(true);
		Iterator<Integer> it = selectedTracks.iterator();
		while(it.hasNext()){
			Set< Spot > spots= model.getTrackModel().trackSpots(it.next());
			final Comparator< Spot > comparator = Spot.frameComparator;
            final List< Spot > sorted = new ArrayList< Spot >( spots );
            Collections.sort( sorted, comparator );
            double msdX = 0; //Mean squared displacment x-direction
            double msdY = 0; //Mean squared displacement y-direction
            int msdN = 0;
            double locNoiseX = 0; //Localization noise x-direction
            double locNoiseY = 0; //Localization noise y-direction
            int locNoiseN = 0;
            logger.write("Track: " + sorted.size());
            for(int j = 1; j < sorted.size(); j++){
            	Spot spot1 = sorted.get(j-1);
            	Spot spot2 = sorted.get(j);

            	
            	if((spot2.diffTo(spot1, Spot.FRAME)-1)<0.001){ //gabs will be skipped
            		double dx0 = spot2.diffTo(spot1, Spot.POSITION_X)-drift[0];
            		double dy0 = spot2.diffTo(spot1, Spot.POSITION_Y)-drift[1];
            		msdX+=dx0*dx0;
            		msdY+=dy0*dy0;
            		msdN++;
            		if(j-2>0){
            			Spot spot0 = sorted.get(j-2);
            			if((spot1.diffTo(spot0, Spot.FRAME)-1)<0.001){ //gabs will be skipped
            				double dx1 = spot1.diffTo(spot0, Spot.POSITION_X)-drift[0];
            				double dy1 = spot1.diffTo(spot0, Spot.POSITION_Y)-drift[1];
            				
            				locNoiseX += dx0*dx1;
            				locNoiseY += dy0*dy1;
            				
            				locNoiseN++;
            			}
            		}
            		
            		
            	}
            }
            if(locNoiseN>0 && msdN >0){
            	double dt=1.0/framesPerSecond;
            	msdX = msdX/(msdN*2*dt);
            	msdY = msdY/(msdN*2*dt);
            	locNoiseX = locNoiseX/(locNoiseN*dt);
            	locNoiseY = locNoiseY/(locNoiseN*dt);
            	
            	double Dx = msdX+locNoiseX;
            	double Dy = msdY+locNoiseY;
            	if(Dx>0 && Dy>0){
            		double D = (Dx+Dy)/2;
            		dcs.add(D);
            		weights.add(msdN);
            	}
            	
            }
		}
		//Fill in data structure
		data = new double[dcs.size()][2];
		for(int i = 0; i < dcs.size(); i++){
			data[i][0] = dcs.get(i);
			data[i][1] = weights.get(i);
		}
		
		
	
	}
	
	private double[] getDrift(){
		double[] drift = new double[2];
		int K =0;
		int N = model.getTrackModel().nTracks(true);
		for(int i = 0; i < N; i++){
			Set< Spot > spots= model.getTrackModel().trackSpots(i);
			final Comparator< Spot > comparator = Spot.frameComparator;
            final List< Spot > sorted = new ArrayList< Spot >( spots );
            Collections.sort( sorted, comparator );
            
            for(int j = 1; j < sorted.size(); j++){
            	Spot spot1 = sorted.get(j-1);
            	Spot spot2 = sorted.get(j);
            	if(spot2.diffTo(spot1, Spot.FRAME)==1){ //gabs will be skipped
            		double dx0 = spot2.diffTo(spot1, Spot.POSITION_X);
            		double dy0 = spot2.diffTo(spot1, Spot.POSITION_Y);
            		
            		drift[0] += dx0;
            		drift[1] += dy0;
            		K++;
            	}
            }
		}
		drift[0] = drift[0]/K;
		drift[1] = drift[1]/K;
		
		return drift;
	}
	
	private double[][] getWeightedHistogram(double[][] data, double binsize){
		
		//Sort the data structure;
				Arrays.sort(data, new Comparator<double[]>() {

					@Override
					public int compare(double[] arg0, double[] arg1) {
						final double v1 = arg0[0];
		                final double v2 = arg1[0];
		                
		                if(v1>v2){
		                	return 1;
		                }
		                if(v1<v2){
		                	return -1;
		                }
						return 0;
					}
				
				});
		
		//Normalization factor
		double norm = 0;
		for( int i =0; i < data.length; i++){
			norm += data[i][1];
		}
		norm = 1/norm;
		//Minimum Value
		double maxValue = data[data.length-1][0];
		
		int numberOfBins = (int)Math.ceil(maxValue/binsize);
		
		
		//Set each bin value to center of the bin
		double[][] weightedHistogram = new double[numberOfBins][2];
		for(int i = 0; i < weightedHistogram.length; i++){
			weightedHistogram[i][0] += binsize*(2*i+1)/2f;
		}
		
		//Add normalized weights to the corresponding bin 
		for(int i = 0; i < data.length; i++){
			int bin = (int)(data[i][0]/binsize)-1;
			bin = bin > 0 ? bin:0;
			weightedHistogram[bin][1] += data[i][1]*norm;
		}
		
		
		return weightedHistogram;
	}
	
	private void plotHistogram(double[][] hist){
		XYSeries xyseries = new XYSeries("");
		for(int i = 0; i < hist.length; i++){
			xyseries.add(hist[i][0], hist[i][1]);
		}
		XYSeriesCollection xyseriescollection = new XYSeriesCollection(xyseries);
		JFreeChart jfreechart = ChartFactory.createXYBarChart("Size Distribution", "Diameter [nm]" , false, 
				"Rel. Frequency", xyseriescollection, PlotOrientation.VERTICAL, false,true,false);
		ChartPanel chartPanel = new ChartPanel(jfreechart);
		JPanel main = new JPanel();
		
		main.add(chartPanel);
		
		JFrame frame = new JFrame();
		frame.setContentPane(main);
		frame.pack();
		frame.setVisible(true);
	}

	private void showGui(){
		
		GenericDialogPlus gd = new GenericDialogPlus("Brownian Motion Sizer");
		gd.addNumericField("Temperatur [kelvin]", 22, 1);
		gd.addNumericField("Pixelsize [nm]", 166, 0);
		gd.addNumericField("FPS", 1/trackMateInstance.getSettings().dt, 0);
		gd.addChoice(methods[0], methods, methods[0]);
		gd.showDialog();
		
		temp = gd.getNextNumber() + 273.15;
		double andrade = Math.exp(-6.944+2036.8/temp) * Math.pow(10, -12); //Andrade-Gleichung [kg nm^-1 s^-2]
		visk = andrade* Math.pow(10, 7); // To kg cm^-2 s^-2
		nmPerPixel = gd.getNextNumber();
		framesPerSecond = gd.getNextNumber();
		selectedMethod = gd.getNextChoiceIndex();
		logger.write("Used temperatur in Kelvin: " + temp + "\n");
		logger.write("Viscosity: " + visk +"\n");
		logger.write("Method: " + methods[selectedMethod] + "\n");
		
		
		
	}
	
	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
		
	}

}
