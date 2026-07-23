package fiji.plugin.trackmate.io.geff;

import static fiji.plugin.trackmate.Spot.FRAME;
import static fiji.plugin.trackmate.Spot.POSITION_X;
import static fiji.plugin.trackmate.Spot.POSITION_Y;
import static fiji.plugin.trackmate.Spot.POSITION_Z;
import static fiji.plugin.trackmate.Spot.RADIUS;
import static fiji.plugin.trackmate.SpotCollection.VISIBILITY;
import static fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer.SPOT_SOURCE_ID;
import static fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer.SPOT_TARGET_ID;
import static fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer.Z_LOCATION;
import static fiji.plugin.trackmate.features.track.TrackDurationAnalyzer.TRACK_START;
import static fiji.plugin.trackmate.features.track.TrackIndexAnalyzer.TRACK_ID;
import static fiji.plugin.trackmate.features.track.TrackMotilityAnalyzer.TRACK_MAX_DISTANCE_TRAVELED;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.LOG_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.PLUGIN_VERSION_ATTRIBUTE_NAME;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.mastodon.geff.GeffAxis;
import org.mastodon.geff.GeffEdge;
import org.mastodon.geff.GeffMetadata;
import org.mastodon.geff.GeffMetadata.DisplayHints;
import org.mastodon.geff.GeffMetadata.RelatedObjects;
import org.mastodon.geff.GeffNode;
import org.mastodon.geff.GeffNode.Builder;
import org.mastodon.geff.PropMetadata;
import org.mastodon.geff.VarlengthProperty;

import com.google.gson.JsonElement;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Spot.SpotVisitor;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.features.track.TrackLocationAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.json.FeatureModelIO;
import fiji.plugin.trackmate.io.json.SettingsIO;
import fiji.plugin.trackmate.util.TMUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class TmGeffWriter
{

	private static final String GEFF_VERSION = "1.0.0";

	static final String TRACKMATE_SPOT_ID_PROP = "trackmate_spot_id";

	static final String NAME_PROP = "name";

	static final String TRACK_GEFF_NAME = "tracks.geff";

	private final String geffPath;

	private final GeffMetadata metadata;

	private final GeffMetadata trackMetadata;

	private final List< GeffNode > geffNodes = new ArrayList<>();

	private final List< GeffEdge > geffEdges = new ArrayList<>();

	private final List< GeffNode > geffTrackNodes = new ArrayList<>();

	private final Map< String, Object > trackmateInfo;

	public TmGeffWriter( final String zarrPath )
	{
		this.geffPath = zarrPath;
		this.metadata = new GeffMetadata( GEFF_VERSION, true );
		this.trackMetadata = new GeffMetadata( GEFF_VERSION, true );
		this.trackmateInfo = new HashMap<>();
		final Map< String, Object > extra = Map.of( "trackmate", trackmateInfo );
		metadata.setExtra( extra );
		trackmateInfo.put( PLUGIN_VERSION_ATTRIBUTE_NAME, TrackMate.PLUGIN_NAME_VERSION );
	}

	public void appendModel( final Model model )
	{
		final String spaceUnits = model.getSpaceUnits();
		final String timeUnits = model.getTimeUnits();
		final boolean is2D = DetectionUtils.is2D( model );

		/*
		 * Serialize spots
		 */
		final SpotCollection spots = model.getSpots();
		final GeffSpotVisitor visitor = new GeffSpotVisitor( is2D, model.getTrackModel(), model.getFeatureModel().getSpotFeatureIsInt() );
		spots.iterable( false ).forEach( spot -> spot.accept( visitor ) );

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

			final boolean swap = source.getFeature( FRAME ) > target.getFeature( FRAME );
			final GeffEdge edgeNode = new GeffEdge.Builder()
					.setId( edgeId++ )
					.setSourceNodeId( swap ? tgtId : srcId )
					.setTargetNodeId( swap ? srcId : tgtId )
					.build();

			// Feature
			for ( final String edgeFeature : fm.getEdgeFeatures() )
			{
				if ( edgeFeature.equals( SPOT_SOURCE_ID ) || edgeFeature.equals( SPOT_TARGET_ID ) )
					continue;
				if ( is2D && edgeFeature.equals( Z_LOCATION ) )
					continue;

				final Double ef = fm.getEdgeFeature( edge, edgeFeature );
				edgeNode.setProp( edgeFeature, ef );
			}
			geffEdges.add( edgeNode );
		}

		/*
		 * Metadata
		 */

		// Axes
		final List< GeffAxis > axes = buildAxes( spaceUnits, timeUnits, is2D, model );
		metadata.setGeffAxes( axes );

		// Display hints
		DisplayHints displayHints = new DisplayHints()
				.displayHorizontal( POSITION_X )
				.displayVertical( POSITION_Y )
				.displayTime( FRAME );
		if ( !is2D )
			displayHints = displayHints.displayDepth( POSITION_Z );
		metadata.setDisplayHints( displayHints );

		// Spot features

		// First, determine what spot features have an actual value.
		final Set< String > spotFeaturesWithValue = new HashSet<>();
		for ( final Spot spot : spots.iterable( false ) )
			spotFeaturesWithValue.addAll( spot.getFeatures().keySet() );

		// Then add only those to the metadata.
		final Map< String, Boolean > isInt = fm.getSpotFeatureIsInt();
		final Map< String, PropMetadata > nodePropsMetadata = new HashMap<>();
		for ( final String spotFeature : spotFeaturesWithValue )
		{
			if ( is2D && spotFeature.equals( POSITION_Z ) )
				continue;
			if ( spotFeature.equals( RADIUS ) || spotFeature.equals( TRACK_ID ) )
				continue;

			final String dType = isInt.get( spotFeature ) ? "int32" : "float64";
			final Dimension dimension = fm.getSpotFeatureDimensions().get( spotFeature );
			final String unit = dimension.units( spaceUnits, timeUnits );
			final String name = fm.getSpotFeatureNames().get( spotFeature );
			final PropMetadata propMetadata = new PropMetadata( spotFeature, dType, false, unit, name, null );
			nodePropsMetadata.put( spotFeature, propMetadata );
		}
		// Add the TRACKMATE_ID feature
		final PropMetadata trackmateIdPropMetadata = new PropMetadata( TRACKMATE_SPOT_ID_PROP, "int32", false, null, "TrackMate ID", "The TrackMate internal ID of spots" );
		nodePropsMetadata.put( TRACKMATE_SPOT_ID_PROP, trackmateIdPropMetadata );
		// Add the 'radius' feature -> mandatory for GEFF
		final PropMetadata radiusPropMetadata = new PropMetadata( "radius", "float64", false, spaceUnits, "Radius", "The radius of the spot" );
		nodePropsMetadata.put( "radius", radiusPropMetadata );
		// Spot name property
		nodePropsMetadata.put( NAME_PROP, new PropMetadata( NAME_PROP, "uint8", true, null, "Spot name", "The name of the spot" ) );
		// Add the TRACK_ID feature -> map it to the GEFF 'lineage' property
		final PropMetadata trackIdPropMetadata = new PropMetadata( TRACK_ID, "int32", false, null, "Track ID", "The TrackMate track ID of the spot" );
		nodePropsMetadata.put( TRACK_ID, trackIdPropMetadata );
		final Map< String, String > trackNodePros = Map.of( "lineage", TRACK_ID );
		metadata.setTrackNodeProps( trackNodePros );

		metadata.setNodePropsMetadata( nodePropsMetadata );

		// Edge features

		// First, determine what edge features have an actual value.
		final Set< String > edgeFeaturesWithValue = new HashSet<>();
		for ( final DefaultWeightedEdge edge : edges )
		{
			for ( final String edgeFeature : fm.getEdgeFeatures() )
			{
				final Double ef = fm.getEdgeFeature( edge, edgeFeature );
				if ( ef != null )
					edgeFeaturesWithValue.add( edgeFeature );
			}
		}

		final Map< String, PropMetadata > edgePropsMetadata = new HashMap<>();
		for ( final String edgeFeature : edgeFeaturesWithValue )
		{
			if ( edgeFeature.equals( SPOT_SOURCE_ID ) || edgeFeature.equals( SPOT_TARGET_ID ) )
				continue;
			if ( is2D && edgeFeature.equals( Z_LOCATION ) )
				continue;

			final String dType = fm.getEdgeFeatureIsInt().get( edgeFeature ) ? "int32" : "float64";
			final Dimension dimension = fm.getEdgeFeatureDimensions().get( edgeFeature );
			final String unit = dimension.units( spaceUnits, timeUnits );
			final String name = fm.getEdgeFeatureNames().get( edgeFeature );
			final PropMetadata propMetadata = new PropMetadata( edgeFeature, dType, false, unit, name, null );
			edgePropsMetadata.put( edgeFeature, propMetadata );
		}
		metadata.setEdgePropsMetadata( edgePropsMetadata );

		// Feature declarations
		trackmateInfo.put( "featureDeclarations", FeatureModelIO.toJsonTree( fm ) );

		/*
		 * Tracks feature values -> on a sub GEFF file (geffception) that has no
		 * edges, only nodes.
		 */

		for ( final Integer trackID : trackModel.trackIDs( false ) )
		{
			final double xt = fm.getTrackFeature( trackID, TrackLocationAnalyzer.X_LOCATION );
			final double yt = fm.getTrackFeature( trackID, TrackLocationAnalyzer.Y_LOCATION );
			final int tt = fm.getTrackFeature( trackID, TRACK_START ).intValue();
			final double radius = fm.getTrackFeature( trackID, TRACK_MAX_DISTANCE_TRAVELED ) / 2.;

			final GeffNode.Builder builder = new GeffNode.Builder()
					.id( trackID )
					.timepoint( tt )
					.x( xt )
					.y( yt )
					.radius( radius );
			if ( !is2D )
			{
				final double zt = fm.getTrackFeature( trackID, TrackLocationAnalyzer.Z_LOCATION );
				builder.z( zt );
			}
			final GeffNode trackNode = builder.build();

			// Track features
			for ( final String trackFeature : fm.getTrackFeatures() )
			{
				Object val;
				if ( fm.getTrackFeatureIsInt().get( trackFeature ) )
					val = Integer.valueOf( fm.getTrackFeature( trackID, trackFeature ).intValue() );
				else
					val = fm.getTrackFeature( trackID, trackFeature );
				trackNode.setProp( trackFeature, val );
			}
			// Track visibility property
			final Integer visibility = trackModel.isVisible( trackID ) ? 1 : 0;
			trackNode.setProp( VISIBILITY, visibility );
			// Add the 'radius' feature -> mandatory for GEFF
			final PropMetadata trackRadiusPropMetadata = new PropMetadata( "radius", "float64", false, spaceUnits, "Radius", "The radius of excursion of the track" );
			nodePropsMetadata.put( "radius", trackRadiusPropMetadata );
			// Track name -> variable length property
			final String trackName = trackModel.name( trackID );
			final Object[] bytes = toByteArray( trackName );
			trackNode.setVarlengthProperty( NAME_PROP, new VarlengthProperty( NAME_PROP, "uint8", bytes ) );

			geffTrackNodes.add( trackNode );
		}

		/*
		 * Tracks metadata
		 */

		// Axes
		final List< GeffAxis > trackAxes = buildTrackAxes( spaceUnits, timeUnits, is2D, model );
		trackMetadata.setGeffAxes( trackAxes );

		// Display hints
		DisplayHints trackDisplayHints = new DisplayHints()
				.displayHorizontal( TrackLocationAnalyzer.X_LOCATION )
				.displayVertical( TrackLocationAnalyzer.Y_LOCATION )
				.displayTime( TRACK_START );
		if ( !is2D )
			trackDisplayHints = trackDisplayHints.displayDepth( TrackLocationAnalyzer.Z_LOCATION );
		trackMetadata.setDisplayHints( trackDisplayHints );

		// Track features

		// First, determine what track features have an actual value.
		final Set< String > trackFeaturesWithValue = new HashSet<>();
		for ( final DefaultWeightedEdge edge : edges )
		{
			for ( final String trackFeature : fm.getTrackFeatures() )
			{
				final Double ef = fm.getEdgeFeature( edge, trackFeature );
				if ( ef != null )
					trackFeaturesWithValue.add( trackFeature );
			}
		}

		// Then add only those to the metadata.
		final Map< String, Boolean > trackIsInt = fm.getTrackFeatureIsInt();
		final Map< String, PropMetadata > trackNodePropsMetadata = new HashMap<>();
		for ( final String trackFeature : trackFeaturesWithValue )
		{
			if ( is2D && trackFeature.equals( TrackLocationAnalyzer.Z_LOCATION ) )
				continue;

			final String dType = trackIsInt.get( trackFeature ) ? "int32" : "float64";
			final Dimension dimension = fm.getSpotFeatureDimensions().get( trackFeature );
			final String unit = dimension.units( spaceUnits, timeUnits );
			final String name = fm.getSpotFeatureNames().get( trackFeature );
			final PropMetadata propMetadata = new PropMetadata( trackFeature, dType, false, unit, name, null );
			trackNodePropsMetadata.put( trackFeature, propMetadata );
		}
		// Track visibility property
		trackNodePropsMetadata.put( VISIBILITY, new PropMetadata( VISIBILITY, "int32", false, null, Spot.FEATURE_NAMES.get( VISIBILITY ), "Whether the track is visible in the display" ) );
		// Track name property
		trackNodePropsMetadata.put( NAME_PROP, new PropMetadata( NAME_PROP, "uint8", true, null, "Track name", "The name of the track" ) );

		trackMetadata.setNodePropsMetadata( trackNodePropsMetadata );
	}

	public void appendSettings( final Settings settings )
	{
		// All settings
		final JsonElement settingsJson = SettingsIO.toJsonTree( settings );
		trackmateInfo.put( "settings", settingsJson );

		// Path to the image
		String imagePathStr = TMUtils.getImagePathWithoutExtension( settings );
		if ( settings.imageFileName != null && !settings.imageFileName.isEmpty() )
			imagePathStr = imagePathStr + "." + settings.imageFileName.substring( settings.imageFileName.lastIndexOf( '.' ) + 1 );

		// Make it relative to the save path
		final Path imagePath = Paths.get( imagePathStr ).toAbsolutePath().normalize();
		final Path savePath = Paths.get( geffPath ).toAbsolutePath().normalize();
		metadata.setRelatedObjects( new RelatedObjects().image( savePath.getParent().relativize( imagePath ).toString() ) );
	}

	public void appendLog( final String log )
	{
		trackmateInfo.put( LOG_ELEMENT_KEY, log );
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
		// Core GEFF
		GeffNode.writeToZarr( geffNodes, geffPath, metadata );
		GeffEdge.writeToZarr( geffEdges, geffPath, metadata );
		GeffMetadata.writeToZarr( metadata, geffPath );

		// Track GEFF
		final String geffTracksPath = Paths.get( geffPath, TRACK_GEFF_NAME ).toString();
		GeffNode.writeToZarr( geffTrackNodes, geffTracksPath, trackMetadata );
		// Required to be a valid GEFF:
		GeffEdge.writeToZarr( new ArrayList<>(), geffTracksPath, trackMetadata );
		GeffMetadata.writeToZarr( trackMetadata, geffTracksPath );
	}

	private class GeffSpotVisitor implements SpotVisitor
	{

		/** Features not in the general prop, because they are the core node. */
		private static final Set< String > SKIP_PROPS = Set.of( FRAME, POSITION_X, POSITION_Y, POSITION_Z, RADIUS, TRACK_ID );

		private int geffId = 0;

		private final TObjectIntMap< Spot > spotToId = new TObjectIntHashMap< Spot >();

		private final boolean is2d;

		private final TrackModel trackModel;

		private final Map< String, Boolean > isInt;

		public GeffSpotVisitor( final boolean is2d, final TrackModel trackModel, final Map< String, Boolean > isInt )
		{
			this.is2d = is2d;
			this.trackModel = trackModel;
			this.isInt = isInt;
		}

		private void serializeFeatures( final Spot spot, final GeffNode node )
		{
			final Map< String, Double > features = spot.getFeatures();
			for ( final String feature : features.keySet() )
			{
				if ( SKIP_PROPS.contains( feature ) )
					continue;

				final Double obj = spot.getFeature( feature );
				final Object val;
				if ( isInt.get( feature ) )
					val = Integer.valueOf( obj.intValue() );
				else
					val = obj;
				node.setProp( feature, val );
			}

			// Spot ID
			node.setProp( TRACKMATE_SPOT_ID_PROP, spot.ID() );
			// Track ID -> will be mapped to the 'lineage' GEFF property
			final Integer trackID = trackModel.trackIDOf( spot );
			if ( trackID != null )
				node.setProp( TRACK_ID, trackID.intValue() );
			// Name -> variable length property
			final String name = spot.getName();
			if ( name != null )
			{
				final Object[] bytes = toByteArray( name );
				node.setVarlengthProperty( NAME_PROP, new VarlengthProperty( NAME_PROP, "uint8", bytes ) );
			}
		}

		@Override
		public void visit( final SpotBase spot )
		{
			final Builder builder = new GeffNode.Builder()
					.id( geffId )
					.timepoint( spot.getFeature( FRAME ).intValue() )
					.x( spot.getDoublePosition( 0 ) )
					.y( spot.getDoublePosition( 1 ) )
					.radius( spot.getFeature( RADIUS ).doubleValue() );
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
					.timepoint( spot.getFeature( FRAME ).intValue() )
					.x( spot.getDoublePosition( 0 ) )
					.y( spot.getDoublePosition( 1 ) ) // No Z <- 2D
					.radius( spot.getFeature( RADIUS ).doubleValue() )
					.polygonX( polygonX )
					.polygonY( polygonY )
					.build();
			serializeFeatures( spot, node );
			geffNodes.add( node );
			spotToId.put( spot, geffId++ );
		}
	}

	private static final Object[] toByteArray( final String name )
	{
		final byte[] bytes = name.getBytes( java.nio.charset.StandardCharsets.UTF_8 );
		final Object[] data = new Object[ bytes.length ];
		for ( int i = 0; i < bytes.length; i++ )
			data[ i ] = bytes[ i ] & 0xFF;
		return data;
	}

	private static final List< GeffAxis > buildAxes( final String spaceUnit, final String timeUnit, final boolean is2d, final Model model )
	{
		final String[] spaceFeatures = is2d
				? new String[] { POSITION_X, POSITION_Y }
				: new String[] { POSITION_X, POSITION_Y, POSITION_Z };
		return axesOf( TrackMateObject.SPOTS, spaceFeatures, FRAME, spaceUnit, timeUnit, model );
	}

	private static final List< GeffAxis > buildTrackAxes( final String spaceUnit, final String timeUnit, final boolean is2d, final Model model )
	{
		final String[] spaceFeatures = is2d
				? new String[] { TrackLocationAnalyzer.X_LOCATION, TrackLocationAnalyzer.Y_LOCATION }
				: new String[] { TrackLocationAnalyzer.X_LOCATION, TrackLocationAnalyzer.Y_LOCATION, TrackLocationAnalyzer.Z_LOCATION };
		return axesOf( TrackMateObject.TRACKS, spaceFeatures, TRACK_START, spaceUnit, timeUnit, model );
	}

	private static final List< GeffAxis > axesOf(
			final TrackMateObject obj,
			final String[] spaceFeatures,
			final String timeFeature,
			final String spaceUnit,
			final String timeUnit,
			final Model model )
	{
		final String su = spaceUnit;
		final String tu = timeUnit;

		final List< GeffAxis > axes = new ArrayList<>();

		final double[] minMaxT = FeatureUtils.autoMinMax( model, obj, timeFeature, false );
		axes.add( GeffAxis.createTimeAxis( timeFeature, tu, minMaxT[ 0 ], minMaxT[ 1 ] ) );

		for ( final String spaceFeature : spaceFeatures )
		{
			final double[] minMaxX = FeatureUtils.autoMinMax( model, obj, spaceFeature, false );
			axes.add( GeffAxis.createSpaceAxis( spaceFeature, su, minMaxX[ 0 ], minMaxX[ 1 ] ) );
		}
		return axes;
	}
}
