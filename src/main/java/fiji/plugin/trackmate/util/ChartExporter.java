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

import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.swing.JPanel;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * A collection of static utilities made to export a JPanel to various scalable
 * file format.
 * 
 * @author Jean-Yves Tinevez, 2011 - 2021
 */
public class ChartExporter
{

	/**
	 * Exports a JPanel to a SVG file.
	 * 
	 * @param panel
	 *            the JPanel to export
	 * @param svgFile
	 *            the output file.
	 * @throws IOException
	 *             if writing the svgFile fails.
	 */
	public static void exportChartAsSVG( final JPanel panel, final File svgFile ) throws IOException
	{
		// Get a DOMImplementation and create an XML document
		final org.w3c.dom.DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
		final org.w3c.dom.Document document = domImpl.createDocument( null, "svg", null );

		// Create an instance of the SVG Generator
		final SVGGraphics2D svgGenerator = new SVGGraphics2D( document );
		// draw the chart in the SVG generator
		panel.paint( svgGenerator );

		// Write svg file
		try (final OutputStream outputStream = new FileOutputStream( svgFile );
				final Writer out = new OutputStreamWriter( outputStream, "UTF-8" ))
		{
			svgGenerator.stream( out, true );
		}
	}

	/**
	 * Exports a JPanel to a PDF file.
	 * 
	 * @param panel
	 *            the JPanel to export
	 * @param pdfFile
	 *            the output file.
	 * @throws IOException
	 *             if writing the pdfFile fails.
	 * @throws DocumentException
	 */
	public static void exportChartAsPDF( final JPanel panel, final File pdfFile ) throws IOException, DocumentException
	{
		// step 1
		final Rectangle bounds = panel.getBounds();
		final com.itextpdf.text.Rectangle pageSize = new com.itextpdf.text.Rectangle(
				0,
				0,
				bounds.width,
				bounds.height );
		final com.itextpdf.text.Document document = new com.itextpdf.text.Document( pageSize );
		// step 2
		final PdfWriter writer = PdfWriter.getInstance( document, new FileOutputStream( pdfFile ) );
		// step 3
		document.open();
		// step 4
		final PdfContentByte cb = writer.getDirectContent();
		final PdfGraphics2D g2 = new PdfGraphics2D( cb, panel.getWidth(), panel.getHeight() );
		panel.print( g2 );
		g2.dispose();
		// step 5
		document.close();
	}
}
