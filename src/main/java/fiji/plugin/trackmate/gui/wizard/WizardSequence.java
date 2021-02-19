package fiji.plugin.trackmate.gui.wizard;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

/**
 * Interface for classes that allows specifying what descriptors are traversed
 * in the Wizard.
 *
 * @author Jean-Yves Tinevez
 */
public interface WizardSequence
{

	/**
	 * Launches the wizard to play this sequence.
	 * 
	 * @return the {@link JFrame} in which the wizard is displayed.
	 */
	public default JFrame run( final String title )
	{
		final WizardController controller = new WizardController( this );
		final JFrame frame = new JFrame();
		frame.getContentPane().add( controller.getWizardPanel() );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.setSize( 350, 520 );
		frame.setTitle( title );
		controller.init();
		frame.addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosing( final WindowEvent e )
			{
				onClose();
			};
		} );
		return frame;
	}

	/**
	 * Method called when the wizard is closed.
	 */
	public default void onClose()
	{}

	/**
	 * Returns the descriptor currently displayed.
	 *
	 * @return the descriptor currently displayed
	 */
	public WizardPanelDescriptor current();

	/**
	 * Returns the next descriptor to display. Returns <code>null</code> if the
	 * sequence is finished and does not have a next descriptor.
	 *
	 * @return the next descriptor to display.
	 */
	public WizardPanelDescriptor next();

	/**
	 * Returns the previous descriptor to display. Returns <code>null</code> if
	 * the sequence is starting and does not have a previous descriptor.
	 *
	 * @return the previous descriptor to display.
	 */
	public WizardPanelDescriptor previous();

	/**
	 * Returns the descriptor in charge of logging events. It can be accessed
	 * out of the normal sequence by a special button in the wizard.
	 * 
	 * @return the descriptor in charge of logging events.
	 */
	public WizardPanelDescriptor logDescriptor();

	/**
	 * Returns the descriptor in charge of configure the views. It can be
	 * accessed out of the normal sequence by a special button in the wizard.
	 * 
	 * @return the descriptor in charge of configuring the views.
	 */
	public WizardPanelDescriptor configDescriptor();

	/**
	 * Returns <code>true</code> if the sequence has an element after the
	 * current one.
	 *
	 * @return <code>true</code> if the sequence has an element after the
	 *         current one.
	 */
	public boolean hasNext();

	/**
	 * Returns <code>true</code> if the sequence has an element before the
	 * current one.
	 *
	 * @return <code>true</code> if the sequence has an element before the
	 *         current one.
	 */
	public boolean hasPrevious();

	/**
	 * Returns the panel in charge of saving the data.
	 * 
	 * @return the panel in charge of saving the data.
	 */
	public WizardPanelDescriptor save();

	/**
	 * Position the sequence so that its current descriptor is the one with the
	 * specified identifier. If the identifier is unknown to the sequence, do
	 * nothing.
	 * 
	 * @param panelIdentifier
	 *            the descriptor identifier.
	 */
	public void setCurrent( String panelIdentifier );
}
