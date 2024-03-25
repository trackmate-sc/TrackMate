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

import static fiji.plugin.trackmate.gui.Icons.CALCULATOR_ICON;

import java.awt.Frame;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.table.TrackTableView;

public class ExportStatsTablesAction extends AbstractTMAction
{

	public static final String NAME = "Export statistics to tables";

	public static final String KEY = "EXPORT_STATS";

	public static final String INFO_TEXT = "<html>"
			+ "Compute and export all statistics to 3 tables. "
			+ "Statistics are separated in features computed for: "
			+ "<ol> "
			+ "	<li> spots in visible tracks; "
			+ "	<li> edges between those spots; "
			+ "	<li> visible tracks. "
			+ "</ol> "
			+ "Note that spots and edges that are not in "
			+ "visible tracks won't be displayed in the tables."
			+ "</html>";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		createTrackTables( trackmate.getModel(), selectionModel, displaySettings, TMUtils.getImagePathWithoutExtension( trackmate.getSettings() ) ).render();
	}

	public static TrackTableView createTrackTables( final Model model, final SelectionModel selectionModel, final DisplaySettings displaySettings, final String imageFileName )
	{
		return new TrackTableView( model, selectionModel, displaySettings, imageFileName );
	}

	// Invisible because called on the view config panel.
	@Plugin( type = TrackMateActionFactory.class, visible = false )
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
			return new ExportStatsTablesAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return CALCULATOR_ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}
}
