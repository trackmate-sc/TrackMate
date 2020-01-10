package fiji.plugin.trackmate.action;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class LabelImgExporterPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JCheckBox exportSpotsAsDots;

	private final JCheckBox exportTracksOnly;

	public LabelImgExporterPanel()
	{
		final GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout( gridBagLayout );

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets( 5, 5, 5, 5 );
		gbc.gridx = 0;
		gbc.gridy = 0;

		exportSpotsAsDots = new JCheckBox( "Export spots as single pixels", false );
		add( exportSpotsAsDots, gbc );

		exportTracksOnly = new JCheckBox( "Export only spots in tracks", false );
		gbc.gridy++;
		add( exportTracksOnly, gbc );

	}

	public boolean isExportSpotsAsDots()
	{
		return exportSpotsAsDots.isSelected();
	}

	public boolean isExportTracksOnly()
	{
		return exportTracksOnly.isSelected();
	}
}
