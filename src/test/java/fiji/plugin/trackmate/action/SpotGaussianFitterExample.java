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
package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.gui.wizard.TrackMateWizardSequence;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.ImageJ;
import ij.ImagePlus;

public class SpotGaussianFitterExample
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		ImageJ.main( args );
		final TmXmlReader reader = new TmXmlReader( new File( "samples/testimage_gauss_0.0015_poisson_true_0.10-frame=12.xml" ) );
		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		imp.show();
		final Settings settings = reader.readSettings( imp );
		final TrackMate trackmate = new TrackMate( model, settings );
		trackmate.setNumThreads( 1 );

		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		final TrackMateWizardSequence sequence = new TrackMateWizardSequence( trackmate, selectionModel, ds );
		sequence.setCurrent( "Actions" );
		final JFrame frame = sequence.run( "Test Gauss-fitting action" );

		frame.setIconImage( TRACKMATE_ICON.getImage() );
		GuiUtils.positionWindow( frame, settings.imp.getWindow() );
		frame.setVisible( true );

//		final SpotGaussianFitter fitter = new SpotGaussianFitter( model, settings, Logger.IJ_LOGGER );
//		fitter.setNumThreads( trackmate.getNumThreads() );
//		if ( !fitter.checkInput() || !fitter.process() )
//		{
//			System.err.println( fitter.getErrorMessage() );
//			return;
//		}
//
//		System.out.println( "Finished in " + fitter.getProcessingTime() + " ms." );
//
//		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, imp, ds );
//		view.render();

	}
}
