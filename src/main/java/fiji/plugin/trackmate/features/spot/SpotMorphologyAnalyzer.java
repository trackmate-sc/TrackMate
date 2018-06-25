package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory.MORPHOLOGY;
import static fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory.OBLATE;
import static fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory.PROLATE;
import static fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory.SCALENE;
import static fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory.SPHERE;
import static fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory.featurelist_phi;
import static fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory.featurelist_sa;
import static fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory.featurelist_theta;

import java.util.Arrays;
import java.util.Iterator;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.SpotNeighborhoodCursor;
import net.imagej.ImgPlus;
import net.imagej.axis.DefaultLinearAxis;
import net.imglib2.algorithm.region.localneighborhood.EllipseCursor;
import net.imglib2.algorithm.region.localneighborhood.EllipseNeighborhood;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

/**
 * This {@link SpotAnalyzer} computes morphology features for the given spots.
 * <p>
 * It estimates shape parameters by computing the most resembling ellipsoid from
 * the pixels contained within the spot radius. From this ellipsoid, it
 * determines what are its semi-axes lengths, and their orientation.
 * <p>
 * In the 3D case, the features ELLIPSOIDFIT_SEMIAXISLENGTH_* contains the
 * semi-axes lengths, ordered from the largest (A) to the smallest (C).
 * ELLIPSOIDFIT_AXISPHI_* and ELLIPSOIDFIT_AXISTHETA_* give the orientation
 * angles of the corresponding ellipsoid axis, in spherical coordinates. Angles
 * are expressed in radians.
 * <ul>
 * <li>φ is the azimuth int the XY plane and its range is ]-π/2 ; π/2]
 * <li>ϑ is the elevation with respect to the Z axis and ranges from 0 to π
 * </ul>
 * <p>
 * In the 2D case, ELLIPSOIDFIT_SEMIAXISLENGTH_A and ELLIPSOIDFIT_AXISPHI_A are
 * always 0, the THETA angles are 0, and ELLIPSOIDFIT_AXISPHI_B and
 * ELLIPSOIDFIT_AXISPHI_C differ by π/2.
 * <p>
 * From the semi-axis length, a morphology index is computed. Spots are
 * classified according to the shape of their most-resembling ellipsoid. We look
 * for equality between semi-axes, with a certain tolerance, which value is
 * {@link #SIGNIFICANCE_FACTOR}.
 * <p>
 * In the 2D case, if b &gt; c are the semi-axes length
 * <ul>
 * <li>if b ≅ c, then this index has the value
 * {@link SpotMorphologyAnalyzerFactory#SPHERE}
 * <li>otherwise, it has the value {@link SpotMorphologyAnalyzerFactory#PROLATE}
 * </ul>
 * <p>
 * In the 2D case, if a &gt; b &gt; c are the semi-axes length
 * <ul>
 * <li>if a ≅ b ≅ c, then this index has the value
 * {@link SpotMorphologyAnalyzerFactory#SPHERE}
 * <li>if a ≅ b &gt; c, then this index has the value
 * {@link SpotMorphologyAnalyzerFactory#OBLATE}: the spot resembles a flat disk
 * <li>if a &gt; b ≅ c, then this index has the value
 * {@link SpotMorphologyAnalyzerFactory#PROLATE}: the spot resembles a rugby
 * ball
 * <li>otherwise it has the value {@link SpotMorphologyAnalyzerFactory#SCALENE};
 * the spot's shape has nothing particular
 * </ul>
 * 
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; Apr 1, 2011 -
 *         2012
 */
public class SpotMorphologyAnalyzer< T extends RealType< T >> extends IndependentSpotFeatureAnalyzer< T >
{

	/**
	 * Significance factor to determine when a semiaxis length should be
	 * considered significantly larger than the others.
	 */
	public static final double SIGNIFICANCE_FACTOR = 1.2;

	public SpotMorphologyAnalyzer( final ImgPlus< T > imgCT, final Iterator< Spot > spots )
	{
		super( imgCT, spots );
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public final void process( final Spot spot )
	{

		if ( img.numDimensions() == 3 )
		{

			// 3D case
			final SpotNeighborhood< T > neighborhood = new SpotNeighborhood< >( spot, img );
			final SpotNeighborhoodCursor< T > cursor = neighborhood.cursor();

			double x, y, z;
			double x2, y2, z2;
			double mass, totalmass = 0;
			double Ixx = 0, Iyy = 0, Izz = 0, Ixy = 0, Ixz = 0, Iyz = 0;
			final double[] position = new double[ img.numDimensions() ];

			while ( cursor.hasNext() )
			{
				cursor.fwd();
				mass = cursor.get().getRealDouble();
				cursor.getRelativePosition( position );
				x = position[ 0 ];
				y = position[ 1 ];
				z = position[ 2 ];
				totalmass += mass;
				x2 = x * x;
				y2 = y * y;
				z2 = z * z;
				Ixx += mass * ( y2 + z2 );
				Iyy += mass * ( x2 + z2 );
				Izz += mass * ( x2 + y2 );
				Ixy -= mass * x * y;
				Ixz -= mass * x * z;
				Iyz -= mass * y * z;
			}

			final Matrix mat = new Matrix( new double[][] { { Ixx, Ixy, Ixz }, { Ixy, Iyy, Iyz }, { Ixz, Iyz, Izz } } );
			mat.timesEquals( 1 / totalmass );
			final EigenvalueDecomposition eigdec = mat.eig();
			final double[] eigenvalues = eigdec.getRealEigenvalues();
			final Matrix eigenvectors = eigdec.getV();

			final double I1 = eigenvalues[ 0 ];
			final double I2 = eigenvalues[ 1 ];
			final double I3 = eigenvalues[ 2 ];
			final double a = Math.sqrt( 2.5 * ( I2 + I3 - I1 ) );
			final double b = Math.sqrt( 2.5 * ( I3 + I1 - I2 ) );
			final double c = Math.sqrt( 2.5 * ( I1 + I2 - I3 ) );
			final double[] semiaxes = new double[] { a, b, c };

			// Sort semi-axes by ascendent order and get the sorting index
			final double[] semiaxes_ordered = semiaxes.clone();
			Arrays.sort( semiaxes_ordered );
			final int[] order = new int[ 3 ];
			for ( int i = 0; i < semiaxes_ordered.length; i++ )
				for ( int j = 0; j < semiaxes.length; j++ )
					if ( semiaxes_ordered[ i ] == semiaxes[ j ] )
						order[ i ] = j;

			// Get the sorted eigenvalues
			final double[][] uvectors = new double[ 3 ][ 3 ];
			for ( int i = 0; i < eigenvalues.length; i++ )
			{
				uvectors[ i ][ 0 ] = eigenvectors.get( 0, order[ i ] );
				uvectors[ i ][ 1 ] = eigenvectors.get( 1, order[ i ] );
				uvectors[ i ][ 2 ] = eigenvectors.get( 2, order[ i ] );
			}

			// Store in the Spot object
			double theta, phi;
			for ( int i = 0; i < uvectors.length; i++ )
			{
				theta = Math.acos( uvectors[ i ][ 2 ] / Math.sqrt( uvectors[ i ][ 0 ] * uvectors[ i ][ 0 ] + uvectors[ i ][ 1 ] * uvectors[ i ][ 1 ] + uvectors[ i ][ 2 ] * uvectors[ i ][ 2 ] ) );
				phi = Math.atan2( uvectors[ i ][ 1 ], uvectors[ i ][ 0 ] );
				if ( phi < -Math.PI / 2 )
					phi += Math.PI; // For an ellipsoid we care only for the
									// angles in [-pi/2 , pi/2]
				if ( phi > Math.PI / 2 )
					phi -= Math.PI;

				// Store in descending order
				spot.putFeature( featurelist_sa[ i ], semiaxes_ordered[ i ] );
				spot.putFeature( featurelist_phi[ i ], phi );
				spot.putFeature( featurelist_theta[ i ], theta );
			}

			// Store the Spot morphology (needs to be outside the above loop)
			spot.putFeature( MORPHOLOGY, estimateMorphology( semiaxes_ordered ) );

		}
		else if ( img.numDimensions() == 2 )
		{

			// 2D case
			final SpotNeighborhood< T > neighborhood = new SpotNeighborhood< >( spot, img );
			final SpotNeighborhoodCursor< T > cursor = neighborhood.cursor();
			double x, y;
			double x2, y2;
			double mass, totalmass = 0;
			double Ixx = 0, Iyy = 0, Ixy = 0;
			final double[] position = new double[ img.numDimensions() ];

			while ( cursor.hasNext() )
			{
				cursor.fwd();
				mass = cursor.get().getRealDouble();
				cursor.getRelativePosition( position );
				x = position[ 0 ];
				y = position[ 1 ];
				totalmass += mass;
				x2 = x * x;
				y2 = y * y;
				Ixx += mass * ( y2 );
				Iyy += mass * ( x2 );
				Ixy -= mass * x * y;
			}

			final Matrix mat = new Matrix( new double[][] { { Ixx, Ixy }, { Ixy, Iyy } } );
			mat.timesEquals( 1 / totalmass );
			final EigenvalueDecomposition eigdec = mat.eig();
			final double[] eigenvalues = eigdec.getRealEigenvalues();
			final Matrix eigenvectors = eigdec.getV();

			final double I1 = eigenvalues[ 0 ];
			final double I2 = eigenvalues[ 1 ];
			final double a = Math.sqrt( 4 * I1 );
			final double b = Math.sqrt( 4 * I2 );
			final double[] semiaxes = new double[] { a, b };

			// Sort semi-axes by ascendent order and get the sorting index
			final double[] semiaxes_ordered = semiaxes.clone();
			Arrays.sort( semiaxes_ordered );
			final int[] order = new int[ 2 ];
			for ( int i = 0; i < semiaxes_ordered.length; i++ )
				for ( int j = 0; j < semiaxes.length; j++ )
					if ( semiaxes_ordered[ i ] == semiaxes[ j ] )
						order[ i ] = j;

			// Get the sorted eigenvalues
			final double[][] uvectors = new double[ 2 ][ 2 ];
			for ( int i = 0; i < eigenvalues.length; i++ )
			{
				uvectors[ i ][ 0 ] = eigenvectors.get( 0, order[ i ] );
				uvectors[ i ][ 1 ] = eigenvectors.get( 1, order[ i ] );
			}

			// Store in the Spot object
			double theta, phi;
			for ( int i = 0; i < uvectors.length; i++ )
			{
				theta = 0;
				phi = Math.atan2( uvectors[ i ][ 1 ], uvectors[ i ][ 0 ] );
				if ( phi < -Math.PI / 2 )
					phi += Math.PI; // For an ellipsoid we care only for the
									// angles in [-pi/2 , pi/2]
				if ( phi > Math.PI / 2 )
					phi -= Math.PI;

				// Store in descending order
				spot.putFeature( featurelist_sa[ i ], semiaxes_ordered[ i ] );
				spot.putFeature( featurelist_phi[ i ], phi );
				spot.putFeature( featurelist_theta[ i ], theta );
			}
			spot.putFeature( featurelist_sa[ 2 ], Double.valueOf( 0 ) );
			spot.putFeature( featurelist_phi[ 2 ], Double.valueOf( 0 ) );
			spot.putFeature( featurelist_theta[ 2 ], Double.valueOf( 0 ) );

			// Store the Spot morphology (needs to be outside the above loop)
			spot.putFeature( MORPHOLOGY, estimateMorphology( semiaxes_ordered ) );

		}
	}

	/**
	 * Estimates whether a Spot morphology from the semi-axes lengths of its
	 * most resembling ellipsoid.
	 * 
	 * @param semiaxes
	 *            The semi-axis lengths <b>in ascending order</b>.
	 * @return 1 [Ellipsoid] if any semi-axis length(s) are significantly larger
	 *         than the other(s). 0 [Spherical] otherwise.
	 */
	private static final Double estimateMorphology( final double[] semiaxes )
	{

		if ( semiaxes.length == 2 )
		{
			// 2D case
			final double a = semiaxes[ 0 ];
			final double b = semiaxes[ 1 ];
			if ( b >= SIGNIFICANCE_FACTOR * a )
				return PROLATE;
			
			return SPHERE;
		}
		
		// 3D case.
		final double a = semiaxes[ 0 ]; // Smallest
		final double b = semiaxes[ 1 ];
		final double c = semiaxes[ 2 ]; // Largest

		// Sphere: all equals with respect to significance, that is: the
		// largest semi-axes must not
		// be larger that factor * the smallest
		if ( c < SIGNIFICANCE_FACTOR * a )
			return SPHERE;

		// Oblate: the 2 largest are equals with respect to significance
		if ( c < SIGNIFICANCE_FACTOR * b )
			return OBLATE;

		// Prolate: the 2 smallest are equals with respect to significance
		if ( b < SIGNIFICANCE_FACTOR * a )
			return PROLATE;

		return SCALENE;

	}

	public static void main( final String[] args )
	{

		// TEST 2D case

		// Parameters
		final int size_x = 200;
		final int size_y = 200;

		final long a = 10;
		final long b = 5;
		final double phi_r = Math.toRadians( 30 );

		final long max_radius = Math.max( a, b );
		final double[] calibration = new double[] { 1, 1 };

		// Create blank image
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( new long[] { 200, 200 } );
		final ImgPlus< UnsignedByteType > imgplus = new ImgPlus< >( img );
		for ( int d = 0; d < imgplus.numDimensions(); d++ )
		{
			imgplus.setAxis( new DefaultLinearAxis( imgplus.axis( d ).type(), calibration[ d ] ), d );
		}
		final byte on = ( byte ) 255;

		// Create an ellipse
		long start = System.currentTimeMillis();
		System.out.println( String.format( "Creating an ellipse with a = %d, b = %d", a, b ) );
		System.out.println( String.format( "phi = %.1f", Math.toDegrees( phi_r ) ) );
		final long[] center = new long[] { size_x / 2, size_y / 2 };
		final long[] radiuses = new long[] { max_radius, max_radius };

		final EllipseNeighborhood< UnsignedByteType > disc = new EllipseNeighborhood< >( img, center, radiuses );
		final EllipseCursor< UnsignedByteType > sc = disc.cursor();

		double r2, phi, term;
		double cosphi, sinphi;
		while ( sc.hasNext() )
		{
			sc.fwd();
			r2 = sc.getDistanceSquared();
			phi = sc.getPhi();
			cosphi = Math.cos( phi - phi_r );
			sinphi = Math.sin( phi - phi_r );
			term = r2 * cosphi * cosphi / a / a + r2 * sinphi * sinphi / b / b;
			if ( term <= 1 )
				sc.get().set( on );
		}
		final long end = System.currentTimeMillis();
		System.out.println( "Ellipse creation done in " + ( end - start ) + " ms." );
		System.out.println();

		ij.ImageJ.main( args );
		ImageJFunctions.show( imgplus );

		start = System.currentTimeMillis();
		final Spot spot = new Spot( center[ 0 ], center[ 1 ], 0d, max_radius, -1d );

		final SpotMorphologyAnalyzer< UnsignedByteType > bm = new SpotMorphologyAnalyzer< >( imgplus, null );
		bm.process( spot );

		System.out.println( "Blob morphology analyzed in " + ( end - start ) + " ms." );
		double phiv, thetav, lv;
		for ( int j = 0; j < 2; j++ )
		{
			lv = spot.getFeature( featurelist_sa[ j ] );
			phiv = spot.getFeature( featurelist_phi[ j ] );
			thetav = spot.getFeature( featurelist_theta[ j ] );
			System.out.println( String.format( "For axis of semi-length %.1f, orientation is phi = %.1f°, theta = %.1f°", lv, Math.toDegrees( phiv ), Math.toDegrees( thetav ) ) );
		}
		System.out.println( spot.echo() );

		// TEST 3D case
		/*
		 * 
		 * // Parameters int size_x = 200; int size_y = 200; int size_z = 200;
		 * 
		 * double a = 5.5f; double b = 4.9f; double c = 5; double theta_r =
		 * (double) Math.toRadians(0); // I am unable to have it working for
		 * theta_r != 0 double phi_r = (double) Math.toRadians(45);
		 * 
		 * double max_radius = Math.max(a, Math.max(b, c)); double[] calibration
		 * = new double[] {1, 1, 1};
		 * 
		 * // Create blank image Image<UnsignedByteType> img = new
		 * ImageFactory<UnsignedByteType>( new UnsignedByteType(), new
		 * ArrayContainerFactory() ).createImage(new int[] {200, 200, 200});
		 * final byte on = (byte) 255;
		 * 
		 * // Create an ellipse long start = System.currentTimeMillis();
		 * System.out.println(String.format(
		 * "Creating an ellipse with a = %.1f, b = %.1f, c = %.1f", a, b, c));
		 * System.out.println(String.format("phi = %.1f and theta = %.1f",
		 * Math.toDegrees(phi_r), Math.toDegrees(theta_r))); double[] center =
		 * new double[] { size_x/2, size_y/2, size_z/2 };
		 * SphereCursor<UnsignedByteType> sc = new
		 * SphereCursor<UnsignedByteType>(img, center, max_radius, calibration);
		 * double r2, theta, phi, term; double cosphi, sinphi, costheta,
		 * sintheta; while (sc.hasNext()) { sc.fwd(); r2 =
		 * sc.getDistanceSquared(); phi = sc.getPhi(); theta = sc.getTheta();
		 * cosphi = Math.cos(phi-phi_r); sinphi = Math.sin(phi-phi_r); costheta
		 * = Math.cos(theta-theta_r); sintheta = Math.sin(theta-theta_r); term =
		 * r2*cosphi*cosphi*sintheta*sintheta/a/a +
		 * r2*sinphi*sinphi*sintheta*sintheta/b/b + r2*costheta*costheta/c/c; if
		 * (term <= 1) sc.getType().set(on); } sc.close(); long end =
		 * System.currentTimeMillis();
		 * System.out.println("Ellipse creation done in " + (end-start) +
		 * " ms."); System.out.println();
		 * 
		 * ij.ImageJ.main(args); img.getDisplay().setMinMax();
		 * ImageJFunctions.copyToImagePlus(img).show();
		 * 
		 * start = System.currentTimeMillis(); BlobMorphology<UnsignedByteType>
		 * bm = new BlobMorphology<UnsignedByteType>(img, calibration); SpotImp
		 * spot = new SpotImp(center); spot.putFeature(Feature.RADIUS,
		 * max_radius); bm.process(spot); end = System.currentTimeMillis();
		 * System.out.println("Blob morphology analyzed in " + (end-start) +
		 * " ms."); double phiv, thetav, lv; for (int j = 0; j < 3; j++) { lv =
		 * spot.getFeature(featurelist_sa[j]); phiv =
		 * spot.getFeature(featurelist_phi[j]); thetav =
		 * spot.getFeature(featurelist_theta[j]);
		 * System.out.println(String.format(
		 * "For axis of semi-length %.1f, orientation is phi = %.1f°, theta = %.1f°"
		 * , lv, Math.toDegrees(phiv), Math.toDegrees(thetav))); }
		 * System.out.println(spot.echo());
		 */
	}
}
