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
package fiji.plugin.trackmate.action.autonaming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;

public class DefaultAutoNamingRule implements AutoNamingRule
{

	private final String suffixSeparator;

	private final String branchSeparator;

	private final boolean incrementSuffix;

	private final Pattern branchPattern;

	public DefaultAutoNamingRule()
	{
		this( ".", "", true );
	}

	public DefaultAutoNamingRule( final String suffixSeparator, final String branchSeparator, final boolean incrementSuffix )
	{
		this.suffixSeparator = suffixSeparator;
		this.branchSeparator = branchSeparator;
		this.incrementSuffix = incrementSuffix;
		this.branchPattern = Pattern.compile( "^([a-z](?:" + Pattern.quote( branchSeparator ) + "[a-z])*)$" );
	}

	@Override
	public void nameRoot( final Spot root, final TrackModel model )
	{
		final Integer id = model.trackIDOf( root );
		final String trackName = model.name( id );

		final String rootName = ( incrementSuffix )
				? trackName + suffixSeparator + "1"
				: trackName;
		root.setName( rootName );
	}

	@Override
	public void nameBranches( final Spot mother, final Collection< Spot > siblings )
	{

		// Sort siblings by their X position.
		final List< Spot > spots = new ArrayList<>( siblings );
		spots.sort( Comparator.comparing( s -> s.getDoublePosition( 0 ) ) );

		// Predecessor name.
		final String motherName = mother.getName();
		final String[] tokens = motherName.split( Pattern.quote( suffixSeparator ) );

		// Find in what token the branch suffix is stored.
		int branchTokenIndex = -1;
		for ( int i = 0; i < tokens.length; i++ )
		{
			final String token = tokens[ i ];
			final Matcher matcher = branchPattern.matcher( token );
			if ( matcher.matches() )
			{
				branchTokenIndex = i;
				break;
			}
		}

		if ( branchTokenIndex < 0 )
		{
			/*
			 * Could not find it. Maybe because the mother comes from the root
			 * branch. In that case we add the suffix separator and a new char
			 * for the branch.
			 */
			if ( !incrementSuffix )
			{
				// There won't be a '.23' at the end.
				char bname = 'a';
				for ( final Spot spot : spots )
				{
					spot.setName( motherName + suffixSeparator + bname );
					bname += 1;
				}
			}
			else
			{
				// There is a '.23' at then end.
				char bname = 'a';
				for ( final Spot spot : spots )
				{
					final String[] newTokens = new String[ tokens.length + 1 ];
					for ( int i = 0; i < tokens.length; i++ )
						newTokens[ i ] = tokens[ i ];
					newTokens[ tokens.length - 1 ] = "" + bname;
					newTokens[ tokens.length ] = "1"; // restart
					final String branchName = String.join( suffixSeparator, newTokens );
					spot.setName( branchName );
					bname += 1;
				}
			}
			return;
		}

		/*
		 * A branch char combination already exists. We add to it.
		 */
		char bname = 'a';
		for ( final Spot spot : spots )
		{
			// Copy.
			final String[] newTokens = new String[ tokens.length ];
			for ( int i = 0; i < tokens.length; i++ )
				newTokens[ i ] = tokens[ i ];
			// Edit.
			if ( !incrementSuffix )
			{
				newTokens[ newTokens.length - 1 ] += branchSeparator + bname;
			}
			else
			{
				newTokens[ newTokens.length - 2 ] += branchSeparator + bname;
				newTokens[ newTokens.length - 1 ] = "1"; // restart
			}
			final String branchName = String.join( suffixSeparator, newTokens );
			spot.setName( branchName );
			bname += 1;
		}
	}

	@Override
	public void nameSpot( final Spot current, final Spot predecessor )
	{
		if ( incrementSuffix )
		{
			final String name = predecessor.getName();
			final String[] tokens = name.split( Pattern.quote( suffixSeparator ) );
			final String idstr = tokens[ tokens.length - 1 ];
			try
			{
				final Integer id = Integer.valueOf( idstr );
				tokens[ tokens.length - 1 ] = Integer.toString( id + 1 );
				final String name2 = String.join( suffixSeparator, tokens );
				current.setName( name2 );

			}
			catch ( final NumberFormatException e )
			{
				AutoNamingRule.super.nameSpot( current, predecessor );
			}
		}
		else
		{
			AutoNamingRule.super.nameSpot( current, predecessor );
		}
	}

	@Override
	public String toString()
	{
		if ( incrementSuffix )
			return "Append 'a', 'b' for each branch and increment spot index";
		else
			return "Append 'a', 'b' for each branch";
	}

	@Override
	public String getInfoText()
	{
		final StringBuilder str = new StringBuilder();
		str.append( "<html>" );
		str.append( "Rename all the spots in a model giving to daughter branches a name derived "
				+ "from the mother branch. The daughter branch names are determined "
				+ "following simple rules based on the X position of spots:" );
		str.append( "<ul>" );
		str.append( "<li>The root (first spot) of a track takes the name of the track it belongs to.</li>" );
		str.append( "<li>The subsequent branches are named from the mother branch they split from. Their name "
				+ "is suffixed by 'a', 'b', ... depending on the relative X position of the sibbling spots "
				+ "just after division.</li>" );
		if ( !branchSeparator.isEmpty() )
			str.append( "<li>Each of the branch character is separated from others by the character '" + branchSeparator + "'</li>" );
		if ( !suffixSeparator.isEmpty() )
			str.append( "<li>The branch suffix ('a' ...) is separated from the root name by the character '" + suffixSeparator + "'</li>" );
		if ( incrementSuffix )
			str.append( "<li>Inside a branch, the individual spots are suffixed by a supplemental index ('1', '2', ...), "
					+ "indicating their order in the branch.</li>" );
		str.append( "</ul>" );
		str.append( "<p>" );

		str.append( "For instance, the 3rd spot of the branch following two divisions, first one emerging "
				+ "from the leftmost sibling and second one emerging from the rightmost sibbling, in  "
				+ "the track named 'Track_23' will be named: <br>" );
		String example = "Track_23" + suffixSeparator + "a" + branchSeparator + "b";
		if ( incrementSuffix )
			example += suffixSeparator + "3";
		str.append( "<div style='text-align: center;'>" + example + "<br>" );

		str.append( "The results are undefined if a track is not a tree "
				+ "(if it has merge points)." );
		str.append( "</html>" );
		return str.toString();
	}
}
