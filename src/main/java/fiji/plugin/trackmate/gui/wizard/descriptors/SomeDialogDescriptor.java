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

import java.io.File;

import fiji.plugin.trackmate.gui.components.LogPanel;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;

/**
 * An abstract class made for describing panels that generate a dialog, like
 * save and load panels.
 *
 * @author Jean-Yves Tinevez
 *
 */
public abstract class SomeDialogDescriptor extends WizardPanelDescriptor
{

	/**
	 * File that governs saving and loading. We make it a static field so that
	 * loading and sharing events always point to a single file location by
	 * default.
	 */
	public static File file;

	public SomeDialogDescriptor( final String panelIdentifier, final LogPanel logPanel )
	{
		super( panelIdentifier );
		this.targetPanel = logPanel;
	}
}
