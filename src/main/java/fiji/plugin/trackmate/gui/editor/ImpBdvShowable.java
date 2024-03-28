/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
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
package fiji.plugin.trackmate.gui.editor;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.AxisOrder;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.ViewerState;
import fiji.plugin.trackmate.util.TMUtils;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.process.LUT;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.bdv.BdvShowable;
import sc.fiji.labkit.ui.inputimage.ImgPlusViewsOld;

/**
 * A {@link BdvShowable} from a {@link ImgPlus}, but with channel colors, min
 * and max, channel visibility and display mode taken from a specified
 * {@link ImagePlus}.
 * <p>
 * Adapted from Matthias Arzt' ImgPlusBdvShowable, reusing code I made for
 * Mastodon support of IJ ImagePlus.
 *
 * @author Jean-Yves Tinevez
 */
public class ImpBdvShowable implements BdvShowable
{

	/**
	 * Returns a new {@link BdvShowable} that wraps the specified
	 * {@link ImagePlus}. The LUT and display settings are read from the
	 * {@link ImagePlus}.
	 * 
	 * @param <T>
	 *            the pixel type.
	 * @param imp
	 *            the {@link ImagePlus} to wrap and read LUT and display
	 *            settings from.
	 * @return a new {@link BdvShowable}
	 */
	public static < T extends NumericType< T > > ImpBdvShowable fromImp( final ImagePlus imp )
	{
		final ImgPlus< T > src = TMUtils.rawWraps( imp );
		if ( src.dimensionIndex( Axes.CHANNEL ) < 0 )
			Views.addDimension( src );
		return fromImp( src, imp );
	}

	/**
	 * Returns a new {@link BdvShowable} for the specified image, but using the
	 * LUT and display settings of the specified {@link ImagePlus}.
	 * 
	 * @param <T>
	 *            the pixel type.
	 * @param frame
	 *            the image to wrap in a {@link BdvShowable}.
	 * @param imp
	 *            the {@link ImagePlus} to read LUT and display settings from.
	 * @return a new {@link BdvShowable}
	 */
	public static < T extends NumericType< T > > ImpBdvShowable fromImp( final ImgPlus< T > frame, final ImagePlus imp )
	{
		return new ImpBdvShowable( prepareImage( frame ), imp );
	}

	private final ImagePlus imp;

	private final ImgPlus< ? extends NumericType< ? > > image;

	ImpBdvShowable( final ImgPlus< ? extends NumericType< ? > > image, final ImagePlus imp )
	{
		this.image = image;
		this.imp = imp;
	}

	@Override
	public Interval interval()
	{
		return image;
	}

	@Override
	public AffineTransform3D transformation()
	{
		final AffineTransform3D transform = new AffineTransform3D();
		transform.set(
				getCalibration( Axes.X ), 0, 0, 0,
				0, getCalibration( Axes.Y ), 0, 0,
				0, 0, getCalibration( Axes.Z ), 0 );
		return transform;
	}

	@Override
	public BdvStackSource< ? > show( final String title, final BdvOptions options )
	{
		final String name = image.getName();
		final BdvOptions options1 = options.axisOrder( getAxisOrder() ).sourceTransform( transformation() );
		final BdvStackSource< ? extends NumericType< ? > > stackSource = BdvFunctions.show( image, name == null
				? title : name, options1 );

		final List< ConverterSetup > converterSetups = stackSource.getConverterSetups();
		final SynchronizedViewerState state = stackSource.getBdvHandle().getViewerPanel().state();

		transferChannelVisibility( state );
		transferChannelSettings( converterSetups );
		state.setDisplayMode( DisplayMode.FUSED );
		return stackSource;
	}

	private int transferChannelVisibility( final ViewerState state )
	{
		final int nChannels = imp.getNChannels();
		final CompositeImage ci = imp.isComposite() ? ( CompositeImage ) imp : null;
		final List< SourceAndConverter< ? > > sources = state.getSources();
		if ( ci != null && ci.getCompositeMode() == IJ.COMPOSITE )
		{
			final boolean[] activeChannels = ci.getActiveChannels();
			int numActiveChannels = 0;
			for ( int i = 0; i < Math.min( activeChannels.length, nChannels ); ++i )
			{
				final SourceAndConverter< ? > source = sources.get( i );
				state.setSourceActive( source, activeChannels[ i ] );
				state.setCurrentSource( source );
				numActiveChannels += activeChannels[ i ] ? 1 : 0;
			}
			return numActiveChannels;
		}
		else
		{
			final int activeChannel = imp.getChannel() - 1;
			for ( int i = 0; i < nChannels; ++i )
				state.setSourceActive( sources.get( i ), i == activeChannel );
			state.setCurrentSource( sources.get( activeChannel ) );
			return 1;
		}
	}

	private void transferChannelSettings( final List< ConverterSetup > converterSetups )
	{
		final int nChannels = imp.getNChannels();
		final CompositeImage ci = imp.isComposite() ? ( CompositeImage ) imp : null;
		if ( ci != null )
		{
			final int mode = ci.getCompositeMode();
			final boolean transferColor = mode == IJ.COMPOSITE || mode == IJ.COLOR;
			for ( int c = 0; c < nChannels; ++c )
			{
				final LUT lut = ci.getChannelLut( c + 1 );
				final ConverterSetup setup = converterSetups.get( c );
				if ( transferColor )
					setup.setColor( new ARGBType( lut.getRGB( 255 ) ) );
				else
					setup.setColor( new ARGBType( Color.WHITE.getRGB() ) );
				setup.setDisplayRange( lut.min, lut.max );
			}
		}
		else
		{
			final double displayRangeMin = imp.getDisplayRangeMin();
			final double displayRangeMax = imp.getDisplayRangeMax();
			for ( int c = 0; c < nChannels; ++c )
			{
				final ConverterSetup setup = converterSetups.get( c );
				final LUT[] luts = imp.getLuts();
				if ( luts.length != 0 )
					setup.setColor( new ARGBType( luts[ 0 ].getRGB( 255 ) ) );
				setup.setDisplayRange( displayRangeMin, displayRangeMax );
			}
		}
	}

	private static ImgPlus< ? extends NumericType< ? > > prepareImage(
			final ImgPlus< ? extends NumericType< ? > > image )
	{
		final List< AxisType > order = Arrays.asList( Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL,
				Axes.TIME );
		return ImgPlusViewsOld.sortAxes( labelAxes( image ), order );
	}

	private static ImgPlus< ? extends NumericType< ? > > labelAxes(
			final ImgPlus< ? extends NumericType< ? > > image )
	{
		if ( image.firstElement() instanceof ARGBType )
			return ImgPlusViewsOld
					.fixAxes( image, Arrays.asList( Axes.X, Axes.Y, Axes.Z, Axes.TIME ) );
		if ( image.numDimensions() == 4 )
			return ImgPlusViewsOld.fixAxes( image, Arrays
					.asList( Axes.X, Axes.Y, Axes.Z, Axes.TIME, Axes.CHANNEL ) );
		return ImgPlusViewsOld.fixAxes( image, Arrays.asList( Axes.X, Axes.Y, Axes.Z,
				Axes.CHANNEL, Axes.TIME ) );
	}

	private double getCalibration( final AxisType axisType )
	{
		final int d = image.dimensionIndex( axisType );
		if ( d == -1 )
			return 1;
		return image.axis( d ).averageScale( image.min( d ), image.max( d ) );
	}

	private AxisOrder getAxisOrder()
	{
		final String code = IntStream
				.range( 0, image.numDimensions() )
				.mapToObj( i -> image
						.axis( i )
						.type()
						.getLabel().substring( 0, 1 ) )
				.collect( Collectors.joining() );
		try
		{
			return AxisOrder.valueOf( code );
		}
		catch ( final IllegalArgumentException e )
		{
			return AxisOrder.DEFAULT;
		}
	}
}
