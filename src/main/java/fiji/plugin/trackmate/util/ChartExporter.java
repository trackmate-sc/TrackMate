package fiji.plugin.trackmate.util;

import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.swing.JFrame;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;


/**
 * A collection of static utilities made to export {@link JFreeChart} charts 
 * to various scalable file format.
 * 
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; Jul 20, 2011
 */
public class ChartExporter {

	/**
	 * Exports a JFreeChart to a SVG file.
	 * 
	 * @param chart JFreeChart to export
	 * @param bounds the dimensions of the viewport
	 * @param svgFile the output file.
	 * @throws IOException if writing the svgFile fails.
	 */
	public static void exportChartAsSVG(final JFreeChart chart, final Rectangle bounds, final File svgFile) throws IOException {
		// Get a DOMImplementation and create an XML document
		final org.w3c.dom.DOMImplementation domImpl =	GenericDOMImplementation.getDOMImplementation();
		final org.w3c.dom.Document document = domImpl.createDocument(null, "svg", null);

		// Create an instance of the SVG Generator
		final SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

		// draw the chart in the SVG generator
		chart.draw(svgGenerator, bounds);

		// Write svg file
		try (OutputStream outputStream = new FileOutputStream( svgFile );
				Writer out = new OutputStreamWriter( outputStream, "UTF-8" ))
		{
			svgGenerator.stream( out, true );
		}
	}

	/**
	 * Exports a JFreeChart to a PDF file.
	 * <p>
	 * We use a dirty hack for that: we first export to a physical SVG file, reload it, and
	 * use Apache FOP PDF transcoder to convert it to pdfs. It only works partially, for
	 * the text ends up in not being selectable in the pdf.
	 * 
	 * @param chart JFreeChart to export
	 * @param bounds the dimensions of the viewport
	 * @param pdfFile the output file.
	 * @throws IOException if writing the pdfFile fails.
	 * @throws DocumentException  
	 */
	public static void exportChartAsPDF(final JFreeChart chart, final Rectangle bounds, final File pdfFile) throws IOException, DocumentException {
		// step 1
		final com.itextpdf.text.Rectangle pageSize = new com.itextpdf.text.Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
        final com.itextpdf.text.Document document = new com.itextpdf.text.Document(pageSize);
        // step 2
        final PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
        // step 3
        document.open();
        // step 4
        final PdfContentByte canvas = writer.getDirectContent();
        final PdfGraphics2D g2 = new PdfGraphics2D( canvas, pageSize.getWidth(), pageSize.getHeight() );
        chart.draw(g2, bounds);
        g2.dispose();
        // step 5
        document.close();
	}


	private static Object[] createDummyChart() {
		// Collect data
		final int nPoints = 200;
		final double[][] data = new double[2][nPoints];

		int index = 0;
		for (int i = 0; i<nPoints; i++) {
			data[0][index] = Math.random() * 100;
			data[1][index] = Math.random() * 10;
			index++;
		}

		// Plot data
		final String xAxisLabel = "Time (s)";
		final String yAxisLabel = "N spots";
		final String title = "Nspots vs Time for something.";
		final DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries("Nspots", data);

		final JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true, true, false);
		chart.getTitle().setFont(FONT);
		chart.getLegend().setItemFont(SMALL_FONT);

		// The plot
		final XYPlot plot = chart.getXYPlot();
		//				plot.setRenderer(0, pointRenderer);
		plot.getRangeAxis().setLabelFont(FONT);
		plot.getRangeAxis().setTickLabelFont(SMALL_FONT);
		plot.getDomainAxis().setLabelFont(FONT);
		plot.getDomainAxis().setTickLabelFont(SMALL_FONT);

		final ExportableChartPanel panel = new ExportableChartPanel(chart);

		final JFrame frame = new JFrame(title);
		frame.setSize(500, 270);
		frame.getContentPane().add(panel);
		frame.setVisible(true);

		final Object[] out = new Object[2];
		out[0] = chart;
		out[1] = panel;

		return out;
	}

	public static void main(final String[] args) throws IOException, DocumentException {
		final Object[] stuff = createDummyChart();
		final ChartPanel panel = (ChartPanel) stuff[1];
		final JFreeChart chart = (JFreeChart) stuff[0];
		ChartExporter.exportChartAsPDF(chart, panel.getBounds(), new File("/Users/tinevez/Desktop/ExportTest.pdf"));
	}

}
