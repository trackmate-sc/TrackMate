package fiji.plugin.trackmate.util.config;

import static org.scijava.ui.config.utils.GuiUtils.isLikelyUrl;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.scijava.ui.config.Configurator;
import org.scijava.ui.config.visitors.Maps;
import org.scijava.ui.config.visitors.gui.GuiBuilder;
import org.scijava.ui.config.visitors.gui.GuiBuilder.ConfigPanel;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

public class GenericConfigPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	public static Font FONT = UIManager.getFont( "Label.font" );

	protected final Configurator config;

	protected final ConfigPanel mainPanel;

	public GenericConfigPanel( final Configurator config )
	{
		this.config = config;

		final BorderLayout borderLayout = new BorderLayout();
		setLayout( borderLayout );

		/*
		 * HEADER
		 */

		final JPanel header = new JPanel();
		header.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		header.setLayout( new BoxLayout( header, BoxLayout.Y_AXIS ) );

		final ImageIcon icon = ( config.getIcon() != null )
				? new ImageIcon( config.getIcon().getScaledInstance( 64, 64, Image.SCALE_SMOOTH ) )
				: null;
		final JLabel lblDetector = new JLabel( config.getName(), icon, JLabel.RIGHT );
		lblDetector.setFont( FONT.deriveFont( Font.BOLD ) );
		lblDetector.setHorizontalAlignment( SwingConstants.CENTER );
		lblDetector.setAlignmentX( JLabel.CENTER_ALIGNMENT );
		header.add( lblDetector );

		final String help = config.getHelp();
		final String text = help.trim();
		final JEditorPane infoDisplay;
		if ( isLikelyUrl( text ) )
			infoDisplay = GuiUtils.infoDisplay( "<a href=\"" + text + "\">" + text + "</a>", false );
		else
			infoDisplay = GuiUtils.infoDisplay( help, true );
		header.add( Box.createVerticalStrut( 5 ) );
		header.add( infoDisplay );
		add( header, BorderLayout.NORTH );

		/*
		 * CONFIG
		 */

		this.mainPanel = GuiBuilder.build( config );
		final JScrollPane scrollPane = new JScrollPane( mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setBorder( null );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
		add( scrollPane, BorderLayout.CENTER );
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		Maps.fromMap( settings, config );
		mainPanel.refresh();
	}

	@Override
	public Map< String, Object > getSettings()
	{
		return Maps.toMap( config );
	}

	@Override
	public void clean()
	{}
}
