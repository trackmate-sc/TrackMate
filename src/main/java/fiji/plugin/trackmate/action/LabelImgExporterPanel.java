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
package fiji.plugin.trackmate.action;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class LabelImgExporterPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JCheckBox exportSpotsAsDots;

	private final JCheckBox exportTracksOnly;

	public LabelImgExporterPanel()
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout( gridBagLayout );

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets( 5, 5, 5, 5 );
		gbc.gridx = 0;
		gbc.gridy = 0;

		exportSpotsAsDots = new JCheckBox( "Export spots as single pixels", false );
		add( exportSpotsAsDots, gbc );

		exportTracksOnly = new JCheckBox( "Export only spots in tracks", false );
		gbc.gridy++;
		add( exportTracksOnly, gbc );

	}

	public boolean isExportSpotsAsDots()
	{
		return exportSpotsAsDots.isSelected();
	}

	public boolean isExportTracksOnly()
	{
		return exportTracksOnly.isSelected();
	}
}
