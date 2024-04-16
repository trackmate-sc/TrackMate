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
package fiji.plugin.trackmate.gui.components.tracker;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.TEXTFIELD_DIMENSION;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

public class NearestNeighborTrackerSettingsPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private JFormattedTextField maxDistField;

	private JLabel labelTrackerDescription;

	private JLabel labelUnits;

	private JLabel labelTracker;

	private final String infoText;

	private final String trackerName;

	private final String spaceUnits;

	public NearestNeighborTrackerSettingsPanel( final String trackerName, final String infoText, final String spaceUnits )
	{
		this.trackerName = trackerName;
		this.infoText = infoText;
		this.spaceUnits = spaceUnits;
		initGUI();
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > settings = new HashMap<>();
		settings.put( KEY_LINKING_MAX_DISTANCE, ( ( Number ) maxDistField.getValue() ).doubleValue() );
		return settings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		maxDistField.setValue( settings.get( KEY_LINKING_MAX_DISTANCE ) );
	}

	private void initGUI()
	{

		setLayout( null );

		final JLabel lblSettingsForTracker = new JLabel( "Settings for tracker:" );
		lblSettingsForTracker.setBounds( 10, 11, 280, 20 );
		lblSettingsForTracker.setFont( FONT );
		add( lblSettingsForTracker );

		labelTracker = new JLabel( trackerName );
		labelTracker.setFont( BIG_FONT );
		labelTracker.setHorizontalAlignment( SwingConstants.CENTER );
		labelTracker.setBounds( 10, 42, 280, 20 );
		add( labelTracker );

		labelTrackerDescription = new JLabel( "<tracker description>" );
		labelTrackerDescription.setFont( FONT.deriveFont( Font.ITALIC ) );
		labelTrackerDescription.setBounds( 10, 67, 280, 225 );
		labelTrackerDescription.setText( infoText.replace( "<br>", "" ).replace( "<p>", "<p align=\"justify\">" ).replace( "<html>", "<html><p align=\"justify\">" ) );
		add( labelTrackerDescription );

		final JLabel lblMaximalLinkingDistance = new JLabel( "Maximal linking distance: " );
		lblMaximalLinkingDistance.setFont( FONT );
		lblMaximalLinkingDistance.setBounds( 10, 314, 164, 20 );
		add( lblMaximalLinkingDistance );

		maxDistField = new JFormattedTextField( 15. );
		maxDistField.setFont( FONT );
		maxDistField.setBounds( 184, 316, 62, 16 );
		maxDistField.setSize( TEXTFIELD_DIMENSION );
		add( maxDistField );

		labelUnits = new JLabel( spaceUnits );
		labelUnits.setFont( FONT );
		labelUnits.setBounds( 236, 314, 34, 20 );
		add( labelUnits );

		// Select text-fields content on focus.
		GuiUtils.selectAllOnFocus( maxDistField );
	}

	@Override
	public void clean()
	{}
}
