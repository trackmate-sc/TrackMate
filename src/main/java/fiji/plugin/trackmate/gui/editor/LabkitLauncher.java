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
package fiji.plugin.trackmate.gui.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.scijava.Context;
import org.scijava.ui.behaviour.util.AbstractNamedAction;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.LabkitFrame;
import sc.fiji.labkit.ui.inputimage.DatasetInputImage;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.models.DefaultSegmentationModel;

public class LabkitLauncher< T extends IntegerType< T > & NativeType< T > >
{

	private final double[] calibration;

	private final TrackMate trackmate;

	private final EverythingDisablerAndReenabler disabler;

	private int currentTimePoint;

	private final DisplaySettings ds;

	private final boolean is3D;

	public LabkitLauncher( final TrackMate trackmate, final DisplaySettings ds, final EverythingDisablerAndReenabler disabler )
	{
		this.trackmate = trackmate;
		this.ds = ds;
		this.disabler = disabler;
		final ImagePlus imp = trackmate.getSettings().imp;
		this.calibration = TMUtils.getSpatialCalibration( imp );
		this.is3D = !DetectionUtils.is2D( imp );
	}

	/**
	 * Launches the Labkit editor.
	 *
	 * @param singleTimePoint
	 *            if <code>true</code>, will launch the editor using only the
	 *            time-point currently displayed in the main view. Otherwise,
	 *            will edit all time-points.
	 */
	protected void launch( final boolean singleTimePoint )
	{
		final ImagePlus imp = trackmate.getSettings().imp;
		final DatasetInputImage input = makeInput( imp, singleTimePoint );
		final Labeling labeling = makeLabeling( imp, trackmate.getModel().getSpots(), singleTimePoint );

		// Make a labeling model from it.
		final Context context = TMUtils.getContext();
		final DefaultSegmentationModel model = new DefaultSegmentationModel( context, input );
		model.imageLabelingModel().labeling().set( labeling );

		// Store a copy.
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< T > previousIndexImg = copy( ( RandomAccessibleInterval< T > ) labeling.getIndexImg() );

		// Show LabKit.
		String title = "Editing TrackMate data for " + imp.getShortTitle();
		if ( singleTimePoint )
			title += "at frame " + ( currentTimePoint + 1 );
		final LabkitFrame labkit = LabkitFrame.show( model, title );

		// Prepare re-importer.
		final double dt = imp.getCalibration().frameInterval;
		labkit.onCloseListeners().addListener( () -> {
			@SuppressWarnings( "unchecked" )
			final RandomAccessibleInterval< T > indexImg = ( RandomAccessibleInterval< T > ) model.imageLabelingModel().labeling().get().getIndexImg();
			reimport( indexImg, previousIndexImg, dt );
		} );
	}

	private void reimport(
			final RandomAccessibleInterval< T > indexImg,
			final RandomAccessibleInterval< T > previousIndexImg,
			final double dt )
	{
		try
		{
			// Do we have something to reimport?
			final AtomicBoolean modified = new AtomicBoolean( false );
			LoopBuilder.setImages( previousIndexImg, indexImg )
					.multiThreaded()
					.forEachChunk( chunk -> {
						if ( modified.get() )
							return null;
						chunk.forEachPixel( ( p1, p2 ) -> {
							if ( p1.getInteger() != p2.getInteger() )
							{
								modified.set( true );
								return;
							}
						} );
						return null;
					} );
			if ( !modified.get() )
				return;

			// Message the user.
			final double smoothingScale = -1; // TODO
			final long nTimepoints = indexImg.dimension( 3 );
			final String msg = ( nTimepoints <= 1 )
					? "Commit the changes made to the\n"
							+ "segmentation in the image?"
					: ( currentTimePoint < 0 )
							? "Commit the changes made to the\n"
									+ "segmentation in whole movie?"
							: "Commit the changes made to the\n"
									+ "segmentation in frame " + ( currentTimePoint + 1 ) + "?";
			final String title = "Commit edits to TrackMate";
			final int returnedValue = JOptionPane.showConfirmDialog(
					null,
					msg,
					title,
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					Icons.TRACKMATE_ICON );
			if ( returnedValue != JOptionPane.YES_OPTION )
				return;

			final LabkitImporter< T > reimporter = new LabkitImporter<>( trackmate.getModel(), calibration, dt );
			if ( currentTimePoint < 0 && nTimepoints > 1 )
			{
				// All time-points.
				final Logger log = Logger.IJ_LOGGER;
				log.setStatus( "Re-importing from Labkit..." );
				for ( int t = 0; t < nTimepoints; t++ )
				{
					final RandomAccessibleInterval< T > novelIndexImgThisFrame = Views.hyperSlice( indexImg, 3, t );
					final RandomAccessibleInterval< T > previousIndexImgThisFrame = Views.hyperSlice( previousIndexImg, 3, t );
					reimporter.reimport( novelIndexImgThisFrame, previousIndexImgThisFrame, t, smoothingScale );
					log.setProgress( ++t / ( double ) nTimepoints );
				}
				log.setStatus( "" );
				log.setProgress( 0. );
			}
			else
			{
				// Only one.
				final int localT = Math.max( 0, currentTimePoint );
				reimporter.reimport( indexImg, previousIndexImg, localT, smoothingScale );
			}
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			disabler.reenable();
		}
	}

	private Img< T > copy( final RandomAccessibleInterval< T > in )
	{
		final ImgFactory< T > factory = Util.getArrayOrCellImgFactory( in, Util.getTypeFromInterval( in ) );
		final Img< T > out = factory.create( in );
		LoopBuilder.setImages( in, out )
				.multiThreaded()
				.forEachPixel( ( i, o ) -> o.setInteger( i.getInteger() ) );
		return out;
	}

	/**
	 * Creates a new {@link DatasetInputImage} from the specified
	 * {@link ImagePlus}. The embedded label image is empty.
	 *
	 * @param imp
	 *            the input {@link ImagePlus}.
	 * @param singleTimePoint
	 *            if <code>true</code>, then the dataset will be created only
	 *            for the time-point currently displayed in the
	 *            {@link ImagePlus}. This time-point is then stored in the
	 *            {@link #currentTimePoint} field. If <code>false</code>, the
	 *            dataset is created for the whole movie and the
	 *            {@link #currentTimePoint} takes the value -1.
	 * @return a new {@link DatasetInputImage}.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	private final DatasetInputImage makeInput( final ImagePlus imp, final boolean singleTimePoint )
	{
		final ImgPlus src = TMUtils.rawWraps( imp );
		// Possibly reslice for current time-point.
		final ImpBdvShowable showable;
		final ImgPlus inputImg;
		final int timeAxis = src.dimensionIndex( Axes.TIME );
		if ( singleTimePoint && timeAxis >= 0 )
		{
			this.currentTimePoint = imp.getFrame() - 1;
			inputImg = ImgPlusViews.hyperSlice( src, timeAxis, currentTimePoint );
			showable = ImpBdvShowable.fromImp( inputImg, imp );
		}
		else
		{
			this.currentTimePoint = -1;
			showable = ImpBdvShowable.fromImp( imp );
			inputImg = src;
		}
		return new DatasetInputImage( inputImg, showable );
	}

	/**
	 * Prepare the label image for annotation. The labeling is created and each
	 * of its labels receive the name and the color from the spot it is created
	 * from.
	 *
	 * @param imp
	 *            the source image plus.
	 * @param spots
	 *            the spot collection.
	 * @param singleTimePoint
	 *            if <code>true</code> we only annotate one time-point.
	 * @return a new {@link Labeling}.
	 */
	private Labeling makeLabeling( final ImagePlus imp, final SpotCollection spots, final boolean singleTimePoint )
	{
		// Axes.
		final AxisType[] axes = ( is3D )
				? ( singleTimePoint )
						? new AxisType[] { Axes.X, Axes.Y, Axes.Z }
						: new AxisType[] { Axes.X, Axes.Y, Axes.Z, Axes.TIME }
				: ( singleTimePoint )
						? new AxisType[] { Axes.X, Axes.Y }
						: new AxisType[] { Axes.X, Axes.Y, Axes.TIME };

		// N dimensions.
		final int nDims = is3D
				? singleTimePoint ? 3 : 4
				: singleTimePoint ? 2 : 3;

		// Dimensions.
		final long[] dims = new long[ nDims ];
		int dim = 0;
		dims[ dim++ ] = imp.getWidth();
		dims[ dim++ ] = imp.getHeight();
		if ( is3D )
			dims[ dim++ ] = imp.getNSlices();
		if ( !singleTimePoint )
			dims[ dim++ ] = imp.getNFrames();

		// Raw image.
		final Img< UnsignedIntType > lblImg = ArrayImgs.unsignedInts( dims );

		// Calibration.
		final double[] c = TMUtils.getSpatialCalibration( imp );
		final double[] calibration = new double[ nDims ];
		dim = 0;
		calibration[ dim++ ] = c[ 0 ];
		calibration[ dim++ ] = c[ 1 ];
		if ( is3D )
			calibration[ dim++ ] = c[ 2 ];
		if ( !singleTimePoint )
			calibration[ dim++ ] = 1.;

		// Label image holder.
		final ImgPlus< UnsignedIntType > lblImgPlus = new ImgPlus<>( lblImg, "LblImg", axes, calibration );

		// Write spots in it with index = id + 1 and build a map index -> spot.
		final Map< Integer, Spot > spotIDs = new HashMap<>();
		if ( singleTimePoint )
		{
			processFrame( lblImgPlus, spots, currentTimePoint, spotIDs );
		}
		else
		{
			final int timeDim = lblImgPlus.dimensionIndex( Axes.TIME );
			for ( int t = 0; t < imp.getNFrames(); t++ )
			{
				final ImgPlus< UnsignedIntType > lblImgPlusThisFrame = ImgPlusViews.hyperSlice( lblImgPlus, timeDim, t );
				processFrame( lblImgPlusThisFrame, spots, t, spotIDs );
			}
		}
		final Labeling labeling = Labeling.fromImg( lblImgPlus );

		// Fine tune labels name and color.
		final FeatureColorGenerator< Spot > colorGen = FeatureUtils.createSpotColorGenerator( trackmate.getModel(), ds );
		for ( final Label label : labeling.getLabels() )
		{
			final String name = label.name();
			final int index = Integer.parseInt( name );
			final Spot spot = spotIDs.get( index );
			if ( spot == null )
			{
				System.out.println( "Spot is null for index " + index + "!!" ); // DEBUG
				continue;
			}
			label.setName( spot.getName() );
			label.setColor( new ARGBType( colorGen.color( spot ).getRGB() ) );
		}

		return labeling;
	}

	private static final void processFrame( final ImgPlus< UnsignedIntType > lblImgPlus, final SpotCollection spots, final int t, final Map< Integer, Spot > spotIDs )
	{
		final Iterable< Spot > spotsThisFrame = spots.iterable( t, true );
		for ( final Spot spot : spotsThisFrame )
		{
			final int index = spot.ID() + 1;
			spot.iterable( lblImgPlus ).forEach( p -> p.set( index ) );
			spotIDs.put( index, spot );
		}
	}

	public static final AbstractNamedAction getLaunchAction( final TrackMate trackmate, final DisplaySettings ds )
	{
		return new AbstractNamedAction( "launch labkit editor" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent ae )
			{
				new Thread( "TrackMate editor thread" )
				{
					@Override
					public void run()
					{
						// Is shift pressed?
						final int mod = ae.getModifiers();
						final boolean shiftPressed = ( mod & ActionEvent.SHIFT_MASK ) > 0;
						final boolean singleTimepoint = !shiftPressed;

						final JRootPane parent = SwingUtilities.getRootPane( ( Component ) ae.getSource() );
						final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( parent, new Class[] { JLabel.class } );
						disabler.disable();
						try
						{
							@SuppressWarnings( "rawtypes" )
							final LabkitLauncher launcher = new LabkitLauncher( trackmate, ds, disabler );
							launcher.launch( singleTimepoint );
						}
						catch ( final Exception e )
						{
							disabler.reenable();
							e.printStackTrace();
						}
					};
				}.start();
			}
		};
	}
}
