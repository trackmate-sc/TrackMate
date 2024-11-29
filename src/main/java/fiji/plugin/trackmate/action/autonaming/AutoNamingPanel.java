package fiji.plugin.trackmate.action.autonaming;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.Fonts;

public class AutoNamingPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	// Updated names
	final JComboBox<AutoNamingRule> ruleSelectionDropdown;
	final JButton runAutoNamingButton;

	public AutoNamingPanel(final Collection<AutoNamingRule> namingRules) {
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 1.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);

		final JLabel lblTitle = new JLabel("Auto naming spots");
		lblTitle.setFont(Fonts.BIG_FONT);
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		final GridBagConstraints gbcLblTitle = new GridBagConstraints();
		gbcLblTitle.gridwidth = 2;
		gbcLblTitle.insets = new Insets(0, 0, 5, 0);
		gbcLblTitle.fill = GridBagConstraints.HORIZONTAL;
		gbcLblTitle.gridx = 0;
		gbcLblTitle.gridy = 0;
		add(lblTitle, gbcLblTitle);

		ruleSelectionDropdown = new JComboBox<>(new Vector<>(namingRules));
		ruleSelectionDropdown.setFont(Fonts.SMALL_FONT);
		final GridBagConstraints gbcDropdown = new GridBagConstraints();
		gbcDropdown.insets = new Insets(0, 0, 5, 0);
		gbcDropdown.fill = GridBagConstraints.HORIZONTAL;
		gbcDropdown.gridx = 1;
		gbcDropdown.gridy = 2;
		add(ruleSelectionDropdown, gbcDropdown);

		runAutoNamingButton = new JButton("Run Auto-Naming");
		final GridBagConstraints gbcButton = new GridBagConstraints();
		gbcButton.anchor = GridBagConstraints.EAST;
		gbcButton.gridx = 1;
		gbcButton.gridy = 4;
		add(runAutoNamingButton, gbcButton);
	}
}
