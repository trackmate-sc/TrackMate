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
package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.Icons.TRACK_SCHEME_ICON;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.swing.handler.mxRubberband;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.TrackNavigator;

public class TrackSchemeFrame extends JFrame
{

	/*
	 * CONSTANTS
	 */

	private static final long serialVersionUID = 1L;

	/*
	 * FIELDS
	 */

	/** The side pane in which spot selection info will be displayed. */
	private InfoPane infoPane;

	private JGraphXAdapter graph;

	private final TrackScheme trackScheme;

	/** The graph component in charge of painting the graph. */
	TrackSchemeGraphComponent graphComponent;

	protected final Logger logger = Logger.IJTOOLBAR_LOGGER;

	private final DisplaySettings displaySettings;

	/*
	 * CONSTRUCTORS
	 */

	public TrackSchemeFrame( final TrackScheme trackScheme, final DisplaySettings displaySettings )
	{
		this.trackScheme = trackScheme;
		this.displaySettings = displaySettings;
		this.graph = trackScheme.getGraph();

		// Frame look
		setIconImage( TRACK_SCHEME_ICON.getImage() );

		// Layout
		getContentPane().setLayout( new BorderLayout() );

		// Add a ToolBar
		getContentPane().add( createToolBar(), BorderLayout.NORTH );
	}

	/*
	 * PUBLIC METHODS
	 */

	public void init( final JGraphXAdapter lGraph )
	{
		this.graph = lGraph;
		// GraphComponent
		graphComponent = createGraphComponent();

		// Add the info pane
		infoPane = new InfoPane( trackScheme.getModel(), trackScheme.getSelectionModel() );

		// Add the graph outline
		final mxGraphOutline graphOutline = new mxGraphOutline( graphComponent );

		final JSplitPane inner = new JSplitPane( JSplitPane.VERTICAL_SPLIT, graphOutline, infoPane );
		inner.setDividerLocation( 120 );
		inner.setMinimumSize( new Dimension( 0, 0 ) );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, inner, graphComponent );
		splitPane.setDividerLocation( 170 );
		getContentPane().add( splitPane, BorderLayout.CENTER );

		final TrackSchemeKeyboardHandler keyboardHandler = new TrackSchemeKeyboardHandler( graphComponent, new TrackNavigator( trackScheme.getModel(), trackScheme.getSelectionModel() ) );
		keyboardHandler.installKeyboardActions( graphComponent );
		keyboardHandler.installKeyboardActions( infoPane );
	}

	/*
	 * Selection management
	 */

	public void centerViewOn( final mxICell cell )
	{
		graphComponent.scrollCellToVisible( cell, true );
	}

	/**
	 * Instantiate the graph component in charge of painting the graph. Hook for
	 * sub-classers.
	 */
	private TrackSchemeGraphComponent createGraphComponent()
	{
		final TrackSchemeGraphComponent gc = new TrackSchemeGraphComponent( graph, trackScheme, displaySettings );
		gc.getVerticalScrollBar().setUnitIncrement( 16 );
		gc.getHorizontalScrollBar().setUnitIncrement( 16 );

		/*
		 * gc.setExportEnabled(true); Seems to be required to have a preview
		 * when we move cells. Also give the ability to export a cell as an
		 * image clipping
		 */
		gc.getConnectionHandler().setEnabled( TrackScheme.DEFAULT_LINKING_ENABLED );
		/*
		 * By default, can be changed in the track scheme toolbar
		 */

		new mxRubberband( gc );

		// Popup menu
		gc.getGraphControl().addMouseListener( new MouseAdapter()
		{
			@Override
			public void mousePressed( final MouseEvent e )
			{
				if ( e.isPopupTrigger() )
				{
					displayPopupMenu( gc.getCellAt( e.getX(), e.getY(), false ), e.getPoint() );
				}
			}

			@Override
			public void mouseReleased( final MouseEvent e )
			{
				if ( e.isPopupTrigger() )
				{
					displayPopupMenu( gc.getCellAt( e.getX(), e.getY(), false ), e.getPoint() );
				}
			}
		} );

		gc.addMouseWheelListener( new MouseWheelListener()
		{

			@Override
			public void mouseWheelMoved( final MouseWheelEvent e )
			{
				if ( gc.isPanningEvent( e ) )
				{
					final boolean in = e.getWheelRotation() < 0;
					if ( in )
						gc.zoomIn();
					else
						gc.zoomOut();
				}
			}
		} );

		gc.setKeepSelectionVisibleOnZoom( true );
		gc.setPanning( true );
		return gc;
	}

	/**
	 * Instantiate the toolbar of the track scheme.
	 */
	private JToolBar createToolBar()
	{
		return new TrackSchemeToolbar( trackScheme );
	}

	/**
	 * PopupMenu
	 */
	private void displayPopupMenu( final Object cell, final Point point )
	{
		final TrackSchemePopupMenu menu = new TrackSchemePopupMenu( trackScheme, cell, point );
		menu.show( graphComponent.getViewport().getView(), ( int ) point.getX(), ( int ) point.getY() );
	}

}
