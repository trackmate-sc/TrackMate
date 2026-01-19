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
package fiji.plugin.trackmate.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.opencsv.CSVWriter;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.util.TMUtils;

public class CSVExporter
{

	/**
	 * The symbol that will be appended when a value is missing.
	 */
	public static final String MISSING_STR = "ø";

	/**
	 * Exports the spots in the specified model to a CSV file.
	 * <p>
	 * The table contains the spots, one spot per line. All the declared spot
	 * feature values are exported, one feature per column. Additionally, the
	 * following columns are added:
	 * <ol start="0">
	 * <li>The spot ID.
	 * <li>The spot name.
	 * <li>The ID of the track the spot belongs to, if any.
	 * <li>The name of the track the spot belongs to, if any.
	 * <li>Whether the track the spot belongs to is visible, if any. 1 if
	 * visible, 0 otherwise.
	 * </ol>
	 * 
	 * The table will have a header on 4 lines containing:
	 * <ol start="0">
	 * <li>The feature key.
	 * <li>The feature name.
	 * <li>The feature short name.
	 * <li>The feature unit if any.
	 * </ol>
	 * 
	 * @param csvFile
	 *            the path of the file to export to.
	 * @param model
	 *            the model to export.
	 * @param visibleOnly
	 *            if <code>true</code>, only the spots in visible tracks will be
	 *            exported. Otherwise all spots are exported.
	 * @throws IOException
	 *             if something wrong happens while writing to the CSV file.
	 */
	public static final void exportSpots( final String csvFile, final Model model, final boolean visibleOnly ) throws IOException
	{
		final TrackModel tm = model.getTrackModel();
		final Set< Integer > trackIDs = tm.trackIDs( visibleOnly );
		final List< String > features = new ArrayList<>( model.getFeatureModel().getSpotFeatures() );

		// Function to return a feature value.
		final Map< String, Boolean > isInt = model.getFeatureModel().getSpotFeatureIsInt();
		final BiFunction< Spot, String, Number > featureVal = ( s, f ) -> {
			final Double obj = s.getFeature( f );
			if ( obj == null )
				return null;

			if ( isInt.getOrDefault( f, Boolean.FALSE ) )
				return obj.intValue();
			else
				return obj;
		};

		/*
		 * Extra columns.
		 */

		// Spot name function.
		final Function< Spot, String > labelFun = ( s ) -> s.getName();
		// Spot id function/
		final Function< Spot, String > idFun = ( s ) -> Integer.toString( s.ID() );
		// Track id function.
		final Function< Spot, String > trackIdFun = ( s ) -> {
			final Integer trackID = tm.trackIDOf( s );
			if ( trackID == null )
				return MISSING_STR;
			else
				return Integer.toString( trackID );
		};
		// Track name function.
		final Function< Spot, String > trackNameFun = ( s ) -> {
			final Integer trackID = tm.trackIDOf( s );
			if ( trackID == null )
				return MISSING_STR;
			else
				return tm.name( trackID );
		};
		// Track visibility function.
		final Function< Spot, String > trackVisiblityFun = ( s ) -> {
			final Integer trackID = tm.trackIDOf( s );
			if ( trackID == null || !tm.isVisible( trackID ) )
				return "0";
			else
				return "1";
		};

		final List< Function< Spot, String > > extraFuns = Arrays.asList( idFun, labelFun, trackIdFun, trackNameFun, trackVisiblityFun );
		final String[][] extraHeaders = new String[][] {
				{ "ID", "ID", "ID", "" },
				{ "LABEL", "Label", "Label", "" },
				{ "TRACK_ID", "Track ID", "Track ID", "" },
				{ "TRACK_NAME", "Track name", "Track name", "" },
				{ "TRACK_VISIBLE", "Track visibility", "Track visibility", "" }
		};

		try (CSVWriter writer = new CSVWriter( new FileWriter( new File( csvFile ) ),
				CSVWriter.DEFAULT_SEPARATOR,
				CSVWriter.NO_QUOTE_CHARACTER,
				CSVWriter.DEFAULT_ESCAPE_CHARACTER,
				CSVWriter.DEFAULT_LINE_END ))
		{
			// Header.
			writeHeaderSpots( writer, model, extraHeaders );

			final SpotWriter spotWriter = new SpotWriter( writer, features, featureVal, extraFuns );

			// Spots in tracks.
			for ( final Integer trackID : trackIDs )
			{
				final List< Spot > spots = new ArrayList<>( tm.trackSpots( trackID ) );
				spots.sort( Spot.frameComparator );
				for ( final Spot spot : spots )
					spotWriter.write( spot );
			}

			// Possibly spots not in tracks.
			if ( !visibleOnly )
			{
				for ( final Spot spot : model.getSpots().iterable( false ) )
				{
					if ( tm.trackIDOf( spot ) != null )
						continue; // Already done abobe.

					spotWriter.write( spot );
				}
			}
		}
	}

	private static void writeHeaderSpots( final CSVWriter writer, final Model model, final String[][] extra )
	{
		final List< String > features = new ArrayList<>( model.getFeatureModel().getSpotFeatures() );
		final Map< String, String > featureNames = model.getFeatureModel().getSpotFeatureNames();
		final Map< String, String > featureShortNames = model.getFeatureModel().getSpotFeatureShortNames();
		final Map< String, String > featureUnits = new HashMap<>();
		for ( final String feature : features )
		{
			final Dimension dimension = model.getFeatureModel().getSpotFeatureDimensions().get( feature );
			final String units = TMUtils.getUnitsFor( dimension, model.getSpaceUnits(), model.getTimeUnits() );
			featureUnits.put( feature, units );
		}
		writeHeader( writer, features, featureNames, featureShortNames, featureUnits, extra );
	}

	private static void writeHeader(
			final CSVWriter writer,
			final List< String > features,
			final Map< String, String > featureNames,
			final Map< String, String > featureShortNames,
			final Map< String, String > featureUnits,
			final String[][] extra )
	{

		final int columnShift = extra.length;
		final int nCols = columnShift + features.size();
		final String[] content = new String[ nCols ];

		/*
		 * Determine whether we can skip 2nd or 3d line, if it's identical to
		 * the 2nd one (happens when the names are repeated).
		 */

		boolean skipThirdLine = true;
		boolean skipSecondLine = true;
		for ( int i = columnShift; i < content.length; i++ )
		{
			final String feature = features.get( i - columnShift );
			final String name = featureNames.get( features.get( i - columnShift ) );
			final String shortName = featureShortNames.get( features.get( i - columnShift ) );
			if ( !feature.equals( name ) )
				skipSecondLine = false;
			if ( !name.equals( shortName ) )
				skipThirdLine = false;
		}

		// Header 1st line.
		for ( int i = 0; i < extra.length; i++ )
			content[ i ] = extra[ i ][ 0 ];
		for ( int i = columnShift; i < content.length; i++ )
			content[ i ] = features.get( i - columnShift );
		writer.writeNext( content );

		// Header 2nd line.
		if ( !skipSecondLine )
		{
			for ( int i = 0; i < extra.length; i++ )
				content[ i ] = extra[ i ][ 1 ];
			for ( int i = columnShift; i < content.length; i++ )
				content[ i ] = featureNames.get( features.get( i - columnShift ) );
			writer.writeNext( content );
		}

		// Header 3rd line.
		if ( !skipThirdLine )
		{
			for ( int i = 0; i < extra.length; i++ )
				content[ i ] = extra[ i ][ 2 ];
			for ( int i = columnShift; i < content.length; i++ )
				content[ i ] = featureShortNames.get( features.get( i - columnShift ) );
			writer.writeNext( content );
		}

		// Header 4th line.
		for ( int i = 0; i < extra.length; i++ )
			content[ i ] = extra[ i ][ 3 ];
		for ( int i = columnShift; i < content.length; i++ )
		{
			final String feature = features.get( i - columnShift );
			final String units = featureUnits.get( feature );
			final String unitsStr = ( units == null || units.isEmpty() ) ? "" : "(" + units + ")";
			content[ i ] = unitsStr;
		}
		writer.writeNext( content );
	}

	private static class SpotWriter
	{

		private final CSVWriter writer;

		private final List< String > features;

		private final List< Function< Spot, String > > extraFuns;

		private final String[] content;

		private final BiFunction< Spot, String, Number > featureVal;

		private final int columnShift;

		public SpotWriter( final CSVWriter writer, final List< String > features, final BiFunction< Spot, String, Number > featureVal, final List< Function< Spot, String > > extraFuns )
		{
			this.writer = writer;
			this.features = features;
			this.featureVal = featureVal;
			this.extraFuns = extraFuns;
			this.columnShift = extraFuns.size();
			final int nCols = columnShift + features.size();
			this.content = new String[ nCols ];
		}

		public void write( final Spot spot )
		{
			for ( int i = 0; i < columnShift; i++ )
				content[ i ] = extraFuns.get( i ).apply( spot );
			for ( int i = columnShift; i < content.length; i++ )
			{
				final Object val = featureVal.apply( spot, features.get( i - columnShift ) );
				content[ i ] = ( val == null ) ? MISSING_STR : val.toString();
			}

			writer.writeNext( content );
		}
	}

	public static void main( final String[] args ) throws IOException
	{
		final String filename = "samples/FakeTracks.xml";
		final TmXmlReader reader = new TmXmlReader( new File( filename ) );
		if ( !reader.isReadingOk() )
		{
			System.out.println( reader.getErrorMessage() );
			return;
		}

		final String csvFile = filename.replace( ".xml", "-spots_in_tracks.csv" );
		CSVExporter.exportSpots( csvFile, reader.getModel(), false );

		System.out.println( "Done." );
		System.out.println( "__________________" );
		try (BufferedReader br = new BufferedReader( new FileReader( csvFile ) ))
		{
			String line;
			while ( ( line = br.readLine() ) != null )
				System.out.println( line );
		}
	}
}
