package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.Icons.CAMERA_EXPORT_ICON;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.canvas.mxSvgCanvas;
import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxGdCodec;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxCellRenderer.CanvasFactory;
import com.mxgraph.util.mxDomUtils;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.util.png.mxPngEncodeParam;
import com.mxgraph.util.png.mxPngImageEncoder;
import com.mxgraph.view.mxGraph;

import fiji.plugin.trackmate.util.DefaultFileFilter;

public class SaveAction extends AbstractAction {

	private static final long serialVersionUID = 7672151690754466760L;
	protected String lastDir = null;
	private final TrackScheme trackScheme;

	public SaveAction(final TrackScheme trackScheme) {
		putValue( Action.SMALL_ICON, CAMERA_EXPORT_ICON );
		this.trackScheme = trackScheme;
	}

	/**
	 * Saves XML+PNG format.
	 * @param frame 
	 */
	protected void saveXmlPng(final TrackSchemeFrame frame, final String filename, final Color bg) throws IOException {
		final mxGraphComponent graphComponent = trackScheme.getGUI().graphComponent;
		final mxGraph graph = trackScheme.getGraph();

		// Creates the image for the PNG file
		final BufferedImage image = mxCellRenderer.createBufferedImage(graph,	null, 1, bg, graphComponent.isAntiAlias(), null, graphComponent.getCanvas());

		// Creates the URL-encoded XML data
		final mxCodec codec = new mxCodec();
		final String xml = URLEncoder.encode(mxXmlUtils.getXml(codec.encode(graph.getModel())), "UTF-8");
		final mxPngEncodeParam param = mxPngEncodeParam.getDefaultEncodeParam(image);
		param.setCompressedText(new String[] { "mxGraphModel", xml });

		// Saves as a PNG file
		try (FileOutputStream outputStream = new FileOutputStream( new File( filename ) ))
		{
			final mxPngImageEncoder encoder = new mxPngImageEncoder( outputStream, param );
			if ( image != null )
				encoder.encode( image );
			else
				JOptionPane.showMessageDialog( graphComponent, "No Image Data" );
		}
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final mxGraphComponent graphComponent = trackScheme.getGUI().graphComponent;
		final mxGraph graph = trackScheme.getGraph();
		FileFilter selectedFilter = null;
		final DefaultFileFilter xmlPngFilter = new DefaultFileFilter(".png", "PNG+XML file (.png)");
		final FileFilter vmlFileFilter = new DefaultFileFilter(".html", "VML file (.html)");
		String filename = null;
		boolean dialogShown = false;

		String wd;
		if (lastDir != null) {
			wd = lastDir;
		} else {
			wd = System.getProperty("user.dir");
		}

		final JFileChooser fc = new JFileChooser(wd);

		// Adds the default file format
		final FileFilter defaultFilter = xmlPngFilter;
		fc.addChoosableFileFilter(defaultFilter);

		// Adds special vector graphics formats and HTML
		fc.addChoosableFileFilter(new DefaultFileFilter(".pdf", "PDF file (.pdf)"));
		fc.addChoosableFileFilter(new DefaultFileFilter(".svg", "SVG file (.svg)"));
		fc.addChoosableFileFilter(new DefaultFileFilter(".html", "HTML file (.html)"));
		fc.addChoosableFileFilter(vmlFileFilter);
		fc.addChoosableFileFilter(new DefaultFileFilter(".txt", "Graph Drawing file (.txt)"));
		fc.addChoosableFileFilter(new DefaultFileFilter(".mxe", "mxGraph Editor file (.mxe)"));

		// Adds a filter for each supported image format
		Object[] imageFormats = ImageIO.getReaderFormatNames();

		// Finds all distinct extensions
		final HashSet<String> formats = new HashSet<>();

		for (int i = 0; i < imageFormats.length; i++) {
			final String ext = imageFormats[i].toString().toLowerCase();
			formats.add(ext);
		}

		imageFormats = formats.toArray();

		for (int i = 0; i < imageFormats.length; i++) {
			final String ext = imageFormats[i].toString();
			fc.addChoosableFileFilter(new DefaultFileFilter("."	+ ext, ext.toUpperCase() + " File  (." + ext + ")"));
		}

		// Adds filter that accepts all supported image formats
		fc.addChoosableFileFilter(new DefaultFileFilter.ImageFileFilter("All Images"));
		fc.setFileFilter(defaultFilter);
		final int rc = fc.showDialog(null,"Save");
		dialogShown = true;

		if (rc != JFileChooser.APPROVE_OPTION) {
			return;
		}
		lastDir = fc.getSelectedFile().getParent();

		filename = fc.getSelectedFile().getAbsolutePath();
		selectedFilter = fc.getFileFilter();

		if (selectedFilter instanceof DefaultFileFilter) {
			final String ext = ((DefaultFileFilter) selectedFilter).getExtension();

			if (!filename.toLowerCase().endsWith(ext)) {
				filename += ext;
			}
		}

		if (new File(filename).exists() && JOptionPane.showConfirmDialog(graphComponent, "Overwrite existing file?") != JOptionPane.YES_OPTION) {
			return;
		}


		try {
			final String ext = filename.substring(filename.lastIndexOf('.') + 1);

			if (ext.equalsIgnoreCase("svg")) {
				final mxSvgCanvas canvas = (mxSvgCanvas) mxCellRenderer.drawCells(graph, null, 1, null, new CanvasFactory() {
					@Override
					public mxICanvas createCanvas(final int width, final int height) {
						final TrackSchemeSvgCanvas lCanvas = new TrackSchemeSvgCanvas(mxDomUtils.createSvgDocument(width, height));
						lCanvas.setEmbedded(true);
						return lCanvas;
					}
				});

				mxUtils.writeFile(mxXmlUtils.getXml(canvas.getDocument()), filename);

			} else if (selectedFilter == vmlFileFilter) {
				mxUtils.writeFile(mxXmlUtils.getXml(mxCellRenderer.createVmlDocument(graph, null, 1, null, null).getDocumentElement()), filename);

			} else if (ext.equalsIgnoreCase("html")) {
				mxUtils.writeFile(mxXmlUtils.getXml(mxCellRenderer.createHtmlDocument(graph, null, 1, null, null).getDocumentElement()), filename);

			} else if (ext.equalsIgnoreCase("mxe") || ext.equalsIgnoreCase("xml")) {
				final mxCodec codec = new mxCodec();
				final String xml = mxXmlUtils.getXml(codec.encode(graph.getModel()));
				mxUtils.writeFile(xml, filename);

			} else if (ext.equalsIgnoreCase("txt")) {
				final String content = mxGdCodec.encode(graph); // .getDocumentString();
				mxUtils.writeFile(content, filename);

			} else if (ext.equalsIgnoreCase("pdf")) {
				exportGraphToPdf(filename);

			} else {
				Color bg = null;

				if ((!ext.equalsIgnoreCase("gif") && !ext.equalsIgnoreCase("png"))
						|| JOptionPane.showConfirmDialog(graphComponent,"Transparent Background?") != JOptionPane.YES_OPTION) {
					bg = graphComponent.getBackground();
				}

				if (selectedFilter == xmlPngFilter || (ext.equalsIgnoreCase("png") && !dialogShown)) {
					saveXmlPng(trackScheme.getGUI(), filename, bg);
				} else {
					final BufferedImage image = mxCellRenderer.createBufferedImage(graph, null, 1, bg, graphComponent.isAntiAlias(), null, graphComponent.getCanvas());

					if (image != null) {
						ImageIO.write(image, ext, new File(filename));
					} else {
						JOptionPane.showMessageDialog(graphComponent, "No Image Data");
					}
				}
			}

		} catch (final Throwable ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(graphComponent, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void exportGraphToPdf(final String filename) {
		final Rectangle bounds = new Rectangle(trackScheme.getGUI().graphComponent.getViewport().getViewSize());
		// step 1
		final com.itextpdf.text.Rectangle pageSize = new com.itextpdf.text.Rectangle( bounds.x, bounds.y, bounds.width, bounds.height );
		final com.itextpdf.text.Document document = new com.itextpdf.text.Document( pageSize );
		// step 2
		PdfWriter writer = null;
		Graphics2D g2 = null;
		try
		{
			writer = PdfWriter.getInstance( document, new FileOutputStream( filename ) );
			// step 3
			document.open();
			// step 4
			final PdfContentByte canvas = writer.getDirectContent();
			g2 = new PdfGraphics2D( canvas, pageSize.getWidth(), pageSize.getHeight() );
			trackScheme.getGUI().graphComponent.getViewport().paintComponents( g2 );
		}
		catch ( final FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( final DocumentException e )
		{
			e.printStackTrace();
		}
		finally
		{
			if ( null != g2 )
				g2.dispose();
			document.close();
		}
	}
}
