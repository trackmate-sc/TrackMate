package fiji.plugin.trackmate.gui.components.tracker;

import static fiji.plugin.trackmate.gui.Icons.ADD_ICON;
import static fiji.plugin.trackmate.gui.Icons.REMOVE_ICON;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class JPanelFeatureSelectionGui extends javax.swing.JPanel
{

	private static final long serialVersionUID = 1L;

	private final JPanel panelButtons;

	private final JButton btnRemove;

	private final JButton btnAdd;

	private final Stack< JPanelFeaturePenalty > featurePanels = new Stack<>();

	private List< String > features;

	private Map< String, String > featureNames;

	private int index;

	public JPanelFeatureSelectionGui()
	{
		final BoxLayout layout = new BoxLayout( this, BoxLayout.Y_AXIS );
		this.setLayout( layout );
		panelButtons = new JPanel();
		this.add( panelButtons );
		panelButtons.setPreferredSize( new java.awt.Dimension( 260, 25 ) );
		panelButtons.setLayout( null );

		btnRemove = new JButton();
		panelButtons.add( btnRemove );
		btnRemove.setIcon( REMOVE_ICON );
		btnRemove.setBounds( 48, 5, 21, 22 );
		btnRemove.addActionListener( e -> removeButtonPushed() );

		btnAdd = new JButton();
		panelButtons.add( btnAdd );
		btnAdd.setIcon( ADD_ICON );
		btnAdd.setBounds( 12, 5, 24, 22 );
		btnAdd.addActionListener( e -> addButtonPushed() );
		index = -1;
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Set the features and their names that should be presented by this GUI.
	 * The user will be allowed to choose amongst the given features.
	 */
	public void setDisplayFeatures( final Collection< String > features, final Map< String, String > featureNames )
	{
		this.features = new ArrayList<>( features );
		this.featureNames = featureNames;
	}

	public void setSelectedFeaturePenalties( final Map< String, Double > penalties )
	{
		// Remove old features
		while ( !featurePanels.isEmpty() )
		{
			final JPanelFeaturePenalty panel = featurePanels.pop();
			remove( panel );
		}
		// Remove buttons
		remove( panelButtons );
		// Add new panels
		for ( final String feature : penalties.keySet() )
		{
			final int localIndex = features.indexOf( feature );
			final JPanelFeaturePenalty panel = new JPanelFeaturePenalty( features, featureNames, localIndex );
			panel.setSelectedFeature( feature, penalties.get( feature ) );
			add( panel );
			featurePanels.push( panel );
		}
		// Add buttons back
		add( panelButtons );
	}

	public Map< String, Double > getFeaturePenalties()
	{
		final Map< String, Double > weights = new HashMap<>( featurePanels.size() );
		for ( final JPanelFeaturePenalty panel : featurePanels )
			weights.put( panel.getSelectedFeature(), panel.getPenaltyWeight() );
		return weights;
	}

	@Override
	public void setEnabled( final boolean enabled )
	{
		super.setEnabled( enabled );
		final ArrayList< Component > components = new ArrayList<>( 3 + featurePanels.size() );
		components.add( panelButtons );
		components.add( btnAdd );
		components.add( btnRemove );
		components.addAll( featurePanels );
		for ( final Component component : components )
			component.setEnabled( enabled );
	}

	/*
	 * PRIVATE METHODS
	 */

	private void addButtonPushed()
	{
		index = index + 1;
		if ( index >= features.size() )
			index = 0;
		final JPanelFeaturePenalty panel = new JPanelFeaturePenalty( features, featureNames, index );
		featurePanels.push( panel );
		remove( panelButtons );
		add( panel );
		add( panelButtons );
		final Dimension size = getSize();
		setSize( size.width, size.height + panel.getSize().height );
		revalidate();
	}

	private void removeButtonPushed()
	{
		if ( featurePanels.isEmpty() )
			return;
		final JPanelFeaturePenalty panel = featurePanels.pop();
		remove( panel );
		final Dimension size = getSize();
		setSize( size.width, size.height - panel.getSize().height );
		revalidate();
	}
}
