package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxStyleUtils;
import com.mxgraph.view.mxGraphSelectionModel;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateOptionUtils;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import ij.ImagePlus;

public class TrackScheme extends AbstractTrackMateModelView
{

	/*
	 * CONSTANTS
	 */
	private static final boolean DEBUG = false;

	private static final boolean DEBUG_SELECTION = false;

	public static final String INFO_TEXT = "<html>"
			+ "TrackScheme displays the tracking results as track lanes, <br>"
			+ "ignoring the spot actual position. "
			+ "<p>" + "Tracks can be edited through link creation and removal."
			+ "</html>";

	static final int Y_COLUMN_SIZE = 96;

	static final int X_COLUMN_SIZE = 160;

	static final int DEFAULT_CELL_WIDTH = 128;

	static final int DEFAULT_CELL_HEIGHT = 40;

	public static final ImageIcon TRACK_SCHEME_ICON = new ImageIcon( TrackSchemeFrame.class.getResource( "resources/Icon3a_print_transparency.png" ) );

	public static final ImageIcon TRACK_SCHEME_ICON_16x16;

	static
	{
		final Image image = TRACK_SCHEME_ICON.getImage();
		final Image newimg = image.getScaledInstance( 16, 16, java.awt.Image.SCALE_SMOOTH );
		TRACK_SCHEME_ICON_16x16 = new ImageIcon( newimg ); // transform it back
	}

	public static final String DEFAULT_COLOR = "#FF00FF";

	private static final Dimension DEFAULT_SIZE = new Dimension( 800, 600 );

	static final int TABLE_CELL_WIDTH = 40;

	static final Color GRID_COLOR = Color.GRAY;

	/**
	 * Are linking costs displayed by default? Can be changed in the toolbar.
	 */
	static final boolean DEFAULT_DO_DISPLAY_COSTS_ON_EDGES = false;

	/** Do we display the background decorations by default? */
	static final int DEFAULT_PAINT_DECORATION_LEVEL = 1;

	/** Do we toggle linking mode by default? */
	static final boolean DEFAULT_LINKING_ENABLED = false;

	/** Do we capture thumbnails by default? */
	static final boolean DEFAULT_THUMBNAILS_ENABLED = false;

	public static final String KEY = "TRACKSCHEME";

	/*
	 * FIELDS
	 */

	/** The frame in which we display the TrackScheme GUI. */
	private TrackSchemeFrame gui;

	/** The JGraphX object that displays the graph. */
	private JGraphXAdapter graph;

	/** The graph layout in charge of re-aligning the cells. */
	private TrackSchemeGraphLayout graphLayout;

	/**
	 * A flag used to prevent double event firing when setting the selection
	 * programmatically.
	 */
	private boolean doFireSelectionChangeEvent = true;

	/**
	 * A flag used to prevent double event firing when setting the selection
	 * programmatically.
	 */
	private boolean doFireModelChangeEvent = true;

	/**
	 * The current row length for each frame. That is, for frame <code>i</code>,
	 * the number of cells on the row corresponding to frame <code>i</code> is
	 * <code>rowLength.get(i)</code>.
	 */
	private Map< Integer, Integer > rowLengths = new HashMap<>();

	/**
	 * Stores the column index that is the first one after all the track
	 * columns.
	 */
	private int unlaidSpotColumn = 2;

	/**
	 * The instance in charge of generating the string image representation of
	 * spots imported in this view. If <code>null</code>, nothing is done.
	 */
	private SpotImageUpdater spotImageUpdater;

	TrackSchemeStylist stylist;

	/**
	 * If <code>true</code>, thumbnail will be captured and displayed with
	 * styles allowing it.
	 */
	private boolean doThumbnailCapture = DEFAULT_THUMBNAILS_ENABLED;

	/*
	 * CONSTRUCTORS
	 */

	public TrackScheme( final Model model, final SelectionModel selectionModel )
	{
		super( model, selectionModel );
		initDisplaySettings();
		initGUI();
	}

	/*
	 * METHODS
	 */

	public void setSpotImageUpdater( final SpotImageUpdater spotImageUpdater )
	{
		this.spotImageUpdater = spotImageUpdater;
	}

	public SelectionModel getSelectionModel()
	{
		return selectionModel;
	}

	/**
	 * @return the column index that is the first one after all the track
	 *         columns.
	 */
	public int getUnlaidSpotColumn()
	{
		return unlaidSpotColumn;
	}

	/**
	 * @return the first free column for the target row.
	 */
	public int getNextFreeColumn( final int frame )
	{
		Integer columnIndex = rowLengths.get( frame );
		if ( null == columnIndex )
		{
			columnIndex = 2;
		}
		return columnIndex + 1;
	}

	/**
	 * Returns the GUI frame controlled by this class.
	 */
	public TrackSchemeFrame getGUI()
	{
		return gui;
	}

	/**
	 * Returns the {@link JGraphXAdapter} that serves as a model for the graph
	 * displayed in this frame.
	 */
	public JGraphXAdapter getGraph()
	{
		return graph;
	}

	/**
	 * Returns the graph layout in charge of arranging the cells on the graph.
	 */
	public TrackSchemeGraphLayout getGraphLayout()
	{
		return graphLayout;
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Used to instantiate and configure the {@link JGraphXAdapter} that will be
	 * used for display.
	 */
	private JGraphXAdapter createGraph()
	{
		gui.logger.setStatus( "Creating graph adapter." );

		final JGraphXAdapter lGraph = new JGraphXAdapter( model );
		lGraph.setAllowLoops( false );
		lGraph.setAllowDanglingEdges( false );
		lGraph.setCellsCloneable( false );
		lGraph.setCellsSelectable( true );
		lGraph.setCellsDisconnectable( false );
		lGraph.setCellsMovable( true );
		lGraph.setGridEnabled( false );
		lGraph.setLabelsVisible( true );
		lGraph.setDropEnabled( false );

		// Cells removed from JGraphX
		lGraph.addListener( mxEvent.CELLS_REMOVED, new CellRemovalListener() );

		// Cell selection change
		lGraph.getSelectionModel().addListener( mxEvent.CHANGE, new SelectionChangeListener() );

		// Return graph
		return lGraph;
	}

	/**
	 * Updates or creates a cell for the target spot. Is called after the user
	 * modified a spot (location, radius, ...) somewhere else.
	 *
	 * @param spot
	 *            the spot that was modified.
	 */
	private mxICell updateCellOf( final Spot spot )
	{

		mxICell cell = graph.getCellFor( spot );
		graph.getModel().beginUpdate();
		try
		{
			if ( DEBUG )
			{
				System.out.println( "[TrackScheme] modelChanged: updating cell for spot " + spot );
			}
			if ( null == cell )
			{
				/*
				 * mxCell not present in graph. Most likely because the
				 * corresponding spot belonged to an invisible track, and a cell
				 * was not created for it when TrackScheme was launched. So we
				 * create one on the fly now.
				 */
				final int row = getUnlaidSpotColumn();
				cell = insertSpotInGraph( spot, row );
				final int frame = spot.getFeature( Spot.FRAME ).intValue();
				rowLengths.put( frame, row + 1 );
			}

			// Update cell look
			if ( spotImageUpdater != null && doThumbnailCapture )
			{
				String style = cell.getStyle();
				final double radiusFactor = ( Double ) displaySettings.get( KEY_SPOT_RADIUS_RATIO );
				final String imageStr = spotImageUpdater.getImageString( spot, radiusFactor );
				style = mxStyleUtils.setStyle( style, mxConstants.STYLE_IMAGE, "data:image/base64," + imageStr );
				graph.getModel().setStyle( cell, style );
			}
		}
		finally
		{
			graph.getModel().endUpdate();
		}
		return cell;
	}

	/**
	 * Insert a spot in the {@link TrackSchemeFrame}, by creating a
	 * {@link mxCell} in the graph model of this frame and position it according
	 * to its feature.
	 */
	private mxICell insertSpotInGraph( final Spot spot, final int targetColumn )
	{
		mxICell cellAdded = graph.getCellFor( spot );
		if ( cellAdded != null )
		{
			// cell for spot already exist, do nothing and return original spot
			return cellAdded;
		}
		// Instantiate JGraphX cell
		cellAdded = graph.addJGraphTVertex( spot );
		// Position it
		final int row = spot.getFeature( Spot.FRAME ).intValue();
		final double x = ( targetColumn - 1 ) * X_COLUMN_SIZE - DEFAULT_CELL_WIDTH / 2;
		final double y = ( 0.5 + row ) * Y_COLUMN_SIZE - DEFAULT_CELL_HEIGHT / 2;
		final mxGeometry geometry = new mxGeometry( x, y, DEFAULT_CELL_WIDTH, DEFAULT_CELL_HEIGHT );
		cellAdded.setGeometry( geometry );
		// Set its style
		final double radiusFactor = ( Double ) displaySettings.get( KEY_SPOT_RADIUS_RATIO );
		if ( null != spotImageUpdater && doThumbnailCapture )
		{
			final String imageStr = spotImageUpdater.getImageString( spot, radiusFactor );
			graph.getModel().setStyle( cellAdded, mxConstants.STYLE_IMAGE + "=" + "data:image/base64," + imageStr );
		}
		return cellAdded;
	}

	/**
	 * Import a whole track from the {@link Model} and make it visible.
	 *
	 * @param trackIndex
	 *            the index of the track to show in TrackScheme
	 */
	private void importTrack( final int trackIndex )
	{
		model.beginUpdate();
		graph.getModel().beginUpdate();
		try
		{
			// Flag original track as visible
			model.setTrackVisibility( trackIndex, true );
			// Find adequate column
			final int targetColumn = getUnlaidSpotColumn();
			// Create cells for track
			final Set< Spot > trackSpots = model.getTrackModel().trackSpots( trackIndex );
			for ( final Spot trackSpot : trackSpots )
			{
				final int frame = trackSpot.getFeature( Spot.FRAME ).intValue();
				final int column = Math.max( targetColumn, getNextFreeColumn( frame ) );
				insertSpotInGraph( trackSpot, column );
				rowLengths.put( frame, column );
			}
			final Set< DefaultWeightedEdge > trackEdges = model.getTrackModel().trackEdges( trackIndex );
			for ( final DefaultWeightedEdge trackEdge : trackEdges )
			{
				graph.addJGraphTEdge( trackEdge );
			}
		}
		finally
		{
			model.endUpdate();
			graph.getModel().endUpdate();
		}
	}

	/**
	 * This method is called when the user has created manually an edge in the
	 * graph, by dragging a link between two spot cells. It checks whether the
	 * matching edge in the model exists, and tune what should be done
	 * accordingly.
	 *
	 * @param cell
	 *            the mxCell of the edge that has been manually created.
	 */
	protected void addEdgeManually( mxCell cell )
	{
		if ( cell.isEdge() )
		{
			final mxIGraphModel graphModel = graph.getModel();
			cell.setValue( "New" );
			model.beginUpdate();
			graphModel.beginUpdate();
			try
			{

				Spot source = graph.getSpotFor( cell.getSource() );
				Spot target = graph.getSpotFor( cell.getTarget() );

				if ( DEBUG )
				{
					System.out.println( "[TrackScheme] #addEdgeManually: edge is between 2 spots belonging to the same frame. Removing it." );
					System.out.println( "[TrackScheme] #addEdgeManually: adding edge between source " + source + " at frame " + source.getFeature( Spot.FRAME ).intValue() + " and target " + target + " at frame " + target.getFeature( Spot.FRAME ).intValue() );
				}

				if ( Spot.frameComparator.compare( source, target ) == 0 )
				{
					// Prevent adding edges between spots that belong to the
					// same frame

					if ( DEBUG )
					{
						System.out.println( "[TrackScheme] addEdgeManually: edge is between 2 spots belonging to the same frame. Removing it." );
					}
					graph.removeCells( new Object[] { cell } );

				}
				else
				{
					// We can add it to the model

					// Put them right in order: since we use a oriented graph,
					// we want the source spot to precede in time.
					if ( Spot.frameComparator.compare( source, target ) > 0 )
					{

						if ( DEBUG )
						{
							System.out.println( "[TrackScheme] #addEdgeManually: Source " + source + " succeed target " + target + ". Inverting edge direction." );
						}

						final Spot tmp = source;
						source = target;
						target = tmp;
					}
					// We add a new jGraphT edge to the underlying model, if it
					// does not exist yet.
					DefaultWeightedEdge edge = model.getTrackModel().getEdge( source, target );
					if ( null == edge )
					{
						edge = model.addEdge( source, target, -1 );
						if ( DEBUG )
						{
							System.out.println( "[TrackScheme] #addEdgeManually: Creating new edge: " + edge + "." );
						}
					}
					else
					{
						// Ah. There was an existing edge in the model we were
						// trying to re-add there, from the graph.
						// We remove the graph edge we have added,
						if ( DEBUG )
						{
							System.out.println( "[TrackScheme] #addEdgeManually: Edge pre-existed. Retrieve it." );
						}
						graph.removeCells( new Object[] { cell } );
						// And re-create a graph edge from the model edge.
						cell = graph.addJGraphTEdge( edge );
						cell.setValue( String.format( "%.1f", model.getTrackModel().getEdgeWeight( edge ) ) );
						// We also need now to check if the edge belonged to a
						// visible track. If not,
						// we make it visible.
						final int ID = model.getTrackModel().trackIDOf( edge );
						// This will work, because track indices will be
						// reprocessed only after the graphModel.endUpdate()
						// reaches 0. So now, it's like we are dealing with the
						// track indices priori to modification.
						if ( model.getTrackModel().isVisible( ID ) )
						{
							if ( DEBUG )
							{
								System.out.println( "[TrackScheme] #addEdgeManually: Track was visible. Do nothing." );
							}
						}
						else
						{
							if ( DEBUG )
							{
								System.out.println( "[TrackScheme] #addEdgeManually: Track was invisible. Make it visible." );
							}
							importTrack( ID );
						}
					}
					graph.mapEdgeToCell( edge, cell );
				}

			}
			finally
			{
				graphModel.endUpdate();
				model.endUpdate();
				selectionModel.clearEdgeSelection();
			}
		}
	}

	/*
	 * OVERRIDEN METHODS
	 */

	@Override
	public void selectionChanged( final SelectionChangeEvent event )
	{
		if ( DEBUG_SELECTION )
		{
			System.out.println( "[TrackSchemeFrame] selectionChanged: received event " + event.hashCode() + " from " + event.getSource() + ". Fire flag is " + doFireSelectionChangeEvent );
		}
		if ( !doFireSelectionChangeEvent )
		{ return; }
		doFireSelectionChangeEvent = false;

		{
			final ArrayList< Object > newSelection = new ArrayList<>( selectionModel.getSpotSelection().size() + selectionModel.getEdgeSelection().size() );
			final Iterator< DefaultWeightedEdge > edgeIt = selectionModel.getEdgeSelection().iterator();
			while ( edgeIt.hasNext() )
			{
				final mxICell cell = graph.getCellFor( edgeIt.next() );
				if ( null != cell )
				{
					newSelection.add( cell );
				}
			}
			final Iterator< Spot > spotIt = selectionModel.getSpotSelection().iterator();
			while ( spotIt.hasNext() )
			{
				final mxICell cell = graph.getCellFor( spotIt.next() );
				if ( null != cell )
				{
					newSelection.add( cell );
				}
			}
			final mxGraphSelectionModel mGSmodel = graph.getSelectionModel();
			mGSmodel.setCells( newSelection.toArray() );
		}

		// Center on selection if we added one spot exactly
		final Map< Spot, Boolean > spotsAdded = event.getSpots();
		if ( spotsAdded != null && spotsAdded.size() == 1 )
		{
			final boolean added = spotsAdded.values().iterator().next();
			if ( added )
			{
				final Spot spot = spotsAdded.keySet().iterator().next();
				centerViewOn( spot );
			}
		}
		doFireSelectionChangeEvent = true;
	}

	@Override
	public void centerViewOn( final Spot spot )
	{
		gui.centerViewOn( graph.getCellFor( spot ) );
	}

	/**
	 * Used to catch spot creation events that occurred elsewhere, for instance
	 * by manual editing in the {@link AbstractTrackMateModelView}.
	 * <p>
	 * We have to deal with the graph modification ourselves here, because the
	 * {@link Model} model holds a non-listenable JGraphT instance. A
	 * modification made to the model would not be reflected on the graph here.
	 */
	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		// Only catch model changes
		if ( event.getEventID() != ModelChangeEvent.MODEL_MODIFIED )
		{ return; }

		graph.getModel().beginUpdate();
		try
		{
			final ArrayList< mxICell > cellsToRemove = new ArrayList<>();

			final int targetColumn = getUnlaidSpotColumn();

			// Deal with spots
			if ( !event.getSpots().isEmpty() )
			{

				final Collection< mxCell > spotsWithStyleToUpdate = new HashSet<>();

				for ( final Spot spot : event.getSpots() )
				{

					if ( event.getSpotFlag( spot ) == ModelChangeEvent.FLAG_SPOT_ADDED )
					{

						final int frame = spot.getFeature( Spot.FRAME ).intValue();
						// Put in the graph
						final int column = Math.max( targetColumn, getNextFreeColumn( frame ) );
						final mxICell newCell = insertSpotInGraph( spot, column );
						rowLengths.put( frame, column );
						spotsWithStyleToUpdate.add( ( mxCell ) newCell );

					}
					else if ( event.getSpotFlag( spot ) == ModelChangeEvent.FLAG_SPOT_MODIFIED )
					{

						// Change the look of the cell
						final mxICell cell = updateCellOf( spot );
						spotsWithStyleToUpdate.add( ( mxCell ) cell );

					}
					else if ( event.getSpotFlag( spot ) == ModelChangeEvent.FLAG_SPOT_REMOVED )
					{

						final mxICell cell = graph.getCellFor( spot );
						cellsToRemove.add( cell );

					}
				}
				graph.removeCells( cellsToRemove.toArray(), true );
				stylist.updateVertexStyle( spotsWithStyleToUpdate );
			}

		}
		finally
		{
			graph.getModel().endUpdate();
		}

		// Deal with edges
		if ( !event.getEdges().isEmpty() )
		{

			graph.getModel().beginUpdate();
			try
			{

				if ( event.getEdges().size() > 0 )
				{

					/*
					 * Here we keep track of the spot and edge cells which style
					 * we need to update.
					 */
					final Map< Integer, Set< mxCell > > edgesToUpdate = new HashMap<>();
					final Collection< mxCell > spotsWithStyleToUpdate = new HashSet<>();

					for ( final DefaultWeightedEdge edge : event.getEdges() )
					{

						if ( event.getEdgeFlag( edge ) == ModelChangeEvent.FLAG_EDGE_ADDED )
						{

							mxCell edgeCell = graph.getCellFor( edge );
							if ( null == edgeCell )
							{

								// Make sure target & source cells exist
								final Spot source = model.getTrackModel().getEdgeSource( edge );
								final mxCell sourceCell = graph.getCellFor( source );
								final Spot target = model.getTrackModel().getEdgeTarget( edge );
								final mxCell targetCell = graph.getCellFor( target );

								if ( sourceCell == null || targetCell == null )
								{
									/*
									 * Is this missing cell missing because it
									 * belongs to an invisible track? We then
									 * have to import all the spot and edges.
									 */
									final Integer trackID = model.getTrackModel().trackIDOf( edge );
									final Set< Spot > trackSpots = model.getTrackModel().trackSpots( trackID );
									for ( final Spot trackSpot : trackSpots )
									{
										final mxCell spotCell = graph.getCellFor( trackSpot );
										if ( spotCell == null )
										{
											final int frame = trackSpot.getFeature( Spot.FRAME ).intValue();
											// Put in the graph
											final int targetColumn = getUnlaidSpotColumn();
											final int column = Math.max( targetColumn, getNextFreeColumn( frame ) );
											// move in right+1 free column
											final mxCell spotCellAdded = ( mxCell ) insertSpotInGraph( trackSpot, column );
											rowLengths.put( frame, column );
											spotsWithStyleToUpdate.add( spotCellAdded );
										}
									}

									final Set< DefaultWeightedEdge > trackEdges = model.getTrackModel().trackEdges( trackID );
									// Keep track of edges which style must be
									// updated.
									Set< mxCell > edgeSet = edgesToUpdate.get( trackID );
									if ( edgeSet == null )
									{
										edgeSet = new HashSet<>();
										edgesToUpdate.put( trackID, edgeSet );
									}

									// Loop over edges. Those who do not have a
									// cell get a cell.
									for ( final DefaultWeightedEdge trackEdge : trackEdges )
									{
										mxCell edgeCellToAdd = graph.getCellFor( trackEdge );
										if ( null == edgeCellToAdd )
										{
											edgeCellToAdd = graph.addJGraphTEdge( trackEdge );
											graph.getModel().add( graph.getDefaultParent(), edgeCellToAdd, 0 );
											edgeSet.add( edgeCellToAdd );
										}
									}

								}

								// And finally create the edge cell
								edgeCell = graph.addJGraphTEdge( edge );
							}

							graph.getModel().add( graph.getDefaultParent(), edgeCell, 0 );

							// Add it to the map of cells to recolor
							final Integer trackID = model.getTrackModel().trackIDOf( edge );
							Set< mxCell > edgeSet = edgesToUpdate.get( trackID );
							if ( edgeSet == null )
							{
								edgeSet = new HashSet<>();
								edgesToUpdate.put( trackID, edgeSet );
							}
							edgeSet.add( edgeCell );

						}
						else if ( event.getEdgeFlag( edge ) == ModelChangeEvent.FLAG_EDGE_MODIFIED )
						{
							// Add it to the map of cells to recolor
							final Integer trackID = model.getTrackModel().trackIDOf( edge );
							Set< mxCell > edgeSet = edgesToUpdate.get( trackID );
							if ( edgesToUpdate.get( trackID ) == null )
							{
								edgeSet = new HashSet<>();
								edgesToUpdate.put( trackID, edgeSet );
							}
							edgeSet.add( graph.getCellFor( edge ) );

						}
						else if ( event.getEdgeFlag( edge ) == ModelChangeEvent.FLAG_EDGE_REMOVED )
						{

							final mxCell cell = graph.getCellFor( edge );
							graph.removeCells( new Object[] { cell } );
						}
					}

					stylist.execute( edgesToUpdate );
					stylist.updateVertexStyle( spotsWithStyleToUpdate );
					SwingUtilities.invokeLater( new Runnable()
					{
						@Override
						public void run()
						{
							gui.graphComponent.refresh();
							gui.graphComponent.repaint();
						}
					} );

				}
			}
			finally
			{
				graph.getModel().endUpdate();
			}
		}
	}

	@Override
	public Map< String, Object > getDisplaySettings()
	{
		return displaySettings;
	}

	@Override
	public void setDisplaySettings( final String key, final Object value )
	{
		if ( key == KEY_TRACK_COLORING )
		{
			if ( null != stylist )
			{
				// pass the new one to the track overlay - we ignore its spot
				// coloring and keep the spot coloring
				final TrackColorGenerator colorGenerator = ( TrackColorGenerator ) value;
				stylist.setColorGenerator( colorGenerator );
				doTrackStyle();
				refresh();
			}
		}
		displaySettings.put( key, value );
	}

	@Override
	public Object getDisplaySettings( final String key )
	{
		return displaySettings.get( key );
	}

	@Override
	public void render()
	{
		final long start = System.currentTimeMillis();
		// Graph to mirror model
		this.graph = createGraph();
		gui.logger.setProgress( 0.5 );

		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				// Pass graph to GUI
				gui.logger.setStatus( "Generating GUI components." );
				gui.init( graph );

				// Init functions that set look and position
				gui.logger.setStatus( "Creating style manager." );
				TrackScheme.this.stylist = new TrackSchemeStylist( graph, ( TrackColorGenerator ) displaySettings.get( KEY_TRACK_COLORING ) );
				gui.logger.setStatus( "Creating layout manager." );
				TrackScheme.this.graphLayout = new TrackSchemeGraphLayout( graph, model, gui.graphComponent );

				// Execute style and layout
				gui.logger.setProgress( 0.75 );
				doTrackStyle();

				gui.logger.setStatus( "Executing layout." );
				doTrackLayout();

				gui.logger.setProgress( 0.9 );

				gui.logger.setStatus( "Refreshing display." );
				gui.graphComponent.refresh();
				final mxRectangle bounds = graph.getView().validateCellState( graph.getDefaultParent(), false );
				if ( null == bounds )
				{ // This happens when there is not track to display
					return;
				}
				final Dimension dim = new Dimension();
				dim.setSize( bounds.getRectangle().width + bounds.getRectangle().x, bounds.getRectangle().height + bounds.getRectangle().y );
				gui.graphComponent.getGraphControl().setPreferredSize( dim );
				gui.logger.setStatus( "" );

				gui.logger.setProgress( 0 );
				final long end = System.currentTimeMillis();
				gui.logger.log( String.format( "Rendering done in %.1f s.", ( end - start ) / 1000d ) );
			}
		} );
	}

	@Override
	public void refresh()
	{}

	@Override
	public void clear()
	{
		System.out.println( "[TrackScheme] clear() called" );
	}

	@Override
	public Model getModel()
	{
		return model;
	}

	/*
	 * PROTECTED METHODS
	 */

	protected void initDisplaySettings()
	{
		displaySettings.put( KEY_SPOTS_VISIBLE, true );
		displaySettings.put( KEY_DISPLAY_SPOT_NAMES, false );
		// displaySettings.put(KEY_SPOT_COLORING, new
		// SpotColorGenerator(model));
		// displaySettings.put(KEY_TRACK_COLORING, new
		// PerTrackFeatureColorGenerator(model,
		// TrackIndexAnalyzer.TRACK_INDEX));
		displaySettings.put( KEY_SPOT_RADIUS_RATIO, 1.0d );
		displaySettings.put( KEY_TRACKS_VISIBLE, true );
		displaySettings.put( KEY_TRACK_DISPLAY_MODE, DEFAULT_TRACK_DISPLAY_MODE );
		displaySettings.put( KEY_TRACK_DISPLAY_DEPTH, DEFAULT_TRACK_DISPLAY_DEPTH );
		displaySettings.put( KEY_COLORMAP, TrackMateOptionUtils.getOptions().getPaintScale() );
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Called when the user makes a selection change in the graph. Used to
	 * forward this event to the {@link InfoPane} and to other
	 * {@link SelectionChangeListener}s.
	 *
	 * @param added
	 *            the cells <b>removed</b> from selection (careful, inverted)
	 * @param removed
	 *            the cells <b>added</b> to selection (careful, inverted)
	 */
	private void userChangedSelection( final Collection< Object > added, final Collection< Object > removed )
	{ // Seems to be inverted
		if ( !doFireSelectionChangeEvent )
		{ return; }
		final Collection< Spot > spotsToAdd = new ArrayList<>();
		final Collection< Spot > spotsToRemove = new ArrayList<>();
		final Collection< DefaultWeightedEdge > edgesToAdd = new ArrayList<>();
		final Collection< DefaultWeightedEdge > edgesToRemove = new ArrayList<>();

		if ( null != added )
		{
			for ( final Object obj : added )
			{
				final mxCell cell = ( mxCell ) obj;

				if ( cell.getChildCount() > 0 )
				{

					for ( int i = 0; i < cell.getChildCount(); i++ )
					{
						final mxICell child = cell.getChildAt( i );
						if ( child.isVertex() )
						{
							final Spot spot = graph.getSpotFor( child );
							spotsToRemove.add( spot );
						}
						else
						{
							final DefaultWeightedEdge edge = graph.getEdgeFor( child );
							edgesToRemove.add( edge );
						}
					}

				}
				else
				{

					if ( cell.isVertex() )
					{
						final Spot spot = graph.getSpotFor( cell );
						spotsToRemove.add( spot );
					}
					else
					{
						final DefaultWeightedEdge edge = graph.getEdgeFor( cell );
						edgesToRemove.add( edge );
					}
				}
			}
		}

		if ( null != removed )
		{
			for ( final Object obj : removed )
			{
				final mxCell cell = ( mxCell ) obj;

				if ( cell.getChildCount() > 0 )
				{

					for ( int i = 0; i < cell.getChildCount(); i++ )
					{
						final mxICell child = cell.getChildAt( i );
						if ( child.isVertex() )
						{
							final Spot spot = graph.getSpotFor( child );
							spotsToAdd.add( spot );
						}
						else
						{
							final DefaultWeightedEdge edge = graph.getEdgeFor( child );
							edgesToAdd.add( edge );
						}
					}

				}
				else
				{

					if ( cell.isVertex() )
					{
						final Spot spot = graph.getSpotFor( cell );
						spotsToAdd.add( spot );
					}
					else
					{
						final DefaultWeightedEdge edge = graph.getEdgeFor( cell );
						edgesToAdd.add( edge );
					}
				}
			}
		}
		if ( DEBUG_SELECTION )
		{
			System.out.println( "[TrackScheme] userChangeSelection: sending selection change to model." );
		}
		doFireSelectionChangeEvent = false;
		if ( !edgesToAdd.isEmpty() )
		{
			selectionModel.addEdgeToSelection( edgesToAdd );
		}
		if ( !spotsToAdd.isEmpty() )
		{
			selectionModel.addSpotToSelection( spotsToAdd );
		}
		if ( !edgesToRemove.isEmpty() )
		{
			selectionModel.removeEdgeFromSelection( edgesToRemove );
		}
		if ( !spotsToRemove.isEmpty() )
		{
			selectionModel.removeSpotFromSelection( spotsToRemove );
		}
		doFireSelectionChangeEvent = true;
	}

	private void initGUI()
	{
		this.gui = new TrackSchemeFrame( this );
		final String title = "TrackScheme";
		gui.setTitle( title );
		gui.setSize( DEFAULT_SIZE );
		gui.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				model.removeModelChangeListener( TrackScheme.this );
			}
		} );
		gui.setLocationByPlatform( true );
		gui.setVisible( true );
	}

	/*
	 * INNER CLASSES
	 */

	private class CellRemovalListener implements mxIEventListener
	{

		@Override
		public void invoke( final Object sender, final mxEventObject evt )
		{

			if ( DEBUG )
			{
				System.out.println( "[TrackScheme] CellRemovalListener: cells removed - Source of event is " + sender.getClass() + ". Fire flag is " + doFireModelChangeEvent );
			}

			if ( !doFireModelChangeEvent )
			{ return; }

			// Separate spots from edges
			final Object[] objects = ( Object[] ) evt.getProperty( "cells" );
			final HashSet< Spot > spotsToRemove = new HashSet<>();
			final ArrayList< DefaultWeightedEdge > edgesToRemove = new ArrayList<>();
			for ( final Object obj : objects )
			{
				final mxCell cell = ( mxCell ) obj;
				if ( null != cell )
				{
					if ( cell.isVertex() )
					{
						// Build list of removed spots
						final Spot spot = graph.getSpotFor( cell );
						spotsToRemove.add( spot );
						// Clean maps
						graph.removeMapping( spot );
					}
					else if ( cell.isEdge() )
					{
						// Build list of removed edges
						final DefaultWeightedEdge edge = graph.getEdgeFor( cell );
						if ( null == edge )
						{
							continue;
						}
						edgesToRemove.add( edge );
						// Clean maps
						graph.removeMapping( edge );
					}
				}
			}

			evt.consume();

			// Clean model
			doFireModelChangeEvent = false;
			model.beginUpdate();
			try
			{
				selectionModel.clearSelection();
				// We remove edges first so that we ensure we do not end having
				// orphan edges.
				// Normally JGraphT handles that well, but we enforce things
				// here. To be sure.
				for ( final DefaultWeightedEdge edge : edgesToRemove )
				{
					model.removeEdge( edge );
				}
				for ( final Spot spot : spotsToRemove )
				{
					model.removeSpot( spot );
				}

			}
			finally
			{
				model.endUpdate();
			}
			doFireModelChangeEvent = true;
		}

	}

	private class SelectionChangeListener implements mxIEventListener
	{

		@Override
		@SuppressWarnings( "unchecked" )
		public void invoke( final Object sender, final mxEventObject evt )
		{
			if ( DEBUG_SELECTION )
			{
				System.out.println( "[TrackSchemeFrame] SelectionChangeListener: selection changed by " + sender + ". Fire event flag is " + doFireSelectionChangeEvent );
			}
			if ( !doFireSelectionChangeEvent || sender != graph.getSelectionModel() )
			{ return; }
			final Collection< Object > added = ( Collection< Object > ) evt.getProperty( "added" );
			final Collection< Object > removed = ( Collection< Object > ) evt.getProperty( "removed" );
			userChangedSelection( added, removed );
		}
	}

	/*
	 * ACTIONS called from gui parts
	 */

	/**
	 * Toggles whether drag-&amp;-drop linking is allowed.
	 *
	 * @return the current settings value, after toggling.
	 */
	public boolean toggleLinking()
	{
		final boolean enabled = gui.graphComponent.getConnectionHandler().isEnabled();
		gui.graphComponent.getConnectionHandler().setEnabled( !enabled );
		return !enabled;
	}

	/**
	 * Toggles whether thumbnail capture is enabled.
	 *
	 * @return the current settings value, after toggling.
	 */
	public boolean toggleThumbnail()
	{
		if ( !doThumbnailCapture )
		{
			createThumbnails();
		}
		doThumbnailCapture = !doThumbnailCapture;
		return doThumbnailCapture;
	}

	public void zoomIn()
	{
		gui.graphComponent.zoomIn();
	}

	public void zoomOut()
	{
		gui.graphComponent.zoomOut();
	}

	public void resetZoom()
	{
		gui.graphComponent.zoomActual();
	}

	public void doTrackStyle()
	{
		if ( null == stylist )
		{ return; }
		gui.logger.setStatus( "Setting style." );

		graph.getModel().beginUpdate();
		try
		{

			// Collect edges
			final Set< Integer > trackIDs = model.getTrackModel().trackIDs( true );
			final HashMap< Integer, Set< mxCell > > edgeMap = new HashMap<>( trackIDs.size() );
			for ( final Integer trackID : trackIDs )
			{
				final Set< DefaultWeightedEdge > edges = model.getTrackModel().trackEdges( trackID );
				final HashSet< mxCell > set = new HashSet<>( edges.size() );
				for ( final DefaultWeightedEdge edge : edges )
				{
					set.add( graph.getCellFor( edge ) );
				}
				edgeMap.put( trackID, set );
			}

			// Give them style
			final Set< mxICell > verticesUpdated = stylist.execute( edgeMap );

			// Take care of vertices
			final HashSet< mxCell > missedVertices = new HashSet<>( graph.getVertexCells() );
			missedVertices.removeAll( verticesUpdated );
			stylist.updateVertexStyle( missedVertices );

		}
		finally
		{
			graph.getModel().endUpdate();
		}
	}

	/**
	 * Captures and stores the thumbnail image that will be displayed in each
	 * spot cell, when using styles that can display images.
	 */
	private void createThumbnails()
	{
		// Group spots per frame
		final Set< Integer > frames = model.getSpots().keySet();
		final HashMap< Integer, HashSet< Spot > > spotPerFrame = new HashMap<>( frames.size() );
		for ( final Integer frame : frames )
		{
			spotPerFrame.put( frame, new HashSet< Spot >( model.getSpots().getNSpots( frame, true ) ) ); // max
			// size
		}
		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{
			for ( final Spot spot : model.getTrackModel().trackSpots( trackID ) )
			{
				final int frame = spot.getFeature( Spot.FRAME ).intValue();
				spotPerFrame.get( frame ).add( spot );
			}
		}
		// Set spot image to cell style
		if ( null != spotImageUpdater )
		{
			gui.logger.setStatus( "Collecting spot thumbnails." );
			final double radiusFactor = ( Double ) displaySettings.get( KEY_SPOT_RADIUS_RATIO );
			int index = 0;
			try
			{
				graph.getModel().beginUpdate();

				// Iterate per frame
				for ( final Integer frame : frames )
				{
					for ( final Spot spot : spotPerFrame.get( frame ) )
					{
						final mxICell cell = graph.getCellFor( spot );
						final String imageStr = spotImageUpdater.getImageString( spot, radiusFactor );
						String style = cell.getStyle();
						style = mxStyleUtils.setStyle( style, mxConstants.STYLE_IMAGE, "data:image/base64," + imageStr );
						graph.getModel().setStyle( cell, style );

					}
					gui.logger.setProgress( ( double ) index++ / frames.size() );
				}
			}
			finally
			{
				graph.getModel().endUpdate();
				gui.logger.setProgress( 0d );
				gui.logger.setStatus( "" );
			}
		}
	}

	public void doTrackLayout()
	{
		// Position cells
		graphLayout.execute( null );
		rowLengths = graphLayout.getRowLengths();
		int maxLength = 2;
		for ( final int rowLength : rowLengths.values() )
		{
			if ( maxLength < rowLength )
			{
				maxLength = rowLength;
			}
		}
		unlaidSpotColumn = maxLength;
		gui.graphComponent.refresh();
	}

	public void captureUndecorated()
	{
		final BufferedImage image = mxCellRenderer.createBufferedImage( graph, null, 1, Color.WHITE, true, null, gui.graphComponent.getCanvas() );
		final ImagePlus imp = new ImagePlus( "TrackScheme capture", image );
		imp.show();
	}

	public void captureDecorated()
	{
		final JViewport view = gui.graphComponent.getViewport();
		final Point currentPos = view.getViewPosition();
		view.setViewPosition( new Point( 0, 0 ) ); // We have to do that
		// otherwise, top left is
		// not painted
		final Dimension size = view.getViewSize();
		final BufferedImage image = ( BufferedImage ) view.createImage( size.width, size.height );
		final Graphics2D captureG = image.createGraphics();
		view.paintComponents( captureG );
		view.setViewPosition( currentPos );
		final ImagePlus imp = new ImagePlus( "TrackScheme capture", image );
		imp.show();
	}

	public void toggleDisplayDecoration()
	{
		gui.graphComponent.loopPaintDecorationLevel();
		gui.graphComponent.repaint();
	}

	/**
	 * Create links between all the spots currently in the {@link Model}
	 * selection. We update simultaneously the {@link Model} and the
	 * {@link JGraphXAdapter}.
	 */
	public void linkSpots()
	{

		// Sort spots by time
		final TreeMap< Integer, Spot > spotsInTime = new TreeMap<>();
		for ( final Spot spot : selectionModel.getSpotSelection() )
		{
			spotsInTime.put( spot.getFeature( Spot.FRAME ).intValue(), spot );
		}

		// Find adequate column
		final int targetColumn = getUnlaidSpotColumn();

		// Then link them in this order
		model.beginUpdate();
		graph.getModel().beginUpdate();
		try
		{
			final Iterator< Integer > it = spotsInTime.keySet().iterator();
			final Integer previousTime = it.next();
			Spot previousSpot = spotsInTime.get( previousTime );
			// If this spot belong to an invisible track, we make it visible
			Integer ID = model.getTrackModel().trackIDOf( previousSpot );
			if ( ID != null && !model.getTrackModel().isVisible( ID ) )
			{
				importTrack( ID );
			}

			while ( it.hasNext() )
			{
				final Integer currentTime = it.next();
				final Spot currentSpot = spotsInTime.get( currentTime );
				// If this spot belong to an invisible track, we make it visible
				ID = model.getTrackModel().trackIDOf( currentSpot );
				if ( ID != null && !model.getTrackModel().isVisible( ID ) )
				{
					importTrack( ID );
				}
				// Check that the cells matching the 2 spots exist in the graph
				mxICell currentCell = graph.getCellFor( currentSpot );
				if ( null == currentCell )
				{
					currentCell = insertSpotInGraph( currentSpot, targetColumn );
					if ( DEBUG )
					{
						System.out.println( "[TrackScheme] linkSpots: creating cell " + currentCell + " for spot " + currentSpot );
					}
				}
				mxICell previousCell = graph.getCellFor( previousSpot );
				if ( null == previousCell )
				{
					final int frame = previousSpot.getFeature( Spot.FRAME ).intValue();
					final int column = Math.max( targetColumn, getNextFreeColumn( frame ) );
					rowLengths.put( frame, column );
					previousCell = insertSpotInGraph( previousSpot, column );
					if ( DEBUG )
					{
						System.out.println( "[TrackScheme] linkSpots: creating cell " + previousCell + " for spot " + previousSpot );
					}
				}
				// Check if the model does not have already a edge for these 2
				// spots (that is
				// the case if the 2 spot are in an invisible track, which track
				// scheme does not
				// know of).
				DefaultWeightedEdge edge = model.getTrackModel().getEdge( previousSpot, currentSpot );
				if ( null == edge )
				{
					// We create a new edge between 2 spots, and pair it with a
					// new cell edge.
					edge = model.addEdge( previousSpot, currentSpot, -1 );
					final mxCell cell = graph.addJGraphTEdge( edge );
					cell.setValue( "New" );
				}
				else
				{
					// We retrieve the edge, and pair it with a new cell edge.
					final mxCell cell = graph.addJGraphTEdge( edge );
					cell.setValue( String.format( "%.1f", model.getTrackModel().getEdgeWeight( edge ) ) );
					// Also, if the existing edge belonged to an existing
					// invisible track, we make it visible.
					ID = model.getTrackModel().trackIDOf( edge );
					if ( ID != null && !model.getTrackModel().isVisible( ID ) )
					{
						importTrack( ID );
					}
				}
				previousSpot = currentSpot;
			}
		}
		finally
		{
			graph.getModel().endUpdate();
			model.endUpdate();
		}
	}

	/**
	 * Removes the cell selected by the user in the GUI.
	 */
	public void removeSelectedCells()
	{
		graph.getModel().beginUpdate();
		try
		{
			graph.removeCells( graph.getSelectionCells() );
			// Will be caught by the graph listeners
		}
		finally
		{
			graph.getModel().endUpdate();
		}
	}

	public void selectTrack( final Collection< mxCell > vertices, final Collection< mxCell > edges, final int direction )
	{

		// Look for spot and edges matching given mxCells
		final HashSet< Spot > inspectionSpots = new HashSet<>( vertices.size() );
		for ( final mxCell cell : vertices )
		{
			final Spot spot = graph.getSpotFor( cell );
			if ( null == spot )
			{
				if ( DEBUG )
				{
					System.out.println( "[TrackScheme] selectWholeTrack: tried to retrieve cell " + cell + ", unknown to spot map." );
				}
				continue;
			}
			inspectionSpots.add( spot );
		}
		final HashSet< DefaultWeightedEdge > inspectionEdges = new HashSet<>( edges.size() );
		for ( final mxCell cell : edges )
		{
			final DefaultWeightedEdge dwe = graph.getEdgeFor( cell );
			if ( null == dwe )
			{
				if ( DEBUG )
				{
					System.out.println( "[TrackScheme] select whole track: tried to retrieve cell " + cell + ", unknown to edge map." );
				}
				continue;
			}
			inspectionEdges.add( dwe );
		}
		// Forward to selection model
		selectionModel.selectTrack( inspectionSpots, inspectionEdges, direction );
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

}
