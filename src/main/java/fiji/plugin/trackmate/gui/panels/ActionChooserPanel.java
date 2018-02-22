package fiji.plugin.trackmate.gui.panels;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SpringLayout;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.providers.ActionProvider;

public class ActionChooserPanel
{

	private static final Icon EXECUTE_ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/control_play_blue.png" ) );

	public final ActionEvent ACTION_STARTED = new ActionEvent( this, 0, "ActionStarted" );

	public final ActionEvent ACTION_FINISHED = new ActionEvent( this, 1, "ActionFinished" );

	private final LogPanel logPanel;

	private final Logger logger;

	private final List< ImageIcon > icons;

	private final ListChooserPanel panel;

	private final ActionProvider actionProvider;

	private SpringLayout layout;

	private final TrackMateGUIController controller;

	private final TrackMate trackmate;

	/*
	 * CONSTRUCTORS
	 */

	public ActionChooserPanel( final ActionProvider actionProvider, final TrackMate trackmate, final TrackMateGUIController controller )
	{

		this.controller = controller;
		this.trackmate = trackmate;
		final List< String > actionKeys = actionProvider.getVisibleKeys();
		final List< String > names = new ArrayList< >( actionKeys.size() );
		final List< String > infoTexts = new ArrayList< >( actionKeys.size() );
		icons = new ArrayList< >( actionKeys.size() );
		for ( final String key : actionKeys )
		{
			infoTexts.add( actionProvider.getFactory( key ).getInfoText() );
			icons.add( actionProvider.getFactory( key ).getIcon() );
			names.add( actionProvider.getFactory( key ).getName() );
		}

		this.panel = new ListChooserPanel( names, infoTexts, "action" );
		this.logPanel = new LogPanel();
		this.logger = logPanel.getLogger();
		this.actionProvider = actionProvider;
		init();
	}

	/*
	 * PUBLIC METHODS
	 */

	public ListChooserPanel getPanel()
	{
		return panel;
	}

	/*
	 * PRIVATE METHODS
	 */

	private void init()
	{

		layout = ( SpringLayout ) panel.getLayout();
		layout.removeLayoutComponent( panel.jLabelHelpText );

		panel.add( logPanel );
		final JButton executeButton = new JButton( "Execute", EXECUTE_ICON );
		executeButton.setFont( FONT );
		executeButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				new Thread( "TrackMate action thread")
				{
					@Override
					public void run()
					{
						try
						{
							executeButton.setEnabled( false );
							panel.fireAction( ACTION_STARTED );
							final int actionIndex = panel.getChoice();
							final String actionKey = actionProvider.getVisibleKeys().get( actionIndex );
							final TrackMateAction action = actionProvider.getFactory( actionKey ).create( controller );
							if ( null == action )
							{
								logger.error( "Unknown action: " + actionKey + ".\n" );
							}
							else
							{
								action.setLogger( logger );
								action.execute( trackmate );
								panel.fireAction( ACTION_FINISHED );
							}
						}
						finally
						{
							executeButton.setEnabled( true );
						}
					}
				}.start();
			}
		} );
		panel.add( executeButton );

		layout.putConstraint( SpringLayout.NORTH, panel.jLabelHelpText, 5, SpringLayout.SOUTH, panel.jComboBoxChoice );
		layout.putConstraint( SpringLayout.WEST, panel.jLabelHelpText, 10, SpringLayout.WEST, panel );
		layout.putConstraint( SpringLayout.EAST, panel.jLabelHelpText, -10, SpringLayout.EAST, panel );
		panel.jLabelHelpText.setPreferredSize( new Dimension( 600, 150 ) );

		layout.putConstraint( SpringLayout.WEST, executeButton, 10, SpringLayout.WEST, panel );
		layout.putConstraint( SpringLayout.EAST, executeButton, 170, SpringLayout.WEST, panel );
		layout.putConstraint( SpringLayout.NORTH, executeButton, 5, SpringLayout.SOUTH, panel.jLabelHelpText );

		layout.putConstraint( SpringLayout.NORTH, logPanel, 5, SpringLayout.SOUTH, executeButton );
		layout.putConstraint( SpringLayout.SOUTH, logPanel, -10, SpringLayout.SOUTH, panel );
		layout.putConstraint( SpringLayout.WEST, logPanel, 10, SpringLayout.WEST, panel );
		layout.putConstraint( SpringLayout.EAST, logPanel, -10, SpringLayout.EAST, panel );

		final HashMap< String, ImageIcon > iconMap = new HashMap< >();
		for ( int i = 0; i < icons.size(); i++ )
		{
			iconMap.put( panel.items.get( i ), icons.get( i ) );
		}
		final IconListRenderer renderer = new IconListRenderer( iconMap );
		panel.jComboBoxChoice.setRenderer( renderer );

	}

	/*
	 * INNER CLASS
	 */

	private class IconListRenderer extends DefaultListCellRenderer
	{
		private static final long serialVersionUID = 1L;

		private HashMap< String, ImageIcon > lIcons = null;

		public IconListRenderer( final HashMap< String, ImageIcon > icons )
		{
			this.lIcons = icons;
		}

		@Override
		public Component getListCellRendererComponent( final JList< ? > list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus )
		{
			final JLabel label = ( JLabel ) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
			final ImageIcon icon = lIcons.get( value );
			label.setIcon( icon );
			return label;
		}
	}
}
