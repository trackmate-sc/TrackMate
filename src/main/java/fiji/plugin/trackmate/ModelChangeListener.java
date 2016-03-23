package fiji.plugin.trackmate;

import java.util.EventListener;

/**
 * An interface for listeners that will be notified when a {@link Model}
 * is been changed.
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; May 30, 2011
 *
 */
public interface ModelChangeListener extends EventListener {

	/**
	 * This notification is fired when a {@link Model} has been changed.
	 * 
	 * @param event
	 *            the {@link ModelChangeEvent}.
	 */
	public void modelChanged(final ModelChangeEvent event);
	
}
