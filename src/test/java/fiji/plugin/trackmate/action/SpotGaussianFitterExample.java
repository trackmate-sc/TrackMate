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

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.fit.SpotFitterController;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.TrackMateWizardSequence;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.type.numeric.RealType;

public class SpotGaussianFitterExample
{

	public static < T extends RealType< T > > void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		ImageJ.main( args );
		// final TmXmlReader reader = new TmXmlReader( new File(
		// "samples/MAX_1.5x-timelqpe_2021-04-02-1.xml" ) );
		final TmXmlReader reader = new TmXmlReader( new File( "samples/FakeTracks.xml" ) );
		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		imp.show();
		final Settings settings = reader.readSettings( imp );
		final TrackMate trackmate = new TrackMate( model, settings );
		trackmate.setNumThreads( 1 );

		// Main view.
		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings displaySettings = reader.getDisplaySettings();
		final TrackMateModelView displayer = new HyperStackDisplayer( model, selectionModel, settings.imp, displaySettings );
		displayer.render();

		final TrackMateWizardSequence sequence = new TrackMateWizardSequence( trackmate, selectionModel, displaySettings );
		sequence.setCurrent( "ConfigureViews" );
		final JFrame frame = sequence.run( "Test Gauss-fitting action" );
		frame.setIconImage( TRACKMATE_ICON.getImage() );
		GuiUtils.positionWindow( frame, settings.imp.getWindow() );
		frame.setVisible( true );

		// Launch fitting controller.
		final SpotFitterController controller = new SpotFitterController( trackmate, selectionModel, Logger.DEFAULT_LOGGER );
		controller.show();
	}
}
