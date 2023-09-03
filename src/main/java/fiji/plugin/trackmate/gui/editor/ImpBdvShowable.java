package fiji.plugin.trackmate.gui.editor;

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
import sc.fiji.labkit.ui.bdv.BdvShowable;

/**
 * A {@link BdvShowable} from a {@link ImgPlus}, but with channel colors, min &
 * max, channel visibility and display mode taken from a specified
 * {@link ImagePlus}.
 * <p>
 * Adapted from Matthias Arzt' ImgPlusBdvShowable, reusing code I made for
 * Mastodon support of IJ ImagePlus.
 *
 * @author Jean-Yves Tinevez
 */
public class ImpBdvShowable implements BdvShowable
{

	public static < T extends NumericType< T > > ImpBdvShowable fromImp( final ImgPlus< T > frame, final ImagePlus imp )
	{
		return new ImpBdvShowable( frame, imp );
	}

	private final ImgPlus< ? extends NumericType< ? > > image;

	private final ImagePlus imp;

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

		final int numActiveChannels = transferChannelVisibility( state );
		transferChannelSettings( converterSetups );
		state.setDisplayMode( numActiveChannels > 1 ? DisplayMode.FUSED : DisplayMode.SINGLE );
		return stackSource;
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

	private double getCalibration( final AxisType axisType )
	{
		final int d = image.dimensionIndex( axisType );
		if ( d == -1 )
			return 1;
		return image.axis( d ).averageScale( image.min( d ), image.max( d ) );
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
}
