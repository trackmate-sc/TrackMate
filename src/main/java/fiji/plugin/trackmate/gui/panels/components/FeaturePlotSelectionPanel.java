package fiji.plugin.trackmate.gui.panels.components;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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

import com.itextpdf.text.Font;

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

	private JPanel topPanel;

	private JPanel centerPanel;

	private JComboBox< String > jComboBoxYFeature;

	/*
	 * CONSTRUCTOR
	 */

	public FeaturePlotSelectionPanel( final String xKey, final Collection< String > features, final Map< String, String > featureNames )
	{
		super();
		this.xKey = xKey;
		this.features = new ArrayList< >( features );
		this.featureNames = featureNames;
		initGUI();
		addFeature();
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void setEnabled( final boolean enabled )
	{
		for ( final Component component : topPanel.getComponents() )
		{
			component.setEnabled( enabled );
		}
		for ( final Component component : centerPanel.getComponents() )
		{
			component.setEnabled( enabled );
		}
		for ( final Component component : jPanelYFeatures.getComponents() )
		{
			component.setEnabled( enabled );
		}
		super.setEnabled( enabled );
	}

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
		final JComboBox< String > lJComboBoxYFeature = new JComboBox< >();
		lJComboBoxYFeature.setModel( jComboBoxYFeatureModel );
		lJComboBoxYFeature.setMaximumSize( COMBO_BOX_MAX_SIZE );
		lJComboBoxYFeature.setFont( FONT );

		if ( !comboBoxes.isEmpty() )
		{
			int newIndex = comboBoxes.get( comboBoxes.size() - 1 ).getSelectedIndex() + 1;
			if ( newIndex >= features.size() )
				newIndex = 0;
			lJComboBoxYFeature.setSelectedIndex( newIndex );
		}

		final Component strut = Box.createVerticalStrut( 10 );
		jPanelYFeatures.add( strut );
		jPanelYFeatures.add( lJComboBoxYFeature );
		jPanelYFeatures.revalidate();
		comboBoxes.push( lJComboBoxYFeature );
		struts.push( strut );
	}

	private void removeFeature()
	{
		if ( comboBoxes.size() <= 1 )
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
			this.setPreferredSize( new Dimension( 300, 450 ) );
			setLayout( new BorderLayout( 0, 0 ) );
			{
				topPanel = new JPanel();
				topPanel.setPreferredSize( new Dimension( 300, 180 ) );
				topPanel.setMinimumSize( new Dimension( 300, 100 ) );
				add( topPanel, BorderLayout.NORTH );
				topPanel.setLayout( null );
				final JButton plotButton = new JButton( "Plot features", PLOT_ICON );
				plotButton.setBounds( 80, 27, 140, 40 );
				plotButton.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent arg0 )
					{
						firePlotSelectionData();
					}
				} );
				topPanel.add( plotButton );
				plotButton.setFont( FONT.deriveFont( Font.BOLD ) );
			}
			{
				jLabelXFeature = new JLabel();
				jLabelXFeature.setBounds( 10, 93, 170, 13 );
				topPanel.add( jLabelXFeature );
				jLabelXFeature.setText( "Feature for X axis:" );
				jLabelXFeature.setFont( FONT.deriveFont( 12 ) );
			}
			{
				final ComboBoxModel< String > jComboBoxXFeatureModel = new DefaultComboBoxModel< >(
						TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
				jComboBoxXFeature = new JComboBox< >();
				jComboBoxXFeature.setBounds( 30, 117, COMBO_BOX_MAX_SIZE.width, COMBO_BOX_MAX_SIZE.height );
				topPanel.add( jComboBoxXFeature );
				jComboBoxXFeature.setModel( jComboBoxXFeatureModel );
				jComboBoxXFeature.setFont( FONT );
				jComboBoxXFeature.setSelectedIndex( features.indexOf( xKey ) );
			}
			{
				jLabelYFeatures = new JLabel();
				jLabelYFeatures.setBounds( 10, 149, 280, 20 );
				topPanel.add( jLabelYFeatures );
				jLabelYFeatures.setPreferredSize( new Dimension( 250, 20 ) );
				jLabelYFeatures.setText( "Features for Y axis:" );
				jLabelYFeatures.setFont( FONT.deriveFont( 12 ) );
			}
			{
				centerPanel = new JPanel();
				centerPanel.setBorder( null );
				add( centerPanel, BorderLayout.CENTER );
				centerPanel.setLayout( new BorderLayout( 0, 0 ) );
			}
			{
				jScrollPaneYFeatures = new JScrollPane();
				jScrollPaneYFeatures.setBorder( null );
				centerPanel.add( jScrollPaneYFeatures );
				jScrollPaneYFeatures.setPreferredSize( new java.awt.Dimension( 169, 137 ) );
				jScrollPaneYFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
				{
					jPanelYFeatures = new JPanel();
					jPanelYFeatures.setBorder( null );
					jPanelYFeatures.setLayout( new BoxLayout( jPanelYFeatures, BoxLayout.Y_AXIS ) );
					jScrollPaneYFeatures.setViewportView( jPanelYFeatures );
				}
			}
			{
				jPanelButtons = new JPanel();
				jPanelButtons.setPreferredSize( new Dimension( 250, 50 ) );
				final BoxLayout jPanelButtonsLayout = new BoxLayout( jPanelButtons, javax.swing.BoxLayout.X_AXIS );
				jPanelButtons.setLayout( jPanelButtonsLayout );
				this.add( jPanelButtons, BorderLayout.SOUTH );
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
			}
			{
				final ComboBoxModel< String > jComboBoxYFeatureModel = new DefaultComboBoxModel< >(
						TMUtils.getArrayFromMaping( features, featureNames ).toArray( new String[] {} ) );
				jComboBoxYFeature = new JComboBox< >();
				jComboBoxYFeature.setModel( jComboBoxYFeatureModel );
				jComboBoxYFeature.setPreferredSize( COMBO_BOX_MAX_SIZE );
				jComboBoxYFeature.setMaximumSize( COMBO_BOX_MAX_SIZE );
				jComboBoxYFeature.setFont( FONT );
			}
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
