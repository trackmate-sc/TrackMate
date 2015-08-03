package fiji.plugin.trackmate.action.brownianmotion;

/*
The MIT License (MIT)

NanoTrackJ is a software to characterize the size of nanoparticles by its trajectories
Copyright (C) 2013  Thorsten Wagner wagner@biomedical-imaging.de

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

import ij.IJ;

import org.apache.commons.math3.special.Gamma;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
/**
* Implements Walker, J.G., 2012. Improved nano-particle tracking analysis. Measurement Science and Technology, 23(6), p.065605.
* @author Thorsten Wagner
*/
public class WalkerMethodEstimator {

private double temp=22;										// [k]
private double visk;										// [kg cm^-1 s^-1]
private double frameduration=1.0/30; 						// [s]
private double kB = 1.3806488* Math.pow(10, -19); 			// [kg cm^2 s^-2 K^-1]
private double[][] data; 									// [k][0] = MSD [k][1] = Tracklength
private int binSizeInnm = 2; 								// [nm]
private double deltaR = binSizeInnm*Math.pow(10, -7); 		// cm^2
private double maxRadiusInNm = 400; 						//[nm]
private int binNumber = (int)(maxRadiusInNm/binSizeInnm);
private int minTrackLength;
private int maxTrackLength;
private int[] Nk;

private double msdMax;
private double deltaB;
private int histBinNumber;
private double[] histogramMSD;
private double lastChiSquared;
private double[] logMapK;
private double[] logMapGammaK;
/**
 * 
 * @param data Containes the mean squared displacement and the Tracklength for each track. data[i][0] = MSD data[i][1] = Tracklength
 * @param temp Temperature of the suspension in kelvin
 * @param visk Viscosity of the suspension
 * @param framerate Framerate in hertz
 * @param maxdiameter The maximum diameter for the estimation
 */
public WalkerMethodEstimator(double[][] data, double temp, double visk, double framerate, int maxdiameter) {
	
	this.data = data;
	this.temp = temp;
	this.visk = visk; 
	this.frameduration = 1/framerate;
	
	minTrackLength = Integer.MAX_VALUE;
	maxTrackLength = Integer.MIN_VALUE;
	//msdMin = Double.MAX_VALUE;
	msdMax = Double.MIN_VALUE;
	//double convFact = Math.pow(10, -10);

	for(int i = 0; i < data.length; i++){
		//10^-10 cm^2 -> cm^2
		//this.data[i][0] = this.data[i][0]*convFact; //- 4*17.562862475*17.562862475*Math.pow(10, -7)*Math.pow(10, -7); 
		if(data[i][0]>msdMax){
			msdMax = data[i][0];
		}
		if(this.data[i][1]>maxTrackLength){
			maxTrackLength = (int)this.data[i][1];
		}
		if(this.data[i][1]<minTrackLength){
			minTrackLength = (int)this.data[i][1];
		}
		//IJ.log("MSD " + this.data[i][0]);
	}

	logMapK=new double[maxTrackLength+1];
	logMapGammaK=new double[maxTrackLength+1];
	java.util.Arrays.fill(logMapK, Double.NaN);
	java.util.Arrays.fill(logMapGammaK, Double.NaN);
	maxRadiusInNm = maxdiameter/2.0;
	
	binNumber = (int)(maxRadiusInNm/binSizeInnm);
	histBinNumber = (int)Math.ceil(Math.sqrt(data.length));
	deltaB = msdMax/histBinNumber;

	histogramMSD = new double[histBinNumber];
	java.util.Arrays.fill(histogramMSD, 0);
	Nk = new int[maxTrackLength+1];
	java.util.Arrays.fill(Nk, 0);
	for(int i = 0; i < data.length; i++){
		int index = (int)this.data[i][1];
		Nk[index]++;
		
		int index2 = (int)Math.floor(data[i][0]/deltaB - 0.001);
		
		histogramMSD[index2]++;
	}
}

private double probMSD(double msd, double k, double r){
	
	double pmsd=0;
	double thetaFactor = (2*kB*temp*frameduration)/(3*Math.PI*visk);
	double theta = thetaFactor/r;
	pmsd = (logK(k)+(k-1)*(logK(k)+Math.log(msd)) + (-k*msd/theta) ) - (k*Math.log(theta) + logGammaK(k)); 
	//pmsd = (Math.log(k)+(k-1)*(Math.log(k)+Math.log(msd)) + (-k*msd/theta) ) - (k*Math.log(theta) + Gamma.logGamma(k)); 
	pmsd = Math.exp(pmsd);
	return pmsd;
	
}

private double logK(double k){
	if(!Double.isNaN(logMapK[(int)k])){
		return logMapK[(int)k];
	}
	logMapK[(int)k] = Math.log(k);
	return logMapK[(int)k];
}

private double logGammaK(double k){
	if(!Double.isNaN(logMapGammaK[(int)k])){
		return logMapGammaK[(int)k];
	}
	logMapGammaK[(int)k] = Gamma.logGamma(k);
	return logMapGammaK[(int)k];
}

private double[] getHistogramML(double[] pm){
	double[] histMl = new double[histBinNumber];

	Sum sum = new Sum();
	for(int b = 0; b < histMl.length; b++){
		double outersum = 0;
		for(int k = minTrackLength; k <= maxTrackLength; k++){
			double sumpm = sum.evaluate(pm);
			double innersum = 0;
			for(int m = 0; m < pm.length; m++){
				innersum += (probMSD((b+1)*deltaB, k, (m+1)*deltaR) * deltaB * pm[m])/sumpm;
			}
			outersum += Nk[k]*innersum;
		}
		histMl[b] = outersum;
	}
	return histMl;
}

private double getChiSquared(double[] pm){
	double sumchi = 0;
	double[] histML = getHistogramML(pm);
	for(int b = 0; b < histBinNumber; b++){
		double diff = histogramMSD[b]-histML[b];
		sumchi = sumchi + diff*diff/histML[b];
	}
	return sumchi;
}

/**
 * 
 * @return Histogram [i][j]: i = bin, j = density
 */
public double[][] estimate() {
	
	double[] dens = new double[binNumber];
	java.util.Arrays.fill(dens, 1.0/binNumber);
	Sum sum = new Sum();
	//IJ.log(""+dens[2]);
	lastChiSquared=getChiSquared(dens);
	double changeChiSquared = Double.MAX_VALUE;
	IJ.showStatus("Size Distribution Estimation by Walker's Method");
	while(changeChiSquared>0.01){
		
		IJ.showProgress((int)((1-changeChiSquared)*100),99);
		for(int m = 0; m < dens.length; m++){
			double sumpm = sum.evaluate(dens);
			double help2 = 0;
			
			for(int k = 0; k < data.length; k++){
				double help1 = 0;
				double prob = probMSD(data[k][0],data[k][1], (m+1)*deltaR);
				
				for(int l=0; l < dens.length; l++){
					double prob2=probMSD(data[k][0], data[k][1], (l+1)*deltaR);
					help1 += prob2*dens[l]/sumpm;
				}
				help2 = help2 + prob/help1;
			}
			dens[m] = dens[m] * 1.0/data.length * help2;
		}
		double newChiSquared = getChiSquared(dens);
		changeChiSquared = Math.abs(newChiSquared-lastChiSquared)/lastChiSquared;
		lastChiSquared = newChiSquared;
	}
	IJ.showProgress(99,99);
	
	//Normalize
	double sumdens = sum.evaluate(dens);
	double[][] densxy = new double[dens.length][2];
	for(int i = 0; i < dens.length; i++){
		densxy[i][0] = binSizeInnm*(i+1)*2.0; //To Diamter in [nm]
		dens[i] = dens[i]/sumdens; //Normalize
		densxy[i][1] = dens[i];
		
	}

	return densxy;
	
}
}
