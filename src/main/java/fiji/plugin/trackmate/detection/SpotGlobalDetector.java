package fiji.plugin.trackmate.detection;

import fiji.plugin.trackmate.SpotCollection;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface SpotGlobalDetector< T extends RealType< T > & NativeType< T > > extends OutputAlgorithm< SpotCollection >, Benchmark
{}
