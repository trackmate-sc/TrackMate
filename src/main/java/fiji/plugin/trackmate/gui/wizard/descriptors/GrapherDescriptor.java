/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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
package fiji.plugin.trackmate.gui.wizard.descriptors;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.components.GrapherPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;

public class GrapherDescriptor extends WizardPanelDescriptor
{

	private static final String KEY = "GraphFeatures";

	public GrapherDescriptor( final TrackMate trackmate, final DisplaySettings displaySettings )
	{
		super( KEY );
		this.targetPanel = new GrapherPanel( trackmate, displaySettings );
	}

	@Override
	public void aboutToDisplayPanel()
	{
		final GrapherPanel panel = ( GrapherPanel ) targetPanel;
		panel.refresh();
	}
}
