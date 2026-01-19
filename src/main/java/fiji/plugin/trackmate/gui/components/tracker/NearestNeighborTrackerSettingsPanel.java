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
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTrackerFactory;

public class NearestNeighborTrackerSettingsPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private JFormattedTextField maxDistField;

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
		setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );

		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 164, 40, 54, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 40, 225, 30, 60 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblSettingsForTracker = new JLabel();
		lblSettingsForTracker.setFont( FONT );
		final GridBagConstraints gbcLblSettingsForTracker = new GridBagConstraints();
		gbcLblSettingsForTracker.fill = GridBagConstraints.BOTH;
		gbcLblSettingsForTracker.insets = new Insets( 0, 0, 5, 0 );
		gbcLblSettingsForTracker.gridwidth = 3;
		gbcLblSettingsForTracker.gridx = 0;
		gbcLblSettingsForTracker.gridy = 0;
		add( lblSettingsForTracker, gbcLblSettingsForTracker );

		final JLabel labelTracker = new JLabel( trackerName, NearestNeighborTrackerFactory.ICON, SwingConstants.CENTER );
		labelTracker.setFont( BIG_FONT );
		final GridBagConstraints gbcLabelTracker = new GridBagConstraints();
		gbcLabelTracker.fill = GridBagConstraints.BOTH;
		gbcLabelTracker.insets = new Insets( 0, 0, 5, 0 );
		gbcLabelTracker.gridwidth = 3;
		gbcLabelTracker.gridx = 0;
		gbcLabelTracker.gridy = 1;
		add( labelTracker, gbcLabelTracker );

		final GridBagConstraints gbcLabelTrackerDescription = new GridBagConstraints();
		gbcLabelTrackerDescription.anchor = GridBagConstraints.NORTH;
		gbcLabelTrackerDescription.fill = GridBagConstraints.BOTH;
		gbcLabelTrackerDescription.insets = new Insets( 0, 0, 5, 0 );
		gbcLabelTrackerDescription.gridwidth = 3;
		gbcLabelTrackerDescription.gridx = 0;
		gbcLabelTrackerDescription.gridy = 2;
		add( GuiUtils.textInScrollPanel( GuiUtils.infoDisplay( infoText ) ), gbcLabelTrackerDescription );

		final JLabel lblMaximalLinkingDistance = new JLabel( "Maximal linking distance: " );
		lblMaximalLinkingDistance.setFont( FONT );
		final GridBagConstraints gbcLblMaximalLinkingDistance = new GridBagConstraints();
		gbcLblMaximalLinkingDistance.fill = GridBagConstraints.BOTH;
		gbcLblMaximalLinkingDistance.insets = new Insets( 0, 0, 0, 5 );
		gbcLblMaximalLinkingDistance.gridx = 0;
		gbcLblMaximalLinkingDistance.gridy = 3;
		add( lblMaximalLinkingDistance, gbcLblMaximalLinkingDistance );

		maxDistField = new JFormattedTextField( 15. );
		maxDistField.setHorizontalAlignment( JTextField.CENTER );
		maxDistField.setFont( FONT );
		final GridBagConstraints gbcMaxDistField = new GridBagConstraints();
		gbcMaxDistField.fill = GridBagConstraints.BOTH;
		gbcMaxDistField.insets = new Insets( 0, 0, 0, 5 );
		gbcMaxDistField.gridx = 1;
		gbcMaxDistField.gridy = 3;
		add( maxDistField, gbcMaxDistField );

		// Select text-fields content on focus.
		GuiUtils.selectAllOnFocus( maxDistField );

		final JLabel labelUnits = new JLabel( spaceUnits );
		labelUnits.setFont( FONT );
		final GridBagConstraints gbcLabelUnits = new GridBagConstraints();
		gbcLabelUnits.anchor = GridBagConstraints.WEST;
		gbcLabelUnits.fill = GridBagConstraints.VERTICAL;
		gbcLabelUnits.gridx = 2;
		gbcLabelUnits.gridy = 3;
		add( labelUnits, gbcLabelUnits );
	}

	@Override
	public void clean()
	{}
}
