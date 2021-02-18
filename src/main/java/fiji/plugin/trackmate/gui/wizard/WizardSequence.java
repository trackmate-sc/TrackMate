package fiji.plugin.trackmate.gui.wizard;

/**
 * Interface for classes that allows specifying what descriptors are traversed
 * in the Wizard.
 *
 * @author Jean-Yves Tinevez
 */
public interface WizardSequence
{

	/**
	 * Performs initialization of the sequence and returns the first descriptor
	 * to display in the wizard. Calling this method again reinitializes the
	 * sequence.
	 *
	 * @return the first descriptor to show.
	 */
	public WizardPanelDescriptor2 init();

	/**
	 * Returns the descriptor currently displayed.
	 *
	 * @return the descriptor currently displayed
	 */
	public WizardPanelDescriptor2 current();

	/**
	 * Returns the next descriptor to display. Returns <code>null</code> if the
	 * sequence is finished and does not have a next descriptor.
	 *
	 * @return the next descriptor to display.
	 */
	public WizardPanelDescriptor2 next();

	/**
	 * Returns the previous descriptor to display. Returns <code>null</code> if
	 * the sequence is starting and does not have a previous descriptor.
	 *
	 * @return the previous descriptor to display.
	 */
	public WizardPanelDescriptor2 previous();

	/**
	 * Returns the descriptor in charge of logging events. It can be accessed
	 * out of the normal sequence by a special button in the wizard.
	 * 
	 * @return the descriptor in charge of logging events.
	 */
	public WizardPanelDescriptor2 logDescriptor();

	/**
	 * Returns the descriptor in charge of configure the views. It can be
	 * accessed out of the normal sequence by a special button in the wizard.
	 * 
	 * @return the descriptor in charge of configuring the views.
	 */
	public WizardPanelDescriptor2 configDescriptor();

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
	public WizardPanelDescriptor2 save();
}
