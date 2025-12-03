package fiji.plugin.trackmate.gui.editor.labkit.component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.ProjectorUtils;
import bdv.viewer.render.ProjectorUtils.ArrayData;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.util.StopWatch;

public class SrcOverAccumulateProjectorARGB
{

	public static class Factory implements AccumulateProjectorFactory< ARGBType >
	{

		private float[] sourceAlphas;

		public Factory( final float[] sourceAlphas )
		{
			this.sourceAlphas = sourceAlphas;
		}

		public void setSourceAlpha( final int sourceIndex, final float alpha )
		{
			if ( sourceIndex >= 0 && sourceIndex < sourceAlphas.length )
			{
				this.sourceAlphas[ sourceIndex ] = Math.max( 0, Math.min( 1, alpha ) );
			}
		}

		public float getSourceAlpha( final int sourceIndex )
		{
			return ( sourceIndex >= 0 && sourceIndex < sourceAlphas.length )
					? sourceAlphas[ sourceIndex ]
					: 1.0f;
		}

		@Override
		public VolatileProjector createProjector(
				final List< VolatileProjector > sourceProjectors,
				final List< SourceAndConverter< ? > > sources,
				final List< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
				final RandomAccessibleInterval< ARGBType > targetScreenImage,
				final int numThreads,
				final ExecutorService executorService )
		{

			final ProjectorData projectorData = getProjectorData( sourceScreenImages, targetScreenImage );
			if ( projectorData == null )
			{
				// Fallback to generic (not optimized)
				return null;
			}

			return new SrcOverProjectorARGBArrayData( sourceProjectors, projectorData, sourceAlphas );
		}
	}

	private static class ProjectorData
	{
		private final ArrayData targetData;

		private final List< int[] > sourceData;

		ProjectorData( final ArrayData targetData, final List< int[] > sourceData )
		{
			this.targetData = targetData;
			this.sourceData = sourceData;
		}

		ArrayData targetData()
		{
			return targetData;
		}

		List< int[] > sourceData()
		{
			return sourceData;
		}
	}

	private static ProjectorData getProjectorData(
			final List< ? extends RandomAccessible< ? extends ARGBType > > sources,
			final RandomAccessibleInterval< ARGBType > target )
	{

		final ArrayData targetData = ProjectorUtils.getARGBArrayData( target );
		if ( targetData == null )
			return null;

		final int numSources = sources.size();
		final List< int[] > sourceData = new ArrayList<>( numSources );
		for ( int i = 0; i < numSources; ++i )
		{
			final RandomAccessible< ? extends ARGBType > source = sources.get( i );
			if ( !( source instanceof RandomAccessibleInterval ) )
				return null;
			if ( !Intervals.equals( target, ( Interval ) source ) )
				return null;
			final int[] data = ProjectorUtils.getARGBArrayImgData( source );
			if ( data == null )
				return null;
			sourceData.add( data );
		}

		return new ProjectorData( targetData, sourceData );
	}

	private static class SrcOverProjectorARGBArrayData implements VolatileProjector
	{

		private List< VolatileProjector > sourceProjectors;

		private final List< int[] > sources;

		private final ArrayData target;

		private final float[] sourceAlphas;

		private long lastFrameRenderNanoTime;

		private volatile boolean canceled = false;

		private volatile boolean valid = false;

		public SrcOverProjectorARGBArrayData(
				final List< VolatileProjector > sourceProjectors,
				final ProjectorData projectorData,
				final float[] sourceAlphas )
		{

			this.sourceProjectors = sourceProjectors;
			this.target = projectorData.targetData();
			this.sources = projectorData.sourceData();
			this.sourceAlphas = sourceAlphas;
		}

		@Override
		public boolean map( final boolean clearUntouchedTargetPixels )
		{
			if ( canceled )
				return false;

			if ( isValid() )
				return true;

			final StopWatch stopWatch = StopWatch.createAndStart();

			// Render each source
			sourceProjectors.forEach( p -> p.map( clearUntouchedTargetPixels ) );

			if ( canceled )
				return false;

			// Composite with SRC_OVER
			mapAccumulate();

			sourceProjectors = sourceProjectors.stream()
					.filter( p -> !p.isValid() )
					.collect( Collectors.toList() );

			lastFrameRenderNanoTime = stopWatch.nanoTime();
			valid = sourceProjectors.isEmpty();
			return !canceled;
		}

		/**
		 * Accumulate pixels of all sources to target using SRC_OVER blending.
		 */
		private void mapAccumulate()
		{
			if ( canceled )
				return;

			final int numSources = sources.size();
			final int width = target.width();
			final int height = target.height();
			final int[] targetData = target.data();
			final int targetStride = target.stride();
			final int targetOx = target.ox();
			final int targetOy = target.oy();

			// Process each row
			for ( int y = 0; y < height; ++y )
			{
				final int oTarget = ( y + targetOy ) * targetStride + targetOx;
				final int oSource = y * width;

				// Process each pixel in the row
				for ( int x = 0; x < width; ++x )
				{
					int resultARGB = 0;

					// Composite each source front-to-back
					for ( int s = 0; s < numSources; ++s )
					{
						final int[] source = sources.get( s );
						final int sourceARGB = source[ oSource + x ];

						// Get global alpha for this source
						final float globalAlpha = ( s < sourceAlphas.length )
								? sourceAlphas[ s ]
								: 1.0f;

						// Extract source components
						int srcA = ARGBType.alpha( sourceARGB );
						final int srcR = ARGBType.red( sourceARGB );
						final int srcG = ARGBType.green( sourceARGB );
						final int srcB = ARGBType.blue( sourceARGB );

						// Apply global alpha to per-pixel alpha
						srcA = ( int ) ( srcA * globalAlpha );

						// SRC_OVER blending
						if ( s == 0 || resultARGB == 0 )
						{
							// First layer or nothing underneath
							resultARGB = ARGBType.rgba( srcR, srcG, srcB, srcA );
						}
						else
						{
							// SRC_OVER: C_out = C_src + C_dst * (1 - alpha_src)
							final int dstA = ARGBType.alpha( resultARGB );
							final int dstR = ARGBType.red( resultARGB );
							final int dstG = ARGBType.green( resultARGB );
							final int dstB = ARGBType.blue( resultARGB );

							final float srcAlphaF = srcA / 255.0f;
							final float oneMinusSrcAlpha = 1.0f - srcAlphaF;

							final int outR = ( int ) ( srcR * srcAlphaF + dstR * oneMinusSrcAlpha );
							final int outG = ( int ) ( srcG * srcAlphaF + dstG * oneMinusSrcAlpha );
							final int outB = ( int ) ( srcB * srcAlphaF + dstB * oneMinusSrcAlpha );
							final int outA = ( int ) ( srcA + dstA * oneMinusSrcAlpha );

							resultARGB = ARGBType.rgba( outR, outG, outB, Math.min( 255, outA ) );
						}
					}

					targetData[ oTarget + x ] = resultARGB;
				}
			}
		}

		@Override
		public void cancel()
		{
			canceled = true;
			for ( final VolatileProjector p : sourceProjectors )
				p.cancel();
		}

		@Override
		public long getLastFrameRenderNanoTime()
		{
			return lastFrameRenderNanoTime;
		}

		@Override
		public boolean isValid()
		{
			return valid;
		}
	}
}
