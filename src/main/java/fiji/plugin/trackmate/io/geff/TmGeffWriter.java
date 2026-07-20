package fiji.plugin.trackmate.io.geff;

import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.mastodon.geff.GeffAxis;
import org.mastodon.geff.GeffEdge;
import org.mastodon.geff.GeffMetadata;
import org.mastodon.geff.GeffNode;
import org.mastodon.geff.GeffNode.Builder;
import org.mastodon.geff.PropMetadata;

import com.google.gson.JsonElement;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Spot.SpotVisitor;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class TmGeffWriter
{

	private static final String GEFF_VERSION = "1.0.0";

	private final String zarrPath;

	private final GeffMetadata metadata;

	private final List< GeffNode > geffNodes = new ArrayList<>();

	private final List< GeffEdge > geffEdges = new ArrayList<>();

	private final Map< String, Object > trackmateInfo;

	public TmGeffWriter( final String zarrPath )
	{
		this.zarrPath = zarrPath;
		this.metadata = new GeffMetadata( GEFF_VERSION, true );
		this.trackmateInfo = new HashMap<>();
		final Map< String, Object > extra = Map.of( "trackmate", trackmateInfo );
		metadata.setExtra( extra );
	}

	public void appendModel( final Model model )
	{
		final String spaceUnits = ZarrUnits.normalizeSpaceUnit( model.getSpaceUnits() );
		final String timeUnits = ZarrUnits.normalizeTimeUnit( model.getTimeUnits() );
		final boolean is2D = DetectionUtils.is2D( model );

		/*
		 * Serialize spots
		 */
		final SpotCollection spots = model.getSpots();
		final Map< String, Boolean > isInt = model.getFeatureModel().getSpotFeatureIsInt();
		final GeffSpotVisitor visitor = new GeffSpotVisitor( isInt, is2D );
		spots.iterable( true ).forEach( spot -> spot.accept( visitor ) );

		/*
		 * Serialize edges
		 */
		final TrackModel trackModel = model.getTrackModel();
		final Set< DefaultWeightedEdge > edges = trackModel.edgeSet();
		final FeatureModel fm = model.getFeatureModel();


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
				if ( edgeFeature.equals( EdgeTargetAnalyzer.SPOT_SOURCE_ID ) || edgeFeature.equals( EdgeTargetAnalyzer.SPOT_TARGET_ID ) )
					continue;
				if ( is2D && edgeFeature.equals( EdgeTimeLocationAnalyzer.Z_LOCATION ) )
					continue;

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
		 * Metadata
		 */

		// Axes
		final List< GeffAxis > axes = buildAxes( spaceUnits, timeUnits, spots, is2D );
		metadata.setGeffAxes( axes );

		// Spot features
		final Map< String, PropMetadata > nodePropsMetadata = new HashMap<>();
		for ( final String spotFeature : fm.getSpotFeatures() )
		{
			if ( is2D && spotFeature.equals( Spot.POSITION_Z ) )
				continue;

			final String dType = isInt.get( spotFeature ) ? "int32" : "float64";
			final Dimension dimension = fm.getSpotFeatureDimensions().get( spotFeature );
			final String unit = dimension.units( spaceUnits, timeUnits );
			final String name = fm.getSpotFeatureNames().get( spotFeature );
			final PropMetadata propMetadata = new PropMetadata( spotFeature, dType, false, unit, name, null );
			nodePropsMetadata.put( spotFeature, propMetadata );
		}
		metadata.setNodePropsMetadata( nodePropsMetadata );

		// Edge features
		final Map< String, PropMetadata > edgePropsMetadata = new HashMap<>();
		for ( final String edgeFeature : fm.getEdgeFeatures() )
		{
			if ( edgeFeature.equals( EdgeTargetAnalyzer.SPOT_SOURCE_ID ) || edgeFeature.equals( EdgeTargetAnalyzer.SPOT_TARGET_ID ) )
				continue;
			if ( is2D && edgeFeature.equals( EdgeTimeLocationAnalyzer.Z_LOCATION ) )
				continue;

			final String dType = fm.getEdgeFeatureIsInt().get( edgeFeature ) ? "int32" : "float64";
			final Dimension dimension = fm.getEdgeFeatureDimensions().get( edgeFeature );
			final String unit = dimension.units( spaceUnits, timeUnits );
			final String name = fm.getEdgeFeatureNames().get( edgeFeature );
			final PropMetadata propMetadata = new PropMetadata( edgeFeature, dType, false, unit, name, null );
			edgePropsMetadata.put( edgeFeature, propMetadata );
		}
		metadata.setEdgePropsMetadata( edgePropsMetadata );
	}

	public void appendLog( final String log )
	{
		trackmateInfo.put( "log", log );
	}

	public void appendDisplaySettings( final DisplaySettings ds )
	{
		final JsonElement json = DisplaySettingsIO.toJsonTree( ds );
		trackmateInfo.put( "displaySettings", json );
	}

	public void appendGUIState( final String currentPanelIdentifier )
	{
		trackmateInfo.put( GUI_STATE_ELEMENT_KEY, currentPanelIdentifier );
	}

	public void write() throws IOException
	{
		GeffNode.writeToZarr( geffNodes, zarrPath, metadata );
		GeffEdge.writeToZarr( geffEdges, zarrPath, metadata );
		GeffMetadata.writeToZarr( metadata, zarrPath );
	}

	private static final List< GeffAxis > buildAxes( final String spaceUnit, final String timeUnit, final SpotCollection spots, final boolean is2d )
	{
		final String su = ZarrUnits.normalizeSpaceUnit( spaceUnit );
		final String tu = ZarrUnits.normalizeTimeUnit( timeUnit );

		final List< GeffAxis > axes = new ArrayList<>();

		final double[] minMaxT = featureMinMax( spots.iterable( true ), Spot.FRAME );
		axes.add( GeffAxis.createTimeAxis( Spot.FRAME, tu, minMaxT[ 0 ], minMaxT[ 1 ] ) );

		final double[] minMaxX = posMinMax( spots.iterable( true ), 0 );
		axes.add( GeffAxis.createSpaceAxis( Spot.POSITION_X, su, minMaxX[ 0 ], minMaxX[ 1 ] ) );

		final double[] minMaxY = posMinMax( spots.iterable( true ), 1 );
		axes.add( GeffAxis.createSpaceAxis( Spot.POSITION_Y, su, minMaxY[ 0 ], minMaxY[ 1 ] ) );

		if ( !is2d )
		{
			final double[] minMaxZ = posMinMax( spots.iterable( true ), 2 );
			axes.add( GeffAxis.createSpaceAxis( Spot.POSITION_Z, su, minMaxZ[ 0 ], minMaxZ[ 1 ] ) );
		}

		return axes;
	}

	private static final double[] featureMinMax( final Iterable< Spot > iterable, final String feature )
	{
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for ( final Spot spot : iterable )
		{
			final double val = spot.getFeature( feature );
			if ( val < min )
				min = val;
			if ( val > max )
				max = val;
		}
		return new double[] { min, max };
	}

	private static final double[] posMinMax( final Iterable< Spot > spots, final int d )
	{
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for ( final Spot spot : spots )
		{
			final double left = spot.realMin( d );
			final double right = spot.realMax( d );
			if ( left < min )
				min = left;
			if ( right > max )
				max = right;
		}
		return new double[] { min, max };
	}

	private class GeffSpotVisitor implements SpotVisitor
	{

		/** Features not in the general prop, because they are the core node. */
		private static final Set< String > SKIP_PROPS = Set.of( "FRAME", "POSITION_X", "POSITION_Y", "POSITION_Z", "RADIUS" );

		private int geffId = 0;

		private final TObjectIntMap< Spot > spotToId = new TObjectIntHashMap< Spot >();

		private final Map< String, Boolean > isInt;

		private final boolean is2d;

		public GeffSpotVisitor( final Map< String, Boolean > isInt, final boolean is2d )
		{
			this.isInt = isInt;
			this.is2d = is2d;
		}

		private void serializeFeatures( final Spot spot, final GeffNode node )
		{
			final Map< String, Double > features = spot.getFeatures();
			for ( final Map.Entry< String, Double > entry : features.entrySet() )
			{
				final String name = entry.getKey();
				if ( SKIP_PROPS.contains( name ) )
					continue;

				final Object val = ( isInt.get( name ) ? entry.getValue().intValue() : entry.getValue() );
				node.setProp( name, val );
			}
		}

		@Override
		public void visit( final SpotBase spot )
		{
			final Builder builder = new GeffNode.Builder()
					.id( geffId )
					.timepoint( spot.getFeature( Spot.FRAME ).intValue() )
					.x( spot.getDoublePosition( 0 ) )
					.y( spot.getDoublePosition( 1 ) )
					.radius( spot.getFeature( Spot.RADIUS ).doubleValue() );
			if ( !is2d )
				builder.z( spot.getDoublePosition( 2 ) );

			final GeffNode node = builder.build();
			serializeFeatures( spot, node );
			geffNodes.add( node );
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
					.y( spot.getDoublePosition( 1 ) ) // No Z <- 2D
					.radius( spot.getFeature( Spot.RADIUS ).doubleValue() )
					.polygonX( polygonX )
					.polygonY( polygonY )
					.build();
			serializeFeatures( spot, node );
			geffNodes.add( node );
			spotToId.put( spot, geffId++ );
		}
	}

}
