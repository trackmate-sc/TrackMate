package fiji.plugin.trackmate.gui;

import fiji.plugin.trackmate.gui.descriptors.WizardPanelDescriptor;

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
	public WizardPanelDescriptor init();

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
}
