function  trackMap = trackmateEdges(filePath, featureList)
%%TRACKMATEEDGES Import edges from a TrackMate data file.
%
%   trackMap = TRACKMATEEDGES(file_path) imports the edges - or links -
%   contained in the TrackMate XML file file_path. TRACKMATEEDGES only
%   imports the edges of visible tracks.
%
%   trackMap = TRACKMATEEDGES(file_path, feature_list) where feature_list
%   is a cell array of string only imports the edge features whose names
%   are in the cell array.
%
% INPUT:
%
%   file_path must be a path to a TrackMate file, containing the whole
%   TrackMate data, and not the simplified XML file that contains only
%   linear tracks. Such simplified tracks are imported using the
%   importTrackMateTracks function.
%
%   A TrackMate file is a XML file that starts with the following header:
%   <?xml version="1.0" encoding="UTF-8"?>
%       <TrackMate version="3.3.0">
%       ...    
%   and has a Model element in it:
%         <Model spatialunits="pixel" timeunits="sec">
%
% OUTPUT:
%
%   The output is a collection of tracks. trackMap is a Map that links
%   track names to a MATLAB table containing the edges of this track. The
%   columns of the table depend on the feature_list specified as second
%   argument, but it always contains at least the SPOT_SOURCE_ID and
%   SPOT_TARGET_ID features, that store the IDs of the source and target
%   spots.
%
% EXAMPLE:
%
%   >> trackMap = trackmateEdges(file_path);   
%   >> trackNames = trackMap.keys;
%   >> trackNames{1}
%
%   ans =
%       Track_0
%
%   >> trackMap('Track_0')
% 
%   ans = 
%     SPOT_SOURCE_ID    SPOT_TARGET_ID    DISPLACEMENT    LINK_COST    VELOCITY
%     ______________    ______________    ____________    _________    ________
% 
%     14580             16501             4.7503          1            4.7503  
%     12683             14580             2.8316          1            2.8316  
%     10813             12683             8.1622          1            8.1622  
%      5295              7123              3.193          1             3.193  
%      1715              3487             4.3063          1            4.3063  
%      7123              8953             3.0804          1            3.0804  
%      8953             10813             3.3689          1            3.3689  
%         0              1715             6.2733          1            6.2733  
%      3487              5295             5.9587          1            5.9587 
% 


% __
% Jean-Yves Tinevez - 2016

%% Import the XPath classes.
    import javax.xml.xpath.*
    
    %% Constants definition.

    TRACKMATE_ELEMENT           = 'TrackMate';
    TRACK_ID_ATTRIBUTE          = 'TRACK_ID';
    TRACK_NAME_ATTRIBUTE        = 'name';
    SPOT_SOURCE_ID_ATTRIBUTE    = 'SPOT_SOURCE_ID';
    SPOT_TARGET_ID_ATTRIBUTE    = 'SPOT_TARGET_ID';

    %% Open file

    try
        xmlDoc = xmlread( filePath );
    catch
        error('Failed to read XML file %s.',filePath);
    end
    xmlRoot = xmlDoc.getFirstChild();

    if ~strcmp(xmlRoot.getTagName, TRACKMATE_ELEMENT)
        error('MATLAB:trackMateGraph:BadXMLFile', ...
            'File does not seem to be a proper TrackMate file.')
    end
    
    
    %% XPath initialization.
    factory = XPathFactory.newInstance;
    xPath = factory.newXPath;
    
    %% Retrieve edge feature list
    if nargin < 2 || isempty( featureList )
        xPathEdgeFilter = xPath.compile('//Edge');
        edgeNode        = xPathEdgeFilter.evaluate(xmlDoc, XPathConstants.NODE );
        featureList     = getEdgeFeatureList( edgeNode );
    end
    
    % Add spot source and target, whether they are here or not.
    featureList = union( SPOT_TARGET_ID_ATTRIBUTE, featureList, 'stable'  );
    featureList = union( SPOT_SOURCE_ID_ATTRIBUTE, featureList, 'stable' );
    nFeatures = numel( featureList );
    
    %% XPath to retrieve filtered track IDs.

    xPathFTrackFilter   = xPath.compile('//Model/FilteredTracks/TrackID');
    fTrackNodeList      = xPathFTrackFilter.evaluate(xmlDoc, XPathConstants.NODESET);
    nFTracks            = fTrackNodeList.getLength();
    
    fTrackIDs = NaN( nFTracks, 1);
    for i = 1 : nFTracks
        fTrackIDs( i ) = str2double( fTrackNodeList.item( i-1 ).getAttribute( TRACK_ID_ATTRIBUTE ) );
    end
    
    %% XPath to retrieve filtered track elements.
    
    xPathTrackFilter    = xPath.compile('//Model/AllTracks/Track');
    trackNodeList       = xPathTrackFilter.evaluate(xmlDoc, XPathConstants.NODESET);
    nTracks             = trackNodeList.getLength();
    
    % Prepare a map: trackName -> edge table. 
    trackMap = containers.Map('KeyType', 'char', 'ValueType', 'any');
    
    xPathEdgeFilter     = xPath.compile('./Edge');
    for i = 1 : nTracks
       
        trackNode       = trackNodeList.item( i-1 );
        trackID         = str2double( trackNode.getAttribute( TRACK_ID_ATTRIBUTE ) );
        trackName       = char( trackNode.getAttribute( TRACK_NAME_ATTRIBUTE ) );
        
        if any( trackID == fTrackIDs )
           
            edgeNodeList    = xPathEdgeFilter.evaluate( trackNode, XPathConstants.NODESET );
            nEdges          = edgeNodeList.getLength();
            features = NaN( nEdges, nFeatures );
            
            % Read all edge nodes.
            for k = 1 : nEdges
                node = edgeNodeList.item( k-1 );
                for j = 1 : nFeatures
                    features( k, j ) = str2double( node.getAttribute( featureList{ j } ) );
                end
            end
            
            % Create table.
            edgeTable = table();
            for j = 1 : nFeatures
                edgeTable.( featureList{ j } )   = features( :, j );
            end
            
            % Set table metadata.
            edgeTable.Properties.DimensionNames = { 'Edge', 'Feature' };
            
            vNames = edgeTable.Properties.VariableNames;
            nVNames = numel( vNames );
            vDescriptions   = cell( nVNames, 1);
            vUnits          = cell( nVNames, 1);
            
            [ ~, ef ] = trackmateFeatureDeclarations( filePath );
            for l = 1 : nVNames
                vn = vNames{ l };
                vDescriptions{ l }  = ef( vn ).name;
                vUnits{ l }         = ef( vn ).units;
            end
            edgeTable.Properties.VariableDescriptions   = vDescriptions;
            edgeTable.Properties.VariableUnits          = vUnits;
           
            trackMap( trackName ) = edgeTable;
            
        end
        
    end
    
     %% Subfunction.
    
    function featureList = getEdgeFeatureList(node)
        
        attribute_map = node.getAttributes;
        n_attributes = attribute_map.getLength;
        
        featureList = cell(n_attributes, 1);
        index = 1;
        for ii = 1 : n_attributes
            
            namel = node.getAttributes.item(ii-1).getName;
            featureList{index} = char(namel);
            index = index + 1;
            
        end
    end
    
end