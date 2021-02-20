package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Icons.ADD_ICON;
import static fiji.plugin.trackmate.gui.Icons.PLOT_ICON;
import static fiji.plugin.trackmate.gui.Icons.REMOVE_ICON;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.components.FeaturePlotSelectionPanel;
import fiji.plugin.trackmate.gui.components.FeaturePlotSelectionPanel.PlotAction;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * A simple Panel to allow the selection of a X key amongst an enum, and of
 * multiple Y keys from the same enum. This is intended as a GUI panel to
 * prepare for the plotting of data.
 * <p>
 * It has the same functionality that of {@link FeaturePlotSelectionPanel} but
 * has a more compact UI.
 *
 * @author Jean-Yves Tinevez - January 2011 - 2012
 */
public class SmallFeaturePlotSelectionPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final Dimension BUTTON_SIZE = new Dimension( 24, 24 );

	private static final Dimension COMBO_BOX_SIZE = new java.awt.Dimension( 150, 22 );

	private static final int MAX_FEATURE_ALLOWED = 10;

	private final JPanel panelYFeatures;

	private final Stack< JComboBox< String > > comboBoxes = new Stack<>();

	private final Stack< Component > struts = new Stack<>();

	private final List< String > features;

	private final Map< String, String > featureNames;

	/*
	 * CONSTRUCTOR
	 */

	public SmallFeaturePlotSelectionPanel(
			final String xKey,
			final List< String > features,
			final Map< String, String > featureNames,
			final PlotAction plotAction )
	{
		this.features = features;
		this.featureNames = featureNames;

		this.setLayout( null );
		this.setPreferredSize( new Dimension( 170, 284 ) );

		final JLabel jLabelXFeature = new JLabel();
		this.add( jLabelXFeature );
		jLabelXFeature.setText( "Feature for X axis:" );
		jLabelXFeature.setFont( FONT );
		jLabelXFeature.setBounds( 8, 66, 148, 26 );

		final ComboBoxModel< String > jComboBoxXFeatureModel = new DefaultComboBoxModel<>(
				TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
		final JComboBox< String > jComboBoxXFeature = new JComboBox<>();
		this.add( jComboBoxXFeature );
		jComboBoxXFeature.setModel( jComboBoxXFeatureModel );
		jComboBoxXFeature.setFont( SMALL_FONT );
		jComboBoxXFeature.setBounds( 8, 92, COMBO_BOX_SIZE.width, COMBO_BOX_SIZE.height );
		jComboBoxXFeature.setSelectedItem( xKey );

		final JLabel jLabelYFeatures = new JLabel();
		this.add( jLabelYFeatures );
		jLabelYFeatures.setText( "Features for Y axis:" );
		jLabelYFeatures.setFont( FONT );
		jLabelYFeatures.setBounds( 8, 114, 148, 22 );

		final JScrollPane jScrollPaneYFeatures = new JScrollPane();
		this.add( jScrollPaneYFeatures );
		jScrollPaneYFeatures.setPreferredSize( new java.awt.Dimension( 169, 137 ) );
		jScrollPaneYFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		jScrollPaneYFeatures.setBounds( 0, 139, 168, 105 );

		panelYFeatures = new JPanel();
		panelYFeatures.setLayout( new BoxLayout( panelYFeatures, BoxLayout.Y_AXIS ) );
		jScrollPaneYFeatures.setViewportView( panelYFeatures );

		final JPanel jPanelButtons = new JPanel();
		final BoxLayout jPanelButtonsLayout = new BoxLayout( jPanelButtons, javax.swing.BoxLayout.X_AXIS );
		jPanelButtons.setLayout( jPanelButtonsLayout );
		this.add( jPanelButtons );
		jPanelButtons.setBounds( 10, 244, 148, 29 );

		final JButton jButtonAdd = new JButton();
		jPanelButtons.add( jButtonAdd );
		jButtonAdd.setIcon( ADD_ICON );
		jButtonAdd.setMaximumSize( BUTTON_SIZE );
		jButtonAdd.addActionListener( e -> addFeature() );

		final JButton jButtonRemove = new JButton();
		jPanelButtons.add( jButtonRemove );
		jButtonRemove.setIcon( REMOVE_ICON );
		jButtonRemove.setMaximumSize( BUTTON_SIZE );
		jButtonRemove.addActionListener( e -> removeFeature() );

		final JButton plotButton = new JButton( "Plot features", PLOT_ICON );
		plotButton.setFont( FONT.deriveFont( Font.BOLD ) );
		plotButton.setHorizontalAlignment( SwingConstants.RIGHT );
		plotButton.setBounds( 24, 21, 119, 34 );
		add( plotButton );

		// Listener.
		plotButton.addActionListener( e -> {
			final String sKey = features.get( jComboBoxXFeature.getSelectedIndex() );
			final Set< String > yKeys = new HashSet<>( comboBoxes.size() );
			for ( final JComboBox< String > box : comboBoxes )
				yKeys.add( features.get( box.getSelectedIndex() ) );
			plotAction.plot( sKey, yKeys );
		} );

		// Add a feature.
		addFeature();
	}

	/*
	 * PRIVATE METHODS
	 */

	private void addFeature()
	{
		if ( comboBoxes.size() > MAX_FEATURE_ALLOWED )
			return;

		final ComboBoxModel< String > jComboBoxYFeatureModel = new DefaultComboBoxModel<>(
				TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
		final JComboBox< String > jComboBoxYFeature = new JComboBox<>();
		jComboBoxYFeature.setModel( jComboBoxYFeatureModel );
		jComboBoxYFeature.setPreferredSize( COMBO_BOX_SIZE );
		jComboBoxYFeature.setMaximumSize( COMBO_BOX_SIZE );
		jComboBoxYFeature.setFont( SMALL_FONT );

		if ( !comboBoxes.isEmpty() )
		{
			int newIndex = comboBoxes.get( comboBoxes.size() - 1 ).getSelectedIndex() + 1;
			if ( newIndex >= features.size() )
				newIndex = 0;
			jComboBoxYFeature.setSelectedIndex( newIndex );
		}

		final Component strut = Box.createVerticalStrut( 5 );
		panelYFeatures.add( strut );
		panelYFeatures.add( jComboBoxYFeature );
		panelYFeatures.revalidate();
		comboBoxes.push( jComboBoxYFeature );
		struts.push( strut );
	}

	private void removeFeature()
	{
		if ( comboBoxes.isEmpty() )
			return;
		panelYFeatures.remove( comboBoxes.pop() );
		panelYFeatures.remove( struts.pop() );
		panelYFeatures.revalidate();
		panelYFeatures.repaint();
	}
}
