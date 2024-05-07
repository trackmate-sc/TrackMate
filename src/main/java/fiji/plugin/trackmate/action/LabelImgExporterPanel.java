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
package fiji.plugin.trackmate.action;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fiji.plugin.trackmate.action.LabelImgExporter.LabelIdPainting;
import fiji.plugin.trackmate.gui.Fonts;

public class LabelImgExporterPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JCheckBox exportSpotsAsDots;

	private final JCheckBox exportTracksOnly;

	private final JComboBox< LabelIdPainting > labelIdPainting;

	public LabelImgExporterPanel()
	{
		setPreferredSize( new Dimension( 250, 200 ) );
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowWeights = new double[] { 0., 0., 0., 1. };
		setLayout( gridBagLayout );

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets( 5, 5, 5, 5 );
		gbc.gridx = 0;
		gbc.gridy = 0;

		exportSpotsAsDots = new JCheckBox( "Export spots as single pixels", false );
		add( exportSpotsAsDots, gbc );

		exportTracksOnly = new JCheckBox( "Export only spots in tracks", false );
		gbc.gridy++;
		add( exportTracksOnly, gbc );

		gbc.gridy++;
		add( new JLabel( "Label ID is:" ), gbc );

		labelIdPainting = new JComboBox<>( LabelIdPainting.values() );
		gbc.gridy++;
		add( labelIdPainting, gbc );

		final JLabel info = new JLabel();
		info.setFont( Fonts.SMALL_FONT );
		gbc.gridy++;
		gbc.fill = GridBagConstraints.BOTH;
		add( info, gbc );

		labelIdPainting.addItemListener( e -> info.setText( "<html>"
						+ ( ( LabelIdPainting ) labelIdPainting.getSelectedItem() ).getInfo()
						+ "</html>" ) );
		labelIdPainting.setSelectedIndex( 1 );
		labelIdPainting.setSelectedIndex( 0 );
	}

	public boolean isExportSpotsAsDots()
	{
		return exportSpotsAsDots.isSelected();
	}

	public boolean isExportTracksOnly()
	{
		return exportTracksOnly.isSelected();
	}

	public LabelIdPainting labelIdPainting()
	{
		return ( LabelIdPainting ) labelIdPainting.getSelectedItem();
	}
}
