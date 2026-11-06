package fiji.plugin.trackmate.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.mastodon.geff.GeffAxis;
import org.mastodon.geff.GeffEdge;
import org.mastodon.geff.GeffMetadata;
import org.mastodon.geff.GeffNode;
import org.mastodon.geff.PropMetadata;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Spot.SpotVisitor;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.TrackModel;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class TmGeffWriter
{

	public static void write( final Model model, final String zarrPath ) throws IOException
	{

		/*
		 * Serialize spots
		 */
		final SpotCollection spots = model.getSpots();
		final Map< String, Boolean > isInt = model.getFeatureModel().getSpotFeatureIsInt();
		final GeffSpotVisitor visitor = new GeffSpotVisitor( isInt );
		spots.iterable( true ).forEach( spot -> spot.accept( visitor ) );

		/*
		 * Serialize edges
		 */
		final TrackModel trackModel = model.getTrackModel();
		final Set< DefaultWeightedEdge > edges = trackModel.edgeSet();
		final FeatureModel fm = model.getFeatureModel();
		
		final List< GeffEdge > geffEdges = new ArrayList<>();
		int edgeId = 0;
		for ( final DefaultWeightedEdge edge : edges )
		{
			final Spot source = trackModel.getEdgeSource( edge );
			final Integer srcId = visitor.spotToId.get( source );
			final Spot target = trackModel.getEdgeTarget( edge );
			final Integer tgtId = visitor.spotToId.get( target );
			if ( srcId == null || tgtId == null )
				continue;

			final boolean swap = source.getFeature( Spot.FRAME ) > target.getFeature( Spot.FRAME );
			final GeffEdge edgeNode = new GeffEdge.Builder()
					.setId( edgeId++ )
					.setSourceNodeId( swap ? tgtId : srcId )
					.setTargetNodeId( swap ? srcId : tgtId )
					.setScore( trackModel.getEdgeWeight( edge ) )
					.setDistance( Math.sqrt( source.squareDistanceTo( target ) ) )
					.build();

			// Feature
			for ( final String edgeFeature : fm.getEdgeFeatures() )
			{
				final Double ef = fm.getEdgeFeature( edge, edgeFeature );
				if ( ef == null )
					continue;
				final Object val = ( fm.getEdgeFeatureIsInt().get( edgeFeature )
						? ef.intValue()
						: ef.doubleValue() );
				edgeNode.setProp( edgeFeature, val );
			}
			geffEdges.add( edgeNode );
		}

		/*
		 * Serialize tracks
		 */
		// TODO -> nodes with a specific path.

		/*
		 * Metadata
		 */

		// Axes
		final List< GeffAxis > axes = buildAxes( model.getTimeUnits(), model.getSpaceUnits() );
		final GeffMetadata metadata = new GeffMetadata( "1.0.0", true, axes );

		// Spot features
		final Map< String, PropMetadata > nodePropsMetadata = new HashMap<>();
		for ( final String spotFeature : fm.getSpotFeatures() )
		{
			final String dType = isInt.get( spotFeature ) ? "int32" : "float64";
			final Dimension dimension = fm.getSpotFeatureDimensions().get( spotFeature );
			final String unit = dimension.units( model.getSpaceUnits(), model.getTimeUnits() );
			final String name = fm.getSpotFeatureNames().get( spotFeature );
			// Description stores short names.
			final String description = fm.getSpotFeatureShortNames().get( spotFeature );
			final PropMetadata propMetadata = new PropMetadata( spotFeature, dType, false, unit, name, description );
			nodePropsMetadata.put( spotFeature, propMetadata );
		}
		metadata.setNodePropsMetadata( nodePropsMetadata );

		// Edge features
		final Map< String, PropMetadata > edgePropsMetadata = new HashMap<>();
		for ( final String edgeFeature : fm.getEdgeFeatures() )
		{
			final String dType = fm.getEdgeFeatureIsInt().get( edgeFeature ) ? "int32" : "float64";
			final Dimension dimension = fm.getEdgeFeatureDimensions().get( edgeFeature );
			final String unit = dimension.units( model.getSpaceUnits(), model.getTimeUnits() );
			final String name = fm.getEdgeFeatureNames().get( edgeFeature );
			final String description = fm.getEdgeFeatureShortNames().get( edgeFeature );
			final PropMetadata propMetadata = new PropMetadata( edgeFeature, dType, false, unit, name, description );
			edgePropsMetadata.put( edgeFeature, propMetadata );
		}
		metadata.setEdgePropsMetadata( edgePropsMetadata );

		/*
		 * Write to disk
		 */

		GeffNode.writeToZarr( visitor.nodes, zarrPath, metadata );
		GeffEdge.writeToZarr( geffEdges, zarrPath, metadata );
		GeffMetadata.writeToZarr( metadata, zarrPath );
	}

	private static List< GeffAxis > buildAxes( final String timeUnit, final String spaceUnit )
	{
		return Arrays.asList(
				GeffAxis.createTimeAxis( GeffAxis.NAME_TIME, timeUnit, null, null ),
				GeffAxis.createSpaceAxis( GeffAxis.NAME_SPACE_X, normalizeSpaceUnit( spaceUnit ), null, null ),
				GeffAxis.createSpaceAxis( GeffAxis.NAME_SPACE_Y, normalizeSpaceUnit( spaceUnit ), null, null ),
				GeffAxis.createSpaceAxis( GeffAxis.NAME_SPACE_Z, normalizeSpaceUnit( spaceUnit ), null, null ) );
	}

	/**
	 * Maps common unit abbreviations to OME-Zarr compliant names. See
	 * https://ngff.openmicroscopy.org/latest/#axes-md for the valid set.
	 */
	static String normalizeSpaceUnit( final String unit )
	{
		if ( unit == null || unit.isEmpty() )
			return "pixel";
		switch ( unit.trim() )
		{
		case "um":
		case "µm":
		case "μm":
		case "micron":
		case "microns":
			return "micrometer";
		case "nm":
			return "nanometer";
		case "mm":
			return "millimeter";
		case "cm":
			return "centimeter";
		case "m":
			return "meter";
		case "km":
			return "kilometer";
		case "pm":
			return "picometer";
		case "Å":
			return "angstrom";
		default:
			return unit;
		}
	}

	public static class GeffSpotVisitor implements SpotVisitor
	{

		/** Features not in the general prop, because they are the core node. */
		private static final Set< String > SLOP_PROPS = Set.of( "FRAME", "POSITION_X", "POSITION_Y", "POSITION_Z", "RADIUS" );

		private int geffId = 0;

		private final TObjectIntMap< Spot > spotToId = new TObjectIntHashMap< Spot >();

		private final List< GeffNode > nodes = new ArrayList<>();

		private final Map< String, Boolean > isInt;

		public GeffSpotVisitor( final Map< String, Boolean > isInt )
		{
			this.isInt = isInt;
		}

		private void serializeFeatures( final Spot spot, final GeffNode node )
		{
			final Map< String, Double > features = spot.getFeatures();
			for ( final Map.Entry< String, Double > entry : features.entrySet() )
			{
				final String name = entry.getKey();
				if ( SLOP_PROPS.contains( name ) )
					continue;

				final Object val = ( isInt.get( name ) ? entry.getValue().intValue() : entry.getValue() );
				node.setProp( name, val );
			}
		}

		@Override
		public void visit( final SpotBase spot )
		{
			final GeffNode node = new GeffNode.Builder()
					.id( geffId )
					.timepoint( spot.getFeature( Spot.FRAME ).intValue() )
					.x( spot.getDoublePosition( 0 ) )
					.y( spot.getDoublePosition( 1 ) )
					.z( spot.getDoublePosition( 2 ) )
					.radius( spot.getFeature( Spot.RADIUS ).doubleValue() )
					.build();
			serializeFeatures( spot, node );
			nodes.add( node );
			spotToId.put( spot, geffId++ );
		}

		@Override
		public void visit( final SpotRoi spot )
		{
			final int nPoints = spot.nPoints();
			final double[] polygonX = new double[ nPoints ];
			final double[] polygonY = new double[ nPoints ];
			for ( int i = 0; i < nPoints; i++ )
			{
				polygonX[ i ] = spot.x( i );
				polygonY[ i ] = spot.y( i );
			}

			final GeffNode node = new GeffNode.Builder()
					.id( geffId )
					.timepoint( spot.getFeature( Spot.FRAME ).intValue() )
					.x( spot.getDoublePosition( 0 ) )
					.y( spot.getDoublePosition( 1 ) )
					.z( spot.getDoublePosition( 2 ) )
					.radius( spot.getFeature( Spot.RADIUS ).doubleValue() )
					.polygonX( polygonX )
					.polygonY( polygonY )
					.build();
			serializeFeatures( spot, node );
			nodes.add( node );
			spotToId.put( spot, geffId++ );
		}
	}
}
