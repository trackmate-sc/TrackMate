package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackmateConstants;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzer;
import fiji.plugin.trackmate.gui.panels.components.ColorByFeatureGUIPanel.Category;
import fiji.plugin.trackmate.gui.panels.components.FilterGuiPanel;
import fiji.plugin.trackmate.tracking.spot.DefaultSpotCollection;
import fiji.plugin.trackmate.tracking.spot.SpotCollection;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;
import ij.ImagePlus;
import ij.process.StackConverter;
import ij3d.Image3DUniverse;
import ij3d.Install_J3D;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class SpotDisplayer3DTestDrive
{

	public static void main( final String[] args )
	{

		System.out.println( Install_J3D.getJava3DVersion() );

		final int N_BLOBS = 20;
		final double RADIUS = 5; // µm
		final Random RAN = new Random();
		final double WIDTH = 100; // µm
		final double HEIGHT = 100; // µm
		final double DEPTH = 50; // µm
		final double[] CALIBRATION = new double[] { 0.5, 0.5, 1 };
		final AxisType[] AXES = new AxisType[] { Axes.X, Axes.Y, Axes.Z };

		// Create 3D image
		System.out.println( "Creating image...." );
		final Img< UnsignedByteType > source = new ArrayImgFactory< UnsignedByteType >().create( new int[] { ( int ) ( WIDTH / CALIBRATION[ 0 ] ), ( int ) ( HEIGHT / CALIBRATION[ 1 ] ), ( int ) ( DEPTH / CALIBRATION[ 2 ] ) }, new UnsignedByteType() );
		final ImgPlus< UnsignedByteType > img = new ImgPlus< UnsignedByteType >( source, "test", AXES, CALIBRATION );

		// Random blobs
		final double[] radiuses = new double[ N_BLOBS ];
		final ArrayList< double[] > centers = new ArrayList< double[] >( N_BLOBS );
		final int[] intensities = new int[ N_BLOBS ];
		for ( int i = 0; i < N_BLOBS; i++ )
		{
			radiuses[ i ] = RADIUS + RAN.nextGaussian();
			final double x = WIDTH * RAN.nextDouble();
			final double y = HEIGHT * RAN.nextDouble();
			final double z = DEPTH * RAN.nextDouble();
			centers.add( i, new double[] { x, y, z } );
			intensities[ i ] = RAN.nextInt( 200 );
		}

		// Put the blobs in the image
		for ( int i = 0; i < N_BLOBS; i++ )
		{
			final Spot tmpSpot = new Spot( centers.get( i )[ 0 ], centers.get( i )[ 1 ], centers.get( i )[ 2 ], radiuses[ i ], -1d );
			final SpotNeighborhood< UnsignedByteType > sphere = new SpotNeighborhood< UnsignedByteType >( tmpSpot, img );
			for ( final UnsignedByteType pixel : sphere )
			{
				pixel.set( intensities[ i ] );
			}
		}

		// Start ImageJ
		ij.ImageJ.main( args );

		// Cast the Img the ImagePlus and convert to 8-bit
		final ImagePlus imp = ImageJFunctions.wrap( img, img.toString() );
		if ( imp.getType() != ImagePlus.GRAY8 )
			new StackConverter( imp ).convertToGray8();

		imp.getCalibration().pixelWidth = CALIBRATION[ 0 ];
		imp.getCalibration().pixelHeight = CALIBRATION[ 1 ];
		imp.getCalibration().pixelDepth = CALIBRATION[ 2 ];
		imp.setTitle( "3D blobs" );

		// Create a Spot arrays
		final List< Spot > spots = new ArrayList< Spot >( N_BLOBS );
		Spot spot;
		for ( int i = 0; i < N_BLOBS; i++ )
		{
			spot = new Spot( centers.get( i )[ 0 ], centers.get( i )[ 1 ], centers.get( i )[ 2 ], RADIUS, -1d, "Spot " + i );
			spot.putFeature( TrackmateConstants.POSITION_T, Double.valueOf( 0 ) );
			spots.add( spot );
		}

		System.out.println( "Grabbing features..." );
		final SpotIntensityAnalyzer< UnsignedByteType > analyzer = new SpotIntensityAnalyzer< UnsignedByteType >( img, spots.iterator() );
		analyzer.process();
		for ( final Spot s : spots )
			System.out.println( s );

		// Launch renderer
		final SpotCollection allSpots = new DefaultSpotCollection();
		allSpots.put( 0, spots );
		final TrackMate trackmate = new TrackMate();
		trackmate.getModel().setSpots( allSpots, false );
		trackmate.getSettings().imp = imp;
		final Image3DUniverse universe = new Image3DUniverse();
		universe.show();
		final SpotDisplayer3D displayer = new SpotDisplayer3D( trackmate.getModel(), new SelectionModel( trackmate.getModel() ), universe );
		displayer.render();

		// Launch threshold GUI
		final List< FeatureFilter > ff = new ArrayList< FeatureFilter >();
		final FilterGuiPanel gui = new FilterGuiPanel( trackmate.getModel(), Arrays.asList( new Category[] { Category.SPOTS } ) );
		gui.setFilters( ff );

		// Set listeners
		gui.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				trackmate.getSettings().setSpotFilters( gui.getFeatureFilters() );
				trackmate.execSpotFiltering( false );
			}
		} );
		gui.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				if ( e == gui.COLOR_FEATURE_CHANGED )
				{
					new Thread()
					{
						@Override
						public void run()
						{
							final String feature = gui.getColorFeature();
							@SuppressWarnings( "unchecked" )
							final FeatureColorGenerator< Spot > spotColorGenerator = ( FeatureColorGenerator< Spot > ) displayer.getDisplaySettings( TrackMateModelView.KEY_SPOT_COLORING );
							spotColorGenerator.setFeature( feature );
							displayer.setDisplaySettings( TrackMateModelView.KEY_SPOT_COLORING, spotColorGenerator );
							displayer.setDisplaySettings( TrackMateModelView.KEY_SPOT_RADIUS_RATIO, RAN.nextFloat() + 1 );
							displayer.refresh();
						};
					}.start();
				}
			}
		} );

		// Display GUI
		final JFrame frame = new JFrame();
		frame.getContentPane().add( gui );
		frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );

		// Add a panel
		gui.addFilterPanel( TrackmateConstants.POSITION_Z );

	}

}
