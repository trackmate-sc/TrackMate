package fiji.plugin.trackmate.action.fit;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;

public interface SpotFitter extends MultiThreaded, Benchmark
{

	void process( Iterable< Spot > spots, Logger logger );

	void fit( Spot spot );

}
