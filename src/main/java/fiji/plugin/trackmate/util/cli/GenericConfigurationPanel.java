/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.util.cli;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder.ConfigPanel;

public class GenericConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	protected final ConfigPanel mainPanel;

	protected final Configurator config;

	public GenericConfigurationPanel(
			final Configurator config,
			final String title,
			final Icon icon,
			final String docURL )
	{
		this.config = config;

		final BorderLayout borderLayout = new BorderLayout();
		setLayout( borderLayout );

		/*
		 * HEADER
		 */

		final JPanel header = new JPanel();
		header.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		header.setLayout( new BoxLayout( header, BoxLayout.Y_AXIS ) );

		final JLabel lblDetector = new JLabel( title, icon, JLabel.RIGHT );
		lblDetector.setFont( BIG_FONT );
		lblDetector.setHorizontalAlignment( SwingConstants.CENTER );
		lblDetector.setAlignmentX( JLabel.CENTER_ALIGNMENT );
		header.add( lblDetector );
		if ( docURL != null )
		{
			final JEditorPane infoDisplay = GuiUtils.infoDisplay( "<html>" + "Documentation for this module "
					+ "<a href=\"" + docURL + "\">on the ImageJ Wiki</a>."
					+ "</html>", false );
			infoDisplay.setMaximumSize( new Dimension( 100_000, 40 ) );
			header.add( Box.createVerticalStrut( 5 ) );
			header.add( infoDisplay );
		}
		add( header, BorderLayout.NORTH );

		/*
		 * CONFIG
		 */

		this.mainPanel = ConfigGuiBuilder.build( config );
		final JScrollPane scrollPane = new JScrollPane( mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setBorder( null );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
		add( scrollPane, BorderLayout.CENTER );
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		try
		{
			TrackMateSettingsBuilder.fromTrackMateSettings( settings, config );
		}
		catch ( final IllegalArgumentException e )
		{
			// Incompatible settings, we keep the defaults.
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			mainPanel.refresh();
		}
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > map = new HashMap<>();
		TrackMateSettingsBuilder.toTrackMateSettings( map, config );
		return map;
	}

	@Override
	public void clean()
	{}
}
