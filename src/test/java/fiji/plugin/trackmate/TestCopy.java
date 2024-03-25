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
package fiji.plugin.trackmate;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.gui.wizard.TrackMateWizardSequence;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;
import ij.ImagePlus;

public class TestCopy
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		ImageJ.main( args );

		final String path = "samples/FakeTracks.xml";
		final TmXmlReader reader = new TmXmlReader( new File( path ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();
		final Model copy = model.copy();

		final ImagePlus imp = reader.readImage();
		imp.show();
		final Settings settings = reader.readSettings( imp );

		final SelectionModel selectionModel = new SelectionModel( copy );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		final HyperStackDisplayer displayer = new HyperStackDisplayer( copy, selectionModel, imp, ds );
		displayer.render();

		final TrackMateWizardSequence sequence = new TrackMateWizardSequence( new TrackMate( copy, settings ), selectionModel, ds );
		sequence.setCurrent( "ConfigureViews" );
		final JFrame frame = sequence.run( "Copy model" );
		frame.setLocationRelativeTo( imp.getWindow() );
		frame.setVisible( true );
	}
}
