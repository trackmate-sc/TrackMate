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
package fiji.plugin.trackmate.gui.wizard.descriptors;

import org.scijava.Cancelable;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.components.LogPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;

public class ExecuteDetectionDescriptor extends WizardPanelDescriptor
{

	public static final String KEY = "ExecuteDetection";

	private final TrackMate trackmate;

	public ExecuteDetectionDescriptor( final TrackMate trackmate, final LogPanel logPanel )
	{
		super( KEY );
		this.trackmate = trackmate;
		this.targetPanel = logPanel;
	}

	@Override
	public Runnable getForwardRunnable()
	{
		return () -> {

			// Read ROI from imp NOW.
			final Settings settings = trackmate.getSettings();
			settings.setRoi( settings.imp.getRoi() );
			// Exec detection.
			final long start = System.currentTimeMillis();
			final boolean ok = trackmate.execDetection();
			if ( !ok )
				trackmate.getModel().getLogger().error( trackmate.getErrorMessage() + '\n' );
			final long end = System.currentTimeMillis();
			trackmate.getModel().getLogger().log( String.format( "Detection done in %.1f s.\n", ( end - start ) / 1e3f ) );
		};
	}

	@Override
	public Cancelable getCancelable()
	{
		return trackmate;
	}
}
