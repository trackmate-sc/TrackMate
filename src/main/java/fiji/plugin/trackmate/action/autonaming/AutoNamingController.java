/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.action.autonaming;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JLabel;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import fiji.plugin.trackmate.util.Threads;
import ij.ImagePlus;

public class AutoNamingController
{

	private final AutoNamingPanel gui;

	private final Logger logger;

	private final GuiModel guiModel;

	public AutoNamingController( final GuiModel guiModel, final Logger logger )
	{
		this.guiModel = guiModel;
		this.logger = logger;

		final Collection< AutoNamingRule > namingRules = new ArrayList<>( 3 );
		namingRules.add( new CopyTrackNameNamingRule() );
		namingRules.add( new DefaultAutoNamingRule( ".", "", false ) );
		namingRules.add( new DefaultAutoNamingRule( ".", "", true ) );

		this.gui = new AutoNamingPanel( namingRules );

		gui.btnRun.addActionListener( e -> run( ( ( AutoNamingRule ) gui.cmbboxRule.getSelectedItem() ) ) );
	}

	private void run( final AutoNamingRule autoNaming )
	{
		final Model model = guiModel.getModel();
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
		disabler.disable();
		Threads.run( "TrackMateAutoNamingThread", () ->
		{
			try
			{
				logger.log( "Applying naming rule: " + autoNaming.toString() + ".\n" );
				logger.setStatus( "Spot auto-naming" );
				AutoNamingPerformer.autoNameSpots( model, autoNaming );
				model.notifyFeaturesComputed();
				logger.log( "Spot auto-naming done.\n" );
			}
			finally
			{
				disabler.reenable();
			}
		} );
	}

	public void show()
	{
		if ( gui.getParent() != null && gui.getParent().isVisible() )
			return;

		final JFrame frame = new JFrame( "Spot auto-naming" );
		frame.setIconImage( Icons.TRACK_SCHEME_ICON.getImage() );
		frame.setSize( 500, 400 );
		frame.getContentPane().add( gui );
		final ImagePlus imp = guiModel.getSettings().imp;
		if ( imp != null )
			GuiUtils.positionWindow( frame, imp.getCanvas() );
		else
			frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}
}
