/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MAX_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MEAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MEDIAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.MIN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.STD_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory.TOTAL_INTENSITY;

import org.scijava.util.DoubleArray;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotUtil;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

public class SpotIntensityMultiCAnalyzer< T extends RealType< T > > extends AbstractSpotFeatureAnalyzer< T >
{

	private final int channel;

	private final ImgPlus< T > imgCT;

	public SpotIntensityMultiCAnalyzer( final ImgPlus< T > imgCT, final int channel )
	{
		this.imgCT = imgCT;
		this.channel = channel;
	}

	@Override
	public void process( final Spot spot )
	{
		final IterableInterval< T > neighborhood = SpotUtil.iterable( spot, imgCT );
		final DoubleArray intensities = new DoubleArray();

		for ( final T pixel : neighborhood )
			intensities.addValue( pixel.getRealDouble() );

		Util.quicksort( intensities.getArray(), 0, intensities.size() - 1 );
		spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MEAN_INTENSITY, channel ), Double.valueOf( TMUtils.average( intensities ) ) );
		spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MEDIAN_INTENSITY, channel ), Double.valueOf( intensities.getArray()[ intensities.size() / 2 ] ) );
		spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MIN_INTENSITY, channel ), Double.valueOf( intensities.getArray()[ 0 ] ) );
		spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( MAX_INTENSITY, channel ), Double.valueOf( intensities.getArray()[ intensities.size() - 1 ] ) );
		spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( TOTAL_INTENSITY, channel ), Double.valueOf( TMUtils.sum( intensities ) ) );
		spot.putFeature( SpotIntensityMultiCAnalyzerFactory.makeFeatureKey( STD_INTENSITY, channel ), Double.valueOf( TMUtils.standardDeviation( intensities ) ) );
	}
}
