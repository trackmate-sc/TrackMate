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
package fiji.plugin.trackmate.gui.components;

import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Icons.ADD_ICON;
import static fiji.plugin.trackmate.gui.Icons.PLOT_ICON;
import static fiji.plugin.trackmate.gui.Icons.REMOVE_ICON;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import fiji.plugin.trackmate.util.TMUtils;

/**
 * A simple Panel to allow the selection of a X key amongst an enum, and of
 * multiple Y keys from the same enum. This is intended as a GUI panel to
 * prepare for the plotting of data.
 *
 * @author Jean-Yves Tinevez - January 2011 - 2012
 */
public class FeaturePlotSelectionPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/**
	 * Action to execute when the plot button is clicked.
	 */
	public static interface PlotAction
	{
		public void plot( String xKey, List< String > yKeys );
	}

	private static final Dimension BUTTON_SIZE = new Dimension( 24, 24 );
	private static final Dimension COMBO_BOX_MAX_SIZE = new java.awt.Dimension( 220, 22 );
	private static final int MAX_FEATURE_ALLOWED = 10;

	private final JPanel panelYFeatures;

	private final JComboBox< String > cmbboxXFeature;

	private final Stack< JComboBox< String > > comboBoxes = new Stack<>();

	private final Stack< Component > struts = new Stack<>();

	private List< String > features;

	private Map< String, String > featureNames;

	/*
	 * CONSTRUCTOR
	 */

	public FeaturePlotSelectionPanel(
			final String xKey,
			final String yKey,
			final Collection< String > features,
			final Map< String, String > featureNames,
			final PlotAction plotAction )
	{
		this.features = new ArrayList<>( features );
		this.featureNames = featureNames;

		this.setPreferredSize( new Dimension( 300, 450 ) );
		setLayout( new BorderLayout( 0, 0 ) );
		final JPanel topPanel = new JPanel();
		topPanel.setPreferredSize( new Dimension( 300, 180 ) );
		topPanel.setMinimumSize( new Dimension( 300, 100 ) );
		add( topPanel, BorderLayout.NORTH );
		topPanel.setLayout( null );
		final JButton plotButton = new JButton( "Plot features", PLOT_ICON );
		plotButton.setBounds( 80, 27, 140, 40 );
		topPanel.add( plotButton );
		plotButton.setFont( FONT.deriveFont( Font.BOLD ) );

		final JLabel jLabelXFeature = new JLabel();
		jLabelXFeature.setBounds( 10, 93, 170, 13 );
		topPanel.add( jLabelXFeature );
		jLabelXFeature.setText( "Feature for X axis:" );
		jLabelXFeature.setFont( FONT.deriveFont( 12 ) );

		final ComboBoxModel< String > cmbboxXFeatureModel = new DefaultComboBoxModel<>(
				TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
		cmbboxXFeature = new JComboBox<>();
		cmbboxXFeature.setBounds( 30, 117, COMBO_BOX_MAX_SIZE.width, COMBO_BOX_MAX_SIZE.height );
		topPanel.add( cmbboxXFeature );
		cmbboxXFeature.setModel( cmbboxXFeatureModel );
		cmbboxXFeature.setFont( FONT );
		cmbboxXFeature.setSelectedItem( xKey );

		final JLabel lblYFeatures = new JLabel();
		lblYFeatures.setBounds( 10, 149, 280, 20 );
		topPanel.add( lblYFeatures );
		lblYFeatures.setPreferredSize( new Dimension( 250, 20 ) );
		lblYFeatures.setText( "Features for Y axis:" );
		lblYFeatures.setFont( FONT.deriveFont( 12 ) );

		final JPanel centerPanel = new JPanel();
		centerPanel.setBorder( null );
		add( centerPanel, BorderLayout.CENTER );
		centerPanel.setLayout( new BorderLayout( 0, 0 ) );

		final JScrollPane scrlpnYFeatures = new JScrollPane();
		scrlpnYFeatures.setBorder( null );
		centerPanel.add( scrlpnYFeatures );
		scrlpnYFeatures.setPreferredSize( new java.awt.Dimension( 169, 137 ) );
		scrlpnYFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

		panelYFeatures = new JPanel();
		panelYFeatures.setBorder( null );
		panelYFeatures.setLayout( new BoxLayout( panelYFeatures, BoxLayout.Y_AXIS ) );
		scrlpnYFeatures.setViewportView( panelYFeatures );

		final JPanel panelButtons = new JPanel();
		panelButtons.setPreferredSize( new Dimension( 250, 50 ) );
		final BoxLayout jPanelButtonsLayout = new BoxLayout( panelButtons, javax.swing.BoxLayout.X_AXIS );
		panelButtons.setLayout( jPanelButtonsLayout );
		this.add( panelButtons, BorderLayout.SOUTH );

		final JButton btnAdd = new JButton();
		panelButtons.add( btnAdd );
		btnAdd.setIcon( ADD_ICON );
		btnAdd.setMaximumSize( BUTTON_SIZE );
		btnAdd.addActionListener( e -> addFeature() );

		final JButton btnRemove = new JButton();
		panelButtons.add( btnRemove );
		btnRemove.setIcon( REMOVE_ICON );
		btnRemove.setMaximumSize( BUTTON_SIZE );
		btnRemove.addActionListener( e -> removeFeature() );

		final ComboBoxModel< String > jComboBoxYFeatureModel = new DefaultComboBoxModel<>(
				TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
		final JComboBox< String > cmbboxYFeature = new JComboBox<>();
		cmbboxYFeature.setModel( jComboBoxYFeatureModel );
		cmbboxYFeature.setPreferredSize( COMBO_BOX_MAX_SIZE );
		cmbboxYFeature.setMaximumSize( COMBO_BOX_MAX_SIZE );
		cmbboxYFeature.setFont( FONT );

		// Add the default feature
		addFeature( yKey );

		// Listener.
		plotButton.addActionListener( e -> {
			final String sKey = this.features.get( cmbboxXFeature.getSelectedIndex() );
			final List< String > yKeys = new ArrayList<>( comboBoxes.size() );
			for ( final JComboBox< String > box : comboBoxes )
				yKeys.add( this.features.get( box.getSelectedIndex() ) );

			plotAction.plot( sKey, yKeys );
		} );
	}

	public void setFeatures( final Collection< String > features, final Map< String, String > featureNames )
	{
		this.features = new ArrayList<>( features );
		this.featureNames = featureNames;
		final Object previousKey = cmbboxXFeature.getSelectedItem();

		final ComboBoxModel< String > cmbboxXFeatureModel = new DefaultComboBoxModel<>(
				TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
		cmbboxXFeature.setModel( cmbboxXFeatureModel );
		cmbboxXFeature.setSelectedItem( previousKey );

		for ( final JComboBox< String > cb : comboBoxes )
		{
			final Object previousYKey = cb.getSelectedItem();
			final ComboBoxModel< String > cmbboxYFeatureModel = new DefaultComboBoxModel<>(
					TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
			cb.setModel( cmbboxYFeatureModel );
			cb.setSelectedItem( previousYKey );
		}
	}

	/*
	 * PRIVATE METHODS
	 */

	private void addFeature( final String yKey )
	{
		if ( comboBoxes.size() > MAX_FEATURE_ALLOWED )
			return;

		final ComboBoxModel< String > cmbboxYFeatureModel = new DefaultComboBoxModel<>(
				TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
		final JComboBox< String > cmbboxYFeature = new JComboBox<>();
		cmbboxYFeature.setModel( cmbboxYFeatureModel );
		cmbboxYFeature.setMaximumSize( COMBO_BOX_MAX_SIZE );
		cmbboxYFeature.setFont( FONT );
		cmbboxYFeature.setSelectedItem( yKey );

		final Component strut = Box.createVerticalStrut( 10 );
		panelYFeatures.add( strut );
		panelYFeatures.add( cmbboxYFeature );
		panelYFeatures.revalidate();
		comboBoxes.push( cmbboxYFeature );
		struts.push( strut );
	}

	private void addFeature()
	{
		String nextFeature = "";
		if ( !comboBoxes.isEmpty() )
		{
			int newIndex = comboBoxes.get( comboBoxes.size() - 1 ).getSelectedIndex() + 1;
			if ( newIndex >= features.size() )
				newIndex = 0;
			nextFeature = featureNames.get( features.get( newIndex ) );
		}
		addFeature( nextFeature );
	}

	private void removeFeature()
	{
		if ( comboBoxes.size() <= 1 )
			return;
		panelYFeatures.remove( comboBoxes.pop() );
		panelYFeatures.remove( struts.pop() );
		panelYFeatures.revalidate();
		panelYFeatures.repaint();
	}
}
