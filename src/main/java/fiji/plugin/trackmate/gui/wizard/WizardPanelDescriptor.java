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
package fiji.plugin.trackmate.gui.wizard;

import java.awt.Component;

import org.scijava.Cancelable;

public class WizardPanelDescriptor
{

	protected Component targetPanel;

	protected final String panelIdentifier;

	public WizardPanelDescriptor( final String panelIdentifier )
	{
		this.panelIdentifier = panelIdentifier;
	}

	public final Component getPanelComponent()
	{
		return targetPanel;
	}

	public final String getPanelDescriptorIdentifier()
	{
		return panelIdentifier;
	}

	public void aboutToHidePanel()
	{}

	public void aboutToDisplayPanel()
	{}

	public void displayingPanel()
	{}

	public Runnable getForwardRunnable()
	{
		return null;
	}

	public Runnable getBackwardRunnable()
	{
		return null;
	}

	public Cancelable getCancelable()
	{
		return null;
	}
}
