package fiji.plugin.trackmate.gui.components;

import static fiji.plugin.trackmate.gui.Icons.ADD_ICON;
import static fiji.plugin.trackmate.gui.Icons.REMOVE_ICON;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class JPanelFeatureSelectionGui extends javax.swing.JPanel {

	private static final long serialVersionUID = -891462567905389989L;

	private JPanel jPanelButtons;
	private JButton jButtonRemove;
	private JButton jButtonAdd;

	private final Stack<JPanelFeaturePenalty> featurePanels = new Stack<>();
	private List<String> features;
	private Map<String, String> featureNames;
	private int index;

	public JPanelFeatureSelectionGui() {
		initGUI();
		index = -1;
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Set the features and their names that should be presented by this GUI.
	 * The user will be allowed to choose amongst the given features. 
	 */
	public void setDisplayFeatures(final Collection<String> features, final Map<String, String> featureNames) {
		this.features = new ArrayList<>(features);
		this.featureNames = featureNames;
	}

	public void setSelectedFeaturePenalties(final Map<String, Double> penalties) {
		// Remove old features
		while (!featurePanels.isEmpty()) {
			final JPanelFeaturePenalty panel = featurePanels.pop();
			remove(panel);
		}
		// Remove buttons 
		remove(jPanelButtons);
		// Add new panels
		for (final String feature : penalties.keySet()) {
			final int localIndex = features.indexOf(feature);
			final JPanelFeaturePenalty panel = new JPanelFeaturePenalty(features, featureNames, localIndex);
			panel.setSelectedFeature(feature, penalties.get(feature));
			add(panel);
			featurePanels.push(panel);
		}
		// Add buttons back
		add(jPanelButtons);
	}

	public Map<String, Double>	getFeaturePenalties() {
		final Map<String, Double> weights = new HashMap<>(featurePanels.size());
		for (final JPanelFeaturePenalty panel : featurePanels) 
			weights.put(panel.getSelectedFeature(), panel.getPenaltyWeight());
		return weights;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);
		final ArrayList<Component> components = new ArrayList<>(3 + featurePanels.size());
		components.add(jPanelButtons);
		components.add(jButtonAdd);
		components.add(jButtonRemove);
		components.addAll(featurePanels);
		for(final Component component : components)
			component.setEnabled(enabled);
	}

	/*
	 * PRIVATE METHODS
	 */

	private void addButtonPushed() {
		index = index + 1;
		if (index >= features.size())
			index = 0;
		final JPanelFeaturePenalty panel = new JPanelFeaturePenalty(features, featureNames, index);
		featurePanels.push(panel);
		remove(jPanelButtons);
		add(panel);
		add(jPanelButtons);
		final Dimension size = getSize();
		setSize(size.width, size.height + panel.getSize().height);
		revalidate();
	}

	private void removeButtonPushed() {
		if (featurePanels.isEmpty())
			return;
		final JPanelFeaturePenalty panel = featurePanels.pop();
		remove(panel);
		final Dimension size = getSize();
		setSize(size.width, size.height - panel.getSize().height);
		revalidate();
	}

	private void initGUI() {
		try {
			final BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
			this.setLayout(layout);
			{
				jPanelButtons = new JPanel();
				this.add(jPanelButtons);
				jPanelButtons.setPreferredSize(new java.awt.Dimension(260, 25));
				jPanelButtons.setLayout(null);
				{
					jButtonRemove = new JButton();
					jPanelButtons.add(jButtonRemove);
					jButtonRemove.setIcon( REMOVE_ICON );
					jButtonRemove.setBounds(48, 5, 21, 22);
					jButtonRemove.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							removeButtonPushed();
						}
					});
				}
				{
					jButtonAdd = new JButton();
					jPanelButtons.add(jButtonAdd);
					jButtonAdd.setIcon( ADD_ICON );
					jButtonAdd.setBounds(12, 5, 24, 22);
					jButtonAdd.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							addButtonPushed();
						}
					});
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
