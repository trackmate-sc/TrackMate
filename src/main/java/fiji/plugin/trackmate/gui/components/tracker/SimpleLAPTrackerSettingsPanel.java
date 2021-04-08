package fiji.plugin.trackmate.gui.components.tracker;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.TEXTFIELD_DIMENSION;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_SPLITTING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

/**
 * A simplified configuration panel for the
 * {@link fiji.plugin.trackmate.tracking.oldlap.LAPTracker}.
 * 
 * @author Jean-Yves Tinevez &lt;tinevez@pasteur.fr&gt; - 2010-2011
 */
public class SimpleLAPTrackerSettingsPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = -1L;

	private final JFormattedTextField txtfldGapClosingTimeCutoff;

	private final JFormattedTextField txtfldGapClosingDistanceCutoff;

	private final JFormattedTextField txtfldLinkingDistance;

	public SimpleLAPTrackerSettingsPanel( final String trackerName, final String infoText, final String spaceUnits )
	{
		final DecimalFormat decimalFormat = new DecimalFormat( "0.0" );

		this.setPreferredSize( new java.awt.Dimension( 300, 500 ) );
		final GridBagLayout thisLayout = new GridBagLayout();
		thisLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 };
		thisLayout.rowHeights = new int[] { 31, 50, 119, 7, 50, 50, 50, 50 };
		thisLayout.columnWeights = new double[] { 0.0, 0.0, 0.1 };
		thisLayout.columnWidths = new int[] { 203, 42, 7 };
		this.setLayout( thisLayout );

		final JLabel lbl1 = new JLabel();
		this.add( lbl1, new GridBagConstraints( 0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 10 ), 0, 0 ) );
		lbl1.setFont( FONT );
		lbl1.setText( "Settings for tracker:" );

		final JLabel lblTrackerName = new JLabel();
		this.add( lblTrackerName, new GridBagConstraints( 0, 1, 3, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets( 10, 20, 0, 0 ), 0, 0 ) );
		lblTrackerName.setHorizontalTextPosition( SwingConstants.CENTER );
		lblTrackerName.setHorizontalAlignment( SwingConstants.CENTER );
		lblTrackerName.setFont( BIG_FONT );
		lblTrackerName.setText( trackerName );

		final JLabel lblTrackerDescription = new JLabel();
		this.add( lblTrackerDescription, new GridBagConstraints( 0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets( 10, 10, 10, 10 ), 0, 0 ) );
		lblTrackerDescription.setFont( FONT.deriveFont( Font.ITALIC ) );
		lblTrackerDescription.setText( infoText
				.replace( "<br>", "" )
				.replace( "<p>", "<p align=\"justify\">" )
				.replace( "<html>", "<html><p align=\"justify\">" ) );

		final JLabel lbl2 = new JLabel();
		this.add( lbl2, new GridBagConstraints( 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 0 ), 0, 0 ) );
		lbl2.setFont( FONT );
		lbl2.setText( "Linking max distance:" );

		final JLabel lbl3 = new JLabel();
		this.add( lbl3, new GridBagConstraints( 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 0 ), 0, 0 ) );
		lbl3.setFont( FONT );
		lbl3.setText( "Gap-closing max distance:" );

		final JLabel lbl4 = new JLabel();
		this.add( lbl4, new GridBagConstraints( 0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 10, 0, 0 ), 0, 0 ) );
		lbl4.setFont( FONT );
		lbl4.setText( "Gap-closing max frame gap:" );

		txtfldLinkingDistance = new JFormattedTextField( decimalFormat );
		txtfldLinkingDistance.setMinimumSize( TEXTFIELD_DIMENSION );
		this.add( txtfldLinkingDistance, new GridBagConstraints( 1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldLinkingDistance.setFont( FONT );
		txtfldLinkingDistance.setHorizontalAlignment( JFormattedTextField.CENTER );

		txtfldGapClosingDistanceCutoff = new JFormattedTextField( decimalFormat );
		txtfldGapClosingDistanceCutoff.setMinimumSize( TEXTFIELD_DIMENSION );
		this.add( txtfldGapClosingDistanceCutoff, new GridBagConstraints( 1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldGapClosingDistanceCutoff.setFont( FONT );
		txtfldGapClosingDistanceCutoff.setHorizontalAlignment( JFormattedTextField.CENTER );

		txtfldGapClosingTimeCutoff = new JFormattedTextField( Integer.valueOf( 2 ) );
		txtfldGapClosingTimeCutoff.setMinimumSize( TEXTFIELD_DIMENSION );
		this.add( txtfldGapClosingTimeCutoff, new GridBagConstraints( 1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		txtfldGapClosingTimeCutoff.setFont( FONT );
		txtfldGapClosingTimeCutoff.setHorizontalAlignment( JFormattedTextField.CENTER );

		final JLabel lblMaxDistanceUnit = new JLabel();
		this.add( lblMaxDistanceUnit, new GridBagConstraints( 2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 10 ), 0, 0 ) );
		lblMaxDistanceUnit.setFont( FONT );
		lblMaxDistanceUnit.setText( " " + spaceUnits );

		final JLabel lblGapClosingMaxDistanceUnit = new JLabel();
		this.add( lblGapClosingMaxDistanceUnit, new GridBagConstraints( 2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 10 ), 0, 0 ) );
		lblGapClosingMaxDistanceUnit.setFont( FONT );
		lblGapClosingMaxDistanceUnit.setText( " " + spaceUnits );

		final JLabel lblGapClosingTimeCutoffUnit = new JLabel();
		this.add( lblGapClosingTimeCutoffUnit, new GridBagConstraints( 2, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 10 ), 0, 0 ) );
		lblGapClosingTimeCutoffUnit.setFont( FONT );

		// Select text-fields content on focus.
		GuiUtils.selectAllOnFocus( txtfldLinkingDistance );
		GuiUtils.selectAllOnFocus( txtfldGapClosingDistanceCutoff );
		GuiUtils.selectAllOnFocus( txtfldGapClosingTimeCutoff );
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		echoSettings( settings );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > settings = new HashMap<>();
		// Linking
		settings.put( KEY_LINKING_FEATURE_PENALTIES, DEFAULT_LINKING_FEATURE_PENALTIES );
		// Gap closing
		settings.put( KEY_ALLOW_GAP_CLOSING, true );
		settings.put( KEY_GAP_CLOSING_FEATURE_PENALTIES, DEFAULT_GAP_CLOSING_FEATURE_PENALTIES );
		// Track splitting
		settings.put( KEY_ALLOW_TRACK_SPLITTING, false );
		settings.put( KEY_SPLITTING_MAX_DISTANCE, DEFAULT_SPLITTING_MAX_DISTANCE );
		settings.put( KEY_SPLITTING_FEATURE_PENALTIES, DEFAULT_SPLITTING_FEATURE_PENALTIES );
		// Track merging
		settings.put( KEY_ALLOW_TRACK_MERGING, false );
		settings.put( KEY_MERGING_MAX_DISTANCE, DEFAULT_MERGING_MAX_DISTANCE );
		settings.put( KEY_MERGING_FEATURE_PENALTIES, DEFAULT_MERGING_FEATURE_PENALTIES );
		// Others
		settings.put( KEY_BLOCKING_VALUE, DEFAULT_BLOCKING_VALUE );
		settings.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR );
		settings.put( KEY_CUTOFF_PERCENTILE, DEFAULT_CUTOFF_PERCENTILE );
		// Panel ones
		settings.put( KEY_LINKING_MAX_DISTANCE, ( ( Number ) txtfldLinkingDistance.getValue() ).doubleValue() );
		settings.put( KEY_GAP_CLOSING_MAX_DISTANCE, ( ( Number ) txtfldGapClosingDistanceCutoff.getValue() ).doubleValue() );
		settings.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, ( ( Number ) txtfldGapClosingTimeCutoff.getValue() ).intValue() );
		// Hop!
		return settings;
	}

	/*
	 * PRIVATE METHODS
	 */

	private void echoSettings( final Map< String, Object > settings )
	{
		txtfldLinkingDistance.setValue( settings.get( KEY_LINKING_MAX_DISTANCE ) );
		txtfldGapClosingDistanceCutoff.setValue( settings.get( KEY_GAP_CLOSING_MAX_DISTANCE ) );
		txtfldGapClosingTimeCutoff.setValue( settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) );
	}

	@Override
	public void clean()
	{}
}
