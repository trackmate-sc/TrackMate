package fiji.plugin.trackmate.detection;

import java.util.List;

import fiji.plugin.trackmate.Spot;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for Spot detector classes, that are able to segment spots of a
 * given estimated radius within a 2D or 3D image.
 * <p>
 * Normally, concrete implementation are not expected to be multi-threaded.
 * Indeed, the {@link fiji.plugin.trackmate.TrackMate} trackmate generates one
 * instance of the concrete implementation per thread, to process multiple
 * frames simultaneously.
 * 
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; 2010 - 2012
 * 
 */
public interface SpotDetector< T extends RealType< T > & NativeType< T >> extends OutputAlgorithm< List< Spot >>, Benchmark
{}
