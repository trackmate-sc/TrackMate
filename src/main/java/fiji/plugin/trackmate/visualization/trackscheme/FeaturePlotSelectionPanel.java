package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.SwingConstants;

import com.itextpdf.text.Font;

import fiji.plugin.trackmate.gui.panels.ActionListenablePanel;
import fiji.plugin.trackmate.util.TMUtils;

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

	private static final ImageIcon PLOT_ICON = new ImageIcon( TrackSchemeFrame.class.getResource( "resources/plots.png" ) );

	private static final ImageIcon ADD_ICON = new ImageIcon( FeaturePlotSelectionPanel.class.getResource( "resources/add.png" ) );

	private static final ImageIcon REMOVE_ICON = new ImageIcon( FeaturePlotSelectionPanel.class.getResource( "resources/delete.png" ) );

	private static final Dimension BUTTON_SIZE = new Dimension( 24, 24 );

	private static final Dimension COMBO_BOX_SIZE = new java.awt.Dimension( 150, 22 );

	private static final int MAX_FEATURE_ALLOWED = 10;

	private JLabel jLabelXFeature;

	private JScrollPane jScrollPaneYFeatures;

	private JButton jButtonRemove;

	private JPanel jPanelYFeatures;

	private JButton jButtonAdd;

	private JPanel jPanelButtons;

	private JLabel jLabelYFeatures;

	private JComboBox< String > jComboBoxXFeature;

	private final Stack< JComboBox< String > > comboBoxes = new Stack< >();

	private final Stack< Component > struts = new Stack< >();

	private final String xKey;

	private final List< String > features;

	private final Map< String, String > featureNames;

	/*
	 * CONSTRUCTOR
	 */

	public FeaturePlotSelectionPanel( final String xKey, final List< String > features, final Map< String, String > featureNames )
	{
		super();
		this.xKey = xKey;
		this.features = features;
		this.featureNames = featureNames;
		initGUI();
		addFeature();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Return the enum constant selected in the X combo-box feature.
	 */
	public String getXKey()
	{
		return features.get( jComboBoxXFeature.getSelectedIndex() );
	}

	/**
	 * Return a set of the keys selected in the Y feature panel. Since we use a
	 * {@link Set}, duplicates are trimmed.
	 */
	public Set< String > getYKeys()
	{
		final Set< String > yKeys = new HashSet< >( comboBoxes.size() );
		for ( final JComboBox< String > box : comboBoxes )
			yKeys.add( features.get( box.getSelectedIndex() ) );
		return yKeys;
	}

	/*
	 * PRIVATE METHODS
	 */

	private void addFeature()
	{

		if ( comboBoxes.size() > MAX_FEATURE_ALLOWED )
			return;

		final ComboBoxModel< String > jComboBoxYFeatureModel = new DefaultComboBoxModel< >(
				TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
		final JComboBox< String > jComboBoxYFeature = new JComboBox< >();
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
		jPanelYFeatures.add( strut );
		jPanelYFeatures.add( jComboBoxYFeature );
		jPanelYFeatures.revalidate();
		comboBoxes.push( jComboBoxYFeature );
		struts.push( strut );
	}

	private void removeFeature()
	{
		if ( comboBoxes.isEmpty() )
			return;
		jPanelYFeatures.remove( comboBoxes.pop() );
		jPanelYFeatures.remove( struts.pop() );
		jPanelYFeatures.revalidate();
		jPanelYFeatures.repaint();

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
		{
			command += ( Y[ i ] + ", " );
		}
		command += Y[ Y.length - 1 ];
		command += " vs " + getXKey();

		final ActionEvent plotEvent = new ActionEvent( this, 0, command );
		for ( final ActionListener listener : actionListeners )
		{
			listener.actionPerformed( plotEvent );
		}
	}

	private void initGUI()
	{
		try
		{
			this.setLayout( null );
			this.setPreferredSize( new Dimension( 170, 284 ) );
			{
				jLabelXFeature = new JLabel();
				this.add( jLabelXFeature );
				jLabelXFeature.setText( "Feature for X axis:" );
				jLabelXFeature.setFont( FONT );
				jLabelXFeature.setBounds( 8, 66, 148, 26 );
			}
			{
				final ComboBoxModel< String > jComboBoxXFeatureModel = new DefaultComboBoxModel< >(
						TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
				jComboBoxXFeature = new JComboBox< >();
				this.add( jComboBoxXFeature );
				jComboBoxXFeature.setModel( jComboBoxXFeatureModel );
				jComboBoxXFeature.setFont( SMALL_FONT );
				jComboBoxXFeature.setBounds( 8, 92, COMBO_BOX_SIZE.width, COMBO_BOX_SIZE.height );
				jComboBoxXFeature.setSelectedIndex( features.indexOf( xKey ) );
			}
			{
				jLabelYFeatures = new JLabel();
				this.add( jLabelYFeatures );
				jLabelYFeatures.setText( "Features for Y axis:" );
				jLabelYFeatures.setFont( FONT );
				jLabelYFeatures.setBounds( 8, 114, 148, 22 );
			}
			{
				jScrollPaneYFeatures = new JScrollPane();
				this.add( jScrollPaneYFeatures );
				jScrollPaneYFeatures.setPreferredSize( new java.awt.Dimension( 169, 137 ) );
				jScrollPaneYFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
				jScrollPaneYFeatures.setBounds( 0, 139, 168, 105 );
				{
					jPanelYFeatures = new JPanel();
					jPanelYFeatures.setLayout( new BoxLayout( jPanelYFeatures, BoxLayout.Y_AXIS ) );
					jScrollPaneYFeatures.setViewportView( jPanelYFeatures );
				}
			}
			{
				jPanelButtons = new JPanel();
				final BoxLayout jPanelButtonsLayout = new BoxLayout( jPanelButtons, javax.swing.BoxLayout.X_AXIS );
				jPanelButtons.setLayout( jPanelButtonsLayout );
				this.add( jPanelButtons );
				jPanelButtons.setBounds( 10, 244, 148, 29 );
				{
					jButtonAdd = new JButton();
					jPanelButtons.add( jButtonAdd );
					jButtonAdd.setIcon( ADD_ICON );
					jButtonAdd.setMaximumSize( BUTTON_SIZE );
					jButtonAdd.addActionListener( new ActionListener()
					{
						@Override
						public void actionPerformed( final ActionEvent e )
						{
							addFeature();
						}
					} );
				}
				{
					jButtonRemove = new JButton();
					jPanelButtons.add( jButtonRemove );
					jButtonRemove.setIcon( REMOVE_ICON );
					jButtonRemove.setMaximumSize( BUTTON_SIZE );
					jButtonRemove.addActionListener( new ActionListener()
					{
						@Override
						public void actionPerformed( final ActionEvent e )
						{
							removeFeature();
						}
					} );
				}
				{
					final JButton plotButton = new JButton( "Plot features", PLOT_ICON );
					plotButton.setFont( FONT.deriveFont( Font.BOLD ) );
					plotButton.setHorizontalAlignment( SwingConstants.RIGHT );
					plotButton.setBounds( 24, 21, 119, 34 );
					plotButton.addActionListener( new ActionListener()
					{
						@Override
						public void actionPerformed( final ActionEvent e )
						{
							firePlotSelectionData();
						}
					} );
					add( plotButton );

				}
			}
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
