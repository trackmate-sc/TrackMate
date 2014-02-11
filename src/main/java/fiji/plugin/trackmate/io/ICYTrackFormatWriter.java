package fiji.plugin.trackmate.io;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;

import org.jdom2.Element;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition;

public class ICYTrackFormatWriter implements Algorithm, Benchmark
{

	private static final String BASE_ERROR_MSG = "[ICYTrackFormatWriter] ";

	private static final String ROOT_ELEMENT = "root";

	private static final String TRACK_FILE = "trackfile";

	private static final String TRACK_GROUP = "trackgroup";

	private final File file;

	private final Model model;

	private long processingTime;

	private String errorMessage;

	public ICYTrackFormatWriter( final File file, final Model model )
	{
		this.file = file;
		this.model = model;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public boolean checkInput()
	{
		if ( !file.canWrite() )
		{
			errorMessage = BASE_ERROR_MSG + "Cannot write to " + file + ".\n";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		// ICY does not accept middle links.
		final ConvexBranchesDecomposition splitter = new ConvexBranchesDecomposition( model, true );
		if ( !splitter.checkInput() || !splitter.process() )
		{
			errorMessage = splitter.getErrorMessage();
			return false;
		}

		final Element root = new Element( ROOT_ELEMENT );

		final Element trackFile = new Element( TRACK_FILE );
		trackFile.setAttribute( "version", "1" );
		root.addContent( trackFile );

		/*
		 * Track group.
		 */

		final Element trackGroup = new Element( TRACK_GROUP );
		trackGroup.setAttribute( "description", TrackMate.PLUGIN_NAME_STR + "_v" + TrackMate.PLUGIN_NAME_VERSION + "_export" );

		final Map< Spot, Integer > beginnings = new HashMap< Spot, Integer >();
		final Map< Spot, Integer > endings = new HashMap< Spot, Integer >();

		final Collection< List< Spot >> branches = splitter.getBranches();

		/*
		 * Link list
		 */

		final Collection< List< Spot >> links = splitter.getLinks();


		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

}
