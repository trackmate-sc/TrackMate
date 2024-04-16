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
package fiji.plugin.trackmate;

import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.WizardSequence;
import fiji.plugin.trackmate.gui.wizard.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.tracking.manual.ManualTrackerFactory;
import ij.ImageJ;
import ij.ImagePlus;

public class ManualTrackingPlugIn extends TrackMatePlugIn
{

	@Override
	protected WizardSequence createSequence( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		final WizardSequence sequence = super.createSequence( trackmate, selectionModel, displaySettings );
		sequence.setCurrent( ConfigureViewsDescriptor.KEY );
		return sequence;
	}

	@SuppressWarnings( "rawtypes" )
	@Override
	protected Settings createSettings( final ImagePlus imp )
	{
		final Settings lSettings = super.createSettings( imp );
		// Manual detection
		lSettings.detectorFactory = new ManualDetectorFactory();
		lSettings.detectorSettings = lSettings.detectorFactory.getDefaultSettings();
		// Manual tracker
		lSettings.trackerFactory = new ManualTrackerFactory();
		lSettings.trackerSettings = lSettings.trackerFactory.getDefaultSettings();
		return lSettings;
	}

	@Override
	protected TrackMate createTrackMate( final Model model, final Settings settings )
	{
		final TrackMate trackmate = super.createTrackMate( model, settings );
		// Trigger computation of features so that they analyzers are declared
		// in the model.
		trackmate.computeSpotFeatures( false );
		trackmate.computeEdgeFeatures( false );
		trackmate.computeTrackFeatures( false );
		return trackmate;
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new ManualTrackingPlugIn().run( "samples/Merged.tif" );
	}
}
