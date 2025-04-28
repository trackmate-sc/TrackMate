package fiji.plugin.trackmate.detection.onestep;

import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface SpotDetectorTracker< T extends RealType< T > & NativeType< T > > extends OutputAlgorithm< SpotDetectorTrackerOutput >, MultiThreaded
{

}
