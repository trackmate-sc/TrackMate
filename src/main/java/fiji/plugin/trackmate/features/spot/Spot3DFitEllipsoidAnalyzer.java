package fiji.plugin.trackmate.features.spot;

import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.ASPECTRATIO;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.ELLIPSOID_SHAPE;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.MAJOR;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.MAJOR_PHI;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.MAJOR_THETA;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.MEDIAN;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.MEDIAN_PHI;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.MEDIAN_THETA;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.MINOR;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.MINOR_PHI;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.MINOR_THETA;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.SHAPE_CLASS_TOLERANCE;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.SHAPE_ELLIPSOID;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.SHAPE_OBLATE;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.SHAPE_PROLATE;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.SHAPE_SPHERE;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.X0;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.Y0;
import static fiji.plugin.trackmate.features.spot.Spot3DFitEllipsoidAnalyzerFactory.Z0;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.util.mesh.EllipsoidFitter;
import fiji.plugin.trackmate.util.mesh.EllipsoidFitter.EllipsoidFit;
import net.imglib2.RealLocalizable;
import net.imglib2.type.numeric.RealType;

public class Spot3DFitEllipsoidAnalyzer< T extends RealType< T > > extends AbstractSpotFeatureAnalyzer< T >
{

	private final boolean is3D;

	public Spot3DFitEllipsoidAnalyzer( final boolean is3D )
	{
		this.is3D = is3D;
	}

	@Override
	public void process( final Spot spot )
	{
		final double x0;
		final double y0;
		final double z0;
		final double rA;
		final double rB;
		final double rC;
		final double phiA;
		final double thetaA;
		final double phiB;
		final double thetaB;
		final double phiC;
		final double thetaC;
		final double aspectRatio;
		final int shapeIndex;

		if ( is3D )
		{
			final SpotMesh sm = spot.getMesh();
			if ( sm != null )
			{
				final EllipsoidFit fit = EllipsoidFitter.fit( sm.mesh );
				x0 = fit.center.getDoublePosition( 0 );
				y0 = fit.center.getDoublePosition( 1 );
				z0 = fit.center.getDoublePosition( 2 );
				rA = Math.abs( fit.r1 );
				rB = Math.abs( fit.r2 );
				rC = Math.abs( fit.r3 );
				aspectRatio = rA / rC;
				final double drAB = ( rB - rA ) / rB;
				final double drBC = ( rC - rB ) / rC;
				if ( drAB < SHAPE_CLASS_TOLERANCE && drBC < SHAPE_CLASS_TOLERANCE )
					shapeIndex = SHAPE_SPHERE;
				else if ( drBC < SHAPE_CLASS_TOLERANCE )
					shapeIndex = SHAPE_OBLATE;
				else if ( drAB < SHAPE_CLASS_TOLERANCE )
					shapeIndex = SHAPE_PROLATE;
				else
					shapeIndex = SHAPE_ELLIPSOID;

				phiA = phi( fit.ev1 );
				phiB = phi( fit.ev2 );
				phiC = phi( fit.ev3 );
				thetaA = theta( fit.ev1 );
				thetaB = theta( fit.ev2 );
				thetaC = theta( fit.ev3 );

			}
			else
			{ 
				// Assume plain sphere.
				x0 = 0.;
				y0 = 0.;
				z0 = 0.;
				final double radius = spot.getFeature( Spot.RADIUS );
				rA = radius;
				rB = radius;
				rC = radius;
				aspectRatio = 1.;
				shapeIndex = SHAPE_ELLIPSOID;

				phiA = 0.;
				phiB = 0.;
				phiC = 0.;
				thetaA = 0.;
				thetaB = 0.;
				thetaC = 0.;
			}
		}
		else
		{
			// Undefined for 2D: default to NaN.
			x0 = Double.NaN;
			y0 = Double.NaN;
			z0 = Double.NaN;
			rA = Double.NaN;
			rB = Double.NaN;
			rC = Double.NaN;
			aspectRatio = Double.NaN;
			shapeIndex = SHAPE_ELLIPSOID;

			phiA = Double.NaN;
			phiB = Double.NaN;
			phiC = Double.NaN;
			thetaA = Double.NaN;
			thetaB = Double.NaN;
			thetaC = Double.NaN;
		}
		spot.putFeature( X0, x0 );
		spot.putFeature( Y0, y0 );
		spot.putFeature( Z0, z0 );
		spot.putFeature( MINOR, rA );
		spot.putFeature( MEDIAN, rB );
		spot.putFeature( MAJOR, rC );
		spot.putFeature( MINOR_PHI, phiA );
		spot.putFeature( MEDIAN_PHI, phiB );
		spot.putFeature( MAJOR_PHI, phiC );
		spot.putFeature( MINOR_THETA, thetaA );
		spot.putFeature( MEDIAN_THETA, thetaB );
		spot.putFeature( MAJOR_THETA, thetaC );
		spot.putFeature( ASPECTRATIO, aspectRatio );
		spot.putFeature( ELLIPSOID_SHAPE, ( double ) shapeIndex );
	}

	private double theta( final RealLocalizable v )
	{
		final double z = v.getDoublePosition( 2 );
		return Math.acos( z );
	}

	private static final double phi( final RealLocalizable v )
	{
		final double x = v.getDoublePosition( 0 );
		final double y = v.getDoublePosition( 1 );
		return Math.atan2( y, x );
	}
}
