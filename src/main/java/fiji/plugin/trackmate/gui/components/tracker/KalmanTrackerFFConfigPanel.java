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
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_EXPECTED_MOVEMENT;

import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.JTextField;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

public class KalmanTrackerFFConfigPanel extends ConfigurationPanel
{
	private static final long serialVersionUID = 1L;

	private final JFormattedTextField tfInitSearchRadius;

	private final JFormattedTextField tfSearchRadius;

	private final JFormattedTextField tfMaxFrameGap;

	private final JTextField tfExpectedMovement;

	public KalmanTrackerFFConfigPanel( final String trackerName, final String infoText, final String spaceUnits )
	{
		setLayout( null );

		final JLabel lbl1 = new JLabel( "Settings for tracker:" );
		lbl1.setBounds( 6, 6, 288, 16 );
		lbl1.setFont( FONT );
		add( lbl1 );

		final JLabel lblTrackerName = new JLabel( trackerName );
		lblTrackerName.setFont( BIG_FONT );
		lblTrackerName.setHorizontalAlignment( SwingConstants.CENTER );
		lblTrackerName.setBounds( 6, 34, 288, 32 );
		add( lblTrackerName );

		final JLabel lblTrackerDescription = new JLabel( "<tracker description>" );
		lblTrackerDescription.setFont( FONT.deriveFont( Font.ITALIC ) );
		lblTrackerDescription.setVerticalAlignment( SwingConstants.TOP );
		lblTrackerDescription.setBounds( 6, 81, 288, 175 );
		lblTrackerDescription.setText( infoText
				.replace( "<br>", "" )
				.replace( "<p>", "<p align=\"justify\">" )
				.replace( "<html>", "<html><p align=\"justify\">" ) );
		add( lblTrackerDescription );

		final JLabel lblInitSearchRadius = new JLabel( "Initial search radius:" );
		lblInitSearchRadius.setFont( FONT );
		lblInitSearchRadius.setBounds( 6, 348, 173, 16 );
		add( lblInitSearchRadius );

		final JLabel lblSearchRadius = new JLabel( "Search radius:" );
		lblSearchRadius.setFont( FONT );
		lblSearchRadius.setBounds( 6, 376, 173, 16 );
		add( lblSearchRadius );

		final JLabel lblMaxFrameGap = new JLabel( "Max frame gap:" );
		lblMaxFrameGap.setFont( FONT );
		lblMaxFrameGap.setBounds( 6, 404, 173, 16 );
		add( lblMaxFrameGap );

		tfInitSearchRadius = new JFormattedTextField( 15. );
		tfInitSearchRadius.setHorizontalAlignment( SwingConstants.CENTER );
		tfInitSearchRadius.setFont( FONT );
		tfInitSearchRadius.setBounds( 167, 348, 60, 28 );
		add( tfInitSearchRadius );
		tfInitSearchRadius.setSize( TEXTFIELD_DIMENSION );

		tfSearchRadius = new JFormattedTextField( 15. );
		tfSearchRadius.setHorizontalAlignment( SwingConstants.CENTER );
		tfSearchRadius.setFont( FONT );
		tfSearchRadius.setBounds( 167, 376, 60, 28 );
		add( tfSearchRadius );
		tfSearchRadius.setSize( TEXTFIELD_DIMENSION );

		tfMaxFrameGap = new JFormattedTextField( 2 );
		tfMaxFrameGap.setHorizontalAlignment( SwingConstants.CENTER );
		tfMaxFrameGap.setFont( FONT );
		tfMaxFrameGap.setBounds( 167, 404, 60, 28 );
		add( tfMaxFrameGap );
		tfMaxFrameGap.setSize( TEXTFIELD_DIMENSION );

		tfExpectedMovement = new JTextField("0;0;0"); // Initialize with default value
        tfExpectedMovement.setHorizontalAlignment(SwingConstants.CENTER);
        tfExpectedMovement.setFont(FONT);
        tfExpectedMovement.setBounds(167, 432, 100, 28); // Adjust position as needed
        add(tfExpectedMovement);

        // Adding input validation
        tfExpectedMovement.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!validateExpectedMovementInput(tfExpectedMovement.getText())) {
                    JOptionPane.showMessageDialog(null, "Invalid input. Please enter a semicolon-separated list of three numbers (e.g., '123;0;0').", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    tfExpectedMovement.setText(String.format("%.1f;%.1f;%.1f", 0, 0, 0));
                }
            }
        });

		final JLabel lblSpaceUnits1 = new JLabel( spaceUnits );
		lblSpaceUnits1.setFont( FONT );
		lblSpaceUnits1.setBounds( 219, 348, 51, 16 );
		add( lblSpaceUnits1 );

		final JLabel lblSpaceUnits2 = new JLabel( spaceUnits );
		lblSpaceUnits2.setFont( FONT );
		lblSpaceUnits2.setBounds( 219, 376, 51, 16 );
		add( lblSpaceUnits2 );

		final JLabel lblFrameUnits = new JLabel( "frames" );
		lblFrameUnits.setFont( FONT );
		lblFrameUnits.setBounds( 219, 404, 51, 16 );
		add( lblFrameUnits );

		// Select text-fields content on focus.
		GuiUtils.selectAllOnFocus( tfInitSearchRadius );
		GuiUtils.selectAllOnFocus( tfMaxFrameGap );
		GuiUtils.selectAllOnFocus( tfSearchRadius );
		GuiUtils.selectAllOnFocus( tfExpectedMovement );
	}

	private boolean validateExpectedMovementInput(String input) {
        String[] parts = input.split(";");
        if (parts.length != 3)
            return false;
        try {
            for (String part : parts) {
                Double.parseDouble(part.trim());
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		tfInitSearchRadius.setValue( settings.get( KEY_LINKING_MAX_DISTANCE ) );
		tfSearchRadius.setValue( settings.get( KEY_KALMAN_SEARCH_RADIUS ) );
		tfMaxFrameGap.setValue( settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
		double[] expectedMovementArray = (double[]) settings.get( KEY_EXPECTED_MOVEMENT );
        tfExpectedMovement.setText( expectedMovementArray[0] + ";" + expectedMovementArray[1] + ";" + expectedMovementArray[2] );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > settings = new HashMap<>();
		settings.put( KEY_LINKING_MAX_DISTANCE, ( ( Number ) tfInitSearchRadius.getValue() ).doubleValue() );
		settings.put( KEY_KALMAN_SEARCH_RADIUS, ( ( Number ) tfSearchRadius.getValue() ).doubleValue() );
		settings.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, ( ( Number ) tfMaxFrameGap.getValue() ).intValue() );
		
		String[] parts = tfExpectedMovement.getText().split(";");
        double[] expectedMovement = new double[3];
        for (int i = 0; i < parts.length; i++) {
            expectedMovement[i] = Double.parseDouble(parts[i].trim());
        }
        settings.put(KEY_EXPECTED_MOVEMENT, expectedMovement);

		return settings;
	}

	@Override
	public void clean()
	{}
}
