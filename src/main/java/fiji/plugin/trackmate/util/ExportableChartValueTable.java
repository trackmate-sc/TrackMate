/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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
package fiji.plugin.trackmate.util;

import static fiji.plugin.trackmate.gui.Icons.CSV_ICON;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import fiji.plugin.trackmate.features.ModelDataset;
import fiji.plugin.trackmate.features.manual.ManualSpotColorAnalyzerFactory;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.FileChooser.SelectionMode;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.table.TablePanel;

public class ExportableChartValueTable extends JFrame
{

	private static final long serialVersionUID = 1L;

	public static String selectedFile = System.getProperty( "user.home" ) + File.separator + "export.csv";

	private final TablePanel< TableRow > table;

	public ExportableChartValueTable( final ModelDataset dataset, final String xLabel, final String xUnits, final String yLabel, final String yUnits )
	{
		super( yLabel );
		setName( yLabel );
		setIconImage( Icons.PLOT_ICON.getImage() );

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BorderLayout() );

		// Table.
		this.table = createDatasetTable( dataset, xLabel, xUnits, yUnits );

		mainPanel.add( table.getPanel(), BorderLayout.CENTER );

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
			table.exportToCsv( new File( csvFile ) );
		}
		catch ( final IOException e )
		{
			System.err.println( "Problem exporting to file " + csvFile + "\n" + e.getMessage() );
		}
	}

	public static final TablePanel< TableRow > createDatasetTable(
			final ModelDataset dataset,
			final String xFeature,
			final String xUnits,
			final String yUnits )
	{
		final int nSeries = dataset.getSeriesCount();

		// Features names and units.
		final List< String > features = new ArrayList<>( nSeries + 1 );
		final Map< String, String > featureNames = new HashMap<>( nSeries + 1 );
		final Map< String, String > featureUnits = new HashMap<>( nSeries + 1 );
		final Map< String, Boolean > isInts = new HashMap<>( nSeries + 1 );
		final Map< String, String > infoTexts = new HashMap<>();
		features.add( xFeature );
		featureNames.put( xFeature, xFeature );
		featureUnits.put( xFeature, xUnits );
		isInts.put( xFeature, Boolean.FALSE );
		for ( int i = 0; i < nSeries; i++ )
		{
			final String str = dataset.getSeriesKey( i ).toString();
			features.add( str );
			featureNames.put( str, str );
			featureUnits.put( str, yUnits );
			isInts.put( str, Boolean.FALSE );
		}

		// The rows.
		final Iterable< TableRow > rows = toRows( dataset, xFeature );
		// Map row and column (feature str) to value.
		final BiFunction< TableRow, String, Double > featureFun = ( row, feature ) -> row.map.get( feature );

		// Row names.
		final Function< TableRow, String > labelGenerator = row -> row.label;
		final BiConsumer< TableRow, String > labelSetter = ( row, label ) -> dataset.setItemLabel( row.id, label );

		// Coloring. None for now.
		final Supplier< FeatureColorGenerator< TableRow > > coloring = () -> ( row ) -> Color.WHITE;
		final BiConsumer< TableRow, Color > colorSetter = null;

		// The table.
		final TablePanel< TableRow > table =
				new TablePanel<>(
						rows,
						features,
						featureFun,
						featureNames,
						featureNames,
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

	private static Iterable< TableRow > toRows( final ModelDataset dataset, final String xFeature )
	{
		return new DatasetWrapper( dataset, xFeature );
	}

	private static final class DatasetWrapper implements Iterable< TableRow >
	{

		private final List< TableRow > list;

		public DatasetWrapper( final ModelDataset dataset, final String xFeature )
		{
			final int nItems = dataset.getItemCount( 0 );
			this.list = new ArrayList<>( nItems );
			final int nSeries = dataset.getSeriesCount();
			for ( int j = 0; j < nItems; j++ )
			{
				final String label = dataset.getItemLabel( j );
				final Double x = ( Double ) dataset.getX( 0, j );
				final TableRow row = new TableRow( label, j );
				row.map.put( xFeature, x );
				for ( int i = 0; i < nSeries; i++ )
				{
					final String seriesName = dataset.getSeriesKey( i ).toString();
					final Double y = ( Double ) dataset.getY( i, j );
					row.map.put( seriesName, y );
				}
				list.add( row );
			}
			list.sort( Comparator.comparingDouble( row -> row.map.get( xFeature ) ) );
		}

		@Override
		public Iterator< TableRow > iterator()
		{
			return list.iterator();
		}
	}

	private static final class TableRow
	{

		private final String label;

		private final Map< String, Double > map = new HashMap<>();

		private final int id;

		public TableRow( final String label, final int id )
		{
			this.label = label;
			this.id = id;
		}
	}
}
