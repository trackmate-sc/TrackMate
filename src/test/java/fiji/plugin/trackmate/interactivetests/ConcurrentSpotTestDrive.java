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
package fiji.plugin.trackmate.interactivetests;

import java.util.Iterator;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.providers.DetectorProvider;
import ij.ImagePlus;
import ij.gui.NewImage;

public class ConcurrentSpotTestDrive
{

	public static void main( final String[] args )
	{

		final int nFrames = 20;

		final Model model = new Model();

		// Create blank image
		final ImagePlus imp = NewImage.createByteImage( "Noise", 200, 200, nFrames, NewImage.FILL_BLACK );

		// Add noise to it
		for ( int i = 0; i < imp.getStackSize(); i++ )
		{
			imp.getStack().getProcessor( i + 1 ).noise( 50 );
		}

		// Setup calibration
		imp.setDimensions( 1, 1, nFrames );

		// Run track mate on it

		// Make settings
		final Settings settings = new Settings( imp );
		final DetectorProvider provider = new DetectorProvider();
		settings.detectorFactory = provider.getFactory( LogDetectorFactory.DETECTOR_KEY );
		settings.detectorSettings = settings.detectorFactory.getDefaultSettings();

		// Execute detection
		final TrackMate trackmate = new TrackMate( model, settings );
		trackmate.execDetection();

		// Retrieve spots
		final SpotCollection spots = trackmate.getModel().getSpots();

		// Parse spots and detect duplicate IDs
		final int[] IDs = new int[ Spot.IDcounter.get() + 1 ];
		final Iterator< Spot > it = spots.iterator( false );
		while ( it.hasNext() )
		{
			final Spot si = it.next();
			final int id = si.ID();
			IDs[ id ]++;
		}

		boolean ok = true;
		for ( int i = 0; i < IDs.length; i++ )
		{
			if ( IDs[ i ] > 1 )
			{
				System.out.println( "Found " + IDs[ i ] + " spots with the same ID = " + i );
				ok = false;
			}
		}
		if ( ok )
		{
			System.out.println( "No duplicate ID found." );
		}

	}

}
