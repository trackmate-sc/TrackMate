package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.Icons.DISPLAY_CONFIG_ICON;
import static fiji.plugin.trackmate.gui.Icons.LOG_ICON;
import static fiji.plugin.trackmate.gui.Icons.NEXT_ICON;
import static fiji.plugin.trackmate.gui.Icons.PREVIOUS_ICON;
import static fiji.plugin.trackmate.gui.Icons.SAVE_ICON;
import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.scijava.object.ObjectService;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.descriptors.WizardPanelDescriptor;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * A GUI for TrackMate, strongly inspired from the spots detection GUI of the
 * Imaris® software from <a href="http://www.bitplane.com/">Bitplane</a>.
 * 
 * @author Jean-Yves Tinevez - September 2010 - 2014
 */
public class TrackMateWizard extends JFrame implements ActionListener
{

	final JButton jButtonSave;

	final JButton jButtonPrevious;

	final JButton jButtonNext;

	final JButton jButtonLog;

	final JButton jButtonDisplayConfig;

	/*
	 * DEFAULT VISIBILITY & PUBLIC CONSTANTS
	 */

	public static final Font FONT = new Font( "Arial", Font.PLAIN, 10 );

	public static final Font BIG_FONT = new Font( "Arial", Font.PLAIN, 14 );

	public static final Font SMALL_FONT = FONT.deriveFont( 8 );

	public static final Dimension TEXTFIELD_DIMENSION = new Dimension( 40, 18 );

	public static final String NEXT_TEXT = "Next";

	public static final String RESUME_TEXT = "Resume";

	/*
	 * PRIVATE CONSTANTS
	 */

	private static final long serialVersionUID = -4092131926852771798L;

	/*
	 * DEFAULT VISIBILITY FIELDS
	 */

	/** This {@link ActionEvent} is fired when the 'next' button is pressed. */
	final ActionEvent NEXT_BUTTON_PRESSED = new ActionEvent( this, 0, "NextButtonPressed" );

	/**
	 * This {@link ActionEvent} is fired when the 'previous' button is pressed.
	 */
	final ActionEvent PREVIOUS_BUTTON_PRESSED = new ActionEvent( this, 1, "PreviousButtonPressed" );

	/** This {@link ActionEvent} is fired when the 'load' button is pressed. */
	final ActionEvent LOAD_BUTTON_PRESSED = new ActionEvent( this, 2, "LoadButtonPressed" );

	/** This {@link ActionEvent} is fired when the 'save' button is pressed. */
	final ActionEvent SAVE_BUTTON_PRESSED = new ActionEvent( this, 3, "SaveButtonPressed" );

	/** This {@link ActionEvent} is fired when the 'log' button is pressed. */
	final ActionEvent LOG_BUTTON_PRESSED = new ActionEvent( this, 4, "LogButtonPressed" );

	/**
	 * This {@link ActionEvent} is fired when the 'display config' button is
	 * pressed.
	 */
	final ActionEvent DISPLAY_CONFIG_BUTTON_PRESSED = new ActionEvent( this, 5, "ConfigDisplayButtonPressed" );

	/*
	 * FIELDS
	 */

	private final ArrayList< ActionListener > listeners = new ArrayList<>();

	private final JPanel jPanelButtons;

	private final JPanel jPanelMain;

	private final LogPanel logPanel;

	private final TrackMateGUIController controller;

	/*
	 * CONSTRUCTOR
	 */

	public TrackMateWizard( final TrackMateGUIController controller )
	{
		this.controller = controller;
		setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		setIconImage( TRACKMATE_ICON.getImage() );
		setTitle( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION );

		jPanelMain = new JPanel();
		getContentPane().add( jPanelMain, BorderLayout.CENTER );
		jPanelMain.setLayout( new BorderLayout() );
		jPanelMain.setPreferredSize( new java.awt.Dimension( 310, 461 ) );

		jPanelButtons = new JPanel();
		jPanelButtons.setLayout( new BoxLayout( jPanelButtons, BoxLayout.X_AXIS ) );
		getContentPane().add( jPanelButtons, BorderLayout.SOUTH );
		jPanelButtons.setLayout( new BoxLayout( jPanelButtons, BoxLayout.LINE_AXIS ) );

		jButtonSave = addButton( "Save", SAVE_ICON, SAVE_BUTTON_PRESSED );
		jPanelButtons.add( Box.createHorizontalGlue() );
		jButtonLog = addButton( null, LOG_ICON, LOG_BUTTON_PRESSED );
		jButtonDisplayConfig = addButton( null, DISPLAY_CONFIG_ICON, DISPLAY_CONFIG_BUTTON_PRESSED );
		jPanelButtons.add( Box.createHorizontalGlue() );
		jButtonPrevious = addButton( null, PREVIOUS_ICON, PREVIOUS_BUTTON_PRESSED );
		jButtonNext = addButton( NEXT_TEXT, NEXT_ICON, NEXT_BUTTON_PRESSED );

		logPanel = new LogPanel();
		pack();
	}

	/*
	 * PUBLIC METHODS
	 */

	/** Exposes the controller managing this GUI. */
	public TrackMateGUIController getController()
	{
		return controller;
	}

	/**
	 * Adds an {@link ActionListener} to the list of listeners of this GUI, that
	 * will be notified when one the of push buttons is pressed.
	 */
	public void addActionListener( final ActionListener listener )
	{
		listeners.add( listener );
	}

	/**
	 * Removes an {@link ActionListener} from the list of listeners of this GUI.
	 *
	 * @return true if the listener was present in the list for this GUI and was
	 *         successfully removed from it.
	 */
	public boolean removeActionListener( final ActionListener listener )
	{
		return listeners.remove( listener );
	}

	/**
	 * Returns a {@link Logger} suitable for use with this view.
	 */
	public Logger getLogger()
	{
		return logPanel.getLogger();
	}

	public LogPanel getLogPanel()
	{
		return logPanel;
	}

	/**
	 * Simply forwards the caught event to listeners of this main frame.
	 */
	@Override
	public void actionPerformed( final ActionEvent event )
	{
		fireAction( event );
	}

	@Override
	public void dispose()
	{
		final ObjectService objectService = TMUtils.getContext().service( ObjectService.class );
		if ( objectService != null )
			objectService.removeObject( controller.getPlugin() );

		super.dispose();
	}

	/**
	 * Sets the current panel to that identified by the WizardPanelDescriptor
	 * passed in.
	 * 
	 * @param descriptor
	 *            the descriptor to show.
	 */
	public void show( final WizardPanelDescriptor descriptor )
	{
		SwingUtilities.invokeLater( new Runnable()
		{

			@Override
			public void run()
			{
				// Register component instance with the layout on the fly
				jPanelMain.removeAll();
				jPanelMain.add( descriptor.getComponent(), BorderLayout.CENTER );
				jPanelMain.validate();
				jPanelMain.repaint();
			}
		} );
	}

	public void setNextButtonEnabled( final boolean b )
	{
		controller.guimodel.nextButtonState = b;
		SwingUtilities.invokeLater( () -> {
			jButtonNext.setEnabled( b );
			if ( b )
				jButtonNext.requestFocusInWindow();
			else
				jButtonPrevious.requestFocusInWindow();
		} );
	}

	public void setLogButtonEnabled( final boolean b )
	{
		controller.guimodel.loadButtonState = b;
		SwingUtilities.invokeLater( () -> jButtonLog.setEnabled( b ) );
	}

	public void setDisplayConfigButtonEnabled( final boolean b )
	{
		controller.guimodel.displayConfigButtonState = b;
		SwingUtilities.invokeLater( () -> jButtonDisplayConfig.setEnabled( b ) );
	}

	public void setPreviousButtonEnabled( final boolean b )
	{
		controller.guimodel.previousButtonState = b;
		SwingUtilities.invokeLater( () -> jButtonPrevious.setEnabled( b ) );
	}

	public void setSaveButtonEnabled( final boolean b )
	{
		controller.guimodel.saveButtonState = b;
		SwingUtilities.invokeLater( () -> jButtonSave.setEnabled( b ) );
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Forwards the given {@link ActionEvent} to the listeners of this GUI.
	 */
	private void fireAction( final ActionEvent event )
	{
		synchronized ( event )
		{
			for ( final ActionListener listener : listeners )
				listener.actionPerformed( event );
		}
	}

	private JButton addButton( final String label, final Icon icon, final ActionEvent action )
	{
		final JButton button = new JButton();
		jPanelButtons.add( button );
		button.setText( label );
		button.setIcon( icon );
		button.setFont( FONT );
		button.addActionListener( e -> fireAction( action ) );
		return button;
	}

	public JButton getNextButton()
	{
		return jButtonNext;
	}
}
