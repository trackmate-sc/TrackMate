/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate;

import java.util.EventListener;

/**
 * An interface for listeners that will be notified when a {@link Model} is been
 * changed.
 * 
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; May 30, 2011
 *
 */
public interface ModelChangeListener extends EventListener
{

	/**
	 * This notification is fired when a {@link Model} has been changed.
	 * 
	 * @param event
	 *            the {@link ModelChangeEvent}.
	 */
	public void modelChanged( final ModelChangeEvent event );

}
