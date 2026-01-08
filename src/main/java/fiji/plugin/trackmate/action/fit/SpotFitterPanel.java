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
package fiji.plugin.trackmate.action.fit;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.Fonts;

public class SpotFitterPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JComboBox< String > cmbboxModel;

	final JButton btnUndo;

	final JButton btnFit;

	final JRadioButton rdbtnAll;

	final JRadioButton rdbtnSelection;

	final JRadioButton rdbtnTracks;

	private final JSlider sliderChannel;

	public SpotFitterPanel( final List< String > fits, final List< String > docs, final int nChannels )
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblTitle = new JLabel( "Refine spot position by fitting" );
		lblTitle.setFont( Fonts.BIG_FONT );
		lblTitle.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcLblTitle = new GridBagConstraints();
		gbcLblTitle.gridwidth = 2;
		gbcLblTitle.fill = GridBagConstraints.BOTH;
		gbcLblTitle.insets = new Insets( 5, 5, 5, 5 );
		gbcLblTitle.gridx = 0;
		gbcLblTitle.gridy = 0;
		add( lblTitle, gbcLblTitle );

		final JLabel lblChannel = new JLabel( "Channel:" );
		sliderChannel = new JSlider( 1, nChannels, 1 );

		// Visible only if N channels > 1.
		if ( nChannels > 1 )
		{
			final GridBagConstraints gbcLblChannel = new GridBagConstraints();
			gbcLblChannel.anchor = GridBagConstraints.EAST;
			gbcLblChannel.insets = new Insets( 5, 5, 5, 5 );
			gbcLblChannel.gridx = 0;
			gbcLblChannel.gridy = 1;
			add( lblChannel, gbcLblChannel );

			final JPanel panelSlider = new JPanel();
			final GridBagConstraints gbc_panelSlider = new GridBagConstraints();
			gbc_panelSlider.fill = GridBagConstraints.BOTH;
			gbc_panelSlider.insets = new Insets( 5, 5, 5, 5 );
			gbc_panelSlider.gridx = 1;
			gbc_panelSlider.gridy = 1;
			add( panelSlider, gbc_panelSlider );
			panelSlider.setLayout( new BoxLayout( panelSlider, BoxLayout.X_AXIS ) );
			panelSlider.add( sliderChannel );
			sliderChannel.setPaintLabels( true );
			sliderChannel.setSnapToTicks( true );
			sliderChannel.setPaintTicks( true );
			sliderChannel.setFont( Fonts.SMALL_FONT );

			final JLabel lblChannelIndex = new JLabel( "1" );
			lblChannelIndex.setFont( Fonts.FONT.deriveFont( Font.BOLD ) );
			lblChannelIndex.setHorizontalAlignment( SwingConstants.CENTER );
			panelSlider.add( lblChannelIndex );
			sliderChannel.addChangeListener( e -> lblChannelIndex.setText( "" + sliderChannel.getValue() ) );
		}

		final JLabel lblFitType = new JLabel( "Fit type:" );
		lblFitType.setFont( Fonts.FONT );
		final GridBagConstraints gbcLblFitType = new GridBagConstraints();
		gbcLblFitType.anchor = GridBagConstraints.EAST;
		gbcLblFitType.insets = new Insets( 5, 5, 5, 5 );
		gbcLblFitType.gridx = 0;
		gbcLblFitType.gridy = 2;
		add( lblFitType, gbcLblFitType );

		final String[] arr = fits.toArray( new String[] {} );
		this.cmbboxModel = new JComboBox<>( arr );
		cmbboxModel.setFont( Fonts.FONT );
		final GridBagConstraints gbcComboBox = new GridBagConstraints();
		gbcComboBox.insets = new Insets( 5, 5, 5, 5 );
		gbcComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbcComboBox.gridx = 1;
		gbcComboBox.gridy = 2;
		add( cmbboxModel, gbcComboBox );

		final JLabel lblDoc = new JLabel( " " );
		lblDoc.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcLblDoc = new GridBagConstraints();
		gbcLblDoc.fill = GridBagConstraints.HORIZONTAL;
		gbcLblDoc.gridwidth = 2;
		gbcLblDoc.insets = new Insets( 5, 5, 5, 5 );
		gbcLblDoc.gridx = 0;
		gbcLblDoc.gridy = 3;
		add( lblDoc, gbcLblDoc );

		final JPanel panelSelection = new JPanel();
		final GridBagConstraints gbcPanelSelection = new GridBagConstraints();
		gbcPanelSelection.gridwidth = 2;
		gbcPanelSelection.insets = new Insets( 5, 5, 5, 5 );
		gbcPanelSelection.fill = GridBagConstraints.HORIZONTAL;
		gbcPanelSelection.gridx = 0;
		gbcPanelSelection.gridy = 4;
		add( panelSelection, gbcPanelSelection );
		panelSelection.setLayout( new BoxLayout( panelSelection, BoxLayout.X_AXIS ) );

		final JLabel lblPerformOn = new JLabel( "Fit:" );
		lblPerformOn.setFont( Fonts.FONT );
		panelSelection.add( lblPerformOn );

		rdbtnAll = new JRadioButton( "All spots" );
		rdbtnSelection = new JRadioButton( "Selection" );
		rdbtnTracks = new JRadioButton( "Tracks of selection" );
		rdbtnAll.setFont( Fonts.FONT );
		rdbtnSelection.setFont( Fonts.FONT );
		rdbtnTracks.setFont( Fonts.FONT );

		panelSelection.add( Box.createHorizontalGlue() );
		panelSelection.add( rdbtnAll );
		panelSelection.add( rdbtnSelection );
		panelSelection.add( rdbtnTracks );

		final JPanel panelButtons = new JPanel();
		final GridBagConstraints gbcPanelButtons = new GridBagConstraints();
		gbcPanelButtons.gridwidth = 2;
		gbcPanelButtons.fill = GridBagConstraints.BOTH;
		gbcPanelButtons.gridx = 0;
		gbcPanelButtons.gridy = 5;
		add( panelButtons, gbcPanelButtons );
		panelButtons.setLayout( new BoxLayout( panelButtons, BoxLayout.X_AXIS ) );

		btnUndo = new JButton( "Undo last fit" );
		btnUndo.setFont( Fonts.FONT );
		panelButtons.add( btnUndo );

		final Component horizontalGlue = Box.createHorizontalGlue();
		panelButtons.add( horizontalGlue );

		btnFit = new JButton( "Fit" );
		btnFit.setFont( Fonts.FONT );
		panelButtons.add( btnFit );

		final ButtonGroup btngroup = new ButtonGroup();
		btngroup.add( rdbtnAll );
		btngroup.add( rdbtnSelection );
		btngroup.add( rdbtnTracks );
		rdbtnSelection.setSelected( true );

		cmbboxModel.addActionListener( e -> lblDoc.setText( docs.get( cmbboxModel.getSelectedIndex() ) ) );
		cmbboxModel.setSelectedIndex( 0 );
	}

	/**
	 * Returns the selected channel. 1-based.
	 * 
	 * @return the selected channel.
	 */
	public int getSelectedChannel()
	{
		return sliderChannel.getValue();
	}

	public int getSelectedFitIndex()
	{
		return cmbboxModel.getSelectedIndex();
	}
}
