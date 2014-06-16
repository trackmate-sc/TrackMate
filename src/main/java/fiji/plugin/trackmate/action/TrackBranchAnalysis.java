package fiji.plugin.trackmate.action;

import ij.IJ;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition;
import fiji.plugin.trackmate.graph.TimeDirectedNeighborIndex;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.io.TmXmlReader;

public class TrackBranchAnalysis extends AbstractTMAction
{

	private static final String INFO_TEXT = "<html>This action splits each track in branches and run analysis on each branch.</html>";

	private static final String KEY = "TRACK_BRANCH_ANALYSIS";

	private static final String NAME = "Branches analysis";

	private static final ImageIcon ICON = null;


	@Override
	public void execute( final TrackMate trackmate )
	{
		logger.log( "Generating track branches analysis.\n" );
		final Model model = trackmate.getModel();
		final int ntracks = model.getTrackModel().nTracks( true );
		if ( ntracks == 0 )
		{
			logger.log( "No visible track found. Aborting.\n" );
			return;
		}

		final ConvexBranchesDecomposition splitter = new ConvexBranchesDecomposition( model, true, false );
		if ( !splitter.checkInput() || !splitter.process() )
		{
			logger.error( splitter.getErrorMessage() );
			return;
		}

		final TimeDirectedNeighborIndex neighborIndex = model.getTrackModel().getDirectedNeighborIndex();

		final Collection< List< Spot >> branches = splitter.getBranches();
		logger.log( "Found " + branches.size() + " branches in " + model.getTrackModel().nTracks( true ) + " tracks.\n" );

		final Set< Spot > visited = new HashSet< Spot >();

		for ( final List< Spot > branch : branches )
		{
			if ( branch.size() < 2 )
			{
				logger.log( "Branch " + branch + " is too small. Skipping.\n" );
				continue;
			}

			final Spot first = branch.get( 0 );
			final Spot last = branch.get( branch.size() - 1 );
			String branchName = "" + first.getName() + "-" + last.getName();

			if ( neighborIndex.predecessorsOf( first ).size() < 1 )
			{
				logger.log( "Branch " + branchName + " starting point is not defined. Skipping.\n" );
				continue;
			}

			final Set< Spot > nextOnes = neighborIndex.successorsOf( last );
			final int nSucc = nextOnes.size();
			if ( nSucc < 1 )
			{
				logger.log( "Branch " + branchName + " end point is not defined. Skipping.\n" );
				continue;
			}


			final Spot nextOne = nextOnes.iterator().next();
			if ( visited.contains( nextOne ) )
			{
				continue;
			}
			visited.add( nextOne );
			if ( nSucc == 1 )
			{
				// It is a fusion - change the branch name
				final Spot previous = neighborIndex.predecessorsOf( first ).iterator().next();
				branchName = "" + previous.getName() + "-" + nextOne.getName();
			}

			final int nPred = neighborIndex.predecessorsOf( nextOne ).size();
			final DivisionType type = DivisionType.getType( nSucc, nPred );
			final int dt = ( int ) last.diffTo( first, Spot.FRAME );
			logger.log( "Branch " + branchName + ", " + type.getName() + ", dt = " + dt + " frames.\n" );
		}
		logger.log( "Done.\n" );

	}

	/*
	 * STATIC CLASSES AND ENUMS
	 */

	public static enum DivisionType
	{

		BIPOLAR_DIVISION( "Bipolar division" ), TRIPOLAR_DIVISION( "Tripolar division" ), TETRAPOLAR_DIVISION( "Tetrapolar division" ), FUSION( "Fusion" ), OTHER( "Other cases" );

		private final String name;

		DivisionType(final String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}

		public static DivisionType getType( final int nSucc, final int nPred )
		{
			if ( nPred < 2 )
			{
				switch ( nSucc )
				{
				case 2:
					return BIPOLAR_DIVISION;
				case 3:
					return TRIPOLAR_DIVISION;
				case 4:
					return TETRAPOLAR_DIVISION;
				default:
					return OTHER;
				}
			}
			else
			{
				if ( nSucc == 1 )
				{
					return FUSION;
				}
				else
				{
					return OTHER;
				}
			}
		}


	}

	@Plugin( type = TrackMateActionFactory.class, enabled = true )
	public static class Factory implements TrackMateActionFactory
	{



		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new TrackBranchAnalysis();
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		final File file = new File( "/Users/tinevez/Desktop/Video_C03-1-fullHD.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		if ( !reader.isReadingOk() )
		{
			IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, reader.getErrorMessage() );
			return;
		}
		final Model model = reader.getModel();
		if ( !reader.isReadingOk() )
		{
			IJ.error( "Problem reading the model:\n" + reader.getErrorMessage() );
		}

		final TrackMate trackmate = new TrackMate( model, new Settings() );

		final TrackBranchAnalysis analyzer = new TrackBranchAnalysis();
		analyzer.setLogger( Logger.IJ_LOGGER );
		analyzer.execute( trackmate );
	}

}
