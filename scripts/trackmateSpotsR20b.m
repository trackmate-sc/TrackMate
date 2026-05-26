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
% % Jean-Yves Tinevez & contributors - 2026

    %% Import the XPath classes.
    % import javax.xml.xpath.*

    % readstruct() returns a 4GB struct from a 850 MB xml containing 880k
    % spots. Memory use sounds reasonable, but there's a lingering ~4GB
    % footprint even after running `clear('all')`.
    % This is unlike the readtable() version, whose high watermark is much
    % higher (owing to multiple passes) but aftermath is clean.

    % matlab.io.xml.xpath displays unacceptable performance even when
    % requesting a single node (~50 min and 20+GB memory for said file)

    %% Constants definition.

    TRACKMATE_ELEMENT           = 'TrackMate';
    SPOT_ID_ATTRIBUTE           = 'ID';
    SPOT_NAME_ATTRIBUTE         = 'name';
    ROI_N_POINTS_ATTTRIBUTE     = 'ROI_N_POINTS';
    ATTRIBUTE_SUFFIX            = '__';

    %% XPath to retrieve the model node
    xPathExp = ['/', TRACKMATE_ELEMENT, '[1]/Model'];
    try
        xmlStruct = readstruct(filePath, 'FileType', 'xml', ...
            'StructSelector', xPathExp, 'ImportAttributes', true, ...
            'AttributeSuffix', ATTRIBUTE_SUFFIX);
    catch ME
        switch ME.identifier
            case 'MATLAB:UndefinedFunction'
                % readstruct() starts from R2020b
                error('Your MATLAB is too old (pre-R2020b) to run this script.')
            case 'MATLAB:io:xml:readstruct:NonexistentStructSelector'
                % <TrackMate>/<Model> not found
                error('MATLAB:trackMateGraph:BadXMLFile', ...
                    'File does not seem to be a proper TrackMate file.');
            otherwise
                % case 'MATLAB:io:xml:common:InvalidXMLFile', etc.
                error(ME, 'Failed to read XML file %s.', filePath);
        end
    end
   % redundantFields = {'AllTracks' 'FilteredTracks'};
   % willRemove = isfield(xmlStruct, redundantFields);
   % xmlStruct = rmfield(xmlStruct, redundantFields(willRemove));

    %% Extract the spots
    % Indexing into a field of an array returns a comma-separated list -
    % concatenate to mimic XPath's behavior. Struct arrays are horizontal.
    try
        spotsStruct = [xmlStruct.AllSpots]; % Can be multiple?
        spotsStruct = [spotsStruct.SpotsInFrame]; % Likely multiple
        spotsStruct = [spotsStruct.Spot]; % Usually multiple

        % Select the visible spots
        spotsStruct = spotsStruct([spotsStruct.(['VISIBILITY' ATTRIBUTE_SUFFIX])] == 1);
    catch ME
        switch ME.identifier
            case 'MATLAB:nonExistentField'
                % XPath points to 0 nodes
                spotsStruct = struct([SPOT_ID_ATTRIBUTE ATTRIBUTE_SUFFIX], [], ...
                    [SPOT_NAME_ATTRIBUTE ATTRIBUTE_SUFFIX], []);
            otherwise
                rethrow(ME)
        end
    end

    nSpots = numel(spotsStruct);

    %% Retrieve spot feature list. Duplication of trackmateFeatureDeclarations.m
    SPATIAL_UNITS_ATTRIBUTE     = 'spatialunits';
    TIME_UNITS_ATTRIBUTE        = 'timeunits';
    FEATURE_KEY_ATTRIBUTE       = 'feature';
    FEATURE_NAME_ATTRIBUTE      = 'name';
    FEATURE_SHORTNAME_ATTRIBUTE = 'shortname';
    FEATURE_DIMENSION_ATTRIBUTE = 'dimension';
    FEATURE_ISINT_ATTRIBUTE     = 'isint';

    attrs = append({FEATURE_KEY_ATTRIBUTE, FEATURE_NAME_ATTRIBUTE, ...
        FEATURE_SHORTNAME_ATTRIBUTE FEATURE_DIMENSION_ATTRIBUTE, ...
        FEATURE_ISINT_ATTRIBUTE}, ...
        ATTRIBUTE_SUFFIX);
    fields = cell(size(attrs)+1);
    fields(1,:) = {'feature' 'name' 'shortName' 'dimension' 'isInt' 'units'};
    try
        spaceUnits = char(xmlStruct(1).([SPATIAL_UNITS_ATTRIBUTE ATTRIBUTE_SUFFIX]));
        timeUnits = char(xmlStruct(1).([TIME_UNITS_ATTRIBUTE ATTRIBUTE_SUFFIX]));
        featureStruct = horzcat(xmlStruct.FeatureDeclarations);
        featureStruct = horzcat(featureStruct.SpotFeatures);
        featureStruct = horzcat(featureStruct.Feature);
        for i = 1 : (numel(attrs)-1)
            fields{2,i} = cellstr(vertcat(featureStruct.(attrs{i})));
        end
        fields{2,i+1} = strcmp('true', vertcat(featureStruct.(attrs{i+1})));
        fields{2,i+2} = cellfun(@(str)determineUnits(str, spaceUnits, timeUnits), ...
            fields{2,4}, 'UniformOutput', false);
        featureMap = containers.Map(fields{2,1}, num2cell(struct(fields{:})));
    catch ME
        switch ME.identifier
            case 'MATLAB:nonExistentField'
                featureMap = containers.Map('KeyType', 'char', 'ValueType', 'any');
            otherwise
                rethrow(ME)
        end
    end

    %% Guess the attribute name from struct names
    % Valid XML name is a superset of MATLAB variable name, so MATLAB
    % may have modified them when importing into struct fields

    % Combine knowledge from FeatureDeclarations and user input
    if exist('featureList', 'var')
        fList = union( featureList, keys(featureMap));
    else
        fList = keys(featureMap);
    end

    [fList_mod, havemodd1] = matlab.lang.makeValidName(fList);
    [fList_mod, havemodd2] = matlab.lang.makeUniqueStrings(fList_mod);
    fList_mod = append(fList_mod, ATTRIBUTE_SUFFIX);
    whichModified = havemodd1 | havemodd2;

    if nargin < 2 || isempty( featureList )
        % List of feature is all spot attributes. Look up the original name
        % if it's *known* to be non-trivially renamed.
        % Still, we will lose the original attribute name if it doesn't
        % appear in <FeatureDeclarations>

        featureList_mod = fieldnames(spotsStruct);
        % May contain a Text field for the node's text
        featureList_mod = featureList_mod(endsWith(featureList_mod, ATTRIBUTE_SUFFIX));
        frontOfList = append({SPOT_ID_ATTRIBUTE; SPOT_NAME_ATTRIBUTE}, ATTRIBUTE_SUFFIX);
        featureList_mod = union(frontOfList, featureList_mod, 'stable');
        featureList = cell(size(featureList_mod));

        if any(whichModified)
            renameMap = containers.Map(fList_mod(whichModified), fList(whichModified));
            willLookup = iskey(renameMap, featureList_mod);
            for k = 1:numel(featureList_mod)
                feature_mod = featureList_mod{k};
                if willLookup(k)
                    featureList{k} = renameMap(feature_mod);
                else
                    featureList{k} = extractBefore(feature_mod, ...
                        ATTRIBUTE_SUFFIX+textBoundary('end'));
                end
            end
        else
            featureList = extractBefore(featureList_mod, ...
                ATTRIBUTE_SUFFIX+textBoundary('end'));
        end

    else
        % List of feature is the input list. Still, the renaming is done
        % according to real nodes, so we look up the modified names.
        featureList = featureList(:);
        if isstring(featureList)
            featureList = cellstr(featureList);
        end
        % Push ID and name to the front
        frontOfList = {SPOT_ID_ATTRIBUTE; SPOT_NAME_ATTRIBUTE};
        featureList = union( frontOfList, featureList, 'stable' );

        if any(whiwhModified)
            renameMapRev = containers.Map(fList(whichModified), fList_mod(whichModified));
            willLookup = iskey(renameMapRev, featureList);
            featureList_mod = cell(size(featureList));
            for k = 1:numel(featureList)
                feature = featureList{k};
                if willLookup(k)
                    featureList_mod{k} = renameMapRev(feature);
                else
                    featureList_mod{k} = append(feature, ATTRIBUTE_SUFFIX);
                end
            end
        else
            featureList_mod = append(featureList, ATTRIBUTE_SUFFIX);
        end
    end

    %% Create table
    n_features = numel( featureList );
    spotTable = table('Size', [nSpots n_features], 'VariableNames', featureList, ...
        'VariableTypes', repmat({'double'}, size(featureList)));
    for k = 1:n_features
        featureID = featureList_mod{k};
        willBeChar = strcmp( [SPOT_NAME_ATTRIBUTE ATTRIBUTE_SUFFIX], featureID );

        if isfield(spotsStruct, featureID)
            % If *some* values are missing, they are read as missing() and
            % automatically converted to corresponding missing values upon
            % concatenation.
            features = vertcat(spotsStruct.(featureID));

            if willBeChar
                if ~iscellstr(features) %#ok<ISCLSTR>
                    if ~isstring(features)
                        features = string(features);
                    end
                    features = cellstr(features);
                end
            elseif ~isa(features, 'double')
                % Is it even possible that other features are accidentally
                % read as a string? Deal with that anyway
                features = double(features);
            end

        else % Asked for a non-existent attribute
            if willBeChar
                features = cellstr(strings(nSpots, 1));
            else
                features = nan(nSpots, 1 , 'double');
            end
        end

        spotTable.(featureList{k}) = features;
    end

    %% Set table metadata.
    spotTable.Properties.DimensionNames = { 'Spot', 'Feature' };

    % vDescriptions   = cell( n_features, 1);
    % vUnits          = cell( n_features, 1);
    %
    % % Secret shortcut - pass the struct to skip reading again.
    % featureMap = trackmateFeatureDeclarations( xmlStruct );

    [vDescriptions, vUnits] = cellfun(@lookupDescriptionAndUnit, ...
        featureList, 'UniformOutput', false);
    spotTable.Properties.VariableDescriptions   = vDescriptions;
    spotTable.Properties.VariableUnits          = vUnits;

    %% Generate map ID -> table row number.
    spotIDMap = containers.Map( spotTable.ID, 1 : nSpots, ...
        'UniformValues', true);

    %% Read ROI coords if it's requested.
    if nargout >= 3
        rois = cell(nSpots, 1);
        if isfield(spotsStruct, 'Text')
            for i = 1 : nSpots
                coords_str = spotsStruct(i).Text;
                if ~isempty( coords_str )
                    A = sscanf(coords_str,'%f');
                    A = reshape(A, 2, []).';
                    rois{i} = A;
                end
            end
        end
    end

    %% Subfunction.
    function [description, unit] = lookupDescriptionAndUnit(featureName)
        switch featureName
            case SPOT_ID_ATTRIBUTE
                description = 'Spot ID';
                unit        = '';
            case SPOT_NAME_ATTRIBUTE
                description = 'Spot ID';
                unit        = '';
            case ROI_N_POINTS_ATTTRIBUTE
                description = 'ROI N points';
                unit        = '';
            otherwise
                description = featureMap( featureName ).name;
                unit        = featureMap( featureName ).units;
        end
    end
end

function  units = determineUnits( dimension, spaceUnits, timeUnits )
    switch ( dimension )
        case 'ANGLE'
            units = 'Radians';
        case 'INTENSITY'
            units = 'Counts';
        case 'INTENSITY_SQUARED'
            units = 'Counts^2';
        case' NONE'
            units = '';
        case { 'POSITION', 'LENGTH' }
            units = spaceUnits;
        case 'QUALITY'
            units = 'Quality';
        case 'TIME'
            units = timeUnits;
        case 'VELOCITY'
            units = [ spaceUnits '/' timeUnits];
        case 'RATE'
            units = [ '/' timeUnits];
        case 'STRING'
            units = '';
        otherwise
            units = 'no unit';
    end
end
