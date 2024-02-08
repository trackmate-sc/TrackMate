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
package fiji.plugin.trackmate.action.meshtools;

import java.awt.Component;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JLabel;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import net.imglib2.algorithm.MultiThreaded;

public class MeshSmootherController implements MultiThreaded
{

	private final Model model;

	private final SelectionModel selectionModel;

	private final MeshSmootherPanel gui;

	private final MeshSmoother smoother;

	private final Logger logger;

	public MeshSmootherController( final Model model, final SelectionModel selectionModel, final Logger logger )
	{
		this.model = model;
		this.selectionModel = selectionModel;
		this.logger = logger;
		this.gui = new MeshSmootherPanel();
		this.smoother = new MeshSmoother( logger );

		gui.btnRun.addActionListener( e -> run( gui.getModel() ) );
		gui.btnUndo.addActionListener( e -> undo() );
	}

	public void show( final Component parent )
	{
		final JFrame frame = new JFrame( "Smoothing params" );
		frame.getContentPane().add( gui );
		frame.setSize( 400, 300 );
		frame.setIconImage( Icons.TRACKMATE_ICON.getImage() );
		GuiUtils.positionWindow( frame, parent );
		frame.setVisible( true );
	}

	private void run( final MeshSmootherModel smootherModel )
	{
		final Iterable< Spot > spots;
		if ( gui.rdbtnAll.isSelected() )
			spots = model.getSpots().iterable( true );
		else
			spots = selectionModel.getSpotSelection();

		new Thread( () -> {
			final EverythingDisablerAndReenabler enabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
			try
			{
				enabler.disable();
				final Collection< Spot > modifiedSpots = smoother.smooth( smootherModel, spots );
				fireEvent( modifiedSpots );
			}
			catch ( final Exception err )
			{
				err.printStackTrace();
			}
			finally
			{
				enabler.reenable();
			}
		}, "TrackMate mesh smoother thread" ).start();
	}

	private void undo()
	{
		new Thread( () -> {
			final EverythingDisablerAndReenabler enabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
			try
			{
				enabler.disable();
				final Collection< Spot > modifiedSpots = smoother.undo();
				fireEvent( modifiedSpots );
			}
			finally
			{
				enabler.reenable();
			}
		}, "TrackMate mesh smoothing undoer thread" ).start();
	}

	private void fireEvent( final Collection< Spot > modifiedSpots )
	{
		logger.log( "Updating spot features and meshes.\n" );
		final ModelChangeEvent event = new ModelChangeEvent( this, ModelChangeEvent.MODEL_MODIFIED );
		event.addAllSpots( modifiedSpots );
		modifiedSpots.forEach( s -> event.putSpotFlag( s, ModelChangeEvent.FLAG_SPOT_MODIFIED ) );
		model.getModelChangeListener().forEach( l -> l.modelChanged( event ) );
		logger.log( "Done.\n" );
	}

	@Override
	public void setNumThreads()
	{
		smoother.setNumThreads();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		smoother.setNumThreads( numThreads );
	}

	@Override
	public int getNumThreads()
	{
		return smoother.getNumThreads();
	}
}
