/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.io.icy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.graph.ConvexBranchesDecomposition;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;

public class IcyTrackFormatWriter implements Algorithm, Benchmark
{

	private static final String BASE_ERROR_MSG = "[ICYTrackFormatWriter] ";

	private static final String ROOT_ELEMENT = "root";

	private static final String TRACK_FILE = "trackfile";

	private static final String TRACK_GROUP = "trackgroup";

	private static final String TRACK = "track";

	private static final String DETECTION = "detection";

	private static final String LINK_LIST = "linklist";

	private static final String LINK = "link";

	private static final String DESCRIPTION_ATTRIBUTE = "description";

	private final File file;

	private final Model model;

	private long processingTime;

	private String errorMessage;

	private final double[] calibration;

	private final String description;

	public IcyTrackFormatWriter( final File file, final Model model, final double[] calibration )
	{
		this( file, model, calibration, null );
	}

	public IcyTrackFormatWriter( final File file, final Model model, final double[] calibration, final String description )
	{
		this.file = file;
		this.model = model;
		this.calibration = calibration;
		this.description = description;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public boolean checkInput()
	{
		if ( !file.exists() )
		{
			try
			{
				file.createNewFile();
			}
			catch ( final IOException e )
			{
				errorMessage = BASE_ERROR_MSG + e.getMessage();
				return false;
			}
		}

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
		final long start = System.currentTimeMillis();

		// ICY does not accept middle links nor gaps.
		final ConvexBranchesDecomposition splitter = new ConvexBranchesDecomposition( model, true, true );
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
		if ( null != description && !description.isEmpty() )
			trackGroup.setAttribute( DESCRIPTION_ATTRIBUTE, description );

		final Map< Spot, Integer > beginnings = new HashMap<>();
		final Map< Spot, Integer > endings = new HashMap<>();

		final Collection< List< Spot > > branches = splitter.getBranches();
		for ( final List< Spot > branch : branches )
		{
			final int branchID = branch.hashCode();

			// build a map for later
			beginnings.put( branch.get( 0 ), Integer.valueOf( branchID ) );
			endings.put( branch.get( branch.size() - 1 ), Integer.valueOf( branchID ) );

			// Create the track element
			final Element track = new Element( TRACK );
			track.setAttribute( "id", "" + branchID );
			for ( final Spot spot : branch )
			{
				final double x = spot.getDoublePosition( 0 ) / calibration[ 0 ];
				final double y = spot.getDoublePosition( 1 ) / calibration[ 1 ];
				final int z = ( int ) ( spot.getDoublePosition( 2 ) / calibration[ 2 ] );
				final int t = spot.getFeature( Spot.FRAME ).intValue();
				final Element detection = new Element( DETECTION );
				detection.setAttribute( "t", Integer.toString( t ) );
				detection.setAttribute( "x", "" + x );
				detection.setAttribute( "y", "" + y );
				detection.setAttribute( "z", Integer.toString( z ) );
				detection.setAttribute( "classname", "plugins.nchenouard.particleTracking.sequenceGenerator.ProfileSpotTrack" );
				detection.setAttribute( "type", "1" );
				track.addContent( detection );
			}
			trackGroup.addContent( track );
		}
		root.addContent( trackGroup );

		/*
		 * Link list
		 */

		final Element linklist = new Element( LINK_LIST );
		final Collection< List< Spot > > links = splitter.getLinks();
		for ( final List< Spot > link : links )
		{
			final Spot spotA = link.get( 0 );
			final Spot spotB = link.get( 1 );

			final int from = endings.get( spotA ).intValue();
			final int to = beginnings.get( spotB ).intValue();

			final Element linkEl = new Element( LINK );
			linkEl.setAttribute( "from", "" + from );
			linkEl.setAttribute( "to", "" + to );

			linklist.addContent( linkEl );
		}
		root.addContent( linklist );

		/*
		 * Write doc to file
		 */

		final Document document = new Document( root );
		final XMLOutputter outputter = new XMLOutputter( Format.getPrettyFormat() );
		try
		{
			outputter.output( document, new FileOutputStream( file ) );
		}
		catch ( final FileNotFoundException e )
		{
			errorMessage = e.getMessage();
			return false;
		}
		catch ( final IOException e )
		{
			errorMessage = e.getMessage();
			return false;
		}

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}
}
