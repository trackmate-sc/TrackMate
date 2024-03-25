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

import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Icons.PREVIEW_ICON;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.DoubleConsumer;

import javax.swing.JButton;
import javax.swing.JPanel;

import fiji.plugin.trackmate.Logger;

public class DetectionPreviewPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final String TOOLTIP_PREVIEW = "<html>"
			+ "Preview the current settings on the current frame."
			+ "<p>"
			+ "Advice: change the settings until you get at least <br>"
			+ "<b>all</b> the spots you want, and do not mind the <br>"
			+ "spurious spots too much. You will get a chance to <br>"
			+ "get rid of them later."
			+ "</html>";

	final Logger logger;

	final JButton btnPreview;

	final QualityHistogramChart chart;

	public DetectionPreviewPanel( final DoubleConsumer thresholdUpdater, final String axisLabel )
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0 };
		gridBagLayout.rowWeights = new double[] { 0., 0. };
		gridBagLayout.rowHeights = new int[] { 120, 20 };

		setLayout( gridBagLayout );

		this.chart = new QualityHistogramChart( thresholdUpdater, axisLabel );
		final GridBagConstraints gbcHistogram = new GridBagConstraints();
		gbcHistogram.gridwidth = 2;
		gbcHistogram.insets = new Insets( 0, 0, 5, 0 );
		gbcHistogram.fill = GridBagConstraints.BOTH;
		gbcHistogram.gridx = 0;
		gbcHistogram.gridy = 0;
		add( chart, gbcHistogram );

		final JLabelLogger labelLogger = new JLabelLogger();
		labelLogger.setText( "   " );
		final GridBagConstraints gbcLabelLogger = new GridBagConstraints();
		gbcLabelLogger.insets = new Insets( 5, 5, 0, 5 );
		gbcLabelLogger.fill = GridBagConstraints.BOTH;
		gbcLabelLogger.gridx = 0;
		gbcLabelLogger.gridy = 1;
		add( labelLogger, gbcLabelLogger );
		this.logger = labelLogger.getLogger();

		this.btnPreview = new JButton( "Preview", PREVIEW_ICON );
		btnPreview.setToolTipText( TOOLTIP_PREVIEW );
		final GridBagConstraints gbcBtnPreview = new GridBagConstraints();
		gbcBtnPreview.anchor = GridBagConstraints.NORTHEAST;
		gbcBtnPreview.insets = new Insets( 5, 5, 0, 0 );
		gbcBtnPreview.gridx = 1;
		gbcBtnPreview.gridy = 1;
		this.add( btnPreview, gbcBtnPreview );
		btnPreview.setFont( SMALL_FONT );
	}
}
