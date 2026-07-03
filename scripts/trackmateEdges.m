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

    TRACK_ID_ATTRIBUTE          = 'TRACK_ID';
    TRACK_NAME_ATTRIBUTE        = 'name';
    SPOT_SOURCE_ID_ATTRIBUTE    = 'SPOT_SOURCE_ID';
    SPOT_TARGET_ID_ATTRIBUTE    = 'SPOT_TARGET_ID';

    %% Open file
    global isNotFirst xmlDoc %#ok<GVMIS>
    if isNotFirst
        % Being called by other function
        willClear = false;
    else
        isNotFirst = true;
        willClear = true;
    end
    % We'll call trackmateFeatureDeclarations() to fill in table properties
    % no matter what, so let's reuse that one's validation function.
    try
        [ ~, ef ] = trackmateFeatureDeclarations( filePath );
    catch ME
        rethrow(ME)
    end

    rootObj = xmlDoc.getDocumentElement;
    modelNodes = rootObj.getElementsByTagName('Model');
    
    %% Retrieve edge feature list
    if nargin < 2 || isempty( featureList )
        % //Edge. Why not full path?
        edgeList = rootObj.getElementsByTagName( 'Edge' );
        if edgeList.Length > 0
            attrMap = edgeList.node(1).getAttributes;
            nFeatures = attrMap.Length;
            featureList = strings(nFeatures, 1);
            for k = 1 : nFeatures
                featureList{k} = attrMap.item(k-1).Name;
            end
        else
            featureList = strings(0);
        end
    end
    
    % Add spot source and target, whether they are here or not.
    frontOfList = { SPOT_SOURCE_ID_ATTRIBUTE; SPOT_TARGET_ID_ATTRIBUTE };
    featureList = union(frontOfList, featureList, 'stable');
    nFeatures = numel( featureList );
    
    %% XPath to retrieve filtered track IDs.
    % Prepare a map: trackName -> edge table. 
    trackMap = containers.Map('KeyType', 'char', 'ValueType', 'any');

    % XPath: //Model/FilteredTracks/TrackID/@TrackID
    fTracks = zeros(rootObj.getElementsByTagName('TrackID').Length, 1);
    iTracks = 0;
    for j = 1:modelNodes.Length
        node = modelNodes.node(j);
        % Model level
        fTNode = node.getFirstElementChild;
        while ~isempty(fTNode)
        if strcmp('FilteredTracks', fTNode.TagName)
            % FilteredTracks level
            tIDNode = fTNode.getFirstElementChild;
            while ~isempty(tIDNode)
            if strcmp('TrackID', tIDNode.TagName)
                % Found TrackID node
                iTracks = iTracks + 1;
                fTracks(iTracks) = double(string( ...
                    tIDNode.getAttribute(TRACK_ID_ATTRIBUTE)));
            end
            tIDNode = tIDNode.getNextElementSibling;
            end
        end
        fTNode = fTNode.getNextElementSibling;
        end
    end

    fTracks(iTracks+1:end) = [];

    if isempty(fTracks)
        % No selected track, return empty map
        if willClear
            clear global isNotFirst xmlDoc xmlDocFileName
        end
        return
    end
    
    %% XPath to retrieve filtered track elements.
    % Read Tracks table
    neTracks = rootObj.getElementsByTagName('Track').Length;
    names = strings(neTracks, 1);
    tracks = zeros(neTracks, 1);
    iTracks = 0;
    for j = 1 : modelNodes.Length
        node = modelNodes.node(j);
        % Model level
        aTNode = node.getFirstElementChild;
        while ~isempty(aTNode)
        if strcmp('AllTracks', aTNode.TagName)
            % AllTracks level
            tNode = aTNode.getFirstElementChild;
            while ~isempty(tNode)
            if strcmp('Track', tNode.TagName)
                % Track level
                iTracks = iTracks + 1;
                names{iTracks} = tNode.getAttribute(TRACK_NAME_ATTRIBUTE);
                tracks(iTracks) = double(string(tNode.getAttribute(TRACK_ID_ATTRIBUTE)));
            end
            tNode = tNode.getNextElementSibling;
            end
        end
        aTNode = aTNode.getNextElementSibling;
        end
    end
    names(iTracks+1:neTracks) = [];
    tracks(iTracks+1:neTracks) = [];

    % Find the selected Track IDs and cache the result.
    whichSel = ismember( tracks, fTracks);

    % Prepare metadata once
    if ~isempty(whichSel)
        nVNames = numel( featureList );
        vDescriptions = strings( nVNames, 1);
        vUnits        = strings( nVNames, 1);

        for l = 1 : nVNames
            vn = featureList{ l };
            vDescriptions{ l }  = ef( vn ).name;
            vUnits{ l }         = ef( vn ).units;
        end
    else
        % None of the selected tracks were found, return empty map.
        if willClear
            clear global isNotFirst xmlDoc xmlDocFileName
        end
        return
    end
    
    % Current track No.
    iTracks = 0;

    % XPath: (//Model/AllTracks/Track)/Edge
    for j = 1 : modelNodes.Length
        node = modelNodes.node(j);
        % Model level
        aTNode = node.getFirstElementChild;
        while ~isempty(aTNode)
        if strcmp('AllTracks', aTNode.TagName)
            % AllTracks level
            tNode = aTNode.getFirstElementChild;
            while ~isempty(tNode)
            if strcmp('Track', tNode.TagName)
                % Track level
                iTracks = iTracks + 1;
                if whichSel(iTracks)
            % Create table.
                    edgeTable = walkAndMakeTable(tNode);
            % Set table metadata.
                    edgeTable.Properties.DimensionNames = { 'Edge', 'Feature' };
                    edgeTable.Properties.VariableDescriptions   = vDescriptions;
                    edgeTable.Properties.VariableUnits          = vUnits;

                    trackMap( names{iTracks} ) = edgeTable;
                end
            end
            tNode = tNode.getNextElementSibling;
            end
        end
        aTNode = aTNode.getNextElementSibling;
        end
    end
        
    if willClear
        clear global isNotFirst xmlDoc xmlDocFileName
    end
    
     %% Subfunction.
    
    function edgeTable = walkAndMakeTable(trackNode)
        % Assuming every child element is an <Edge>...
        neEdges = trackNode.getChildElementCount;
        holders = cell(1, nFeatures);
        for i = 1 : nFeatures
            holders{i} = zeros(neEdges, 1);
        end
        nEdges = 0;
        edgeNode = trackNode.getFirstElementChild;
        while ~isempty(edgeNode)
            if strcmp('Edge', edgeNode.TagName)
                nEdges = nEdges+1;
                for i = 1 : nFeatures
                    holders{i}(nEdges) = double(string(edgeNode.getAttribute(featureList{i})));
                end
            end
            edgeNode = edgeNode.getNextElementSibling;
        end

        % And trim the end if that turns out to be false
        if nEdges < neEdges
            for i = 1 : nFeatures
                holders{i}(nEdges+1:end) = [];
            end
        end

        edgeTable = table(holders{:}, 'VariableNames', featureList);
    end
    
end