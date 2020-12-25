package fiji.plugin.trackmate.gui.panels.components;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import fiji.plugin.trackmate.gui.panels.ActionListenablePanel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

/**
 * A simple Panel to allow the selection of a X key amongst an enum, and of
 * multiple Y keys from the same enum. This is intended as a GUI panel to
 * prepare for the plotting of data.
 *
 * @author Jean-Yves Tinevez &lt;tinevez@pasteur.fr&gt; - January 2011 - 2012
 */
public class FeaturePlotSelectionPanel extends ActionListenablePanel
{

	private static final long serialVersionUID = 1L;

	private static final ImageIcon PLOT_ICON = new ImageIcon( TrackScheme.class.getResource( "resources/plots.png" ) );
	private static final ImageIcon ADD_ICON = new ImageIcon( TrackScheme.class.getResource( "resources/add.png" ) );
	private static final ImageIcon REMOVE_ICON = new ImageIcon( TrackScheme.class.getResource( "resources/delete.png" ) );
	private static final Dimension BUTTON_SIZE = new Dimension( 24, 24 );
	private static final Dimension COMBO_BOX_MAX_SIZE = new java.awt.Dimension( 220, 22 );
	private static final int MAX_FEATURE_ALLOWED = 10;

	private final JPanel panelYFeatures;

	private final JComboBox< String > cmbboxXFeature;

	private final Stack< JComboBox< String > > comboBoxes = new Stack<>();

	private final Stack< Component > struts = new Stack<>();

	private final List< String > features;

	private final Map< String, String > featureNames;

	/*
	 * CONSTRUCTOR
	 */

	public FeaturePlotSelectionPanel( final String xKey, final String yKey, final Collection< String > features, final Map< String, String > featureNames )
	{
		super();
		this.features = new ArrayList<>( features );
		this.featureNames = featureNames;

		/*
		 * GUI.
		 */

		this.setPreferredSize( new Dimension( 300, 450 ) );
		setLayout( new BorderLayout( 0, 0 ) );
		final JPanel topPanel = new JPanel();
		topPanel.setPreferredSize( new Dimension( 300, 180 ) );
		topPanel.setMinimumSize( new Dimension( 300, 100 ) );
		add( topPanel, BorderLayout.NORTH );
		topPanel.setLayout( null );
		final JButton plotButton = new JButton( "Plot features", PLOT_ICON );
		plotButton.setBounds( 80, 27, 140, 40 );
		plotButton.addActionListener( e -> firePlotSelectionData() );
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

		/*
		 * Add a feature
		 */

		addFeature( yKey );
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Return the enum constant selected in the X combo-box feature.
	 */
	public String getXKey()
	{
		return features.get( cmbboxXFeature.getSelectedIndex() );
	}

	/**
	 * Return a set of the keys selected in the Y feature panel. Since we use a
	 * {@link Set}, duplicates are trimmed.
	 */
	public Set< String > getYKeys()
	{
		final Set< String > yKeys = new HashSet<>( comboBoxes.size() );
		for ( final JComboBox< String > box : comboBoxes )
			yKeys.add( features.get( box.getSelectedIndex() ) );
		return yKeys;
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

	/**
	 * Notifies listeners that the plot feature button has been pressed.
	 */
	private void firePlotSelectionData()
	{
		// Prepare command string. Does not matter actually, but let's do it
		// right.
		String command = "Plot ";
		final String[] Y = getYKeys().toArray( new String[] {} );
		for ( int i = 0; i < Y.length - 1; i++ )
			command += ( Y[ i ] + ", " );

		command += Y[ Y.length - 1 ];
		command += " vs " + getXKey();

		final ActionEvent plotEvent = new ActionEvent( this, 0, command );
		for ( final ActionListener listener : actionListeners )
			listener.actionPerformed( plotEvent );
	}
}
