package fiji.plugin.trackmate.tracking.jonkervolgenant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SparseStructure< K, L >
{

	private final List< K > sources;

	private final List< L > targets;

	private final List< Double > costs;

	private final List< K > rows;

	private final List< L > cols;

	public SparseStructure( final List< K > sources, final List< L > targets, final List< Double > costs )
	{
		this.sources = sources;
		this.targets = targets;
		this.costs = costs;

		// Extract unique objects in the rows
		final ArrayList< K > tmpRows = new ArrayList< K >();
		final ArrayList< L > tmpCols = new ArrayList< L >();
		 int row = -1;
		 int col = -1;
		for ( int l = 0; l < sources.size(); l++ )
		{
			final K source = sources.get( l );
			if ( !tmpRows.contains( source ) )
			{
				tmpRows.add( source );
				row++;
			}

			final L target = targets.get( l );
			if ( !tmpCols.contains( target ) )
			{
				tmpCols.add( target );
				col++;
			}
		}

		this.rows = Collections.unmodifiableList( tmpRows );
		this.cols = Collections.unmodifiableList( tmpCols );

		/*
		 * 
		 */
	}

}
