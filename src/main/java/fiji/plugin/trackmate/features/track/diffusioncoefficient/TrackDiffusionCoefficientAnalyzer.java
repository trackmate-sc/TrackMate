package fiji.plugin.trackmate.features.track.diffusioncoefficient;

import ij.IJ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;

@Plugin( type = TrackAnalyzer.class )
public class TrackDiffusionCoefficientAnalyzer implements TrackAnalyzer{
	
	private static final String KEY = "DIFFUSION_COEFFICIENT_ANALYZER";
	
	private static final String TRACK_DIFFUSION_COEFFICIENT = "DIFFUSION_COEFFICIENT";
	
	private static final String TRACK_HYDRODYNAMIC_DIAMETER = "HYDRODYNAMIC_DIAMETER";
	
	private static final List< String > FEATURES = new ArrayList< String >(2);
	 
	private static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< String, String >( 2 );
	 
	private static final Map< String, String > FEATURE_NAMES = new HashMap< String, String >( 2 );
	 
	private static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< String, Dimension >( 2 );
	
	private static final Map< String, Boolean > IS_INT = new HashMap< String, Boolean >( 2 );
	
	static {
		FEATURES.add(TRACK_DIFFUSION_COEFFICIENT);
		FEATURES.add(TRACK_HYDRODYNAMIC_DIAMETER);
		
		FEATURE_NAMES.put(TRACK_DIFFUSION_COEFFICIENT, "Diffusion Coefficient");
		FEATURE_NAMES.put(TRACK_HYDRODYNAMIC_DIAMETER, "Hydrodynamic Diameter");
		
		FEATURE_SHORT_NAMES.put(TRACK_DIFFUSION_COEFFICIENT, "DC");
		FEATURE_SHORT_NAMES.put(TRACK_HYDRODYNAMIC_DIAMETER, "Hydr. Dia.");
		
		FEATURE_DIMENSIONS.put(TRACK_DIFFUSION_COEFFICIENT, Dimension.NONE);
		FEATURE_DIMENSIONS.put(TRACK_HYDRODYNAMIC_DIAMETER, Dimension.NONE);
		
		IS_INT.put(TRACK_DIFFUSION_COEFFICIENT, false); 	
		IS_INT.put(TRACK_HYDRODYNAMIC_DIAMETER, false);
		
	}
	private long processingTime;
	@Override
	public long getProcessingTime() {
		// TODO Auto-generated method stub
		return processingTime;
	}

	@Override
	public List<String> getFeatures() {
		// TODO Auto-generated method stub
		return FEATURES;
	}

	@Override
	public Map<String, String> getFeatureShortNames() {
		// TODO Auto-generated method stub
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map<String, String> getFeatureNames() {
		// TODO Auto-generated method stub
		return FEATURE_NAMES;
	}

	@Override
	public Map<String, Dimension> getFeatureDimensions() {
		// TODO Auto-generated method stub
		return FEATURE_DIMENSIONS;
	}

	@Override
	public Map<String, Boolean> getIsIntFeature() {
		// TODO Auto-generated method stub
		return Collections.unmodifiableMap( IS_INT );
	}

	@Override
	public boolean isManualFeature() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getInfoText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImageIcon getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return KEY;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Diffusion coefficient and hydrodynamic diameter";
	}

	@Override
	public void setNumThreads() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNumThreads(int numThreads) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getNumThreads() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public void process(Collection<Integer> trackIDs, Model model) {
		final FeatureModel fm = model.getFeatureModel();
		for (Integer i : trackIDs) {
			Set< Spot > spots= model.getTrackModel().trackSpots(i);
			
			final Comparator< Spot > comparator = Spot.frameComparator;
            final List< Spot > sorted = new ArrayList< Spot >( spots );
            Collections.sort( sorted, comparator );
            double msdX = 0; //Mean squared displacment x-direction
            double msdY = 0; //Mean squared displacement y-direction
            int msdN = 0;
            double locNoiseX = 0; //Localization noise x-direction
            double locNoiseY = 0; //Localization noise y-direction
            int locNoiseN = 0;
            for(int j = 1; j < sorted.size(); j++){
            	Spot spot1 = sorted.get(j-1);
            	Spot spot2 = sorted.get(j);

            	
            	if(spot2.diffTo(spot1, Spot.FRAME)==1){ //gabs will be skipped
            		double dx0 = spot2.diffTo(spot1, Spot.POSITION_X);
            		double dy0 = spot2.diffTo(spot1, Spot.POSITION_Y);
            		msdX+=dx0*dx0;
            		msdY+=dy0*dy0;
            		msdN++;
            		if(j-2>0){
            			Spot spot0 = sorted.get(j-2);
            			if(spot1.diffTo(spot0, Spot.FRAME)==1){ //gabs will be skipped
            				double dx1 = spot1.diffTo(spot0, Spot.POSITION_X);
            				double dy1 = spot1.diffTo(spot0, Spot.POSITION_Y);
            				
            				locNoiseX += dx0*dx1;
            				locNoiseY += dy0*dy1;
            				
            				locNoiseN++;
            			}
            		}
            		
            		
            	}
            }
            double dt=1.0/30;
    		msdX = msdX/(msdN*2*dt);
    		msdY = msdY/(msdN*2*dt);
    		locNoiseX = locNoiseX/(locNoiseN*dt);
    		locNoiseY = locNoiseY/(locNoiseN*dt);
    		
    		double Dx = msdX+locNoiseX;
    		double Dy = msdY+locNoiseY;
    		
    		double D = (Dx+Dy)/2;
 
    		fm.putTrackFeature(i,TRACK_DIFFUSION_COEFFICIENT,D);
    		fm.putTrackFeature(i,TRACK_HYDRODYNAMIC_DIAMETER, convertToHydodynamicDiameter(D));
    		
    		
		}
		
	}

	private double convertToHydodynamicDiameter(double D){
		return D;
	}

	@Override
	public boolean isLocal() {
		// TODO Auto-generated method stub
		return true;
	}

}
