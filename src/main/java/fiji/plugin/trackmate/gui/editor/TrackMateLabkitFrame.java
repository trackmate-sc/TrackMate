/*-
 * #%L
 * The Labkit image segmentation tool for Fiji.
 * %%
 * Copyright (C) 2017 - 2024 Matthias Arzt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package fiji.plugin.trackmate.gui.editor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;

import org.scijava.Context;

import fiji.plugin.trackmate.gui.Icons;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import sc.fiji.labkit.pixel_classification.utils.SingletonContext;
import sc.fiji.labkit.ui.InitialLabeling;
import sc.fiji.labkit.ui.inputimage.DatasetInputImage;
import sc.fiji.labkit.ui.inputimage.InputImage;
import sc.fiji.labkit.ui.models.DefaultSegmentationModel;
import sc.fiji.labkit.ui.models.SegmentationModel;
import sc.fiji.labkit.ui.utils.Notifier;

/**
 * The main Labkit window. (This window allows to segment a single image. It has
 * to be distinguished from the LabkitProjectFrame, which allows to operation on
 * multiple images.) The window only contains a {@link SegmentationComponent}
 * and shows the associated main menu.
 *
 * @author Matthias Arzt
 */
public class TrackMateLabkitFrame
{

	private final JFrame frame = initFrame();

	private final Notifier onCloseListeners = new Notifier();

	public static TrackMateLabkitFrame showForFile( Context context,
			final String filename )
	{
		if ( context == null )
			context = SingletonContext.getInstance();
		final Dataset dataset = openDataset( context, filename );
		return showForImage( context, new DatasetInputImage( dataset ) );
	}

	private static Dataset openDataset( final Context context, final String filename )
	{
		try
		{
			return context.service( DatasetIOService.class ).open( filename );
		}
		catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
	}

	public static TrackMateLabkitFrame showForImage( Context context, final InputImage inputImage )
	{
		if ( context == null )
			context = SingletonContext.getInstance();
		final SegmentationModel model = new DefaultSegmentationModel( context, inputImage );
		model.imageLabelingModel().labeling().set( InitialLabeling.initialLabeling( context, inputImage ) );
		return show( model, inputImage.imageForSegmentation().getName() );
	}

	public static TrackMateLabkitFrame show( final SegmentationModel model, final String title )
	{
		return new TrackMateLabkitFrame( model, title );
	}

	private TrackMateLabkitFrame( final SegmentationModel model, final String title )
	{
		@SuppressWarnings( "unused" )
		final TrackMateLabKitSegmentationComponent segmentationComponent = initSegmentationComponent( model );
		setTitle( title );
		frame.setIconImage( Icons.TRACKMATE_ICON.getImage() );
//		frame.setJMenuBar( new MenuBar( segmentationComponent::createMenu ) );
		frame.setVisible( true );
	}

	private TrackMateLabKitSegmentationComponent initSegmentationComponent( final SegmentationModel segmentationModel )
	{
		final TrackMateLabKitSegmentationComponent segmentationComponent = new TrackMateLabKitSegmentationComponent( frame, segmentationModel, false );
		frame.add( segmentationComponent );
		frame.addWindowListener( new WindowAdapter()
		{

			@Override
			public void windowClosed( final WindowEvent e )
			{
				segmentationComponent.close();
				onCloseListeners.notifyListeners();
			}
		} );
		return segmentationComponent;
	}

	private JFrame initFrame()
	{
		final JFrame frame = new JFrame();
		frame.setBounds( 50, 50, 1200, 900 );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		return frame;
	}

	private void setTitle( final String name )
	{
		if ( name == null || name.isEmpty() )
			frame.setTitle( "Labkit" );
		else
			frame.setTitle( "Labkit - " + name );
	}

	public Notifier onCloseListeners()
	{
		return onCloseListeners;
	}
}
