package fiji.plugin.trackmate.visualization.table;

import static fiji.plugin.trackmate.gui.Icons.CSV_ICON;
import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition.TrackBranchDecomposition;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;
import fiji.plugin.trackmate.util.FileChooser;
import fiji.plugin.trackmate.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.FileChooser.SelectionMode;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class BranchTableView extends JFrame implements TrackMateModelView
{

	private static final long serialVersionUID = 1L;

	private static final String KEY = "SPOT_TABLE";

	public static String selectedFile = TrackTableView.selectedFile;

	private final Model model;

	private final TablePanel< Branch > branchTable;

	public BranchTableView( final Model model, final SelectionModel selectionModel )
	{
		super( "Branch table" );
		setIconImage( TRACKMATE_ICON.getImage() );
		this.model = model;

		/*
		 * GUI.
		 */

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BorderLayout() );

		// Table.
		this.branchTable = createBranchTable( model, selectionModel );

		mainPanel.add( branchTable.getPanel(), BorderLayout.CENTER );

		// Tool bar.
		final JPanel toolbar = new JPanel();
		final BoxLayout layout = new BoxLayout( toolbar, BoxLayout.LINE_AXIS );
		toolbar.setLayout( layout );
		final JButton exportBtn = new JButton( "Export to CSV", CSV_ICON );
		exportBtn.addActionListener( e -> exportToCsv() );
		toolbar.add( exportBtn );
		toolbar.add( Box.createHorizontalGlue() );
		mainPanel.add( toolbar, BorderLayout.NORTH );

		getContentPane().add( mainPanel );
		pack();
	}

	public TablePanel< Branch > getBranchTable()
	{
		return branchTable;
	}

	public void exportToCsv()
	{
		final File file = FileChooser.chooseFile(
				this,
				selectedFile,
				new FileNameExtensionFilter( "CSV files", "csv" ),
				"Export table to CSV",
				DialogType.SAVE,
				SelectionMode.FILES_ONLY );
		if ( null == file )
			return;

		selectedFile = file.getAbsolutePath();
		exportToCsv( selectedFile );
	}

	public void exportToCsv( final String csvFile )
	{
		try
		{
			branchTable.exportToCsv( new File( csvFile ) );
		}
		catch ( final IOException e )
		{
			model.getLogger().error( "Problem exporting to file "
					+ csvFile + "\n" + e.getMessage() );
		}
	}

	public static final TablePanel< Branch > createBranchTable( final Model model, final SelectionModel selectionModel )
	{
		final Logger logger = model.getLogger();

		logger.log( "Generating track branches analysis.\n" );
		final int ntracks = model.getTrackModel().nTracks( true );
		if ( ntracks == 0 )
			logger.log( "No visible track found. Aborting.\n" );

		final TimeDirectedNeighborIndex neighborIndex = model.getTrackModel().getDirectedNeighborIndex();

		final List< Branch > brs = new ArrayList<>();
		for ( final Integer trackID : model.getTrackModel().unsortedTrackIDs( true ) )
		{
			final TrackBranchDecomposition branchDecomposition = ConvexBranchesDecomposition.processTrack( trackID, model.getTrackModel(), neighborIndex, true, false );
			final SimpleDirectedGraph< List< Spot >, DefaultEdge > branchGraph = ConvexBranchesDecomposition.buildBranchGraph( branchDecomposition );

			final Map< Branch, Set< List< Spot > > > successorMap = new HashMap<>();
			final Map< Branch, Set< List< Spot > > > predecessorMap = new HashMap<>();
			final Map< List< Spot >, Branch > branchMap = new HashMap<>();

			for ( final List< Spot > branch : branchGraph.vertexSet() )
			{
				final Branch br = new Branch();
				branchMap.put( branch, br );

				// Track name from ID
				br.trackName = model.getTrackModel().name( trackID );
				br.putFeature( TRACK_ID, Double.valueOf( trackID ) );

				// First and last spot.
				final Spot first = branch.get( 0 );
				br.first = first;
				br.putFeature( FIRST, Double.valueOf( first.ID() ) );

				final Spot last = branch.get( branch.size() - 1 );
				br.last = last;
				br.putFeature( LAST, Double.valueOf( last.ID() ) );

				// Delta T
				br.putFeature( DELTA_T, Double.valueOf( br.dt() ) );

				// Distance traveled.
				final double distanceTraveled = Math.sqrt( br.last.squareDistanceTo( br.first ) );
				br.putFeature( DISTANCE, Double.valueOf( distanceTraveled ) );

				// Compute mean velocity "by hand".
				final double meanV;
				if ( branch.size() < 2 )
				{
					meanV = Double.NaN;
				}
				else
				{
					final Iterator< Spot > it = branch.iterator();
					Spot previous = it.next();
					double sum = 0;
					while ( it.hasNext() )
					{
						final Spot next = it.next();
						final double dr = Math.sqrt( next.squareDistanceTo( previous ) );
						sum += dr;
						previous = next;
					}
					meanV = sum / ( branch.size() - 1 );
				}
				br.putFeature( MEAN_VELOCITY, Double.valueOf( meanV ) );

				// Predecessors
				final Set< DefaultEdge > incomingEdges = branchGraph.incomingEdgesOf( branch );
				final Set< List< Spot > > predecessors = new HashSet<>( incomingEdges.size() );
				for ( final DefaultEdge edge : incomingEdges )
				{
					final List< Spot > predecessorBranch = branchGraph.getEdgeSource( edge );
					predecessors.add( predecessorBranch );
				}

				// Successors
				final Set< DefaultEdge > outgoingEdges = branchGraph.outgoingEdgesOf( branch );
				final Set< List< Spot > > successors = new HashSet<>( outgoingEdges.size() );
				for ( final DefaultEdge edge : outgoingEdges )
				{
					final List< Spot > successorBranch = branchGraph.getEdgeTarget( edge );
					successors.add( successorBranch );
				}

				successorMap.put( br, successors );
				predecessorMap.put( br, predecessors );
			}

			for ( final Branch br : successorMap.keySet() )
			{
				final Set< List< Spot > > succs = successorMap.get( br );
				final Set< Branch > succBrs = new HashSet<>( succs.size() );
				for ( final List< Spot > branch : succs )
				{
					final Branch succBr = branchMap.get( branch );
					succBrs.add( succBr );
				}
				br.successors = succBrs;
				br.putFeature( N_SUCCESSORS, Double.valueOf( succBrs.size() ) );

				final Set< List< Spot > > preds = predecessorMap.get( br );
				final Set< Branch > predBrs = new HashSet<>( preds.size() );
				for ( final List< Spot > branch : preds )
				{
					final Branch predBr = branchMap.get( branch );
					predBrs.add( predBr );
				}
				br.predecessors = predBrs;
				br.putFeature( N_PREDECESSORS, Double.valueOf( predBrs.size() ) );
			}

			brs.addAll( successorMap.keySet() );
		}
		Collections.sort( brs );

		/*
		 * Create the table.
		 */

		final Iterable< Branch > objects = brs;
		final BiFunction< Branch, String, Double > featureFun = ( br, feature ) -> br.getFeature( feature );
		final Map< String, String > featureUnits = new HashMap<>();
		BRANCH_FEATURES_DIMENSIONS.forEach(
				( f, d ) -> featureUnits.put( f, TMUtils.getUnitsFor( d, model.getSpaceUnits(), model.getTimeUnits() ) ) );
		final Map< String, String > infoTexts = new HashMap<>();
		final Function< Branch, String > labelGenerator = b -> b.toString();
		final BiConsumer< Branch, String > labelSetter = null;
		final Supplier< FeatureColorGenerator< Branch > > colorSupplier = () -> b -> Color.WHITE;

		final TablePanel< Branch > table = new TablePanel<>(
				objects,
				BRANCH_FEATURES,
				featureFun,
				BRANCH_FEATURES_NAMES,
				BRANCH_FEATURES_SHORTNAMES,
				featureUnits,
				BRANCH_FEATURES_ISINTS,
				infoTexts,
				colorSupplier,
				labelGenerator,
				labelSetter );

		table.getTable().getSelectionModel().addListSelectionListener(
				new BranchTableSelectionListener( table, model, selectionModel ) );

		return table;
	}

	@Override
	public void render()
	{
		setLocationRelativeTo( null );
		setVisible( true );
	}

	@Override
	public void refresh()
	{
		repaint();
	}

	@Override
	public void centerViewOn( final Spot spot )
	{}

	@Override
	public Model getModel()
	{
		return model;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public void clear()
	{}

	/**
	 * Forward spot table selection to selection model.
	 */
	private static final class BranchTableSelectionListener implements ListSelectionListener
	{

		private final TablePanel< Branch > branchTable;

		private final Model model;

		private final SelectionModel selectionModel;

		public BranchTableSelectionListener( final TablePanel< Branch > branchTable, final Model model, final SelectionModel selectionModel )
		{
			this.branchTable = branchTable;
			this.model = model;
			this.selectionModel = selectionModel;
		}

		@Override
		public void valueChanged( final ListSelectionEvent event )
		{
			if ( event.getValueIsAdjusting() )
				return;

			final int[] selectedRows = branchTable.getTable().getSelectedRows();
			for ( final int row : selectedRows )
			{
				final Branch br = branchTable.getObjectForViewRow( row );
				if ( null == br )
					continue;

				final List< DefaultWeightedEdge > edges = model.getTrackModel().dijkstraShortestPath( br.first, br.last );
				final Set< Spot > spots = new HashSet<>();
				for ( final DefaultWeightedEdge edge : edges )
				{
					spots.add( model.getTrackModel().getEdgeSource( edge ) );
					spots.add( model.getTrackModel().getEdgeTarget( edge ) );
				}
				selectionModel.clearSelection();
				selectionModel.addEdgeToSelection( edges );
				selectionModel.addSpotToSelection( spots );
			}
		}
	}

	/*
	 * STATIC CLASSES AND ENUMS
	 */

	/**
	 * A class to describe a branch.
	 */
	public static class Branch implements Comparable< Branch >
	{

		private final Map< String, Double > features = new HashMap<>();

		private String trackName;

		private Spot first;

		private Spot last;

		private Set< Branch > predecessors;

		private Set< Branch > successors;

		@Override
		public String toString()
		{
			return trackName + ": " + first + " → " + last;
		}

		double dt()
		{
			return last.diffTo( first, Spot.POSITION_T );
		}

		/**
		 * Returns the value corresponding to the specified branch feature.
		 *
		 * @param feature
		 *            The feature string to retrieve the stored value for.
		 * @return the feature value, as a {@link Double}. Will be
		 *         <code>null</code> if it has not been set.
		 */
		public final Double getFeature( final String feature )
		{
			return features.get( feature );
		}

		/**
		 * Stores the specified feature value for this branch.
		 *
		 * @param feature
		 *            the name of the feature to store, as a {@link String}.
		 * @param value
		 *            the value to store, as a {@link Double}. Using
		 *            <code>null</code> will have unpredicted outcomes.
		 */
		public final void putFeature( final String feature, final Double value )
		{
			features.put( feature, value );
		}

		/**
		 * Sort by predecessors number, then successors number, then
		 * alphabetically by first spot name.
		 */
		@Override
		public int compareTo( final Branch o )
		{
			if ( predecessors.size() != o.predecessors.size() )
				return predecessors.size() - o.predecessors.size();
			if ( successors.size() != o.successors.size() )
				return successors.size() - o.successors.size();
			if ( first.getName().compareTo( o.first.getName() ) != 0 )
				return first.getName().compareTo( o.first.getName() );
			return last.getName().compareTo( o.last.getName() );
		}
	}

	private static final String TRACK_ID = "TRACK_ID";
	private static final String N_PREDECESSORS = "N_PREDECESSORS";
	private static final String N_SUCCESSORS = "N_SUCCESSORS";
	private static final String DELTA_T = "DELTA_T";
	private static final String DISTANCE = "DISTANCE";
	private static final String MEAN_VELOCITY = "MEAN_VELOCITY";
	private static final String FIRST = "FIRST";
	private static final String LAST = "LAST";
	private static final List< String > BRANCH_FEATURES = Arrays.asList( new String[] {
			TRACK_ID,
			N_PREDECESSORS,
			N_SUCCESSORS,
			DELTA_T,
			DISTANCE,
			MEAN_VELOCITY,
			FIRST,
			LAST
	} );

	private static final Map< String, String > BRANCH_FEATURES_NAMES = new HashMap<>();
	private static final Map< String, String > BRANCH_FEATURES_SHORTNAMES = new HashMap<>();
	private static final Map< String, Boolean > BRANCH_FEATURES_ISINTS = new HashMap<>();
	private static final Map< String, Dimension > BRANCH_FEATURES_DIMENSIONS = new HashMap<>();
	static
	{
		BRANCH_FEATURES_NAMES.put( TRACK_ID, "Track ID" );
		BRANCH_FEATURES_SHORTNAMES.put( TRACK_ID, "Track ID" );
		BRANCH_FEATURES_ISINTS.put( TRACK_ID, Boolean.TRUE );
		BRANCH_FEATURES_DIMENSIONS.put( TRACK_ID, Dimension.NONE );

		BRANCH_FEATURES_NAMES.put( N_PREDECESSORS, "Track ID" );
		BRANCH_FEATURES_SHORTNAMES.put( N_PREDECESSORS, "N predecessors" );
		BRANCH_FEATURES_ISINTS.put( N_PREDECESSORS, Boolean.TRUE );
		BRANCH_FEATURES_DIMENSIONS.put( N_PREDECESSORS, Dimension.NONE );

		BRANCH_FEATURES_NAMES.put( N_SUCCESSORS, "Track ID" );
		BRANCH_FEATURES_SHORTNAMES.put( N_SUCCESSORS, "N successors" );
		BRANCH_FEATURES_ISINTS.put( N_SUCCESSORS, Boolean.TRUE );
		BRANCH_FEATURES_DIMENSIONS.put( N_SUCCESSORS, Dimension.NONE );

		BRANCH_FEATURES_NAMES.put( DELTA_T, "Branch duration" );
		BRANCH_FEATURES_SHORTNAMES.put( DELTA_T, "Delta T" );
		BRANCH_FEATURES_ISINTS.put( DELTA_T, Boolean.FALSE );
		BRANCH_FEATURES_DIMENSIONS.put( DELTA_T, Dimension.TIME );

		BRANCH_FEATURES_NAMES.put( DISTANCE, "Distance traveled" );
		BRANCH_FEATURES_SHORTNAMES.put( DISTANCE, "Dist" );
		BRANCH_FEATURES_ISINTS.put( DISTANCE, Boolean.FALSE );
		BRANCH_FEATURES_DIMENSIONS.put( DISTANCE, Dimension.LENGTH );

		BRANCH_FEATURES_NAMES.put( MEAN_VELOCITY, "Mean velocity" );
		BRANCH_FEATURES_SHORTNAMES.put( MEAN_VELOCITY, "Mean V" );
		BRANCH_FEATURES_ISINTS.put( MEAN_VELOCITY, Boolean.FALSE );
		BRANCH_FEATURES_DIMENSIONS.put( MEAN_VELOCITY, Dimension.VELOCITY );

		BRANCH_FEATURES_NAMES.put( FIRST, "First spot ID" );
		BRANCH_FEATURES_SHORTNAMES.put( FIRST, "First ID" );
		BRANCH_FEATURES_ISINTS.put( FIRST, Boolean.TRUE );
		BRANCH_FEATURES_DIMENSIONS.put( FIRST, Dimension.NONE );

		BRANCH_FEATURES_NAMES.put( LAST, "Last spot ID" );
		BRANCH_FEATURES_SHORTNAMES.put( LAST, "Last ID" );
		BRANCH_FEATURES_ISINTS.put( LAST, Boolean.TRUE );
		BRANCH_FEATURES_DIMENSIONS.put( LAST, Dimension.NONE );
	}
}
