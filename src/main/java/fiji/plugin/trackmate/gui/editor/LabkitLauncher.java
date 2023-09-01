package fiji.plugin.trackmate.gui.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.Context;
import org.scijava.ui.behaviour.util.AbstractNamedAction;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.MaskUtils;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.test.ImgLib2Assert;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import sc.fiji.labkit.ui.LabkitFrame;
import sc.fiji.labkit.ui.inputimage.DatasetInputImage;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.models.DefaultSegmentationModel;
import sc.fiji.labkit.ui.models.ImageLabelingModel;

public class LabkitLauncher
{

	private final double[] calibration;

	private final TrackMate trackmate;

	private final EverythingDisablerAndReenabler disabler;

	private Labeling previousLabels;

	private int currentTimePoint;

	private final boolean is2D;

	private final double dt;

	public LabkitLauncher( final TrackMate trackmate, final EverythingDisablerAndReenabler disabler )
	{
		this.trackmate = trackmate;
		this.disabler = disabler;
		final ImagePlus imp = trackmate.getSettings().imp;
		this.calibration = TMUtils.getSpatialCalibration( imp );
		this.is2D = DetectionUtils.is2D( imp );
		this.dt = imp.getCalibration().frameInterval;

	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	protected void launch()
	{
		final Context context = TMUtils.getContext();
		final ImagePlus imp = trackmate.getSettings().imp;
		final ImgPlus src = TMUtils.rawWraps( imp );
		final int timeAxis = src.dimensionIndex( Axes.TIME );

		// Reslice for current time-point.
		this.currentTimePoint = imp.getFrame() - 1;
		final ImgPlus frame = ImgPlusViews.hyperSlice( src, timeAxis, currentTimePoint );
		final DatasetInputImage input = new DatasetInputImage( frame );

		// Prepare label image.
		final AxisType[] axes = new AxisType[] { Axes.X, Axes.Y, Axes.Z, };
		final long[] dims = new long[ is2D ? 2 : 3 ];
		for ( int d = 0; d < dims.length; d++ )
			dims[ d ] = src.dimension( src.dimensionIndex( axes[ d ] ) );
		final Img< UnsignedShortType > lblImg = ArrayImgs.unsignedShorts( dims );
		final double[] calibration = TMUtils.getSpatialCalibration( imp );
		final ImgPlus< UnsignedShortType > lblImgPlus = new ImgPlus<>( lblImg, "LblImg", axes, calibration );

		// Write spots in it.
		final Iterable< Spot > spotsThisFrame = trackmate.getModel().getSpots().iterable( currentTimePoint, true );
		for ( final Spot spot : spotsThisFrame )
			spot.iterable( lblImgPlus ).forEach( p -> p.set( spot.ID() + 1 ) );

		// Labeling model.
		final DefaultSegmentationModel model = new DefaultSegmentationModel( context, input );
		model.imageLabelingModel().labeling().set( Labeling.fromImg( lblImgPlus ) );

		// Store a copy.
		final ImgPlus< UnsignedShortType > copy = lblImgPlus.copy();
		this.previousLabels = Labeling.fromImg( copy );

		// Show LabKit.
		final LabkitFrame labkit = LabkitFrame.show( model, "Edit TrackMate data frame " + ( currentTimePoint + 1 ) );
		labkit.onCloseListeners().addWeakListener( () -> reimportData( model.imageLabelingModel(), currentTimePoint ) );
	}

	private void reimportData( final ImageLabelingModel lm, final int currentTimePoint )
	{
		final Model model = trackmate.getModel();
		final SpotCollection spots = model.getSpots();
		final Labeling labeling = lm.labeling().get();
		final List< Label > labels = labeling.getLabels();
		model.beginUpdate();
		try
		{
			for ( final Label label : labels )
			{

				try
				{
					final int id = Integer.parseInt( label.name() ) - 1;
					final Spot spot = spots.search( id );
					if ( spot == null )
						addNewSpot( label, labeling );
					else
						modifySpot( label, labeling, spot );

				}
				catch ( final NumberFormatException nfe )
				{
					addNewSpot( label, labeling );
				}
			}
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			model.endUpdate();
			disabler.reenable();
		}
	}

	private void modifySpot( final Label label, final Labeling labeling, final Spot spot )
	{
		final Label previousLabel = previousLabels.getLabel( label.name() );
		final RandomAccessibleInterval< BitType > previousRegion = previousLabels.getRegion( previousLabel );
		final RandomAccessibleInterval< BitType > region = labeling.getRegion( label );
		if ( !hasChanged( region, previousRegion ) )
			return;

		final boolean simplify = true;
		final int numThreads = 1;
		final RandomAccessibleInterval< UnsignedByteType > qualityImage = null;
		final List< Spot > spots = MaskUtils.fromMaskWithROI( region, region, calibration, simplify, numThreads, qualityImage );

		// Time position.
		for ( final Spot s : spots )
			s.putFeature( Spot.POSITION_T, currentTimePoint * dt );

		// If there is no spot, it's because we removed it.
		final Model model = trackmate.getModel();
		if ( spots.isEmpty() )
		{
			model.removeSpot( spot );
			return;
		}
		// Hopefully there is only one, if not we pick the closest one.
		Spot closest = null;
		double minD2 = Double.POSITIVE_INFINITY;
		for ( final Spot s : spots )
		{
			final double d2 = s.squareDistanceTo( spot );
			if ( d2 < minD2 )
			{
				minD2 = d2;
				closest = s;
			}
		}
		closest.setName( spot.getName() );
		model.addSpotTo( closest, Integer.valueOf( currentTimePoint ) );
		final Set< DefaultWeightedEdge > edges = model.getTrackModel().edgesOf( spot );
		for ( final DefaultWeightedEdge e : edges )
		{
			final double weight = model.getTrackModel().getEdgeWeight( e );
			final Spot source = model.getTrackModel().getEdgeSource( e );
			final Spot target = model.getTrackModel().getEdgeTarget( e );
			if ( source == spot )
				model.addEdge( closest, target, weight );
			else if ( target == spot )
				model.addEdge( source, closest, weight );
			else
				throw new IllegalArgumentException( "The edge of a spot does not have the spot as source or target?!?" );
		}
		model.removeSpot( spot );

		// If not, add the other ones as new ones.
		for ( int i = 1; i < spots.size(); i++ )
		{
			final Spot s = spots.get( i );
			s.setName( spot.getName() + "_" + i );
			model.addSpotTo( s, Integer.valueOf( currentTimePoint ) );
		}
	}

	private static final boolean hasChanged( final RandomAccessibleInterval< BitType > region, final RandomAccessibleInterval< BitType > previousRegion )
	{
		try
		{
			ImgLib2Assert.assertImageEquals( region, previousRegion );
		}
		catch ( final AssertionError e )
		{
			return true;
		}
		return false;
	}


	private void addNewSpot( final Label label, final Labeling labeling )
	{
		final boolean simplify = true;
		final int numThreads = 1;
		final RandomAccessibleInterval< UnsignedByteType > qualityImage = null;

		// Slice by time.
		final RandomAccessibleInterval< BitType > region = labeling.getRegion( label );
		final List< Spot > spots = MaskUtils.fromMaskWithROI( region, region, calibration, simplify, numThreads, qualityImage );
		for ( final Spot spot : spots )
		{
			spot.putFeature( Spot.POSITION_T, currentTimePoint * dt );
			spot.setName( label.name() );
			trackmate.getModel().addSpotTo( spot, Integer.valueOf( currentTimePoint ) );
		}

	}

	public static final AbstractNamedAction getLaunchAction( final TrackMate trackmate )
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
						final JRootPane parent = SwingUtilities.getRootPane( ( Component ) ae.getSource() );
						final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( parent, new Class[] { JLabel.class } );
						disabler.disable();
						try
						{
							final LabkitLauncher launcher = new LabkitLauncher( trackmate, disabler );
							launcher.launch();
						}
						catch ( final Exception e )
						{
							e.printStackTrace();
						}
					};
				}.start();
			}
		};
	}
}
