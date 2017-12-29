package fiji.plugin.trackmate.util;

import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

import com.itextpdf.text.DocumentException;

import ij.IJ;
import ij.measure.ResultsTable;

public class ExportableChartPanel extends ChartPanel
{

	/*
	 * CONSTRUCTORS
	 */

	private static final long serialVersionUID = -6556930372813672992L;

	public ExportableChartPanel( final JFreeChart chart )
	{
		super( chart );
	}

	public ExportableChartPanel( final JFreeChart chart,
			final boolean properties,
			final boolean save,
			final boolean print,
			final boolean zoom,
			final boolean tooltips )
	{
		super( chart, properties, save, print, zoom, tooltips );
	}

	public ExportableChartPanel( final JFreeChart chart, final int width, final int height,
			final int minimumDrawWidth, final int minimumDrawHeight, final int maximumDrawWidth,
			final int maximumDrawHeight, final boolean useBuffer, final boolean properties,
			final boolean save, final boolean print, final boolean zoom, final boolean tooltips )
	{
		super( chart, width, height, minimumDrawWidth, minimumDrawHeight,
				maximumDrawWidth, maximumDrawHeight,
				useBuffer, properties, save, print, zoom, tooltips );
	}

	public ExportableChartPanel( final JFreeChart chart, final int width, final int height,
			final int minimumDrawWidth, final int minimumDrawHeight, final int maximumDrawWidth,
			final int maximumDrawHeight, final boolean useBuffer, final boolean properties,
			final boolean copy, final boolean save, final boolean print, final boolean zoom,
			final boolean tooltips )
	{
		super( chart, width, height, minimumDrawWidth, minimumDrawHeight,
				maximumDrawWidth, maximumDrawHeight,
				useBuffer, properties, copy, save, print, zoom, tooltips );
	}

	/*
	 * METHODS
	 */

	@Override
	protected JPopupMenu createPopupMenu( final boolean properties, final boolean copy, final boolean save, final boolean print, final boolean zoom )
	{
		final JPopupMenu menu = super.createPopupMenu( properties, copy, false, print, zoom );

		menu.addSeparator();

		final JMenuItem displayTableItem = new JMenuItem( "Display data tables" );
		displayTableItem.setActionCommand( "TABLES" );
		displayTableItem.addActionListener( e -> createDataTable() );
		menu.add( displayTableItem );

		final JMenuItem exportToFile = new JMenuItem( "Export plot to file" );
		exportToFile.addActionListener( e -> doSaveAs() );
		menu.add( exportToFile );

		return menu;
	}

	public void createDataTable()
	{
		XYPlot plot = null;
		try
		{
			plot = getChart().getXYPlot();
		}
		catch ( final ClassCastException e )
		{
			return;
		}

		final String xColumnName = plot.getDomainAxis().getLabel();

		final ResultsTable table = new ResultsTable();
		final int nPoints = plot.getDataset( 0 ).getItemCount( 0 );
		for ( int k = 0; k < nPoints; k++ )
		{
			table.incrementCounter();

			final double xVal = plot.getDataset( 0 ).getXValue( 0, k );
			table.addValue( xColumnName, xVal );

			final int nSets = plot.getDatasetCount();
			for ( int i = 0; i < nSets; i++ )
			{

				final XYDataset dataset = plot.getDataset( i );
				if ( dataset instanceof XYEdgeSeriesCollection )
					continue;

				final int nSeries = dataset.getSeriesCount();
				for ( int j = 0; j < nSeries; j++ )
				{

					@SuppressWarnings( "rawtypes" )
					final Comparable seriesKey = dataset.getSeriesKey( j );
					final String yColumnName = seriesKey.toString() + "(" + plot.getRangeAxis().getLabel() + ")";
					final double yVal = dataset.getYValue( j, k );
					table.addValue( yColumnName, yVal );
				}

			}
		}
		table.show( getChart().getTitle().getText() );
	}

	/**
	 * Opens a file chooser and gives the user an opportunity to save the chart
	 * in PNG, PDF or SVG format.
	 */
	@Override
	public void doSaveAs()
	{
		final File file;
		if ( IJ.isMacintosh() )
		{
			Container dialogParent = getParent();
			while ( !( dialogParent instanceof Frame ) )
				dialogParent = dialogParent.getParent();

			final Frame frame = ( Frame ) dialogParent;
			final FileDialog dialog = new FileDialog( frame, "Export chart to PNG, PDF or SVG", FileDialog.SAVE );
			String defaultDir = null;
			if ( getDefaultDirectoryForSaveAs() != null )
				defaultDir = getDefaultDirectoryForSaveAs().getPath();
			
			dialog.setDirectory( defaultDir );
			final FilenameFilter filter = new FilenameFilter()
			{
				@Override
				public boolean accept( final File dir, final String name )
				{
					return name.endsWith( ".png" ) || name.endsWith( ".pdf" ) || name.endsWith( ".svg" );
				}
			};
			dialog.setFilenameFilter( filter );
			dialog.setVisible( true );
			final String selectedFile = dialog.getFile();
			if ( null == selectedFile )
				return;

			file = new File( dialog.getDirectory(), selectedFile );
		}
		else
		{
			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.setCurrentDirectory( getDefaultDirectoryForSaveAs() );
			fileChooser.addChoosableFileFilter( new FileFilter()
			{

				@Override
				public String getDescription()
				{
					return "PNG Image File";
				}

				@Override
				public boolean accept( final File f )
				{
					return f.getName().toLowerCase().endsWith( ".png" );
				}
			} );
			fileChooser.addChoosableFileFilter( new FileFilter()
			{

				@Override
				public String getDescription()
				{
					return "Portable Document File (PDF)";
				}

				@Override
				public boolean accept( final File f )
				{
					return f.getName().toLowerCase().endsWith( ".pdf" );
				}
			} );
			fileChooser.addChoosableFileFilter( new FileFilter()
			{

				@Override
				public String getDescription()
				{
					return "Scalable Vector Graphics (SVG)";
				}

				@Override
				public boolean accept( final File f )
				{
					return f.getName().toLowerCase().endsWith( ".svg" );
				}
			} );
			final int option = fileChooser.showSaveDialog( this );
			if ( option != JFileChooser.APPROVE_OPTION )
				return;

			file = fileChooser.getSelectedFile();
		}

		if ( file.getPath().endsWith( ".png" ) )
		{
			try
			{
				ChartUtils.saveChartAsPNG( file, getChart(), getWidth(), getHeight() );
			}
			catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
		else if ( file.getPath().endsWith( ".pdf" ) )
		{
			try
			{
				ChartExporter.exportChartAsPDF( getChart(), getBounds(), file );
			}
			catch ( final DocumentException | IOException e )
			{
				e.printStackTrace();
			}

		}
		else if ( file.getPath().endsWith( ".svg" ) )
		{
			try
			{
				ChartExporter.exportChartAsSVG( getChart(), getBounds(), file );
			}
			catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
		else
		{
			IJ.error( "Invalid file extension.", "Please choose a filename with one of the 3 supported extension: .png, .pdf or .svg." );
		}
	}

}
