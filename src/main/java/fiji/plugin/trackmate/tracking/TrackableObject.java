package fiji.plugin.trackmate.tracking;

import net.imglib2.EuclideanSpace;
import net.imglib2.RealLocalizable;

public interface TrackableObject extends RealLocalizable, EuclideanSpace {
	
	int ID();
	
	String getName();

	void setName(String name);
	
	int frame();
	
	void setFrame(int frame);
	
	double radius();
	
	void setVisible(boolean visibility);
	
	boolean isVisible();
}
