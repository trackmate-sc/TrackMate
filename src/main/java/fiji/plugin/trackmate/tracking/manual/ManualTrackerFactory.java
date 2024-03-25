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
package fiji.plugin.trackmate.tracking.manual;

import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

@Plugin( type = SpotTrackerFactory.class, priority = Priority.HIGH )
public class ManualTrackerFactory implements SpotTrackerFactory
{
	public static final String TRACKER_KEY = "MANUAL_TRACKER";

	public static final String NAME = "Manual tracking";

	public static final String INFO_TEXT = "<html>" + "Choosing this tracker skips the automated tracking step <br>" + "and keeps the current annotation.</html>";

	private String errorMessage;

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getKey()
	{
		return TRACKER_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public SpotTracker create( final SpotCollection spots, final Map< String, Object > settings )
	{
		return null;
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		return null;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		return true;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		return true;
	}

	@Override
	public String toString( final Map< String, Object > sm )
	{
		if ( !checkSettingsValidity( sm ) )
			return errorMessage;
		return "  Manual tracking.\n";
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		return null;
	}

	@Override
	public boolean checkSettingsValidity( final Map< String, Object > settings )
	{
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public ManualTrackerFactory copy()
	{
		return new ManualTrackerFactory();
	}
}
