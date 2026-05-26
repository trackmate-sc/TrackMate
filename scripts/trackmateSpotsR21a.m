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

    % TRACKMATE_ELEMENT           = 'TrackMate';
    SPOT_ID_ATTRIBUTE           = 'ID';
    SPOT_NAME_ATTRIBUTE         = 'name';
    ROI_N_POINTS_ATTTRIBUTE     = 'ROI_N_POINTS';

    %% Open file.
    % This alone takes as much time as calling
    % trackmateFeatureDeclarations(filePath) (which has 4x readtable())!

        % xmlDoc = parseFile(matlab.io.xml.dom.Parser, filePath)
        % xmlNode = xmlDoc.getFirstChildNode;

    %% XPath to retrieve spot nodes.
    % Use XPath to retrieve all visible spots.
    % Note evaluate() is called on a different objective than JavaX's.
    % This takes ~50min to read through an 850 MB file (R25b). Unacceptable

        % evalObj = matlab.io.xml.xpath.Evaluator();
        % filterObj = evalObj.compileExpression('//Model/AllSpots/SpotsInFrame/Spot[@VISIBILITY=1]');
        % nodeList = evalObj.evaluate(filterObj,xmlDoc, EvalResultType.NodeSet);

    %% Retrieve spot feature list.

    % We'll call trackmateFeatureDeclarations() to fill in table properties
    % no matter what, so let's reuse that one's validation function.
    try
        fs = trackmateFeatureDeclarationsR21a( filePath );
    catch ME
        throw(ME)
    end

    % Difference from original code: for performance reasons, featureList
    % is taken from <FeatureDelarations> instead of the first <Spot> node
    if nargin < 2 || isempty( featureList )
        featureList = keys(fs);
    end

    % % Take featureList from the first <Spot> node
    % if nargin < 2 || isempty( featureList )
    %     ATTRIBUTE_SUFFIX = '__';
    %     try
    %         opt = detectImportOptions(filePath, 'FileType', 'xml', ...
    %             'RowSelector', '/TrackMate/Model/AllSpots/SpotsInFrame/Spot[1]', ...
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
    %             case 'MATLAB:io:xml:detection:RowSelectorInvalidSelection'
    %                 % No spots in file
    %                 featureList = {};
    %             otherwise
    %                 throw(ME)
    %         end
    %     end
    % end
    %% Create table.
    opt = makeXMLOptionsSpot(featureList);

    % Read ROI coords if it's requested
    if nargout >= 3
        willReadROIs = true;
        opt.SelectedVariableNames = [cellstr(opt.SelectedVariableNames) {'rois'}];
    else
        willReadROIs = false;
    end

    spotTable = readtable(filePath, opt);
    nSpots = height(spotTable);

    if willReadROIs
        roistrs = spotTable.rois;
        rois = cell( nSpots, 1 );
        for i = 1 : nSpots
            coords_str = roistrs{i};
            if ~isempty( coords_str )
                A = sscanf( coords_str, '%f' );
                A = reshape( A, 2, [] ).';
                rois{i} = A;
            end
        end
        spotTable = removevars(spotTable, 'rois');
    end

    % Set table metadata.
    spotTable.Properties.DimensionNames = { 'Spot', 'Feature' };

    [vDescriptions,vUnits] = cellfun(@determineDescriptions, ...
        cellstr(spotTable.Properties.VariableNames), ...
        "UniformOutput",false);

    spotTable.Properties.VariableDescriptions   = vDescriptions;
    spotTable.Properties.VariableUnits          = vUnits;

    %% Generate map ID -> table row number.
    if nargout >= 2
        spotIDMap = containers.Map( spotTable.ID, 1 : nSpots, ...
            'UniformValues', true);
    end

    %% Subfunction.

    function opt = makeXMLOptionsSpot(featureList)
        % Remove ID and name, because we will get them anyway.
        featureList = setdiff( featureList, SPOT_ID_ATTRIBUTE );
        featureList = setdiff( featureList, SPOT_NAME_ATTRIBUTE );

        nodePath = '/TrackMate/Model/AllSpots/SpotsInFrame/Spot[@VISIBILITY=1]';

        n_features = numel( featureList );
        varNames = cell( n_features + 3, 1 ); % { ID; name; featureList; rois }
        varTypes = repmat( {'double'}, n_features + 3, 1 );
        varNames{1} = SPOT_ID_ATTRIBUTE;
        % ID is imported as double, mimicking the original code
        varNames{2} = SPOT_NAME_ATTRIBUTE;
        varTypes{2} = 'char';
        varNames(3:end-1) = featureList;
        varNames{end} = 'rois';
        varTypes{end} = 'char';
        % The VISIBILITY attribute wasn't given special treatment (ignore,
        % or read as logical), just like the original code

        varSelectors = append( '(', nodePath, ')/@', varNames );
        varSelectors{end} = nodePath; % Select the node itself for its text

        opt = xmlImportOptions( 'NumVariables', n_features+3, ...
            'VariableNames', varNames, 'VariableTypes', varTypes, ...
            'VariableSelectors', varSelectors, 'RowSelector', nodePath, ...
            'SelectedVariableNames', 1:n_features+2, ...
            'VariableNamingRule', 'preserve', 'MissingRule', 'fill' );

        % Preserve whitespaces in name and rois
        opt = setvaropts(opt, [2 n_features+3], 'WhitespaceRule', 'preserve');
    end

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
