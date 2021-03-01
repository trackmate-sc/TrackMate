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

		final ComboBoxModel< String > jComboBoxFeatureModel = new DefaultComboBoxModel<>(
				TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
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
