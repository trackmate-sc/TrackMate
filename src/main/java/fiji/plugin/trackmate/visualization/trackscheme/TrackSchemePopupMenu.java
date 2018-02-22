package fiji.plugin.trackmate.visualization.trackscheme;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.manual.ManualEdgeColorAnalyzer;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JColorChooser;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.jgrapht.graph.DefaultWeightedEdge;

public class TrackSchemePopupMenu extends JPopupMenu
{

	private static final long serialVersionUID = -1L;

	/**
	 * The cell where the right-click was made, <code>null</code> if the
	 * right-click is made out of a cell.
	 */
	private final Object cell;

	/** The TrackScheme instance. */
	private final TrackScheme trackScheme;

	/** The right-click location. */
	private final Point point;

	private static Color previousColor = Color.RED;

	public TrackSchemePopupMenu( final TrackScheme trackScheme, final Object cell, final Point point )
	{
		this.trackScheme = trackScheme;
		this.cell = cell;
		this.point = point;
		init();
	}

	/*
	 * ACTIONS
	 */

	private void manualColorEdges( final ArrayList< mxCell > edges )
	{
		for ( final mxCell mxCell : edges )
		{
			final DefaultWeightedEdge edge = trackScheme.getGraph().getEdgeFor( mxCell );
			final Double value = Double.valueOf( previousColor.getRGB() );
			trackScheme.getModel().getFeatureModel().putEdgeFeature( edge, ManualEdgeColorAnalyzer.FEATURE, value );
		}
	}

	private void manualColorVertices( final ArrayList< mxCell > vertices )
	{
		for ( final mxCell mxCell : vertices )
		{
			final Spot spot = trackScheme.getGraph().getSpotFor( mxCell );
			final Double value = Double.valueOf( previousColor.getRGB() );
			spot.putFeature( ManualEdgeColorAnalyzer.FEATURE, value );
		}
	}


	private void selectWholeTrack( final ArrayList< mxCell > vertices, final ArrayList< mxCell > edges )
	{
		trackScheme.selectTrack( vertices, edges, 0 );
	}

	private void selectTrackDownwards( final ArrayList< mxCell > vertices, final ArrayList< mxCell > edges )
	{
		trackScheme.selectTrack( vertices, edges, -1 );
	}

	private void selectTrackUpwards( final ArrayList< mxCell > vertices, final ArrayList< mxCell > edges )
	{
		trackScheme.selectTrack( vertices, edges, 1 );
	}

	private void editSpotName()
	{
		trackScheme.getGUI().graphComponent.startEditingAtCell( cell );
	}

	@SuppressWarnings( "unused" )
	private void toggleBranchFolding()
	{
		Object parent;
		if ( trackScheme.getGraph().isCellFoldable( cell, true ) )
		{
			parent = cell;
		}
		else
		{
			parent = trackScheme.getGraph().getModel().getParent( cell );
		}
		trackScheme.getGraph().foldCells( !trackScheme.getGraph().isCellCollapsed( parent ), false, new Object[] { parent } );
	}

	private void multiEditSpotName( final ArrayList< mxCell > vertices, final EventObject triggerEvent )
	{
		/*
		 * We want to display the editing window in the cell that is the closer
		 * to where the user clicked. That is not perfect, because we can
		 * imagine the click is made for from the selected cells, and that the
		 * editing window will not even be displayed on the screen. No idea for
		 * that yet, because JGraphX is expecting to receive a cell as location
		 * for the editing window.
		 */
		final mxCell tc = getClosestCell( vertices );
		vertices.remove( tc );
		final mxGraphComponent graphComponent = trackScheme.getGUI().graphComponent;
		graphComponent.startEditingAtCell( tc, triggerEvent );
		graphComponent.addListener( mxEvent.LABEL_CHANGED, new mxIEventListener()
		{

			@Override
			public void invoke( final Object sender, final mxEventObject evt )
			{
				for ( final mxCell lCell : vertices )
				{
					lCell.setValue( tc.getValue() );
					trackScheme.getGraph().getSpotFor( lCell ).setName( tc.getValue().toString() );
				}
				graphComponent.refresh();
				graphComponent.removeListener( this );
			}
		} );
	}

	/**
	 * Return, from the given list of cell, the one which is the closer to the
	 * {@link #point} of this instance.
	 */
	private mxCell getClosestCell( final Iterable< mxCell > vertices )
	{
		double min_dist = Double.POSITIVE_INFINITY;
		mxCell target_cell = null;
		for ( final mxCell lCell : vertices )
		{
			final Point location = lCell.getGeometry().getPoint();
			final double dist = location.distanceSq( point );
			if ( dist < min_dist )
			{
				min_dist = dist;
				target_cell = lCell;
			}
		}
		return target_cell;
	}

	private void linkSpots()
	{
		trackScheme.linkSpots();
	}

	private void remove()
	{
		trackScheme.removeSelectedCells();
	}

	/*
	 * MENU COMPOSITION
	 */

	@SuppressWarnings( "serial" )
	private void init()
	{

		// Build selection categories
		final Object[] selection = trackScheme.getGraph().getSelectionCells();
		final ArrayList< mxCell > vertices = new ArrayList< >();
		final ArrayList< mxCell > edges = new ArrayList< >();
		for ( final Object obj : selection )
		{
			final mxCell lCell = ( mxCell ) obj;
			if ( lCell.isVertex() )
				vertices.add( lCell );
			else if ( lCell.isEdge() )
				edges.add( lCell );
		}

		// Select whole tracks
		if ( vertices.size() > 0 || edges.size() > 0 )
		{

			add( new AbstractAction( "Select whole track" )
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					selectWholeTrack( vertices, edges );
				}
			} );

			add( new AbstractAction( "Select track downwards" )
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					selectTrackDownwards( vertices, edges );
				}
			} );

			add( new AbstractAction( "Select track upwards" )
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					selectTrackUpwards( vertices, edges );
				}
			} );
		}

		if ( cell != null )
		{
			// Edit
			add( new AbstractAction( "Edit spot name" )
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					editSpotName();
				}
			} );

		}
		else
		{

			if ( vertices.size() > 1 )
			{

				// Multi edit
				add( new AbstractAction( "Edit " + vertices.size() + " spot names" )
				{
					@Override
					public void actionPerformed( final ActionEvent e )
					{
						multiEditSpotName( vertices, e );
					}
				} );
			}

			// Link
			final Action linkAction = new AbstractAction( "Link " + trackScheme.getSelectionModel().getSpotSelection().size() + " spots" )
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					linkSpots();
				}
			};
			if ( trackScheme.getSelectionModel().getSpotSelection().size() > 1 )
			{
				add( linkAction );
			}
		}

		/*
		 * Edges and spot manual coloring
		 */

		if ( edges.size() > 0 || vertices.size() > 0 )
		{
			addSeparator();
		}

		if ( vertices.size() > 0 )
		{
			final String str = "Manual color for " + ( vertices.size() == 1 ? " one spot" : vertices.size() + " spots" );
			add( new AbstractAction( str )
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					previousColor = JColorChooser.showDialog( trackScheme.getGUI(), "Choose Color", previousColor );
					manualColorVertices( vertices );
					SwingUtilities.invokeLater( new Runnable()
					{
						@Override
						public void run()
						{
							trackScheme.doTrackStyle();
						}
					} );
				}
			} );
		}

		if ( edges.size() > 0 )
		{
			final String str = "Manual color for " + ( edges.size() == 1 ? " one edge" : edges.size() + " edges" );
			add( new AbstractAction( str )
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					previousColor = JColorChooser.showDialog( trackScheme.getGUI(), "Choose Color", previousColor );
					manualColorEdges( edges );
					SwingUtilities.invokeLater( new Runnable()
					{
						@Override
						public void run()
						{
							trackScheme.doTrackStyle();
						}
					} );
				}
			} );
		}


		if ( edges.size() > 0 && vertices.size() > 0 )
		{
			final String str = "Manual color for " + ( vertices.size() == 1 ? " one spot and " : vertices.size() + " spots and " ) + ( edges.size() == 1 ? " one edge" : edges.size() + " edges" );
			add( new AbstractAction( str )
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					previousColor = JColorChooser.showDialog( trackScheme.getGUI(), "Choose Color", previousColor );
					manualColorVertices( vertices );
					manualColorEdges( edges );
					SwingUtilities.invokeLater( new Runnable()
					{
						@Override
						public void run()
						{
							trackScheme.doTrackStyle();
						}
					} );
				}
			} );
		}


		// Remove
		if ( selection.length > 0 )
		{
			addSeparator();
			final Action removeAction = new AbstractAction( "Remove spots and links" )
			{
				@Override
				public void actionPerformed( final ActionEvent e )
				{
					remove();
				}
			};
			add( removeAction );
		}
	}

}
