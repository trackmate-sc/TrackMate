package fiji.plugin.trackmate.visualization.bvv;

import bvv.util.Bvv;
import bvv.util.BvvFunctions;
import bvv.util.BvvHandle;
import bvv.util.BvvSource;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.util.TMUtils;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.LUT;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.mesh.nio.BufferMesh;
import net.imagej.mesh.obj.transform.TranslateMesh;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;

public class BVVUtils
{

	public static final StupidMesh createMesh( final Spot spot )
	{
		if ( spot instanceof SpotMesh )
		{
			final SpotMesh sm = ( SpotMesh ) spot;
			final Mesh mesh = TranslateMesh.translate( sm.getMesh(), spot );
			final BufferMesh bm = new BufferMesh( ( int ) mesh.vertices().size(), ( int ) mesh.triangles().size() );
			Meshes.copy( mesh, bm );
			return new StupidMesh( bm );
		}
		return new StupidMesh( Icosahedron.sphere( spot ) );
	}

	public static final < T extends Type< T > > BvvHandle createViewer( final ImagePlus imp )
	{
		final double[] cal = TMUtils.getSpatialCalibration( imp );

		// Convert and split by channels.
		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final int cAxis = img.dimensionIndex( Axes.CHANNEL );
		final BvvHandle bvvHandle;
		if ( cAxis < 0 )
		{
			final BvvSource source = BvvFunctions.show( img, imp.getShortTitle(),
					Bvv.options()
							.maxAllowedStepInVoxels( 0 )
							.renderWidth( 1024 )
							.renderHeight( 1024 )
							.preferredSize( 512, 512 )
							.frameTitle( "3D view " + imp.getShortTitle() )
							.sourceTransform( cal ) );
			source.setDisplayRange( imp.getDisplayRangeMin(), imp.getDisplayRangeMax() );
			if ( imp.getLuts().length > 0 )
			{
				final LUT lut = imp.getLuts()[ 0 ];
				final int rgb = lut.getColorModel().getRGB( ( int ) imp.getDisplayRangeMax() );
				source.setColor( new ARGBType( rgb ) );
			}
			bvvHandle = source.getBvvHandle();
		}
		else
		{
			BvvHandle h = null;
			final long nChannels = img.dimension( cAxis );
			final String st = imp.getShortTitle();
			for ( int c = 0; c < nChannels; c++ )
			{
				final ImgPlus< T > channel = ImgPlusViews.hyperSlice( img, cAxis, c );
				final BvvSource source;
				if ( h == null )
				{
					source = BvvFunctions.show( channel, st + "_c" + ( c + 1 ),
							Bvv.options()
									.maxAllowedStepInVoxels( 0 )
									.renderWidth( 1024 )
									.renderHeight( 1024 )
									.preferredSize( 512, 512 )
									.frameTitle( "3D view " + imp.getShortTitle() )
									.sourceTransform( cal ) );
					h = source.getBvvHandle();
				}
				else
				{
					source = BvvFunctions.show( channel, st + "_c" + ( c + 1 ),
							Bvv.options()
									.maxAllowedStepInVoxels( 0 )
									.renderWidth( 1024 )
									.renderHeight( 1024 )
									.preferredSize( 512, 512 )
									.sourceTransform( cal )
									.addTo( h ) );

				}
				final int i = imp.getStackIndex( c + 1, 1, 1 );
				if ( imp instanceof CompositeImage )
				{
					final CompositeImage cp = ( CompositeImage ) imp;
					source.setDisplayRange( cp.getChannelLut( c + 1 ).min, cp.getChannelLut( c + 1 ).max );
				}
				else
				{
					final ImageProcessor ip = imp.getStack().getProcessor( i );
					source.setDisplayRange( ip.getMin(), ip.getMax() );
				}
				if ( imp.getLuts().length > 0 )
				{
					final LUT lut = imp.getLuts()[ c ];
					final int rgb = lut.getColorModel().getRGB( ( int ) imp.getDisplayRangeMax() );
					source.setColor( new ARGBType( rgb ) );
				}
			}
			bvvHandle = h;
		}
		return bvvHandle;
	}
}
