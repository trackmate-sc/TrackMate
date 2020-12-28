package fiji.plugin.trackmate.features.spot;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Special interface for spot analyzers that can compute feature values based on
 * the contour of spots.
 *
 * @author Jean-Yves Tinevez - 2020
 */
public interface SpotMorphologyAnalyzerFactory< T extends RealType< T > & NativeType< T > > extends SpotAnalyzerFactoryBase< T >
{}
