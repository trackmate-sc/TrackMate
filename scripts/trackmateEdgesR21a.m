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
% Jean-Yves Tinevez & contributors - 2026

    %% Constants definition.

    % TRACKMATE_ELEMENT           = 'TrackMate';
    TRACK_ID_ATTRIBUTE          = 'TRACK_ID';
    TRACK_NAME_ATTRIBUTE        = 'name';
    SPOT_SOURCE_ID_ATTRIBUTE    = 'SPOT_SOURCE_ID';
    SPOT_TARGET_ID_ATTRIBUTE    = 'SPOT_TARGET_ID';

    %% Open file.

    % We'll call trackmateFeatureDeclarations() to fill in table properties
    % no matter what, so let's reuse that one's validation function.
    try
        [ ~, ef ] = trackmateFeatureDeclarationsR21a( filePath );
    catch ME
        throw(ME)
    end

    %% Retrieve edge feature list.
    % Difference from original code: for performance reasons, featureList
    % is taken from <FeatureDelarations> instead of the first <Edge> node
    if nargin < 2 || isempty( featureList )
        featureList = keys(ef);
    end

    % % Take featureList from the first <Edge> node
    % if nargin < 2 || isempty( featureList )
    %     ATTRIBUTE_SUFFIX = '__';
    %     try
    %         % Why not a full path, TrackMate/Model/AllTracks/Track/Edge?
    %         opt = detectImportOptions(filePath, 'FileType', 'xml', ...
    %             'RowNodeName', 'Edge', ...
    %             'ImportAttributes', true, 'AttributeSuffix', ATTRIBUTE_SUFFIX, ...
    %             'VariableNamingRule', 'preserve');
    %         featureList = opt.SelectedVariableNames;
    %         featureListSel = endsWith(featureList, ATTRIBUTE_SUFFIX);
    %         featureList = extractBefore(featureList(featureListSel), ...
    %             ATTRIBUTE_SUFFIX+textBoundary('end'));
    %         if isstring(featureList)
    %             featureList = cellstr(featureList);
    %         end
    %     catch ME
    %         switch ME.identifier
    %             % case 'MATLAB:io:xml:detection:RowSelectorInvalidSelection'
    %             case 'MATLAB:io:xml:common:NonexistentNode'
    %                 % No edge in file
    %                 featureList = {};
    %             otherwise
    %                 throw(ME)
    %         end
    %     end
    % end

    frontOfList = { SPOT_SOURCE_ID_ATTRIBUTE; SPOT_TARGET_ID_ATTRIBUTE };
    featureList = union(frontOfList, featureList, 'stable');

    %% XPath to retrieve filtered track elements.
    % Initialize map
    trackMap = containers.Map('KeyType', 'char', 'ValueType', 'any');

    opt = xmlImportOptions('NumVariables', 1, 'VariableSelectors', ...
        ['//Model/FilteredTracks/TrackID/@' TRACK_ID_ATTRIBUTE], ...
        'VariableTypes', 'double', 'MissingRule', 'omitrow');
    fTracks = readtable(filePath, opt);
    fTracks = fTracks.(1);
    if isempty(fTracks)
        % No selected track, return empty map
        return
    end

    %% Read Tracks table
    opt = makeXMLOptionsTrack();
    tracks = readtable( filePath, opt );
    names = tracks.(TRACK_NAME_ATTRIBUTE);
    tracks = tracks.(TRACK_ID_ATTRIBUTE);

    [~, whichSel, ~] = intersect( tracks, fTracks);

    %% Prepare a map: trackName -> edge table. 
    % 'Track/Edge/../@TRACK_ID' selects n_track nodes, not n_edge, so
    % children won't receive a correct "parent ID" column with XPath 1.0
    % We can't read every Edge into a single table and subdivide later
    % Will have repeatedly call readtable() with
    % //Model/AllTracks/Track[@TRACK_ID == 'num']/Edge
    % On the bright side, we need one table for each ID anyway

    % Prepare metadata once
    if ~isempty(whichSel)
        nVNames = numel( featureList );
        vDescriptions = cell( nVNames, 1);
        vUnits        = cell( nVNames, 1);

        for l = 1 : nVNames
            vn = featureList{ l };
            vDescriptions{ l }  = ef( vn ).name;
            vUnits{ l }         = ef( vn ).units;
        end
    else
        return
    end

    for k = 1 : numel(whichSel)
        rowNum = whichSel(k);
        opt = makeXMLOptionsEdgesOfTrackID( tracks(rowNum) );
        edgetbl = readtable( filePath, opt );

        % Set table metadata.
        edgetbl.Properties.DimensionNames = { 'Edge', 'Feature' };
        edgetbl.Properties.VariableDescriptions   = vDescriptions;
        edgetbl.Properties.VariableUnits          = vUnits;

        trackMap( names{rowNum} ) = edgetbl;
    end

    %% Subfunction.

    function opt = makeXMLOptionsTrack()
        nodePath = '//Model/AllTracks/Track';

        varNames = { TRACK_ID_ATTRIBUTE; TRACK_NAME_ATTRIBUTE };
        varTypes = { 'double'; 'char' };
        varSelectors = append('(', nodePath, ')/@', varNames);

        opt = xmlImportOptions( 'NumVariables', 2, ...
            'VariableNames', varNames, 'VariableTypes', varTypes, ...
            'VariableSelectors', varSelectors, 'RowSelector', nodePath, ...
            'VariableNamingRule', 'preserve', 'MissingRule', 'fill' );
    end


    function opt = makeXMLOptionsEdgesOfTrackID( trackID )
        nodePath = ['//Model/AllTracks/Track[@' TRACK_ID_ATTRIBUTE ' = ''' num2str(trackID) ''']/Edge'];

        n_features = numel( featureList );
        varTypes = repmat( {'double'}, n_features, 1 );
        varSelectors = append( '(', nodePath, ')/@', featureList );

        opt = xmlImportOptions( 'NumVariables', n_features, ...
            'VariableNames', featureList, 'VariableTypes', varTypes, ...
            'VariableSelectors', varSelectors, 'RowSelector', nodePath, ...
            'VariableNamingRule', 'preserve', 'MissingRule', 'fill' );
    end
end
