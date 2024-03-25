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

import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Fonts.TEXTFIELD_DIMENSION;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.util.TMUtils;

public class JPanelFeaturePenalty extends javax.swing.JPanel
{

	private static final long serialVersionUID = 1L;

	private final JComboBox< String > cmbboxFeature;

	private final JFormattedTextField txtfldFeatureWeight;

	private final List< String > features;

	public JPanelFeaturePenalty( final List< String > features, final Map< String, String > featureNames, final int index )
	{
		this.features = features;

		this.setPreferredSize( new java.awt.Dimension( 280, 40 ) );
		this.setSize( 280, 40 );
		this.setLayout( null );

		final ComboBoxModel< String > jComboBoxFeatureModel = new DefaultComboBoxModel<>( TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
		cmbboxFeature = new JComboBox<>();
		this.add( cmbboxFeature );
		cmbboxFeature.setModel( jComboBoxFeatureModel );
		cmbboxFeature.setBounds( 2, 4, 205, 22 );
		cmbboxFeature.setFont( SMALL_FONT );

		txtfldFeatureWeight = new JFormattedTextField( new DecimalFormat( "0.0" ) );
		txtfldFeatureWeight.setHorizontalAlignment( JFormattedTextField.CENTER );
		txtfldFeatureWeight.setValue( Double.valueOf( 1. ) );
		this.add( txtfldFeatureWeight );
		txtfldFeatureWeight.setBounds( 220, 4, 30, 22 );
		txtfldFeatureWeight.setSize( TEXTFIELD_DIMENSION );
		txtfldFeatureWeight.setFont( SMALL_FONT );

		// Select text-field content on focus.
		GuiUtils.selectAllOnFocus( txtfldFeatureWeight );

		// Select default.
		cmbboxFeature.setSelectedIndex( index );
	}

	/*
	 * PUBLIC METHODS
	 */

	public void setSelectedFeature( final String feature, final double weight )
	{
		final int index = features.indexOf( feature );
		if ( index < 0 )
		{ return; }
		cmbboxFeature.setSelectedIndex( index );
		txtfldFeatureWeight.setValue( Double.valueOf( weight ) );
	}

	public String getSelectedFeature()
	{
		return features.get( cmbboxFeature.getSelectedIndex() );
	}

	public double getPenaltyWeight()
	{
		return ( ( Number ) txtfldFeatureWeight.getValue() ).doubleValue();
	}

	@Override
	public void setEnabled( final boolean enabled )
	{
		super.setEnabled( enabled );
		cmbboxFeature.setEnabled( enabled );
		txtfldFeatureWeight.setEnabled( enabled );
	}
}
