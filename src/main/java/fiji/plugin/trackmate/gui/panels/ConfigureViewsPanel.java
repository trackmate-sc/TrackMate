package fiji.plugin.trackmate.gui.panels;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import fiji.plugin.trackmate.gui.FeatureDisplaySelector;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackDisplayMode;

/**
 * A configuration panel used to tune the aspect of spots and tracks in multiple
 * {@link fiji.plugin.trackmate.visualization.AbstractTrackMateModelView}. This
 * GUI takes the role of a controller.
 *
 * @author Jean-Yves Tinevez
 */
public class ConfigureViewsPanel extends ActionListenablePanel
{

	private static final long serialVersionUID = 1L;

	private static final Color BORDER_COLOR = new java.awt.Color( 192, 192, 192 );

	/*
	 * CONSTRUCTOR
	 */

	public ConfigureViewsPanel(
			final DisplaySettings ds,
			final FeatureDisplaySelector featureSelector,
			final String spaceUnits,
			final Action launchTrackSchemeAction,
			final Action showTrackTablesAction,
			final Action showSpotTableAction )
	{
		this.setPreferredSize( new Dimension( 300, 521 ) );
		this.setSize( 300, 500 );

		final GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[] { 1.0, 1.0 };
		layout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 };
		setLayout( layout );

		/*
		 * Title
		 */

		final JLabel lblDisplayOptions = new JLabel();
		lblDisplayOptions.setText( "Display options" );
		lblDisplayOptions.setFont( BIG_FONT );
		lblDisplayOptions.setHorizontalAlignment( SwingConstants.LEFT );
		final GridBagConstraints gbcLabelDisplayOptions = new GridBagConstraints();
		gbcLabelDisplayOptions.gridwidth = 2;
		gbcLabelDisplayOptions.fill = GridBagConstraints.BOTH;
		gbcLabelDisplayOptions.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelDisplayOptions.gridx = 0;
		gbcLabelDisplayOptions.gridy = 0;
		add( lblDisplayOptions, gbcLabelDisplayOptions );

		/*
		 * Display spot checkbox.
		 */

		final JCheckBox chkboxDisplaySpots = new JCheckBox();
		chkboxDisplaySpots.setText( "Display spots" );
		chkboxDisplaySpots.setFont( FONT );
		final GridBagConstraints gbcCheckBoxDisplaySpots = new GridBagConstraints();
		gbcCheckBoxDisplaySpots.anchor = GridBagConstraints.NORTH;
		gbcCheckBoxDisplaySpots.fill = GridBagConstraints.HORIZONTAL;
		gbcCheckBoxDisplaySpots.insets = new Insets( 0, 5, 0, 5 );
		gbcCheckBoxDisplaySpots.gridx = 0;
		gbcCheckBoxDisplaySpots.gridy = 1;
		add( chkboxDisplaySpots, gbcCheckBoxDisplaySpots );

		final JCheckBox chkboxDisplaySpotsAsRois = new JCheckBox();
		chkboxDisplaySpotsAsRois.setText( "as ROIs" );
		final GridBagConstraints gbc_chkboxDisplaySpotsAsRois = new GridBagConstraints();
		gbc_chkboxDisplaySpotsAsRois.insets = new Insets( 0, 0, 0, 5 );
		gbc_chkboxDisplaySpotsAsRois.anchor = GridBagConstraints.EAST;
		gbc_chkboxDisplaySpotsAsRois.gridx = 1;
		gbc_chkboxDisplaySpotsAsRois.gridy = 1;
		add( chkboxDisplaySpotsAsRois, gbc_chkboxDisplaySpotsAsRois );
		chkboxDisplaySpotsAsRois.setFont( FONT );
		chkboxDisplaySpotsAsRois.addActionListener( e -> ds.setSpotDisplayedAsRoi( chkboxDisplaySpotsAsRois.isSelected() ) );
		chkboxDisplaySpotsAsRois.setSelected( ds.isSpotDisplayedAsRoi() );

		/*
		 * Spot options panel.
		 */

		final JPanel panelSpotOptions = new JPanel();
		panelSpotOptions.setBorder( new LineBorder( BORDER_COLOR, 1, true ) );
		final GridBagConstraints gbcPanelSpotOptions = new GridBagConstraints();
		gbcPanelSpotOptions.gridwidth = 2;
		gbcPanelSpotOptions.insets = new Insets( 0, 5, 5, 5 );
		gbcPanelSpotOptions.fill = GridBagConstraints.BOTH;
		gbcPanelSpotOptions.gridx = 0;
		gbcPanelSpotOptions.gridy = 2;
		add( panelSpotOptions, gbcPanelSpotOptions );
		final GridBagLayout gblPanelSpotOptions = new GridBagLayout();
		gblPanelSpotOptions.columnWeights = new double[] { 0.0, 1.0 };
		gblPanelSpotOptions.rowWeights = new double[] { 0.0, 0.0 };
		panelSpotOptions.setLayout( gblPanelSpotOptions );

		final JLabel lblSpotRadius = new JLabel( "Spot display radius ratio:" );
		lblSpotRadius.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSpotRadius = new GridBagConstraints();
		gbcLblSpotRadius.anchor = GridBagConstraints.EAST;
		gbcLblSpotRadius.insets = new Insets( 5, 5, 0, 5 );
		gbcLblSpotRadius.gridx = 0;
		gbcLblSpotRadius.gridy = 0;
		panelSpotOptions.add( lblSpotRadius, gbcLblSpotRadius );

		final JFormattedTextField ftfSpotRadius = new JFormattedTextField();
		GuiUtils.selectAllOnFocus( ftfSpotRadius );
		ftfSpotRadius.setHorizontalAlignment( SwingConstants.CENTER );
		ftfSpotRadius.setFont( SMALL_FONT );
		ftfSpotRadius.setMinimumSize( new Dimension( 80, 20 ) );
		ftfSpotRadius.setColumns( 5 );
		final GridBagConstraints gbcFtfSpotRadius = new GridBagConstraints();
		gbcFtfSpotRadius.insets = new Insets( 5, 0, 0, 0 );
		gbcFtfSpotRadius.anchor = GridBagConstraints.WEST;
		gbcFtfSpotRadius.gridx = 1;
		gbcFtfSpotRadius.gridy = 0;
		panelSpotOptions.add( ftfSpotRadius, gbcFtfSpotRadius );

		final JLabel lblSpotName = new JLabel( "Display spot names:" );
		lblSpotName.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSpotName = new GridBagConstraints();
		gbcLblSpotName.anchor = GridBagConstraints.EAST;
		gbcLblSpotName.insets = new Insets( 0, 0, 0, 5 );
		gbcLblSpotName.gridx = 0;
		gbcLblSpotName.gridy = 1;
		panelSpotOptions.add( lblSpotName, gbcLblSpotName );

		final JCheckBox chkboxSpotNames = new JCheckBox();
		final GridBagConstraints gbcChkboxSpotNames = new GridBagConstraints();
		gbcChkboxSpotNames.anchor = GridBagConstraints.WEST;
		gbcChkboxSpotNames.gridx = 1;
		gbcChkboxSpotNames.gridy = 1;
		panelSpotOptions.add( chkboxSpotNames, gbcChkboxSpotNames );

		final JPanel selectorForSpots = featureSelector.createSelectorForSpots();
		final GridBagConstraints gbcCmbboxSpotColor = new GridBagConstraints();
		gbcCmbboxSpotColor.insets = new Insets( 0, 5, 5, 5 );
		gbcCmbboxSpotColor.fill = GridBagConstraints.BOTH;
		gbcCmbboxSpotColor.gridwidth = 2;
		gbcCmbboxSpotColor.gridx = 0;
		gbcCmbboxSpotColor.gridy = 3;
		panelSpotOptions.add( selectorForSpots, gbcCmbboxSpotColor );

		/*
		 * Tracks.
		 */

		final JCheckBox chkboxDisplayTracks = new JCheckBox();
		chkboxDisplayTracks.setText( "Display tracks" );
		chkboxDisplayTracks.setFont( FONT );
		final GridBagConstraints gbcCheckBoxDisplayTracks = new GridBagConstraints();
		gbcCheckBoxDisplayTracks.anchor = GridBagConstraints.NORTH;
		gbcCheckBoxDisplayTracks.fill = GridBagConstraints.HORIZONTAL;
		gbcCheckBoxDisplayTracks.insets = new Insets( 5, 5, 0, 5 );
		gbcCheckBoxDisplayTracks.gridx = 0;
		gbcCheckBoxDisplayTracks.gridy = 3;
		add( chkboxDisplayTracks, gbcCheckBoxDisplayTracks );

		final JComboBox< TrackDisplayMode > cmbboxTrackDisplayMode = new JComboBox<>( TrackDisplayMode.values() );
		final GridBagConstraints gbc_cmbboxTrackDisplayMode = new GridBagConstraints();
		gbc_cmbboxTrackDisplayMode.fill = GridBagConstraints.HORIZONTAL;
		gbc_cmbboxTrackDisplayMode.insets = new Insets( 5, 0, 0, 5 );
		gbc_cmbboxTrackDisplayMode.gridx = 1;
		gbc_cmbboxTrackDisplayMode.gridy = 3;
		add( cmbboxTrackDisplayMode, gbc_cmbboxTrackDisplayMode );
		cmbboxTrackDisplayMode.setFont( SMALL_FONT );
		cmbboxTrackDisplayMode.addActionListener( e -> ds.setTrackDisplayMode( ( TrackDisplayMode ) cmbboxTrackDisplayMode.getSelectedItem() ) );
		cmbboxTrackDisplayMode.setSelectedItem( ds.getTrackDisplayMode() );

		/*
		 * Tracks display options.
		 */

		final JPanel panelTrackOptions = new JPanel();
		panelTrackOptions.setBorder( new LineBorder( BORDER_COLOR, 1, true ) );
		final GridBagConstraints gbcPanelTrackOptions = new GridBagConstraints();
		gbcPanelTrackOptions.gridwidth = 2;
		gbcPanelTrackOptions.insets = new Insets( 0, 5, 5, 5 );
		gbcPanelTrackOptions.fill = GridBagConstraints.BOTH;
		gbcPanelTrackOptions.gridx = 0;
		gbcPanelTrackOptions.gridy = 4;
		add( panelTrackOptions, gbcPanelTrackOptions );
		final GridBagLayout gblPanelTrackOptions = new GridBagLayout();
		gblPanelTrackOptions.columnWeights = new double[] { 0.0, 0.0, 1.0 };
		gblPanelTrackOptions.rowWeights = new double[] { 0.0, 0.0 };
		panelTrackOptions.setLayout( gblPanelTrackOptions );

		final JLabel lblFadeTracks = new JLabel( "Fade tracks in time:" );
		lblFadeTracks.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblFadeTracks = new GridBagConstraints();
		gbcLblFadeTracks.anchor = GridBagConstraints.EAST;
		gbcLblFadeTracks.insets = new Insets( 0, 5, 0, 5 );
		gbcLblFadeTracks.gridx = 0;
		gbcLblFadeTracks.gridy = 0;
		panelTrackOptions.add( lblFadeTracks, gbcLblFadeTracks );

		final JCheckBox chkboxFadeTracks = new JCheckBox();
		final GridBagConstraints gbcChckbxFadeTracks = new GridBagConstraints();
		gbcChckbxFadeTracks.insets = new Insets( 0, 0, 0, 5 );
		gbcChckbxFadeTracks.anchor = GridBagConstraints.WEST;
		gbcChckbxFadeTracks.gridx = 1;
		gbcChckbxFadeTracks.gridy = 0;
		panelTrackOptions.add( chkboxFadeTracks, gbcChckbxFadeTracks );

		final JLabel lblFadeRange = new JLabel( "Fade range:" );
		lblFadeRange.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblFadeRange = new GridBagConstraints();
		gbcLblFadeRange.anchor = GridBagConstraints.EAST;
		gbcLblFadeRange.insets = new Insets( 0, 0, 0, 5 );
		gbcLblFadeRange.gridx = 0;
		gbcLblFadeRange.gridy = 1;
		panelTrackOptions.add( lblFadeRange, gbcLblFadeRange );

		final SpinnerNumberModel fadeTrackDepthModel = new SpinnerNumberModel( ds.getFadeTrackRange(), 1, 1000, 1 );
		final JSpinner spinnerFadeRange = new JSpinner( fadeTrackDepthModel );
		spinnerFadeRange.setFont( SMALL_FONT );
		final GridBagConstraints gbcSpinnerFadeRange = new GridBagConstraints();
		gbcSpinnerFadeRange.insets = new Insets( 0, 0, 0, 5 );
		gbcSpinnerFadeRange.anchor = GridBagConstraints.WEST;
		gbcSpinnerFadeRange.gridx = 1;
		gbcSpinnerFadeRange.gridy = 1;
		panelTrackOptions.add( spinnerFadeRange, gbcSpinnerFadeRange );

		final JLabel lblFadeRangeUnits = new JLabel( "time-points" );
		lblFadeRangeUnits.setFont( SMALL_FONT );
		final GridBagConstraints gbcFadeRangeUnit = new GridBagConstraints();
		gbcFadeRangeUnit.anchor = GridBagConstraints.WEST;
		gbcFadeRangeUnit.gridx = 2;
		gbcFadeRangeUnit.gridy = 1;
		panelTrackOptions.add( lblFadeRangeUnits, gbcFadeRangeUnit );

		final JPanel selectorForTracks = featureSelector.createSelectorForTracks();
		final GridBagConstraints gbcCmbboxTrackColor = new GridBagConstraints();
		gbcCmbboxTrackColor.insets = new Insets( 5, 5, 5, 5 );
		gbcCmbboxTrackColor.fill = GridBagConstraints.BOTH;
		gbcCmbboxTrackColor.gridwidth = 3;
		gbcCmbboxTrackColor.gridx = 0;
		gbcCmbboxTrackColor.gridy = 3;
		panelTrackOptions.add( selectorForTracks, gbcCmbboxTrackColor );

		/*
		 * Draw Z Depth
		 */

		final JPanel panelDrawingZDepth = new JPanel();
		panelDrawingZDepth.setBorder( new LineBorder( BORDER_COLOR, 1, true ) );
		final FlowLayout flowLayout = (FlowLayout) panelDrawingZDepth.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		final GridBagConstraints gbc_panelDrawingZDepth = new GridBagConstraints();
		gbc_panelDrawingZDepth.gridwidth = 2;
		gbc_panelDrawingZDepth.insets = new Insets( 0, 5, 5, 5 );
		gbc_panelDrawingZDepth.fill = GridBagConstraints.BOTH;
		gbc_panelDrawingZDepth.gridx = 0;
		gbc_panelDrawingZDepth.gridy = 5;
		add(panelDrawingZDepth, gbc_panelDrawingZDepth);

		final JCheckBox chckbxLimitZDepth = new JCheckBox( "Limit drawing Z depth" );
		chckbxLimitZDepth.setFont( SMALL_FONT );
		panelDrawingZDepth.add( chckbxLimitZDepth );

		final SpinnerNumberModel numberModelDrawingZDepth = new SpinnerNumberModel( ds.getZDrawingDepth(), 0.5, 5000., 0.5 );
		final JSpinner spinnerDrawingZDepth = new JSpinner( numberModelDrawingZDepth );
		spinnerDrawingZDepth.setFont( SMALL_FONT );
		panelDrawingZDepth.add( spinnerDrawingZDepth );

		final JLabel lblDrawingZDepthUnits = new JLabel( spaceUnits );
		lblDrawingZDepthUnits.setFont( SMALL_FONT );
		panelDrawingZDepth.add(lblDrawingZDepthUnits);

		/*
		 * Panel for view buttons.
		 */

		final JPanel panelButtons = new JPanel();

		// TrackScheme button.
		final JButton btnShowTrackScheme = new JButton( launchTrackSchemeAction );
		panelButtons.add( btnShowTrackScheme );
		btnShowTrackScheme.setFont( FONT );

		// Do analysis button.
		final JButton btnShowTrackTables = new JButton( showTrackTablesAction );
		panelButtons.add( btnShowTrackTables );
		btnShowTrackTables.setFont( FONT );

		final JButton btnShowSpotTable = new JButton( showSpotTableAction );
		panelButtons.add( btnShowSpotTable );
		btnShowSpotTable.setFont( FONT );

		final GridBagConstraints gbcPanelButtons = new GridBagConstraints();
		gbcPanelButtons.gridwidth = 2;
		gbcPanelButtons.anchor = GridBagConstraints.SOUTH;
		gbcPanelButtons.fill = GridBagConstraints.HORIZONTAL;
		gbcPanelButtons.gridx = 0;
		gbcPanelButtons.gridy = 7;
		add( panelButtons, gbcPanelButtons );


		/*
		 * Listeners & co.
		 */

		chkboxDisplaySpots.addActionListener( e -> {
			setEnabled( panelSpotOptions, chkboxDisplaySpots.isSelected() );
			chkboxDisplaySpotsAsRois.setEnabled( chkboxDisplaySpots.isSelected() );
		} );
		chkboxDisplayTracks.addActionListener( e -> { 
			setEnabled( panelTrackOptions, chkboxDisplayTracks.isSelected() );
			cmbboxTrackDisplayMode.setEnabled( chkboxDisplayTracks.isSelected() );
			} );
		
		final ActionListener fadeTrackBtnEnable = e -> {
			final boolean shouldBeEnabled = chkboxDisplayTracks.isSelected()
					&& cmbboxTrackDisplayMode.getSelectedItem() != TrackDisplayMode.FULL;
			chkboxFadeTracks.setEnabled( shouldBeEnabled );
		};
		chkboxDisplayTracks.addActionListener( fadeTrackBtnEnable );
		cmbboxTrackDisplayMode.addActionListener( fadeTrackBtnEnable );
		
		final ActionListener fadeTrackRangeEnable = e -> {
			final boolean shouldBeEnabled = chkboxDisplayTracks.isSelected() 
					&& cmbboxTrackDisplayMode.getSelectedItem() != TrackDisplayMode.FULL
					&& chkboxFadeTracks.isSelected();
			spinnerFadeRange.setEnabled( shouldBeEnabled );
		};
		cmbboxTrackDisplayMode.addActionListener( fadeTrackRangeEnable );
		chkboxDisplayTracks.addActionListener( fadeTrackRangeEnable );
		chkboxFadeTracks.addActionListener( fadeTrackRangeEnable );
		
		chckbxLimitZDepth.addActionListener( e -> spinnerDrawingZDepth.setEnabled( chckbxLimitZDepth.isSelected() ) );

		chkboxDisplaySpots.addActionListener( e -> ds.setSpotVisible( chkboxDisplaySpots.isSelected() ) );
		ftfSpotRadius.addPropertyChangeListener( "value", e -> ds.setSpotDisplayRadius( ( ( Number ) ftfSpotRadius.getValue() ).doubleValue() ) );
		chkboxSpotNames.addActionListener( e -> ds.setSpotShowName( chkboxSpotNames.isSelected() ) );
		chkboxDisplayTracks.addActionListener( e -> ds.setTrackVisible( chkboxDisplayTracks.isSelected() ) );
		chkboxFadeTracks.addActionListener( e -> ds.setFadeTracks( chkboxFadeTracks.isSelected() ) );
		fadeTrackDepthModel.addChangeListener( e -> ds.setFadeTrackRange( fadeTrackDepthModel.getNumber().intValue() ) );

		/*
		 * Set current values.
		 */

		chkboxDisplaySpots.setSelected( ds.isSpotVisible() );
		chkboxSpotNames.setSelected( ds.isSpotShowName() );
		chkboxDisplayTracks.setSelected( ds.isTrackVisible() );
		chkboxFadeTracks.setSelected( ds.isFadeTracks() );
		chckbxLimitZDepth.setSelected( ds.isZDrawingDepthLimited() );
		ftfSpotRadius.setValue( Double.valueOf( ds.getSpotDisplayRadius() ) );
		fadeTrackDepthModel.setValue( Integer.valueOf( ds.getFadeTrackRange() ) );
		numberModelDrawingZDepth.setValue( Double.valueOf( ds.getZDrawingDepth() ) );
		cmbboxTrackDisplayMode.setSelectedItem( ds.getTrackDisplayMode() );

		spinnerDrawingZDepth.setEnabled( chckbxLimitZDepth.isSelected() );
		fadeTrackBtnEnable.actionPerformed( null );
	}

	private static final void setEnabled( final Container container, final boolean enabled )
	{
		for ( final Component component : container.getComponents() )
		{
			if ( component instanceof JSpinner || component instanceof JCheckBox )
				continue; // Treat them elsewhere.
			component.setEnabled( enabled );
			if (component instanceof Container)
				setEnabled( ( Container ) component, enabled );
		}
	}
}
