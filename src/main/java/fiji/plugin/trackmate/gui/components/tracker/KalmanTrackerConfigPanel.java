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
package fiji.plugin.trackmate.gui.components.tracker;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.tracking.kalman.KalmanTrackerFactory;

public class KalmanTrackerConfigPanel extends ConfigurationPanel
{
	private static final long serialVersionUID = 1L;

	private final JFormattedTextField tfInitSearchRadius;

	private final JFormattedTextField tfSearchRadius;

	private final JFormattedTextField tfMaxFrameGap;

	public KalmanTrackerConfigPanel( final String trackerName, final String infoText, final String spaceUnits )
	{
		setLayout( new GridBagLayout() );
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets( 5, 5, 5, 5 );
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Tracker Name
		final JLabel lblTrackerName = new JLabel( trackerName, KalmanTrackerFactory.ICON, SwingConstants.CENTER );
		lblTrackerName.setFont( BIG_FONT );
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.CENTER;
		add( lblTrackerName, gbc );

		// Tracker Description
		final JEditorPane lblTrackerDescription = GuiUtils.infoDisplay(
				infoText.replace( "</html>", "" )
						+ "<p>Online documentation: <br/>"
						+ "<a href='" + KalmanTrackerFactory.DOC_URL + "'>"
						+ KalmanTrackerFactory.DOC_URL
						+ "</a></html>",
				true );
		lblTrackerDescription.setFont( FONT.deriveFont( Font.ITALIC ) );
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.weighty = 1.;
		gbc.anchor = GridBagConstraints.NORTH;
		add( lblTrackerDescription, gbc );

		// Initial Search Radius Panel
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.weighty = 0.;
		gbc.anchor = GridBagConstraints.WEST;

		final JLabel lblInitSearchRadius = new JLabel( "Initial search radius:" );
		lblInitSearchRadius.setFont( FONT );
		gbc.gridx = 0;
		add( lblInitSearchRadius, gbc );

		tfInitSearchRadius = new JFormattedTextField( 15. );
		tfInitSearchRadius.setHorizontalAlignment( SwingConstants.CENTER );
		tfInitSearchRadius.setFont( FONT );
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		add( tfInitSearchRadius, gbc );

		final JLabel lblSpaceUnits1 = new JLabel( spaceUnits );
		lblSpaceUnits1.setFont( FONT );
		gbc.gridx = 2;
		gbc.weightx = 0.0;
		add( lblSpaceUnits1, gbc );

		// Search Radius Panel
		gbc.gridy = 3;
		gbc.gridx = 0;
		gbc.weightx = 0.0;

		final JLabel lblSearchRadius = new JLabel( "Search radius:" );
		lblSearchRadius.setFont( FONT );
		add( lblSearchRadius, gbc );

		tfSearchRadius = new JFormattedTextField( 15. );
		tfSearchRadius.setHorizontalAlignment( SwingConstants.CENTER );
		tfSearchRadius.setFont( FONT );
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		add( tfSearchRadius, gbc );

		final JLabel lblSpaceUnits2 = new JLabel( spaceUnits );
		lblSpaceUnits2.setFont( FONT );
		gbc.gridx = 2;
		gbc.weightx = 0.0;
		add( lblSpaceUnits2, gbc );

		// Max Frame Gap Panel
		gbc.gridy = 4;
		gbc.gridx = 0;
		gbc.weightx = 0.0;

		final JLabel lblMaxFrameGap = new JLabel( "Max frame gap:" );
		lblMaxFrameGap.setFont( FONT );
		add( lblMaxFrameGap, gbc );

		tfMaxFrameGap = new JFormattedTextField( 2 );
		tfMaxFrameGap.setHorizontalAlignment( SwingConstants.CENTER );
		tfMaxFrameGap.setFont( FONT );
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		add( tfMaxFrameGap, gbc );

		final JLabel lblFrameUnits = new JLabel( "frames" );
		lblFrameUnits.setFont( FONT );
		gbc.gridx = 2;
		gbc.weightx = 0.0;
		add( lblFrameUnits, gbc );

		// Add vertical space at the bottom
		gbc.gridy = 5;
		gbc.gridx = 0;
		gbc.gridwidth = 3;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.VERTICAL;
		add( Box.createVerticalStrut( 80 ), gbc );

		// Select text-fields content on focus.
		GuiUtils.selectAllOnFocus( tfInitSearchRadius );
		GuiUtils.selectAllOnFocus( tfMaxFrameGap );
		GuiUtils.selectAllOnFocus( tfSearchRadius );
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		tfInitSearchRadius.setValue( settings.get( KEY_LINKING_MAX_DISTANCE ) );
		tfSearchRadius.setValue( settings.get( KEY_KALMAN_SEARCH_RADIUS ) );
		tfMaxFrameGap.setValue( settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > settings = new HashMap<>();
		settings.put( KEY_LINKING_MAX_DISTANCE, ( ( Number ) tfInitSearchRadius.getValue() ).doubleValue() );
		settings.put( KEY_KALMAN_SEARCH_RADIUS, ( ( Number ) tfSearchRadius.getValue() ).doubleValue() );
		settings.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, ( ( Number ) tfMaxFrameGap.getValue() ).intValue() );
		return settings;
	}

	@Override
	public void clean()
	{}
}
