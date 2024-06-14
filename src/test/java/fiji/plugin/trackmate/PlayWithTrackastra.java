package fiji.plugin.trackmate;

import java.io.FileReader;
import java.util.Iterator;

import com.opencsv.CSVReader;

import fiji.plugin.trackmate.detection.LabelImageDetectorFactory;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.gui.wizard.TrackMateWizardSequence;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import gnu.trove.map.hash.TIntObjectHashMap;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class PlayWithTrackastra
{
	public static void main( final String[] args )
	{
		ImageJ.main( args );

		/*
		 * Source image (stack of masks).
		 */

		final String input = "/Users/tinevez/Development/PythonWS/trackastra/trackastra/tracked/input_masks/input_masks.tif";
		final ImagePlus imp = IJ.openImage( input );
		imp.show();
		IJ.run( "Grays" );

		/*
		 * Import it as a mask in TrackMate.
		 */

		final Model model = new Model();
		final Settings settings = new Settings( imp );
		settings.detectorFactory = new LabelImageDetectorFactory<>();
		settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
		settings.addAllAnalyzers();

		final TrackMate trackmate = new TrackMate( model, settings );
		trackmate.execDetection();
		trackmate.execInitialSpotFiltering();
		trackmate.computeSpotFeatures( false );
		trackmate.execSpotFiltering( false );

		/*
		 * Import Trackastra results.
		 */

		// Map of frame -> label -> spot.
		final TIntObjectHashMap< TIntObjectHashMap< Spot > > idMap = new TIntObjectHashMap<>();
		for ( final Spot spot : model.getSpots().iterable( false ) )
		{
			final int label = spot.getFeature( "MEDIAN_INTENSITY_CH1" ).intValue();
			final int frame = spot.getFeature( Spot.FRAME ).intValue();
			TIntObjectHashMap< Spot > map = idMap.get( frame );
			if ( map == null )
			{
				map = new TIntObjectHashMap<>();
				idMap.put( frame, map );
			}
			map.put( label, spot );
		}

		model.beginUpdate();
		final String edges = "/Users/tinevez/Development/PythonWS/trackastra/trackastra/tracked/edges.csv";
		try (final CSVReader reader = new CSVReader( new FileReader( edges ) ))
		{		
			final Iterator< String[] > it = reader.readAll().iterator();
			// Skip header
			it.next();
			while ( it.hasNext() )
			{
				final String[] strs = it.next();
				final int sourceFrame = Integer.parseInt( strs[ 0 ] );
				final int sourceLabel = Integer.parseInt( strs[ 1 ] );
				final int targetFrame = Integer.parseInt( strs[ 2 ] );
				final int targetLabel = Integer.parseInt( strs[ 3 ] );
				final double weight = Double.parseDouble( strs[ 4 ] );

				final TIntObjectHashMap< Spot > mapSource = idMap.get( sourceFrame );
				if ( mapSource == null )
				{
					System.out.println( " - no spot in frame " + sourceFrame + ". Skipping." );
					continue;
				}
				final Spot source = mapSource.get( sourceLabel );
				if ( source == null )
				{
					System.out.println( " - no spot matching source label " + sourceLabel + ". Skipping." );
					continue;
				}

				final TIntObjectHashMap< Spot > mapTarget = idMap.get( targetFrame );
				if ( mapTarget == null )
				{
					System.out.println( " - no spot in frame " + targetFrame + ". Skipping." );
					continue;
				}
				final Spot target = mapTarget.get( targetLabel );
				if ( target == null )
				{
					System.out.println( " - no spot matching target label " + targetLabel + ". Skipping." );
					continue;
				}

				model.addEdge( source, target, weight );
			}
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			model.endUpdate();
		}

		trackmate.computeEdgeFeatures( false );
		trackmate.computeTrackFeatures( false );
		trackmate.execTrackFiltering( false );

		/*
		 * Visualize tracks.
		 */

		final String imgs = "/Users/tinevez/Development/PythonWS/trackastra/trackastra/tracked/input_imgs/input_imgs.tif";
		final ImagePlus impImg = IJ.openImage( imgs );

		final SelectionModel sm = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		ds.setSpotColorBy( TrackMateObject.TRACKS, "TRACK_INDEX" );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, sm, impImg, ds );
		view.render();

		final TrackMateWizardSequence seq = new TrackMateWizardSequence( trackmate, sm, ds );
		seq.setCurrent( "ConfigureViews" );
		seq.run( "Trackastra results" ).setVisible( true );
	}
}
