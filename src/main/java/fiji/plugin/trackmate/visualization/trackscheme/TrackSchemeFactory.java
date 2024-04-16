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
package fiji.plugin.trackmate.visualization.trackscheme;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewFactory;

/*
 * We annotate the TrackScheme factory to be NOT visible,
 * because we do not want it to show in the GUI menu.
 */
@Plugin( type = ViewFactory.class, visible = false )
public class TrackSchemeFactory implements ViewFactory
{

	@Override
	public TrackMateModelView create( final Model model, final Settings settings, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		return new TrackScheme( model, selectionModel, displaySettings );
	}

	@Override
	public String getName()
	{
		return "TrackScheme";
	}

	@Override
	public String getKey()
	{
		return TrackScheme.KEY;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getInfoText()
	{
		return "<html>Not redacted!</html>";
	}
}
