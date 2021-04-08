package fiji.plugin.trackmate.gui.components.tracker;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Fonts.TEXTFIELD_DIMENSION;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
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
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.tracking.LAPUtils;

public class JPanelTrackerSettingsMain extends javax.swing.JPanel
{

	private static final long serialVersionUID = -1L;

	private final JLabel lblSplittingMaxDistanceUnit;

	private final JFormattedTextField txtfldSplittingMaxDistance;

	private final JCheckBox chkboxAllowSplitting;

	private final JPanelFeatureSelectionGui panelGapClosing;

	private final JPanelFeatureSelectionGui panelMergingFeatures;

	private final JPanelFeatureSelectionGui panelLinkingFeatures;

	private final JPanelFeatureSelectionGui panelSplittingFeatures;

	private final JScrollPane scrpneMergingFeatures;

	private final JLabel lblMergingMaxDistanceUnit;

	private final JFormattedTextField txtfldMergingMaxDistance;

	private final JCheckBox chkboxAllowMerging;

	private final JScrollPane scrpneSplittingFeatures;

	private final JScrollPane scrpneGapClosingFeatures;

	private final JFormattedTextField txtfldGapClosingMaxFrameInterval;

	private final JLabel lblGapClosingMaxDistanceUnit;

	private final JFormattedTextField txtfldGapClosingMaxDistance;

	private final JCheckBox chkboxAllowGapClosing;

	private final JLabel lblLinkingMaxDistanceUnits;

	private final JFormattedTextField txtfldLinkingMaxDistance;

	private final JLabel lbl6;

	private final JLabel lbl7;

	private final JLabel lbl8;

	private final JLabel lbl10;

	private final JLabel lbl15;

	private final JLabel lbl13;

	private final JLabel lbl16;

	public JPanelTrackerSettingsMain( final String trackerName, final String spaceUnits, final Collection< String > features, final Map< String, String > featureNames )
	{
		final DecimalFormat decimalFormat = new DecimalFormat( "0.0" );

		this.setPreferredSize( new Dimension( 280, 1000 ) );
		final GridBagLayout thisLayout = new GridBagLayout();
		thisLayout.columnWidths = new int[] { 180, 50, 50 };
		thisLayout.columnWeights = new double[] { 0.1, 0.8, 0.1 };
		thisLayout.rowHeights = new int[] { 15, 20, 0, 15, 10, 15, 95, 15, 15, 15, 15, 15, 95, 15, 15, 15, 15, 15, 95, 15, 15, 15, 15, 15, 95 };
		thisLayout.rowWeights = new double[] { 0.0, 0.1, 0.25, 0.1, 0.0, 0.0, 0.25, 0.1, 0.0, 0.0, 0.0, 0.0, 0.25, 0.1, 0.0, 0.0, 0.0, 0.0, 0.25, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0 };
		this.setLayout( thisLayout );

		final JLabel jLabel1 = new JLabel();
		this.add( jLabel1, new GridBagConstraints( 0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 10, 10, 0, 10 ), 0, 0 ) );
		jLabel1.setText( "Settings for tracker:" );
		jLabel1.setFont( FONT );

		final JLabel lblTrackerName = new JLabel();
		this.add( lblTrackerName, new GridBagConstraints( 0, 1, 3, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets( 10, 20, 0, 0 ), 0, 0 ) );
		lblTrackerName.setHorizontalTextPosition( SwingConstants.CENTER );
		lblTrackerName.setHorizontalAlignment( SwingConstants.CENTER );
		lblTrackerName.setFont( BIG_FONT );
		lblTrackerName.setText( trackerName );

		final JLabel lbl2 = new JLabel();
		this.add( lbl2, new GridBagConstraints( 0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl2.setText( "Frame to frame linking:" );
		lbl2.setFont( BIG_FONT.deriveFont( Font.BOLD ) );

		final JLabel lbl3 = new JLabel();
		this.add( lbl3, new GridBagConstraints( 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl3.setText( "Max distance:" );
		lbl3.setFont( SMALL_FONT );

		txtfldLinkingMaxDistance = new JFormattedTextField( decimalFormat );
		this.add( txtfldLinkingMaxDistance, new GridBagConstraints( 1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldLinkingMaxDistance.setFont( SMALL_FONT );
		txtfldLinkingMaxDistance.setSize( TEXTFIELD_DIMENSION );
		txtfldLinkingMaxDistance.setHorizontalAlignment( JFormattedTextField.CENTER );

		lblLinkingMaxDistanceUnits = new JLabel();
		this.add( lblLinkingMaxDistanceUnits, new GridBagConstraints( 2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 5, 0, 0 ), 0, 0 ) );
		lblLinkingMaxDistanceUnits.setFont( SMALL_FONT );
		lblLinkingMaxDistanceUnits.setText( spaceUnits );

		final JLabel lbl4 = new JLabel();
		this.add( lbl4, new GridBagConstraints( 0, 5, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl4.setText( "Feature penalties" );
		lbl4.setFont( SMALL_FONT );

		final JScrollPane scrpneLinkingFeatures = new JScrollPane();
		final MouseWheelListener[] l = scrpneLinkingFeatures.getMouseWheelListeners();
		scrpneLinkingFeatures.removeMouseWheelListener( l[ 0 ] );
		this.add( scrpneLinkingFeatures, new GridBagConstraints( 0, 6, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		scrpneLinkingFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrpneLinkingFeatures.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		panelLinkingFeatures = new JPanelFeatureSelectionGui();
		panelLinkingFeatures.setDisplayFeatures( features, featureNames );
		scrpneLinkingFeatures.setViewportView( panelLinkingFeatures );

		// Gap closing

		final JLabel lbl5 = new JLabel();
		this.add( lbl5, new GridBagConstraints( 0, 7, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 20, 10, 0, 10 ), 0, 0 ) );
		lbl5.setText( "Track segment gap closing:" );
		lbl5.setFont( BIG_FONT.deriveFont( Font.BOLD ) );

		chkboxAllowGapClosing = new JCheckBox();
		this.add( chkboxAllowGapClosing, new GridBagConstraints( 0, 8, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		chkboxAllowGapClosing.setText( "Allow gap closing" );
		chkboxAllowGapClosing.setFont( SMALL_FONT );

		lbl6 = new JLabel();
		this.add( lbl6, new GridBagConstraints( 0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl6.setText( "Max distance:" );
		lbl6.setFont( SMALL_FONT );

		txtfldGapClosingMaxDistance = new JFormattedTextField( decimalFormat );
		this.add( txtfldGapClosingMaxDistance, new GridBagConstraints( 1, 9, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldGapClosingMaxDistance.setSize( TEXTFIELD_DIMENSION );
		txtfldGapClosingMaxDistance.setFont( SMALL_FONT );
		txtfldGapClosingMaxDistance.setHorizontalAlignment( JFormattedTextField.CENTER );

		lblGapClosingMaxDistanceUnit = new JLabel();
		this.add( lblGapClosingMaxDistanceUnit, new GridBagConstraints( 2, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 5, 0, 0 ), 0, 0 ) );
		lblGapClosingMaxDistanceUnit.setFont( SMALL_FONT );
		lblGapClosingMaxDistanceUnit.setText( spaceUnits );

		lbl7 = new JLabel();
		this.add( lbl7, new GridBagConstraints( 0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl7.setText( "Max frame gap:" );
		lbl7.setFont( SMALL_FONT );

		txtfldGapClosingMaxFrameInterval = new JFormattedTextField( Integer.valueOf( 2 ) );
		this.add( txtfldGapClosingMaxFrameInterval, new GridBagConstraints( 1, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldGapClosingMaxFrameInterval.setSize( TEXTFIELD_DIMENSION );
		txtfldGapClosingMaxFrameInterval.setFont( SMALL_FONT );
		txtfldGapClosingMaxFrameInterval.setHorizontalAlignment( JFormattedTextField.CENTER );

		lbl8 = new JLabel();
		this.add( lbl8, new GridBagConstraints( 0, 11, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl8.setText( "Feature penalties:" );
		lbl8.setFont( SMALL_FONT );

		scrpneGapClosingFeatures = new JScrollPane();
		final MouseWheelListener[] l1 = scrpneGapClosingFeatures.getMouseWheelListeners();
		scrpneGapClosingFeatures.removeMouseWheelListener( l1[ 0 ] );
		this.add( scrpneGapClosingFeatures, new GridBagConstraints( 0, 12, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		scrpneGapClosingFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrpneGapClosingFeatures.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		panelGapClosing = new JPanelFeatureSelectionGui();
		panelGapClosing.setDisplayFeatures( features, featureNames );
		scrpneGapClosingFeatures.setViewportView( panelGapClosing );

		// Splitting

		final JLabel lbl9 = new JLabel();
		this.add( lbl9, new GridBagConstraints( 0, 13, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 20, 10, 0, 10 ), 0, 0 ) );
		lbl9.setText( "Track segment splitting:" );
		lbl9.setFont( BIG_FONT.deriveFont( Font.BOLD ) );

		chkboxAllowSplitting = new JCheckBox();
		this.add( chkboxAllowSplitting, new GridBagConstraints( 0, 14, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		chkboxAllowSplitting.setText( "Allow track segment splitting" );
		chkboxAllowSplitting.setFont( SMALL_FONT );

		lbl10 = new JLabel();
		this.add( lbl10, new GridBagConstraints( 0, 15, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 0 ), 0, 0 ) );
		lbl10.setText( "Max distance:" );
		lbl10.setFont( SMALL_FONT );

		txtfldSplittingMaxDistance = new JFormattedTextField( decimalFormat );
		this.add( txtfldSplittingMaxDistance, new GridBagConstraints( 1, 15, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldSplittingMaxDistance.setSize( TEXTFIELD_DIMENSION );
		txtfldSplittingMaxDistance.setFont( SMALL_FONT );
		txtfldSplittingMaxDistance.setHorizontalAlignment( JFormattedTextField.CENTER );

		lblSplittingMaxDistanceUnit = new JLabel();
		this.add( lblSplittingMaxDistanceUnit, new GridBagConstraints( 2, 15, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 5, 0, 0 ), 0, 0 ) );
		lblSplittingMaxDistanceUnit.setFont( SMALL_FONT );
		lblSplittingMaxDistanceUnit.setText( spaceUnits );

		lbl15 = new JLabel();
		this.add( lbl15, new GridBagConstraints( 0, 17, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl15.setText( "Feature penalties:" );
		lbl15.setFont( SMALL_FONT );

		scrpneSplittingFeatures = new JScrollPane();
		final MouseWheelListener[] l2 = scrpneSplittingFeatures.getMouseWheelListeners();
		scrpneSplittingFeatures.removeMouseWheelListener( l2[ 0 ] );
		this.add( scrpneSplittingFeatures, new GridBagConstraints( 0, 18, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		scrpneSplittingFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrpneSplittingFeatures.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		panelSplittingFeatures = new JPanelFeatureSelectionGui();
		panelSplittingFeatures.setDisplayFeatures( features, featureNames );
		scrpneSplittingFeatures.setViewportView( panelSplittingFeatures );

		// Merging

		final JLabel lbl12 = new JLabel();
		this.add( lbl12, new GridBagConstraints( 0, 19, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 20, 10, 0, 10 ), 0, 0 ) );
		lbl12.setText( "Track segment merging:" );
		lbl12.setFont( BIG_FONT.deriveFont( Font.BOLD ) );

		chkboxAllowMerging = new JCheckBox();
		this.add( chkboxAllowMerging, new GridBagConstraints( 0, 20, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		chkboxAllowMerging.setText( "Allow track segment merging" );
		chkboxAllowMerging.setFont( SMALL_FONT );

		lbl13 = new JLabel();
		this.add( lbl13, new GridBagConstraints( 0, 21, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 0 ), 0, 0 ) );
		lbl13.setText( "Max distance:" );
		lbl13.setFont( SMALL_FONT );

		txtfldMergingMaxDistance = new JFormattedTextField( decimalFormat );
		this.add( txtfldMergingMaxDistance, new GridBagConstraints( 1, 21, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldMergingMaxDistance.setSize( TEXTFIELD_DIMENSION );
		txtfldMergingMaxDistance.setFont( SMALL_FONT );
		txtfldMergingMaxDistance.setHorizontalAlignment( JFormattedTextField.CENTER );

		lblMergingMaxDistanceUnit = new JLabel();
		this.add( lblMergingMaxDistanceUnit, new GridBagConstraints( 2, 21, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 5, 0, 0 ), 0, 0 ) );
		lblMergingMaxDistanceUnit.setFont( SMALL_FONT );
		lblMergingMaxDistanceUnit.setText( spaceUnits );

		lbl16 = new JLabel();
		this.add( lbl16, new GridBagConstraints( 0, 23, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl16.setText( "Feature penalties:" );
		lbl16.setFont( SMALL_FONT );

		scrpneMergingFeatures = new JScrollPane();
		final MouseWheelListener[] l3 = scrpneMergingFeatures.getMouseWheelListeners();
		scrpneMergingFeatures.removeMouseWheelListener( l3[ 0 ] );
		this.add( scrpneMergingFeatures, new GridBagConstraints( 0, 24, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		scrpneMergingFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrpneMergingFeatures.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		panelMergingFeatures = new JPanelFeatureSelectionGui();
		panelMergingFeatures.setDisplayFeatures( features, featureNames );
		scrpneMergingFeatures.setViewportView( panelMergingFeatures );

		// Select text-fields content on focus.
		GuiUtils.selectAllOnFocus( txtfldGapClosingMaxDistance );
		GuiUtils.selectAllOnFocus( txtfldGapClosingMaxFrameInterval );
		GuiUtils.selectAllOnFocus( txtfldLinkingMaxDistance );
		GuiUtils.selectAllOnFocus( txtfldMergingMaxDistance );
		GuiUtils.selectAllOnFocus( txtfldSplittingMaxDistance );

		// Listeners.
		chkboxAllowGapClosing.addActionListener( e -> setEnabled(
				new Component[] { lbl6, txtfldGapClosingMaxDistance, lblGapClosingMaxDistanceUnit,
						lbl7, txtfldGapClosingMaxFrameInterval, txtfldGapClosingMaxFrameInterval,
						lbl8, scrpneGapClosingFeatures, panelGapClosing },
				chkboxAllowGapClosing.isSelected() ) );

		chkboxAllowSplitting.addActionListener( e -> setEnabled(
				new Component[] { lbl10, txtfldSplittingMaxDistance, lblSplittingMaxDistanceUnit,
						lbl15, scrpneSplittingFeatures, panelSplittingFeatures },
				chkboxAllowSplitting.isSelected() ) );

		chkboxAllowMerging.addActionListener( e -> setEnabled(
				new Component[] { lbl13, txtfldMergingMaxDistance, lblMergingMaxDistanceUnit,
						lbl16, scrpneMergingFeatures, panelMergingFeatures },
				chkboxAllowMerging.isSelected() ) );
	}

	/*
	 * PUBLIC METHODS
	 */

	@SuppressWarnings( "unchecked" )
	void echoSettings( final Map< String, Object > settings )
	{

		txtfldLinkingMaxDistance.setValue( settings.get( KEY_LINKING_MAX_DISTANCE ) );
		panelLinkingFeatures.setSelectedFeaturePenalties( ( Map< String, Double > ) settings.get( KEY_LINKING_FEATURE_PENALTIES ) );

		chkboxAllowGapClosing.setSelected( ( Boolean ) settings.get( KEY_ALLOW_GAP_CLOSING ) );
		txtfldGapClosingMaxDistance.setValue( settings.get( KEY_GAP_CLOSING_MAX_DISTANCE ) );
		txtfldGapClosingMaxFrameInterval.setValue( settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
		panelGapClosing.setSelectedFeaturePenalties( ( Map< String, Double > ) settings.get( KEY_GAP_CLOSING_FEATURE_PENALTIES ) );

		chkboxAllowSplitting.setSelected( ( Boolean ) settings.get( KEY_ALLOW_TRACK_SPLITTING ) );
		txtfldSplittingMaxDistance.setValue( settings.get( KEY_SPLITTING_MAX_DISTANCE ) );
		panelSplittingFeatures.setSelectedFeaturePenalties( ( Map< String, Double > ) settings.get( KEY_SPLITTING_FEATURE_PENALTIES ) );

		chkboxAllowMerging.setSelected( ( Boolean ) settings.get( KEY_ALLOW_TRACK_MERGING ) );
		txtfldMergingMaxDistance.setValue( settings.get( KEY_SPLITTING_MAX_DISTANCE ) );
		panelMergingFeatures.setSelectedFeaturePenalties( ( Map< String, Double > ) settings.get( KEY_MERGING_FEATURE_PENALTIES ) );

		setEnabled(
				new Component[] { lbl6, txtfldGapClosingMaxDistance, lblGapClosingMaxDistanceUnit,
						lbl7, txtfldGapClosingMaxFrameInterval, txtfldGapClosingMaxFrameInterval,
						lbl8, scrpneGapClosingFeatures, panelGapClosing },
				chkboxAllowGapClosing.isSelected() );

		setEnabled(
				new Component[] { lbl10, txtfldSplittingMaxDistance, lblSplittingMaxDistanceUnit,
						lbl15, scrpneSplittingFeatures, panelSplittingFeatures },
				chkboxAllowSplitting.isSelected() );

		setEnabled(
				new Component[] { lbl13, txtfldMergingMaxDistance, lblMergingMaxDistanceUnit,
						lbl16, scrpneMergingFeatures, panelMergingFeatures },
				chkboxAllowMerging.isSelected() );
	}

	/**
	 * @return a new settings {@link Map} with values taken from this panel.
	 */
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > settings = LAPUtils.getDefaultLAPSettingsMap();

		settings.put( KEY_LINKING_MAX_DISTANCE, ( ( Number ) txtfldLinkingMaxDistance.getValue() ).doubleValue() );
		settings.put( KEY_LINKING_FEATURE_PENALTIES, panelLinkingFeatures.getFeaturePenalties() );

		settings.put( KEY_ALLOW_GAP_CLOSING, chkboxAllowGapClosing.isSelected() );
		settings.put( KEY_GAP_CLOSING_MAX_DISTANCE, ( ( Number ) txtfldGapClosingMaxDistance.getValue() ).doubleValue() );
		settings.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, ( ( Number ) txtfldGapClosingMaxFrameInterval.getValue() ).intValue() );
		settings.put( KEY_GAP_CLOSING_FEATURE_PENALTIES, panelGapClosing.getFeaturePenalties() );

		settings.put( KEY_ALLOW_TRACK_SPLITTING, chkboxAllowSplitting.isSelected() );
		settings.put( KEY_SPLITTING_MAX_DISTANCE, ( ( Number ) txtfldSplittingMaxDistance.getValue() ).doubleValue() );
		settings.put( KEY_SPLITTING_FEATURE_PENALTIES, panelSplittingFeatures.getFeaturePenalties() );

		settings.put( KEY_ALLOW_TRACK_MERGING, chkboxAllowMerging.isSelected() );
		settings.put( KEY_MERGING_MAX_DISTANCE, ( ( Number ) txtfldMergingMaxDistance.getValue() ).doubleValue() );
		settings.put( KEY_MERGING_FEATURE_PENALTIES, panelMergingFeatures.getFeaturePenalties() );

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
