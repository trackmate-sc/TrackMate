package fiji.plugin.trackmate.action;

import java.io.File;

import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.ImagePlus;
import net.imglib2.util.Util;

public class SpotGaussianFitterRefine
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		final double crameRao = 19.5 * Math.sqrt( 2 );
		final String filePath = "D:/Projects/TSabate/Data/FromBLelandais/poisson_noise_100photons.trackmate.xml";

		// Grund truth.
		final double gx = 64.;
		final double gy = 64.;
		final double pixel_size = 120.; // nm

		final TmXmlReader reader = new TmXmlReader( new File( filePath ) );
		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		final Settings settings = reader.readSettings( imp );

		// Before fitting.
		final double[] dng = new double[ model.getSpots().getNSpots( true ) ];
		final Iterable< Spot > iterable = model.getSpots().iterable( true );
		int idx = 0;
		for ( final Spot spot : iterable )
		{
			final double dx = spot.getDoublePosition( 0 ) - gx;
			final double dy = spot.getDoublePosition( 0 ) - gy;

			dng[ idx ] = Math.sqrt( dx * dx + dy * dy );
			idx++;
		}

		System.out.println( "CLASSICAL LOCALIZATION:" );
		System.out.println( String.format( "Mean distance to ground truth: %.1f nm", Util.average( dng ) * pixel_size ) );
		System.out.println( String.format( "Crame-Rao bound: %.1f nm", crameRao ) );

		System.out.println( "\nStarting fitting." );
		final SpotGaussianFitter fitter = new SpotGaussianFitter( model, settings, Logger.IJ_LOGGER );
		if ( !fitter.checkInput() || !fitter.process() )
		{
			System.err.println( fitter.getErrorMessage() );
			return;
		}

		final double[] dg = new double[ model.getSpots().getNSpots( true ) ];
		int idx2 = 0;
		for ( final Spot spot : iterable )
		{
			final double dx = spot.getDoublePosition( 0 ) - gx;
			final double dy = spot.getDoublePosition( 0 ) - gy;

			dg[ idx2 ] = Math.sqrt( dx * dx + dy * dy );
			idx2++;
		}

		System.out.println( "\nGAUSSIAN PEAK FITTING:" );
		System.out.println( String.format( "Mean distance to ground truth: %.1f nm", Util.average( dg ) * pixel_size ) );
		System.out.println( String.format( "Crame-Rao bound: %.1f nm", crameRao ) );
	}
}
