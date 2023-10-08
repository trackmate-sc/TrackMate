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
package fiji.plugin.trackmate.action;

import java.awt.Frame;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

/**
 * This interface describe a track mate action, that can be run on a
 * {@link TrackMate} object to change its content or properties.
 *
 * @author Jean-Yves Tinevez, 2011-2013 revised in 2021
 */
public interface TrackMateAction
{

	/**
	 * Executes this action within an application specified by the parameters.
	 *
	 * @param trackmate
	 *            the {@link TrackMate} instance to use to execute the action.
	 * @param selectionModel
	 *            the {@link SelectionModel} currently used in the application,
	 * @param displaySettings
	 *            the {@link DisplaySettings} used to render the views in the
	 *            application.
	 * @param parent
	 *            the user-interface parent window.
	 */
	public void execute( TrackMate trackmate, SelectionModel selectionModel, DisplaySettings displaySettings, Frame parent );

	/**
	 * Sets the logger that will receive logs when this action is executed.
	 * 
	 * @param logger
	 *            the logger.
	 */
	public void setLogger( Logger logger );
}
