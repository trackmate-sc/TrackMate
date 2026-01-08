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
package fiji.plugin.trackmate.features.spot;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import net.imglib2.mesh.MeshStats;
import net.imglib2.mesh.alg.hull.ConvexHull;
import net.imglib2.mesh.impl.naive.NaiveDoubleMesh;
import net.imglib2.type.numeric.RealType;

public class Spot3DShapeAnalyzer< T extends RealType< T > > extends AbstractSpotFeatureAnalyzer< T >
{

	private final boolean is3D;

	public Spot3DShapeAnalyzer( final boolean is3D )
	{
		this.is3D = is3D;
	}

	@Override
	public void process( final Spot spot )
	{
		double volume;
		double sa;
		double solidity;
		double convexity;
		double sphericity;
		if ( is3D )
		{
			if ( spot instanceof SpotMesh )
			{
				final SpotMesh sm = ( SpotMesh ) spot;
				final NaiveDoubleMesh ch = ConvexHull.calculate( sm.getMesh() );
				volume = sm.volume();
				final double volumeCH = MeshStats.volume( ch );
				solidity = volume / volumeCH;

				sa = MeshStats.surfaceArea( sm.getMesh() );
				final double saCH = MeshStats.surfaceArea( ch );
				convexity = sa / saCH;

				final double sphereArea = Math.pow( Math.PI, 1. / 3. )
						* Math.pow( 6. * volume, 2. / 3. );
				sphericity = sphereArea / sa;
			}
			else
			{
				final double radius = spot.getFeature( Spot.RADIUS );
				volume = 4. / 3. * Math.PI * radius * radius * radius;
				sa = 4. * Math.PI * radius * radius;
				solidity = 1.;
				convexity = 1.;
				sphericity = 1.;
			}
		}
		else
		{
			volume = Double.NaN;
			sa = Double.NaN;
			solidity = Double.NaN;
			convexity = Double.NaN;
			sphericity = Double.NaN;
		}
		spot.putFeature( Spot3DShapeAnalyzerFactory.VOLUME, volume );
		spot.putFeature( Spot3DShapeAnalyzerFactory.SURFACE_AREA, sa );
		spot.putFeature( Spot3DShapeAnalyzerFactory.SPHERICITY, sphericity );
		spot.putFeature( Spot3DShapeAnalyzerFactory.SOLIDITY, solidity );
		spot.putFeature( Spot3DShapeAnalyzerFactory.CONVEXITY, convexity );
	}
}
