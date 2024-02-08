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
package fiji.plugin.trackmate.action.meshtools;

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

public class MeshSmootherAction extends AbstractTMAction
{

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		final MeshSmootherController controller = new MeshSmootherController( trackmate.getModel(), selectionModel, logger );
		controller.setNumThreads( trackmate.getNumThreads() );
		controller.show( parent );
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		public static final String NAME = "Smooth 3D meshes";

		public static final String KEY = "MESH_SMOOTHER";

		public static final String INFO_TEXT = "<html>"
				+ "Displays a tool to smooth the 3D mesh present in "
				+ "the data, using the Taubin smoothing algorithm.</html>";

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
			return new MeshSmootherAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return Icons.VECTOR_ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}

}
