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

import static fiji.plugin.trackmate.gui.Icons.BRANCH_ICON_16x16;

import java.awt.Frame;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.table.BranchTableView;

public class TrackBranchAnalysis extends AbstractTMAction
{

	private static final String INFO_TEXT = "<html>"
			+ "This action analyzes each branch of all "
			+ "tracks, and outputs in an ImageJ results "
			+ "table the number of its predecessors, of "
			+ "successors, and its duration."
			+ "<p>"
			+ "The results table is in sync with the selection. "
			+ "Clicking on a line will select the target branch."
			+ "</html>";

	private static final String KEY = "TRACK_BRANCH_ANALYSIS";

	private static final String NAME = "Branch hierarchy analysis";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		createBranchTable( trackmate.getModel(), selectionModel, TMUtils.getImagePathWithoutExtension( trackmate.getSettings() ) ).render();
	}

	public static final BranchTableView createBranchTable( final Model model, final SelectionModel selectionModel, final String imageFileName )
	{
		return new BranchTableView( model, selectionModel, imageFileName );
	}

	@Plugin( type = TrackMateActionFactory.class, enabled = true )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new TrackBranchAnalysis();
		}

		@Override
		public ImageIcon getIcon()
		{
			return BRANCH_ICON_16x16;
		}
	}
}
