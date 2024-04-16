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
package fiji.plugin.trackmate.action.fit;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.AbstractTMAction;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class SpotGaussianFittingAction extends AbstractTMAction
{

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final java.awt.Frame parent )
	{
		final SpotFitterController controller = new SpotFitterController( trackmate, selectionModel, logger );
		controller.show();
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		public static final String NAME = "Refine spot position with gaussian fitting";

		public static final String KEY = "GAUSS_FIT";

		public static final String INFO_TEXT = "<html>" +
				"This action launches a GUI for the sub-localization of spots using gaussian peak fitting. "
				+ "<p>"
				+ "The fit process will update the spot position and their radius, using the "
				+ "results from the gaussian fit. Of course it works best when the peaks in the image "
				+ "ressemble gaussian functions. The fitting process uses the spots information (position "
				+ "and radius) as initial values for the fit."
				+ "<p>"
				+ "It works for both 2D and 3D images. "
				+ "In 3D it accounts for non-isotropic calibration (and possible PSF "
				+ "deformation in the Z direction) thanks to an elliptic gaussian function, "
				+ "with axes constrained to be along X, Y and Z. "
				+ "In 2D we use an isotropic gaussian."
				+ "</html>";

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
			return new SpotGaussianFittingAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return Icons.SPOT_ICON_16x16;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}

}
