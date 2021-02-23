package fiji.plugin.trackmate.visualization.table;

import static fiji.plugin.trackmate.gui.Icons.CSV_ICON;
import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.features.manual.ManualSpotColorAnalyzerFactory;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.UpdateListener;
import fiji.plugin.trackmate.util.FileChooser;
import fiji.plugin.trackmate.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.FileChooser.SelectionMode;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.trackscheme.utils.SearchBar;

public class AllSpotsTableView extends JFrame implements TrackMateModelView, ModelChangeListener, SelectionChangeListener
{

	private static final long serialVersionUID = 1L;

	private static final String KEY = "SPOT_TABLE";

	public static String selectedFile = TrackTableView.selectedFile;

	private final Model model;

	private final TablePanel< Spot > spotTable;

	private final AtomicBoolean ignoreSelectionChange = new AtomicBoolean( false );

	private final SelectionModel selectionModel;

	public AllSpotsTableView( final Model model, final SelectionModel selectionModel, final DisplaySettings ds )
	{
		super( "All spots table" );
		setIconImage( TRACKMATE_ICON.getImage() );
		this.model = model;
		this.selectionModel = selectionModel;

		/*
		 * GUI.
		 */

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BorderLayout() );

		// Table.
		this.spotTable = createSpotTable( model, ds );

		mainPanel.add( spotTable.getPanel(), BorderLayout.CENTER );

		// Tool bar.
		final JPanel toolbar = new JPanel();
		final BoxLayout layout = new BoxLayout( toolbar, BoxLayout.LINE_AXIS );
		toolbar.setLayout( layout );
		final JButton exportBtn = new JButton( "Export to CSV", CSV_ICON );
		exportBtn.addActionListener( e -> exportToCsv() );
		toolbar.add( exportBtn );
		toolbar.add( Box.createHorizontalGlue() );
		final SearchBar searchBar = new SearchBar( model, this );
		searchBar.setMaximumSize( new java.awt.Dimension( 160, 30 ) );
		toolbar.add( searchBar );
		final JToggleButton tglColoring = new JToggleButton( "coloring" );
		tglColoring.addActionListener( e -> {
			spotTable.setUseColoring( tglColoring.isSelected() );
			refresh();
		} );
		toolbar.add( tglColoring );
		mainPanel.add( toolbar, BorderLayout.NORTH );

		getContentPane().add( mainPanel );
		pack();

		/*
		 * Listeners.
		 */

		spotTable.getTable().getSelectionModel().addListSelectionListener(
				new SpotTableSelectionListener() );

		final UpdateListener refresher = () -> refresh();
		ds.listeners().add( refresher );
		selectionModel.addSelectionChangeListener( this );
		model.addModelChangeListener( this );
		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final java.awt.event.WindowEvent e )
			{
				selectionModel.removeSelectionChangeListener( AllSpotsTableView.this );
				model.removeModelChangeListener( AllSpotsTableView.this );
				ds.listeners().remove( refresher );
			};
		} );
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
			spotTable.exportToCsv( new File( csvFile ) );
		}
		catch ( final IOException e )
		{
			model.getLogger().error( "Problem exporting to file "
					+ csvFile + "\n" + e.getMessage() );
		}
	}

	public static final TablePanel< Spot > createSpotTable( final Model model, final DisplaySettings ds )
	{
		final List< String > features = new ArrayList<>( model.getFeatureModel().getSpotFeatures() );
		final Map< String, String > featureNames = model.getFeatureModel().getSpotFeatureNames();
		final Map< String, String > featureShortNames = model.getFeatureModel().getSpotFeatureShortNames();
		final Map< String, String > featureUnits = new HashMap<>();
		for ( final String feature : features )
		{
			final Dimension dimension = model.getFeatureModel().getSpotFeatureDimensions().get( feature );
			final String units = TMUtils.getUnitsFor( dimension, model.getSpaceUnits(), model.getTimeUnits() );
			featureUnits.put( feature, units );
		}
		final Map< String, Boolean > isInts = model.getFeatureModel().getSpotFeatureIsInt();
		final Map< String, String > infoTexts = new HashMap<>();
		final Function< Spot, String > labelGenerator = spot -> spot.getName();
		final BiConsumer< Spot, String > labelSetter = ( spot, label ) -> spot.setName( label );

		/*
		 * Feature provider. We add a fake one to show the spot track ID.
		 */
		final String TRACK_ID = "TRACK_ID";
		features.add( 0, TRACK_ID );
		featureNames.put( TRACK_ID, "Track ID" );
		featureShortNames.put( TRACK_ID, "Track ID" );
		featureUnits.put( TRACK_ID, "" );
		isInts.put( TRACK_ID, Boolean.TRUE );
		infoTexts.put( TRACK_ID, "The id of the track this spot belongs to." );
		
		final BiFunction< Spot, String, Double > featureFun = ( spot, feature ) -> {
			if ( feature.equals( TRACK_ID ) )
			{
				final Integer trackID = model.getTrackModel().trackIDOf( spot );
				return trackID == null ? null : trackID.doubleValue();
			}
			return spot.getFeature( feature );
		};

		final Supplier< FeatureColorGenerator< Spot > > coloring =
				() -> FeatureUtils.createSpotColorGenerator( model, ds );

		final BiConsumer< Spot, Color > colorSetter =
				( spot, color ) -> spot.putFeature( ManualSpotColorAnalyzerFactory.FEATURE, Double.valueOf( color.getRGB() ) );
				
		final TablePanel< Spot > table =
				new TablePanel<>(
						model.getSpots().iterable( true ),
						features,
						featureFun,
						featureNames,
						featureShortNames,
						featureUnits,
						isInts,
						infoTexts,
						coloring,
						labelGenerator,
						labelSetter,
						ManualSpotColorAnalyzerFactory.FEATURE,
						colorSetter );
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
	public void modelChanged( final ModelChangeEvent event )
	{
		if ( event.getEventID() == ModelChangeEvent.FEATURES_COMPUTED )
		{
			refresh();
			return;
		}

		final List< Spot > spots = new ArrayList<>();
		for ( final Spot spot : model.getSpots().iterable( true ) )
			spots.add( spot );
		spotTable.setObjects( spots );

		refresh();
	}

	/*
	 * Forward selection model changes to the tables.
	 */
	@Override
	public void selectionChanged( final SelectionChangeEvent event )
	{
		if ( ignoreSelectionChange.get() )
			return;
		ignoreSelectionChange.set( true );

		// Vertices table.
		final Set< Spot > selectedVertices = selectionModel.getSpotSelection();
		final JTable vt = spotTable.getTable();
		vt.getSelectionModel().clearSelection();
		for ( final Spot spot : selectedVertices )
		{
			final int row = spotTable.getViewRowForObject( spot );
			vt.getSelectionModel().addSelectionInterval( row, row );
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

		refresh();
		ignoreSelectionChange.set( false );
	}

	@Override
	public void centerViewOn( final Spot spot )
	{
		spotTable.scrollToObject( spot );
	}

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
	private final class SpotTableSelectionListener implements ListSelectionListener
	{

		@Override
		public void valueChanged( final ListSelectionEvent event )
		{
			if ( event.getValueIsAdjusting() || ignoreSelectionChange.get() )
				return;

			ignoreSelectionChange.set( true );

			final int[] selectedRows = spotTable.getTable().getSelectedRows();
			final List< Spot > toSelect = new ArrayList<>( selectedRows.length );
			for ( final int row : selectedRows )
				toSelect.add( spotTable.getObjectForViewRow( row ) );

			selectionModel.clearSelection();
			selectionModel.addSpotToSelection( toSelect );
			refresh();

			ignoreSelectionChange.set( false );
		}
	}
}
