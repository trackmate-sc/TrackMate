function [ spotTable, spotIDMap ] = trackmateSpots(filePath, featureList)
%%TRACKMATESPOTS Import spots from a TrackMate data file.
%
%   S = TRACKMATESPOTS(file_path) imports the spots contained in the
%   TrackMate XML file file_path as a MATLAB table. TRACKMATESPOTS only
%   imports visible spots.
% 
%   S = TRACKMATESPOTS(file_path, feature_list) where feature_list is a
%   cell array of string only imports the spot features whose names are in
%   the cell array.
%
%   [ S, idMap ] = TRACKMATESPOTS( ... ) also returns idMap, a Map from
%   spot ID to row number in the table. idMap is such that idMap(10) the
%   row at which the spot with ID 10 is listed.
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
%   The output is a MATLAB table with at least two columns, ID (the spot
%   ID) and name (the spot name). Extra features listed in the specified
%   feature_list input appear as supplemental column.
%
% EXAMPLES:
%
%   >> [ spotTable, spotIDMap ] = trackmateSpots(file_path, {'POSITION_X', ...
%       'POSITION_Y', 'POSITION_Z' } );
%   >> spotTable(20:25, :)
% 
%   ans = 
%     ID      name       POSITION_X    POSITION_Y    POSITION_Z
%     __    _________    __________    __________    __________
% 
%     18    '18 (18)'    309.04        937.77        713.72    
%     21    '21 (21)'    210.25        1023.7        955.36    
%     20    '20 (20)'    302.03        1271.2        1247.9    
%     23    '23 (23)'    1577.6        888.73        547.66    
%     22    '22 (22)'    253.45        1186.9        1179.4    
%     25    '25 (25)'    947.44        1565.2        1297.1 
%
%   >> r = spotIDMap(20)
%
%   r =
%       22
%
%   >> spotTable(22, :)
%
%   ans = 
%        ID      name       POSITION_X    POSITION_Y    POSITION_Z
%        __    _________    __________    __________    __________
% 
%        20    '20 (20)'    302.03        1271.2        1247.9    
%
%   >> x = spotTable.POSITION_X;
%   >> y = spotTable.POSITION_Y;
%   >> z = spotTable.POSITION_Z;
%   >> plot3(x, y, z, 'k.')
%   >> axis equal


% __
% Jean-Yves Tinevez - 2016

    %% Import the XPath classes.
    import javax.xml.xpath.*
    
    %% Constants definition.

    TRACKMATE_ELEMENT           = 'TrackMate';
    SPOT_ID_ATTRIBUTE           = 'ID';
    SPOT_NAME_ATTRIBUTE         = 'name';

    %% Open file.

    try
        xmlDoc = xmlread(filePath);
    catch
        error('Failed to read XML file %s.',filePath);
    end
    xmlRoot = xmlDoc.getFirstChild();

    if ~strcmp(xmlRoot.getTagName, TRACKMATE_ELEMENT)
        error('MATLAB:trackMateGraph:BadXMLFile', ...
            'File does not seem to be a proper TrackMate file.')
    end
    
    
    %% XPath to retrieve spot nodes.
    
    % Use XPath to retrieve all visible spots.
    factory = XPathFactory.newInstance;
    xPath = factory.newXPath;
    xPathFilter = xPath.compile('//Model/AllSpots/SpotsInFrame/Spot[@VISIBILITY=1]');
    nodeList = xPathFilter.evaluate(xmlDoc, XPathConstants.NODESET);
    
    %% Retrieve spot feature list.
    
    if nargin < 2 || isempty( featureList )
        featureList = getSpotFeatureList(nodeList.item(0));
    end
    
    % Remove ID and name, because we will get them anyway.
    featureList = setdiff( featureList, SPOT_ID_ATTRIBUTE );
    featureList = setdiff( featureList, SPOT_NAME_ATTRIBUTE );
    n_features = numel( featureList );
    
    %% Get filtered spot IDs.

    % Prepare holders.
    nSpots      = nodeList.getLength();
    ID          = NaN( nSpots, 1 );
    name        = cell( nSpots, 1);
    features    = NaN( nSpots, n_features );

    % Read all spot nodes.
    for i = 1 : nSpots
        node = nodeList.item( i-1 );
        ID( i )     = str2double( node.getAttribute( SPOT_ID_ATTRIBUTE ) );
        name{ i }   = char( node.getAttribute( SPOT_NAME_ATTRIBUTE ) );
        for j = 1 : n_features
           features( i, j ) = str2double( node.getAttribute( featureList{ j } ) ); 
        end
    end
    
    % Create table.
    spotTable = table();
    spotTable.( SPOT_ID_ATTRIBUTE )     = ID;
    spotTable.( SPOT_NAME_ATTRIBUTE )   = name;
    for j = 1 : n_features
       spotTable.( featureList{ j } )   = features( :, j ); 
    end
    
    % Set table metadata.
    spotTable.Properties.DimensionNames = { 'Spot', 'Feature' };
    
    vNames = spotTable.Properties.VariableNames;
    nVNames = numel( vNames );
    vDescriptions   = cell( nVNames, 1);
    vUnits          = cell( nVNames, 1);
    
    fs = trackmateFeatureDeclarations( filePath );
    for k = 1 : nVNames
        vn = vNames{ k };
        if strcmp( SPOT_ID_ATTRIBUTE, vn )
            vDescriptions{ k }  = 'Spot ID';
            vUnits{ k }         = '';
        elseif strcmp( SPOT_NAME_ATTRIBUTE, vn )
            vDescriptions{ k }  = 'Spot name';
            vUnits{ k }         = '';
        else
            vDescriptions{ k }  = fs( vn ).name;
            vUnits{ k }         = fs( vn ).units;
        end
    end
    spotTable.Properties.VariableDescriptions   = vDescriptions;
    spotTable.Properties.VariableUnits          = vUnits;
    
    % Generate map ID -> table row number.
    spotIDMap = containers.Map( ID, 1 : nSpots, ...
        'UniformValues', true);
    
    %% Subfunction.
    
    function featureList = getSpotFeatureList(node)
        
        attribute_map = node.getAttributes;
        nAttributes = attribute_map.getLength;
        
        featureList = cell(nAttributes - 1, 1); % -1 for the spot name, which we do not take
        index = 1;
        for ii = 1 : nAttributes
            
            namel = node.getAttributes.item(ii-1).getName;
            if strcmp(namel, SPOT_NAME_ATTRIBUTE)
                continue;
            end
            featureList{index} = char(namel);
            index = index + 1;
            
        end
    end
    
end