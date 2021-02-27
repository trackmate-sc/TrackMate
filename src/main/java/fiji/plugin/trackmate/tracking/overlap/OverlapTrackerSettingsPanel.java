package fiji.plugin.trackmate.tracking.overlap;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_IOU_CALCULATION;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_MIN_IOU;
import static fiji.plugin.trackmate.tracking.overlap.OverlapTrackerFactory.KEY_SCALE_FACTOR;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.tracking.overlap.OverlapTracker.IoUCalculation;

public class OverlapTrackerSettingsPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private final JFormattedTextField ftfScaleFactor;

	private final JFormattedTextField ftfMinIoU;

	private final JRadioButton rdbtnFast;

	private final JRadioButton rdbtnPrecise;

	public OverlapTrackerSettingsPanel()
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 164, 59, 0 };
		gridBagLayout.rowHeights = new int[] { 20, 20, 225, 0, 0, 20, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblSettingsForTracker = new JLabel( "Settings for tracker:" );
		lblSettingsForTracker.setFont( FONT );
		final GridBagConstraints gbc_lblSettingsForTracker = new GridBagConstraints();
		gbc_lblSettingsForTracker.fill = GridBagConstraints.BOTH;
		gbc_lblSettingsForTracker.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblSettingsForTracker.gridwidth = 2;
		gbc_lblSettingsForTracker.gridx = 0;
		gbc_lblSettingsForTracker.gridy = 0;
		add( lblSettingsForTracker, gbc_lblSettingsForTracker );

		final JLabel labelTracker = new JLabel( OverlapTrackerFactory.TRACKER_NAME );
		labelTracker.setFont( BIG_FONT );
		labelTracker.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbc_labelTracker = new GridBagConstraints();
		gbc_labelTracker.fill = GridBagConstraints.BOTH;
		gbc_labelTracker.insets = new Insets( 5, 5, 5, 5 );
		gbc_labelTracker.gridwidth = 2;
		gbc_labelTracker.gridx = 0;
		gbc_labelTracker.gridy = 1;
		add( labelTracker, gbc_labelTracker );

		final JLabel labelTrackerDescription = new JLabel( "<tracker description>" );
		labelTrackerDescription.setFont( FONT.deriveFont( Font.ITALIC ) );
		labelTrackerDescription.setText( OverlapTrackerFactory.TRACKER_INFO_TEXT
				.replace( "<br>", "" )
				.replace( "<p>", "<p align=\"justify\">" )
				.replace( "<html>", "<html><p align=\"justify\">" ) );
		final GridBagConstraints gbc_labelTrackerDescription = new GridBagConstraints();
		gbc_labelTrackerDescription.fill = GridBagConstraints.BOTH;
		gbc_labelTrackerDescription.insets = new Insets( 5, 5, 5, 5 );
		gbc_labelTrackerDescription.gridwidth = 2;
		gbc_labelTrackerDescription.gridx = 0;
		gbc_labelTrackerDescription.gridy = 2;
		add( labelTrackerDescription, gbc_labelTrackerDescription );

		final JPanel panelMethod = new JPanel();
		final FlowLayout flowLayout = ( FlowLayout ) panelMethod.getLayout();
		flowLayout.setHgap( 10 );
		flowLayout.setAlignment( FlowLayout.TRAILING );
		final GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.gridwidth = 2;
		gbc_panel.insets = new Insets( 5, 5, 5, 5 );
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 3;
		add( panelMethod, gbc_panel );

		final JLabel lblIouCalculation = new JLabel( "IoU calculation:" );
		lblIouCalculation.setFont( FONT );
		panelMethod.add( lblIouCalculation );

		rdbtnFast = new JRadioButton( IoUCalculation.FAST.toString() );
		rdbtnFast.setFont( FONT );
		rdbtnFast.setToolTipText( IoUCalculation.FAST.getInfoText() );
		panelMethod.add( rdbtnFast );

		rdbtnPrecise = new JRadioButton( IoUCalculation.PRECISE.toString() );
		rdbtnPrecise.setFont( FONT );
		rdbtnPrecise.setToolTipText( IoUCalculation.PRECISE.getInfoText() );
		panelMethod.add( rdbtnPrecise );

		final ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add( rdbtnPrecise );
		buttonGroup.add( rdbtnFast );

		final JLabel lblMinIoU = new JLabel( "Min IoU:" );
		lblMinIoU.setFont( FONT );
		final GridBagConstraints gbc_lblMinIoU = new GridBagConstraints();
		gbc_lblMinIoU.fill = GridBagConstraints.VERTICAL;
		gbc_lblMinIoU.anchor = GridBagConstraints.EAST;
		gbc_lblMinIoU.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblMinIoU.gridx = 0;
		gbc_lblMinIoU.gridy = 4;
		add( lblMinIoU, gbc_lblMinIoU );

		ftfMinIoU = new JFormattedTextField();
		ftfMinIoU.setText( "0" );
		ftfMinIoU.setFont( FONT );
		ftfMinIoU.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbc_ftfMinIoU = new GridBagConstraints();
		gbc_ftfMinIoU.insets = new Insets( 5, 5, 5, 5 );
		gbc_ftfMinIoU.fill = GridBagConstraints.BOTH;
		gbc_ftfMinIoU.gridx = 1;
		gbc_ftfMinIoU.gridy = 4;
		add( ftfMinIoU, gbc_ftfMinIoU );

		final JLabel lblScaleFactor = new JLabel( "Scale factor:" );
		lblScaleFactor.setFont( FONT );
		final GridBagConstraints gbc_lblScaleFactor = new GridBagConstraints();
		gbc_lblScaleFactor.anchor = GridBagConstraints.EAST;
		gbc_lblScaleFactor.fill = GridBagConstraints.VERTICAL;
		gbc_lblScaleFactor.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblScaleFactor.gridx = 0;
		gbc_lblScaleFactor.gridy = 5;
		add( lblScaleFactor, gbc_lblScaleFactor );

		ftfScaleFactor = new JFormattedTextField( new DecimalFormat( "#.##" ) );
		ftfScaleFactor.setText( "1" );
		ftfScaleFactor.setHorizontalAlignment( SwingConstants.CENTER );
		ftfScaleFactor.setValue( Double.valueOf( 1. ) );
		ftfScaleFactor.setFont( FONT );
		final GridBagConstraints gbc_ftfScaleFactor = new GridBagConstraints();
		gbc_ftfScaleFactor.fill = GridBagConstraints.BOTH;
		gbc_ftfScaleFactor.insets = new Insets( 5, 5, 5, 5 );
		gbc_ftfScaleFactor.gridx = 1;
		gbc_ftfScaleFactor.gridy = 5;
		add( ftfScaleFactor, gbc_ftfScaleFactor );

		// Select text-fields content on focus.
		GuiUtils.selectAllOnFocus( ftfMinIoU );
		GuiUtils.selectAllOnFocus( ftfScaleFactor );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > settings = new HashMap<>();
		settings.put( KEY_SCALE_FACTOR, ( ( Number ) ftfScaleFactor.getValue() ).doubleValue() );
		settings.put( KEY_MIN_IOU, ( ( Number ) ftfMinIoU.getValue() ).doubleValue() );
		final IoUCalculation method = ( rdbtnFast.isSelected() )
				? IoUCalculation.FAST
				: IoUCalculation.PRECISE;
		settings.put( KEY_IOU_CALCULATION, method.name() );
		return settings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		ftfScaleFactor.setValue( settings.get( KEY_SCALE_FACTOR ) );
		ftfMinIoU.setValue( settings.get( KEY_MIN_IOU ) );
		final boolean isFast = ( ( String ) settings.get( KEY_IOU_CALCULATION ) ).equalsIgnoreCase( IoUCalculation.FAST.name() );
		rdbtnFast.setSelected( isFast );
		rdbtnPrecise.setSelected( !isFast );
	}

	@Override
	public void clean()
	{}
}
