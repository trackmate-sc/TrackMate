function [ spotTable, spotIDMap, rois ] = trackmateSpots(filePath, featureList)
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
%   [ S, idMap, rois ] = TRACKMATESPOTS( ... ) also returns rois, a cell
%   array containing the 2D polygons of each spot, if there is one. 
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
%   The first output is a MATLAB table with at least two columns, ID (the
%   spot ID) and name (the spot name). Extra features listed in the
%   specified feature_list input appear as supplemental column.
%
%   The 'rois' output (3rd output) is a cell array. The ith element is a
%   Nx2 array that contains the polygon vertices coordinates (X, Y) for the
%   spot in the ith line of the table S. These coordinates are respective
%   to the (POSITION_X, POSITION_Y) spot center. If a spot does not have a
%   ROI, the cell is empty.
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
% Jean-Yves Tinevez & contributors - 2026

    %% Constants definition.

    SPOT_ID_ATTRIBUTE           = 'ID';
    SPOT_NAME_ATTRIBUTE         = 'name';
    ROI_N_POINTS_ATTTRIBUTE     = 'ROI_N_POINTS';

    %% Open file.
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
        fs = trackmateFeatureDeclarations( filePath );
    catch ME
        rethrow(ME)
    end
    rootObj = xmlDoc.getDocumentElement;
    modelNodes = rootObj.getElementsByTagName('Model');
    
    %% Retrieve spot feature list.
    
    if nargin < 2 || isempty( featureList )
        spotFound = false;
        % XPath: (TrackMate/Model/AllSpots/SpotsInFrame/Spot)[1]
        for j = 1:modelNodes.Length
            aSNode = modelNodes.node(j).getFirstElementChild;
            while ~spotFound && ~isempty(aSNode)
            if strcmp ('AllSpots', aSNode.TagName)
                % AllSpots level
                sIFNode = aSNode.getFirstElementChild;
                while ~spotFound && ~isempty(sIFNode)
                if strcmp('SpotsInFrame', sIFNode.TagName)
                    spotNode = sIFNode.getFirstElementChild;
                    while ~spotFound && ~isempty(spotNode)
                    if strcmp('Spot', spotNode.TagName)
                        spotFound = true;
                        attrMap = spotNode.getAttributes;
                        nFeatures = attrMap.Length;
                        featureList = strings(attrMap.Length, 1);
                        for k = 1:nFeatures
                            featureList{k} = attrMap.item(k-1).Name;
                        end
                        break
                    end
                    spotNode = spotNode.getNextElementSibling;
                    end
                end
                sIFNode = sIFNode.getNextElementSibling;
                end
            end
            aSNode = aSNode.getNextElementSibling;
            end
        end
        if ~spotFound
            featureList = strings(0);
        end
    end
    
    % Remove ID and name, because we will get them anyway.
    featureList = setdiff( featureList, SPOT_ID_ATTRIBUTE );
    featureList = setdiff( featureList, SPOT_NAME_ATTRIBUTE );
    featureList = [{SPOT_ID_ATTRIBUTE; SPOT_NAME_ATTRIBUTE}; featureList];
    n_features = numel( featureList );
    
    %% Get filtered spot IDs.

    % Assuming every Spot node resides in the right place.
    neSpots = 0;
    for j = 1:modelNodes.Length
        neSpots = neSpots + modelNodes.node(j).getElementsByTagName('Spot').Length;
    end

    % Prepare holders.
    holder = cell(1, n_features);
    holder{1} = zeros(neSpots, 1);
    holder{2} = strings(neSpots, 1);
    for k = 3:n_features
            holder{k} = zeros(neSpots, 1);
    end

    % Read ROI coords if it's requested
    if nargout >= 3
        willReadROIs = true;
        rois = cell(neSpots, 1);
    else
        willReadROIs = false;
    end

    % Read all spot nodes.
    nSpots = 0;
    % XPath: //Model/AllSpots/SpotsInFrame/Spot
    for j = 1:modelNodes.Length
        aSNode = modelNodes.node(j).getFirstElementChild;
        while ~isempty(aSNode)
        if strcmp ('AllSpots', aSNode.TagName)
            % AllSpots level
            sIFNode = aSNode.getFirstElementChild;
            while ~isempty(sIFNode)
            if strcmp('SpotsInFrame', sIFNode.TagName)
                % SpotsInFrame level.
                spotNode = sIFNode.getFirstElementChild;
                while ~isempty(spotNode)
                if strcmp('Spot', spotNode.TagName)
                    nSpots = nSpots + 1;
                    holder{1}(nSpots) = double(string(spotNode.getAttribute(featureList{1})));
                    holder{2}{nSpots} = spotNode.getAttribute(featureList{2});
                    for k = 3:nFeatures
                        holder{k}(nSpots) = double(string(spotNode.getAttribute(featureList{k})));
                    end

                    if willReadROIs
                        coords_str = spotNode.TextContent;
                        if ~isempty(coords_str)
                            A = sscanf( coords_str, '%f' );
                            A = reshape( A, 2, [] ).';
                            rois{nSpots} = A;
                        end
                    end
                end
                spotNode = spotNode.getNextElementSibling;
                end
            end
            sIFNode = sIFNode.getNextElementSibling;
            end
        end
        aSNode = aSNode.getNextElementSibling;
        end
    end

    if nSpots ~= neSpots
        for k = 1 : nFeatures
            holder{k}(nSpots+1:end) = [];
        end
        if willReadROIs
            rois(nSpots+1:end) = [];
        end
    end
    
    % Create table.
    holder{2} = cellstr(holder{2});
    spotTable = table(holder{:}, 'VariableNames', featureList);
    
    % Set table metadata.
    spotTable.Properties.DimensionNames = { 'Spot', 'Feature' };
    
    [vDescriptions,vUnits] = cellfun(@determineDescriptions, ...
        featureList, 'UniformOutput', false);
    
    spotTable.Properties.VariableDescriptions   = vDescriptions;
    spotTable.Properties.VariableUnits          = vUnits;
    
    % Generate map ID -> table row number.
    if nargout >= 2
        spotIDMap = containers.Map( spotTable.ID, 1 : nSpots, ...
        'UniformValues', true);
    end
    
    if willClear
        clear global isNotFirst xmlDoc xmlDocFileName
    end

    %% Subfunction.
    
    function  [desc, unit] = determineDescriptions( varName )
        switch ( varName )
            case SPOT_ID_ATTRIBUTE
                desc = 'Spot ID';
                unit = '';
            case SPOT_NAME_ATTRIBUTE
                desc = 'Spot name';
                unit = '';
            case ROI_N_POINTS_ATTTRIBUTE
                desc = 'ROI N points';
                unit = '';
            otherwise
                desc = fs(varName).name;
                unit = fs(varName).units;
        end
    end
    
end