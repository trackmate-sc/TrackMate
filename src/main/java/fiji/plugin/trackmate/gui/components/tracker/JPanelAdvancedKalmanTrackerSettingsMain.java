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
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Fonts.TEXTFIELD_DIMENSION;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.tracking.jaqaman.LAPUtils;

public class JPanelAdvancedKalmanTrackerSettingsMain extends javax.swing.JPanel
{

	private static final long serialVersionUID = -1L;

	private final JLabel lblSplittingMaxDistanceUnit;

	private final JFormattedTextField txtfldSplittingMaxDistance;

	private final JCheckBox chkboxAllowSplitting;

	private final JPanelFeatureSelectionGui panelMergingFeatures;

	private final JPanelFeatureSelectionGui panelKalmanFeatures;

	private final JPanelFeatureSelectionGui panelSplittingFeatures;

	private final JScrollPane scrpneMergingFeatures;

	private final JLabel lblMergingMaxDistanceUnit;

	private final JFormattedTextField txtfldMergingMaxDistance;

	private final JCheckBox chkboxAllowMerging;

	private final JScrollPane scrpneSplittingFeatures;

	private final JLabel lblMaxFrameGapUnits;

	private final JLabel lblSearchRadiusUnits;

	private final JLabel lblInitialSearchRadiusUnits;

	private final JFormattedTextField txtfldMaxFrameGap;

	private final JFormattedTextField txtfldSearchRadius;

	private final JFormattedTextField txtfldInitialSearchRadius;

	private final JLabel lbl10;

	private final JLabel lbl15;

	private final JLabel lbl13;

	private final JLabel lbl16;

	public JPanelAdvancedKalmanTrackerSettingsMain( final String trackerName, final String spaceUnits, final Collection< String > features, final Map< String, String > featureNames )
	{
		final DecimalFormat decimalFormat = new DecimalFormat( "0.0" );
		final DecimalFormat integerFormat = new DecimalFormat( "0" );

		this.setPreferredSize( new Dimension( 280, 1000 ) );
		final GridBagLayout thisLayout = new GridBagLayout();
		thisLayout.columnWidths = new int[] { 180, 50, 50 };
		thisLayout.columnWeights = new double[] { 0.1, 0.8, 0.1 };
		thisLayout.rowHeights = new int[] { 10, 10, 10, 15, 15, 15, 15, 15, 95, 10, 15, 15, 15, 15, 95, 10, 15, 15, 15, 15, 95, 15 };
		thisLayout.rowWeights = new double[] { 0.0, 0.15, 0.1, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.1, 0.01, 0.01, 0.01, 0.01, 0.01, 0.1, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.6 };
		this.setLayout( thisLayout );

		final JLabel jLabel1 = new JLabel();
		this.add( jLabel1, new GridBagConstraints( 0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 10, 10, 0, 0 ), 0, 0 ) );
		jLabel1.setText( "Settings for tracker:" );
		jLabel1.setFont( FONT );

		final JLabel lblTrackerName = new JLabel();
		this.add( lblTrackerName, new GridBagConstraints( 0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		lblTrackerName.setHorizontalTextPosition( SwingConstants.CENTER );
		lblTrackerName.setHorizontalAlignment( SwingConstants.CENTER );
		lblTrackerName.setFont( BIG_FONT );
		lblTrackerName.setText( trackerName );

		int ycur = 2;
		final JLabel lbl2 = new JLabel();
		this.add( lbl2, new GridBagConstraints( 0, ycur, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl2.setText( "Frame to frame linking:" );
		lbl2.setFont( BIG_FONT.deriveFont( Font.BOLD ) );

		ycur++;
		final JLabel lbl3 = new JLabel();
		this.add( lbl3, new GridBagConstraints( 0, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl3.setText( "Initial search radius:" );
		lbl3.setFont( SMALL_FONT );

		txtfldInitialSearchRadius = new JFormattedTextField( decimalFormat );
		this.add( txtfldInitialSearchRadius, new GridBagConstraints( 1, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldInitialSearchRadius.setFont( SMALL_FONT );
		txtfldInitialSearchRadius.setSize( TEXTFIELD_DIMENSION );
		txtfldInitialSearchRadius.setHorizontalAlignment( JFormattedTextField.CENTER );

		lblInitialSearchRadiusUnits = new JLabel();
		this.add( lblInitialSearchRadiusUnits, new GridBagConstraints( 2, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 5, 0, 0 ), 0, 0 ) );
		lblInitialSearchRadiusUnits.setFont( SMALL_FONT );
		lblInitialSearchRadiusUnits.setText( spaceUnits );

		ycur++;
		final JLabel lbl3b = new JLabel();
		this.add( lbl3b, new GridBagConstraints( 0, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl3b.setText( "Search radius:" );
		lbl3b.setFont( SMALL_FONT );

		txtfldSearchRadius = new JFormattedTextField( decimalFormat );
		this.add( txtfldSearchRadius, new GridBagConstraints( 1, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldSearchRadius.setFont( SMALL_FONT );
		txtfldSearchRadius.setSize( TEXTFIELD_DIMENSION );
		txtfldSearchRadius.setHorizontalAlignment( JFormattedTextField.CENTER );

		lblSearchRadiusUnits = new JLabel();
		this.add( lblSearchRadiusUnits, new GridBagConstraints( 2, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 5, 0, 0 ), 0, 0 ) );
		lblSearchRadiusUnits.setFont( SMALL_FONT );
		lblSearchRadiusUnits.setText( spaceUnits );

		ycur++;
		final JLabel lbl3c = new JLabel();
		this.add( lbl3c, new GridBagConstraints( 0, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl3c.setText( "Max Frame Gap:" );
		lbl3c.setFont( SMALL_FONT );

		txtfldMaxFrameGap = new JFormattedTextField( integerFormat );
		this.add( txtfldMaxFrameGap, new GridBagConstraints( 1, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldMaxFrameGap.setFont( SMALL_FONT );
		txtfldMaxFrameGap.setSize( TEXTFIELD_DIMENSION );
		txtfldMaxFrameGap.setHorizontalAlignment( JFormattedTextField.CENTER );

		lblMaxFrameGapUnits = new JLabel( "frames" );
		this.add( lblMaxFrameGapUnits, new GridBagConstraints( 2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 5, 0, 0 ), 0, 0 ) );
		lblMaxFrameGapUnits.setFont( SMALL_FONT );

		ycur += 2;
		final JLabel lbl4 = new JLabel();
		this.add( lbl4, new GridBagConstraints( 0, ycur, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl4.setText( "Feature penalties:" );
		lbl4.setFont( SMALL_FONT );

		ycur++;
		final JScrollPane scrpneLinkingFeatures = new JScrollPane();
		final MouseWheelListener[] l = scrpneLinkingFeatures.getMouseWheelListeners();
		scrpneLinkingFeatures.removeMouseWheelListener( l[ 0 ] );
		this.add( scrpneLinkingFeatures, new GridBagConstraints( 0, ycur, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		scrpneLinkingFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrpneLinkingFeatures.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		panelKalmanFeatures = new JPanelFeatureSelectionGui();
		panelKalmanFeatures.setDisplayFeatures( features, featureNames );
		scrpneLinkingFeatures.setViewportView( panelKalmanFeatures );

		// Splitting

		ycur++;

		final JLabel lbl9 = new JLabel();
		this.add( lbl9, new GridBagConstraints( 0, ycur, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 20, 10, 0, 10 ), 0, 0 ) );
		lbl9.setText( "Track segment splitting:" );
		lbl9.setFont( BIG_FONT.deriveFont( Font.BOLD ) );

		ycur++;

		chkboxAllowSplitting = new JCheckBox();
		this.add( chkboxAllowSplitting, new GridBagConstraints( 0, ycur, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		chkboxAllowSplitting.setText( "Allow track segment splitting" );
		chkboxAllowSplitting.setFont( SMALL_FONT );

		ycur++;

		lbl10 = new JLabel();
		this.add( lbl10, new GridBagConstraints( 0, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 0 ), 0, 0 ) );
		lbl10.setText( "Max distance:" );
		lbl10.setFont( SMALL_FONT );

		txtfldSplittingMaxDistance = new JFormattedTextField( decimalFormat );
		this.add( txtfldSplittingMaxDistance, new GridBagConstraints( 1, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldSplittingMaxDistance.setSize( TEXTFIELD_DIMENSION );
		txtfldSplittingMaxDistance.setFont( SMALL_FONT );
		txtfldSplittingMaxDistance.setHorizontalAlignment( JFormattedTextField.CENTER );

		lblSplittingMaxDistanceUnit = new JLabel();
		this.add( lblSplittingMaxDistanceUnit, new GridBagConstraints( 2, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 5, 0, 0 ), 0, 0 ) );
		lblSplittingMaxDistanceUnit.setFont( SMALL_FONT );
		lblSplittingMaxDistanceUnit.setText( spaceUnits );

		ycur += 2;

		lbl15 = new JLabel();
		this.add( lbl15, new GridBagConstraints( 0, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl15.setText( "Feature penalties:" );
		lbl15.setFont( SMALL_FONT );

		ycur++;

		scrpneSplittingFeatures = new JScrollPane();
		final MouseWheelListener[] l2 = scrpneSplittingFeatures.getMouseWheelListeners();
		scrpneSplittingFeatures.removeMouseWheelListener( l2[ 0 ] );
		this.add( scrpneSplittingFeatures, new GridBagConstraints( 0, ycur, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		scrpneSplittingFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrpneSplittingFeatures.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		panelSplittingFeatures = new JPanelFeatureSelectionGui();
		panelSplittingFeatures.setDisplayFeatures( features, featureNames );
		scrpneSplittingFeatures.setViewportView( panelSplittingFeatures );

		// Merging
		ycur++;
		final JLabel lbl12 = new JLabel();
		this.add( lbl12, new GridBagConstraints( 0, ycur, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 20, 10, 0, 10 ), 0, 0 ) );
		lbl12.setText( "Track segment merging:" );
		lbl12.setFont( BIG_FONT.deriveFont( Font.BOLD ) );

		ycur++;
		chkboxAllowMerging = new JCheckBox();
		this.add( chkboxAllowMerging, new GridBagConstraints( 0, ycur, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		chkboxAllowMerging.setText( "Allow track segment merging" );
		chkboxAllowMerging.setFont( SMALL_FONT );

		ycur++;
		lbl13 = new JLabel();
		this.add( lbl13, new GridBagConstraints( 0, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 0 ), 0, 0 ) );
		lbl13.setText( "Max distance:" );
		lbl13.setFont( SMALL_FONT );

		txtfldMergingMaxDistance = new JFormattedTextField( decimalFormat );
		this.add( txtfldMergingMaxDistance, new GridBagConstraints( 1, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldMergingMaxDistance.setSize( TEXTFIELD_DIMENSION );
		txtfldMergingMaxDistance.setFont( SMALL_FONT );
		txtfldMergingMaxDistance.setHorizontalAlignment( JFormattedTextField.CENTER );

		lblMergingMaxDistanceUnit = new JLabel();
		this.add( lblMergingMaxDistanceUnit, new GridBagConstraints( 2, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 5, 0, 0 ), 0, 0 ) );
		lblMergingMaxDistanceUnit.setFont( SMALL_FONT );
		lblMergingMaxDistanceUnit.setText( spaceUnits );

		ycur += 2;

		lbl16 = new JLabel();
		this.add( lbl16, new GridBagConstraints( 0, ycur, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl16.setText( "Feature penalties:" );
		lbl16.setFont( SMALL_FONT );

		ycur++;
		scrpneMergingFeatures = new JScrollPane();
		final MouseWheelListener[] l3 = scrpneMergingFeatures.getMouseWheelListeners();
		scrpneMergingFeatures.removeMouseWheelListener( l3[ 0 ] );
		this.add( scrpneMergingFeatures, new GridBagConstraints( 0, ycur, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		scrpneMergingFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrpneMergingFeatures.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		panelMergingFeatures = new JPanelFeatureSelectionGui();
		panelMergingFeatures.setDisplayFeatures( features, featureNames );
		scrpneMergingFeatures.setViewportView( panelMergingFeatures );

		// Select text-fields content on focus.
		GuiUtils.selectAllOnFocus( txtfldInitialSearchRadius );
		GuiUtils.selectAllOnFocus( txtfldMaxFrameGap );
		GuiUtils.selectAllOnFocus( txtfldSearchRadius );
		GuiUtils.selectAllOnFocus( txtfldMergingMaxDistance );
		GuiUtils.selectAllOnFocus( txtfldSplittingMaxDistance );

		// Disable feature panels if corresponding event is unselected.
		chkboxAllowSplitting.addActionListener( e -> setEnabled( new Component[] {
				lbl10, txtfldSplittingMaxDistance, lblSplittingMaxDistanceUnit,
				lbl15, scrpneSplittingFeatures, panelSplittingFeatures },
				chkboxAllowSplitting.isSelected() ) );

		chkboxAllowMerging.addActionListener( e -> setEnabled( new Component[] {
				lbl13, txtfldMergingMaxDistance, lblMergingMaxDistanceUnit,
				lbl16, scrpneMergingFeatures, panelMergingFeatures },
				chkboxAllowMerging.isSelected() ) );
	}

	/*
	 * PUBLIC METHODS
	 */

	@SuppressWarnings( "unchecked" )
	void echoSettings( final Map< String, Object > settings )
	{
		txtfldInitialSearchRadius.setValue( settings.get( KEY_LINKING_MAX_DISTANCE ) );
		if ( settings.get( KEY_KALMAN_SEARCH_RADIUS ) == null )
			txtfldSearchRadius.setValue( DEFAULT_KALMAN_SEARCH_RADIUS );
		else
			txtfldSearchRadius.setValue( settings.get( KEY_KALMAN_SEARCH_RADIUS ) );
		txtfldMaxFrameGap.setValue( settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
		panelKalmanFeatures.setSelectedFeaturePenalties( ( Map< String, Double > ) settings.get( KEY_LINKING_FEATURE_PENALTIES ) );

		chkboxAllowSplitting.setSelected( ( Boolean ) settings.get( KEY_ALLOW_TRACK_SPLITTING ) );
		txtfldSplittingMaxDistance.setValue( settings.get( KEY_SPLITTING_MAX_DISTANCE ) );
		panelSplittingFeatures.setSelectedFeaturePenalties( ( Map< String, Double > ) settings.get( KEY_SPLITTING_FEATURE_PENALTIES ) );

		chkboxAllowMerging.setSelected( ( Boolean ) settings.get( KEY_ALLOW_TRACK_MERGING ) );
		txtfldMergingMaxDistance.setValue( settings.get( KEY_MERGING_MAX_DISTANCE ) );
		panelMergingFeatures.setSelectedFeaturePenalties( ( Map< String, Double > ) settings.get( KEY_MERGING_FEATURE_PENALTIES ) );

		setEnabled( new Component[] {
				lbl10, txtfldSplittingMaxDistance, lblSplittingMaxDistanceUnit,
				lbl15, scrpneSplittingFeatures, panelSplittingFeatures },
				chkboxAllowSplitting.isSelected() );

		setEnabled( new Component[] {
				lbl13, txtfldMergingMaxDistance, lblMergingMaxDistanceUnit,
				lbl16, scrpneMergingFeatures, panelMergingFeatures },
				chkboxAllowMerging.isSelected() );
	}

	/**
	 * @return a new settings {@link Map} with values taken from this panel.
	 */
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > settings = getDefaultKalmanSettingsMap();

		settings.put( KEY_LINKING_MAX_DISTANCE, ( ( Number ) txtfldInitialSearchRadius.getValue() ).doubleValue() );
		settings.put( KEY_KALMAN_SEARCH_RADIUS, ( ( Number ) txtfldSearchRadius.getValue() ).doubleValue() );
		settings.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, ( ( Number ) txtfldMaxFrameGap.getValue() ).intValue() );
		settings.put( KEY_LINKING_FEATURE_PENALTIES, panelKalmanFeatures.getFeaturePenalties() );

		settings.put( KEY_ALLOW_GAP_CLOSING, false );

		settings.put( KEY_ALLOW_TRACK_SPLITTING, chkboxAllowSplitting.isSelected() );
		settings.put( KEY_SPLITTING_MAX_DISTANCE, ( ( Number ) txtfldSplittingMaxDistance.getValue() ).doubleValue() );
		settings.put( KEY_SPLITTING_FEATURE_PENALTIES, panelSplittingFeatures.getFeaturePenalties() );

		settings.put( KEY_ALLOW_TRACK_MERGING, chkboxAllowMerging.isSelected() );
		settings.put( KEY_MERGING_MAX_DISTANCE, ( ( Number ) txtfldMergingMaxDistance.getValue() ).doubleValue() );
		settings.put( KEY_MERGING_FEATURE_PENALTIES, panelMergingFeatures.getFeaturePenalties() );

		return settings;
	}

	public static final Map< String, Object > getDefaultKalmanSettingsMap()
	{
		final Map< String, Object > settings = LAPUtils.getDefaultSegmentSettingsMap();
		settings.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
		settings.put( KEY_KALMAN_SEARCH_RADIUS, DEFAULT_KALMAN_SEARCH_RADIUS );
		settings.put( KEY_LINKING_FEATURE_PENALTIES, new HashMap<>( DEFAULT_LINKING_FEATURE_PENALTIES ) );
		return settings;
	}

	/*
	 * PRIVATE METHODS
	 */

	private void setEnabled( final Component[] components, final boolean enable )
	{
		for ( final Component component : components )
			component.setEnabled( enable );
	}
}
