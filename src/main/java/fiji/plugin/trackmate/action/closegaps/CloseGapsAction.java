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
package fiji.plugin.trackmate.action.closegaps;

import java.awt.Frame;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class CloseGapsAction extends AbstractTMAction
{

	public static final String NAME = "Close gaps by introducing new spots";

	public static final String KEY = "CLOSE_GAPS";

	public static final String INFO_TEXT = "<html>"
			+ "This action proposes several methods to close gaps in tracks."
			+ "<p>"
			+ "Gaps are part of tracks where spots are missing in one or "
			+ "several consecutive frames. The listed methods can "
			+ "introduce new spots in such gaps, depending on possibly "
			+ "the other spots in tracks and/or the image data."
			+ "<p>"
			+ "They are useful to fix missed detection when a uninterrupted "
			+ "list of position is required for track analysis. For instance "
			+ "in FRAP experiments, where you need to measure signal intensity "
			+ "changing during time, even if the spot is not visible."
			+ "</html>";

	public static final ImageIcon ICON = Icons.ORANGE_ASTERISK_ICON;

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		final CloseGapsController controller = new CloseGapsController( trackmate, logger );
		controller.show();
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new CloseGapsAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}
}
