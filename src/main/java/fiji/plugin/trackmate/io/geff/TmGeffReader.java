package fiji.plugin.trackmate.io.geff;

import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.mastodon.geff.GeffAxis;
import org.mastodon.geff.GeffEdge;
import org.mastodon.geff.GeffMetadata;
import org.mastodon.geff.GeffMetadata.RelatedObjects;
import org.mastodon.geff.GeffNode;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.json.SettingsIO;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import ij.IJ;
import ij.ImagePlus;

public class TmGeffReader
{

	private final String geffPath;

	private GeffMetadata metadata;

	public TmGeffReader( final String geffPath )
	{
		this.geffPath = geffPath;
	}

	public String getGUIState()
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		@SuppressWarnings( "unchecked" )
		final Map< String, Object > trackmateMD = ( Map< String, Object > ) metadata.getExtra().get( "trackmate" );
		final String guiState = ( String ) trackmateMD.get( GUI_STATE_ELEMENT_KEY );
		return guiState;
	}

	@SuppressWarnings( "unchecked" )
	public DisplaySettings getDisplaySettings()
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		final Map< String, Object > trackmateMD = ( Map< String, Object > ) metadata.getExtra().get( "trackmate" );
		final Map< String, Object > displaySettingsJson = ( Map< String, Object > ) trackmateMD.get( "displaySettings" );
		return DisplaySettingsIO.fromJsonTree( displaySettingsJson );
	}

	public synchronized ImagePlus readImage()
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		final RelatedObjects relatedObjects = metadata.getRelatedObjects();
		if ( null == relatedObjects )
			return null;

		final List< String > imagePaths = relatedObjects.getImagePaths();
		for ( final String imagePath : imagePaths )
		{
			final File imageFile = new File( new File( geffPath ).getParent(), imagePath );
			if ( !imageFile.exists() || !imageFile.canRead() )
				continue;

			final ImagePlus imp = IJ.openImage( imageFile.getAbsolutePath() );
			if ( null == imp )
				continue;
			return imp;
		}
		return null;
	}

	@SuppressWarnings( "unchecked" )
	public synchronized Settings readSettings( final ImagePlus imp )
	{
		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		final Map< String, Object > trackmateMD = ( Map< String, Object > ) metadata.getExtra().get( "trackmate" );
		final Map< String, Object > settingsJson = ( Map< String, Object > ) trackmateMD.get( "settings" );

		final Settings settings = new Settings( imp );
		SettingsIO.updateFromJsonTree( settingsJson, settings );
		return settings;
	}

	public synchronized Model getModel() throws IOException
	{
		final Model model = new Model();

		if ( null == metadata )
			metadata = GeffMetadata.readFromZarr( geffPath );

		// Units
		final boolean is2D = readUnits( model );

		// Spots
		final TIntObjectMap< Spot > spotIdMap = new TIntObjectHashMap<>();
		final TObjectIntMap< Spot > spotTrackIDMap = new TObjectIntHashMap<>();
		readSpots( model, is2D, spotIdMap, spotTrackIDMap );

		// Read edges and tracks
		readEdgesAndTracks( model, spotIdMap, spotTrackIDMap );

		// Read feature declarations
		readFeatureDeclarations( model.getFeatureModel() );

		return model;
	}

	@SuppressWarnings( "unchecked" )
	private void readFeatureDeclarations( final FeatureModel featureModel )
	{
		final Map< String, Object > trackmateMD = ( Map< String, Object > ) metadata.getExtra().get( "trackmate" );
		final Map< String, Object > featureDeclarations = ( Map< String, Object > ) trackmateMD.get( "featureDeclarations" );

		// Spot features
		final List< String > spotFeatures = ( List< String > ) featureDeclarations.get( "spotFeatures" );
		final Map< String, String > spotFeatureNames = ( Map< String, String > ) featureDeclarations.get( "spotFeatureNames" );
		final Map< String, String > spotFeatureShortNames = ( Map< String, String > ) featureDeclarations.get( "spotFeatureShortNames" );
		final Map< String, String > spotFeatureDimensionsStr = ( Map< String, String > ) featureDeclarations.get( "spotFeatureDimensions" );
		final Map< String, Dimension > spotFeatureDimensions = new HashMap<>();
		for ( final String feature : spotFeatureDimensionsStr.keySet() )
		{
			final String dimStr = spotFeatureDimensionsStr.get( feature );
			final Dimension dim = Dimension.valueOf( dimStr );
			spotFeatureDimensions.put( feature, dim );
		}
		final Map< String, Boolean > spotFeatureIsInt = ( Map< String, Boolean > ) featureDeclarations.get( "spotFeatureIsInt" );
		featureModel.declareSpotFeatures( spotFeatures, spotFeatureNames, spotFeatureShortNames, spotFeatureDimensions, spotFeatureIsInt );

		// Edge features
		final List< String > edgeFeatures = ( List< String > ) featureDeclarations.get( "edgeFeatures" );
		final Map< String, String > edgeFeatureNames = ( Map< String, String > ) featureDeclarations.get( "edgeFeatureNames" );
		final Map< String, String > edgeFeatureShortNames = ( Map< String, String > ) featureDeclarations.get( "edgeFeatureShortNames" );
		final Map< String, String > edgeFeatureDimensionsStr = ( Map< String, String > ) featureDeclarations.get( "edgeFeatureDimensions" );
		final Map< String, Dimension > edgeFeatureDimensions = new HashMap<>();
		for ( final String feature : edgeFeatureDimensionsStr.keySet() )
		{
			final String dimStr = edgeFeatureDimensionsStr.get( feature );
			final Dimension dim = Dimension.valueOf( dimStr );
			edgeFeatureDimensions.put( feature, dim );
		}
		final Map< String, Boolean > edgeFeatureIsInt = ( Map< String, Boolean > ) featureDeclarations.get( "edgeFeatureIsInt" );
		featureModel.declareEdgeFeatures( edgeFeatures, edgeFeatureNames, edgeFeatureShortNames, edgeFeatureDimensions, edgeFeatureIsInt );

		// Track features
		final List< String > trackFeatures = ( List< String > ) featureDeclarations.get( "trackFeatures" );
		final Map< String, String > trackFeatureNames = ( Map< String, String > ) featureDeclarations.get( "trackFeatureNames" );
		final Map< String, String > trackFeatureShortNames = ( Map< String, String > ) featureDeclarations.get( "trackFeatureShortNames" );
		final Map< String, String > trackFeatureDimensionsStr = ( Map< String, String > ) featureDeclarations.get( "trackFeatureDimensions" );
		final Map< String, Dimension > trackFeatureDimensions = new HashMap<>();
		for ( final String feature : trackFeatureDimensionsStr.keySet() )
		{
			final String dimStr = trackFeatureDimensionsStr.get( feature );
			final Dimension dim = Dimension.valueOf( dimStr );
			trackFeatureDimensions.put( feature, dim );
		}
		final Map< String, Boolean > trackFeatureIsInt = ( Map< String, Boolean > ) featureDeclarations.get( "trackFeatureIsInt" );
		featureModel.declareTrackFeatures( trackFeatures, trackFeatureNames, trackFeatureShortNames, trackFeatureDimensions, trackFeatureIsInt );
	}

	private void readEdgesAndTracks( final Model model, final TIntObjectMap< Spot > spotIdMap, final TObjectIntMap< Spot > spotTrackIDMap )
	{
		final List< GeffEdge > edges = GeffEdge.readFromZarr( geffPath, metadata );

		final FeatureModel fm = model.getFeatureModel();
		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = new SimpleWeightedGraph<>( DefaultWeightedEdge.class );
		final Map< Integer, Set< Spot > > connectedVertexSet = new HashMap<>();
		final Map< Integer, Set< DefaultWeightedEdge > > connectedEdgeSet = new HashMap<>();

		for ( final GeffEdge edge : edges )
		{
			// Connectivity
			final int sid = edge.getSourceNodeId();
			final Spot source = spotIdMap.get( sid );
			final int tid = edge.getTargetNodeId();
			final Spot target = spotIdMap.get( tid );
			final double score = edge.getScore();

			graph.addVertex( source );
			graph.addVertex( target );
			final DefaultWeightedEdge e = graph.addEdge( source, target );
			graph.setEdgeWeight( e, score );

			// Put back source and target with TrackMate Spot ID
			fm.putEdgeFeature( e, EdgeTargetAnalyzer.SPOT_SOURCE_ID, Double.valueOf( source.ID() ) );
			fm.putEdgeFeature( e, EdgeTargetAnalyzer.SPOT_TARGET_ID, Double.valueOf( target.ID() ) );

			// Features
			final Map< String, Object > props = edge.getProps();
			for ( final String feature : props.keySet() )
				fm.putEdgeFeature( e, feature, ( ( Number ) props.get( feature ) ).doubleValue() );

			// Track ID
			final int trackID = spotTrackIDMap.get( source );
			final Set< Spot > trackSpots = connectedVertexSet.computeIfAbsent( trackID, k -> new HashSet<>() );
			trackSpots.add( source );
			trackSpots.add( target );
			connectedEdgeSet.computeIfAbsent( trackID, k -> new HashSet<>() ).add( e );
		}

		// TODO save and read visibility
		final Map< Integer, Boolean > visibility = new HashMap<>( connectedEdgeSet.size() );
		connectedEdgeSet.keySet().forEach( trackID -> visibility.put( trackID, true ) );
		// TODO save and read track names
		final Map< Integer, String > savedTrackNames = new HashMap<>();
		connectedEdgeSet.keySet().forEach( trackID -> savedTrackNames.put( trackID, "Track " + trackID ) );

		model.getTrackModel().from( graph, connectedVertexSet, connectedEdgeSet, visibility, savedTrackNames );
	}

	private boolean readUnits( final Model model )
	{
		final GeffAxis[] geffAxes = metadata.getGeffAxes();
		String spaceUnits = null;
		String timeUnits = null;
		int spaceDim = 0;
		for ( final GeffAxis axis : geffAxes )
		{
			if ( spaceUnits == null && axis.getType().equals( GeffAxis.TYPE_SPACE ) )
			{
				spaceUnits = axis.getUnit();
				spaceDim++;
			}

			if ( timeUnits == null && axis.getType().equals( GeffAxis.TYPE_TIME ) )
				timeUnits = axis.getUnit();
		}
		model.setPhysicalUnits( spaceUnits, timeUnits );
		final boolean is2D = spaceDim < 3;
		return is2D;
	}

	private TIntObjectMap< Spot > readSpots(
			final Model model,
			final boolean is2D,
			final TIntObjectMap< Spot > spotIdMap,
			final TObjectIntMap< Spot > spotTrackIDMap ) throws IOException
	{
		// Where is stored the track ID?
		final String trackIdProp = metadata.getTrackNodeProps().get( "lineage" );

		final List< GeffNode > geffNodes = GeffNode.readFromZarr( geffPath, metadata );
		final SpotCollection spots = model.getSpots();
		for ( final GeffNode node : geffNodes )
		{
			// Read its internal ID if present
			int trackmateId;
			final Object idObj = node.getProps().get( TmGeffWriter.TRACKMATE_ID_PROP );
			if ( null != idObj && idObj instanceof Integer )
				trackmateId = ( Integer ) idObj;
			else
				trackmateId = Spot.IDcounter.incrementAndGet();

			// Frame
			final int frame = node.getT();

			// Deserialize
			Spot spot = null;

			// Is it a mesh?
			// TODO

			// Is it a polygon?
			final double[] polygonX = node.getPolygonX();
			final double[] polygonY = node.getPolygonY();
			if ( polygonX != null && polygonY != null && polygonX.length > 0 && polygonY.length > 0 )
			{
				// Put coords relative to center
				final double x = node.getX();
				final double y = node.getY();
				for ( int i = 0; i < polygonX.length; i++ )
				{
					polygonX[ i ] -= x;
					polygonY[ i ] -= y;
				}
				spot = new SpotRoi( trackmateId, polygonX, polygonY );
				spot.setPosition( x, 0 );
				spot.setPosition( y, 1 );
			}
			else
			{
				spot = new SpotBase( trackmateId );
				spot.setPosition( node.getX(), 0 );
				spot.setPosition( node.getY(), 1 );
				spot.setPosition( is2D ? 0. : node.getZ(), 2 );
				spot.putFeature( Spot.RADIUS, node.getRadius() );
			}
			spots.add( spot, frame );
			spotIdMap.put( node.getId(), spot );
			spotTrackIDMap.put( spot, ( ( Number ) node.getProp( trackIdProp ) ).intValue() );

			// Features
			final Set< String > SKIP_FEATURES = Set.of( TmGeffWriter.TRACKMATE_ID_PROP, Spot.RADIUS, Spot.POSITION_X, Spot.POSITION_Y, Spot.POSITION_Z );
			final Map< String, Object > props = node.getProps();
			for ( final Map.Entry< String, Object > entry : props.entrySet() )
			{
				final String key = entry.getKey();
				final Object value = entry.getValue();
				if ( SKIP_FEATURES.contains( key ) )
					continue;
				spot.putFeature( key, ( ( Number ) value ).doubleValue() );
			}

			// Names // TODO
			spot.setName( "ID" + spot.ID() );

		}
		return spotIdMap;
	}
}
