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
package fiji.plugin.trackmate.util;

import static fiji.plugin.trackmate.gui.Icons.CSV_ICON;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.chart.renderer.xy.XYItemRenderer;

import fiji.plugin.trackmate.features.ModelDataset;
import fiji.plugin.trackmate.features.ModelDataset.DataItem;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.FileChooser.SelectionMode;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.table.TablePanel;

public class ExportableChartValueTable extends JFrame
{

	private static final long serialVersionUID = 1L;

	public static String selectedFile = System.getProperty( "user.home" ) + File.separator + "export.csv";

	private final TablePanel< DataItem > table;

	public ExportableChartValueTable(
			final ModelDataset dataset,
			final String xFeature,
			final String xFeatureName,
			final String xUnits,
			final String tableTitle,
			final String yUnits )
	{
		super( tableTitle );
		setName( tableTitle );
		setIconImage( Icons.PLOT_ICON.getImage() );
		selectedFile = new File( new File( selectedFile ).getParent(), tableTitle.replaceAll( "\\.+$", "" ) + ".csv" ).getAbsolutePath();

		final JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BorderLayout() );

		// Table.
		this.table = createDatasetTable( dataset, xFeature, xFeatureName, xUnits, yUnits );
		mainPanel.add( table.getPanel(), BorderLayout.CENTER );

		// Tool bar.
		final JPanel toolbar = new JPanel();
		final BoxLayout layout = new BoxLayout( toolbar, BoxLayout.LINE_AXIS );
		toolbar.setLayout( layout );
		final JButton exportBtn = new JButton( "Export to CSV", CSV_ICON );
		exportBtn.addActionListener( e -> exportToCsv() );
		toolbar.add( exportBtn );
		toolbar.add( Box.createHorizontalGlue() );

		final JToggleButton tglColoring = new JToggleButton( "coloring" );
		tglColoring.addActionListener( e -> {
			table.setUseColoring( tglColoring.isSelected() );
			repaint();
		} );
		toolbar.add( tglColoring );

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

	private static final TablePanel< DataItem > createDatasetTable(
			final ModelDataset dataset,
			final String xFeature,
			final String xFeatureName,
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
		featureNames.put( xFeature, xFeatureName );
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

		// Map row and column (feature str) to value.
		final BiFunction< DataItem, String, Double > featureFun = ( row, feature ) -> row.get( feature );

		// Row names.
		final Function< DataItem, String > labelGenerator = row -> dataset.getItemLabel( row.item );
		final BiConsumer< DataItem, String > labelSetter = ( row, label ) -> dataset.setItemLabel( row.item, label );

		// Coloring.
		final XYItemRenderer renderer = dataset.getRenderer();
		final Supplier< FeatureColorGenerator< DataItem > > coloring =
				() -> ( row ) -> ( Color ) renderer.getItemPaint( 0, row.item );

		// The table.
		final TablePanel< DataItem > table = new TablePanel<>(
				dataset,
				features,
				featureFun,
				featureNames,
				featureNames,
				featureUnits,
				isInts,
				infoTexts,
				coloring,
				labelGenerator,
				labelSetter );
		return table;
	}
}
