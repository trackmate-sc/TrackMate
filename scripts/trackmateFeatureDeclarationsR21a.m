function [ sf, ef, tf ] = trackmateFeatureDeclarations(filePath)
%%TRACKMATEFATUREDECLARATIONS Import feature declarations from a TrackMate file.
%
%   [ sf, ef, tf ] = TRACKMATEFEATUREDECLARATIONS(file_path) imports the
%   feature declarations stored in a TrackMate file file_path and returns
%   them as three maps:
%       - sf is the map for spot features;
%       - ef is the map for edge features;
%       - tf is the map for track features.
%   Each map links the feature key to a struct containing the feature
%   declaration.
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
% EXAMPLE:
%
%   >> [ sf, ef, tf ] = trackmateFeatureDeclarations(file_path);
%   >> tf.keys
%   >> tf('TRACK_DISPLACEMENT')
%
%   ans =
%           key: 'TRACK_DISPLACEMENT'
%          name: 'Track displacement'
%     shortName: 'Displacement'
%     dimension: 'LENGTH'
%         isInt: 0
%         units: 'pixels'

% __
% Jean-Yves Tinevez & contributors - 2026


    %% Import the XPath classes.
    % import javax.xml.xpath.*
    % import matlab.io.xml.dom.*
    % import matlab.io.xml.xpath.*

    %% Constants definition.
    TRACKMATE_ELEMENT           = 'TrackMate';
    SPATIAL_UNITS_ATTRIBUTE     = 'spatialunits';
    TIME_UNITS_ATTRIBUTE        = 'timeunits';
    FEATURE_KEY_ATTRIBUTE       = 'feature';
    FEATURE_NAME_ATTRIBUTE      = 'name';
    FEATURE_SHORTNAME_ATTRIBUTE = 'shortname';
    FEATURE_DIMENSION_ATTRIBUTE = 'dimension';
    FEATURE_ISINT_ATTRIBUTE     = 'isint';


    %% Open and check XML...
    % detectImportOptions(filename) would return an XMLImportOptions, but
    % takes (a lot of) time to scan through the file. Instead, we construct
    % the option directly, and let readtable() throw.
    varNames = {SPATIAL_UNITS_ATTRIBUTE TIME_UNITS_ATTRIBUTE};
    modelPath = ['/' TRACKMATE_ELEMENT '[1]/Model'];
    try
        opt_unit = xmlImportOptions('NumVariables', 2, ...
            'VariableNames', varNames, 'VariableTypes', {'char' 'char'}, ...
            'VariableNamingRule', 'preserve', 'RowSelector', modelPath, ...
            'VariableSelectors', append('(', modelPath, ')/@', varNames));
        unitTbl = readtable(filePath, opt_unit);
    catch ME
        switch ME
            case 'MATLAB:UndefinedFunction'
                % xmlImportOptions() Starts from R2021a
                error('Your MATLAB is too old (pre-R2021a) to run this script.');
            otherwise
                % Attach the error struct to facilitate diagnostics.
                error(ME, 'Failed to read XML file %s.', filePath);
        end
    end

    if height(unitTbl) < 1
        % No attribute read from the model node
        error('MATLAB:trackMateGraph:BadXMLFile', ...
            'File does not seem to be a proper TrackMate file.');
    end

    %% And retrieve physical units.
    spaceUnits = unitTbl{1,SPATIAL_UNITS_ATTRIBUTE}{1};
    timeUnits = unitTbl{1,TIME_UNITS_ATTRIBUTE}{1};

    %% XPath to retrieve spot feature declarations.
    % /TrackMate[1]/Model/FeatureDeclarations/EdgeFeatures/Feature
    opt_spot = makeXMLOptionsFeature('SpotFeatures');
    sf = transformFeatureTable(readtable(filePath, opt_spot), spaceUnits, timeUnits);

    %% XPath to retrieve edge feature declarations.
    if nargout >= 2
        % /TrackMate[1]/Model/FeatureDeclarations/EdgeFeatures/Feature
        opt_edge = makeXMLOptionsFeature('EdgeFeatures');
        ef = transformFeatureTable(readtable(filePath, opt_edge), spaceUnits, timeUnits);
    end

    %% XPath to retrieve track feature declarations.
    if nargout >= 3
        % /TrackMate[1]/Model/FeatureDeclarations/TrackFeatures/Feature
        opt_track = makeXMLOptionsFeature('TrackFeatures');
        tf = transformFeatureTable(readtable(filePath, opt_track), spaceUnits, timeUnits);
    end
    %% Subfunctions.

    % It's predetermined that 5 attributes exist at a fixed xpath. Let's
    % fill an XMLImportOptions directly. Also control the names and orders
    % of variables here.
    function opt = makeXMLOptionsFeature(nodeName)
        nodePath = ['/'  TRACKMATE_ELEMENT  '[1]/Model/FeatureDeclarations/' nodeName '/Feature'];
        nodeSelectors = append('(', nodePath, ')/@', {FEATURE_KEY_ATTRIBUTE ...
        FEATURE_NAME_ATTRIBUTE FEATURE_SHORTNAME_ATTRIBUTE ...
        FEATURE_DIMENSION_ATTRIBUTE FEATURE_ISINT_ATTRIBUTE});

        opt = xmlImportOptions('NumVariables', 5, ...
            'VariableNames', {'key' 'name' 'shortName' 'dimension' 'isInt'}, ...
            'VariableTypes', {'char' 'char' 'char' 'char' 'logical'}, ...
            'RowSelector', nodePath, 'VariableSelectors', nodeSelectors);
    end
end

    % Fill in the Units, and transform into Map
    function featureMap = transformFeatureTable(featureTable, spaceUnits, timeUnits)
        units = cellfun(@(dim)determineUnits(dim, spaceUnits, timeUnits), ...
            featureTable.dimension, 'UniformOutput', false);
        featureTable = addvars(featureTable, units, 'NewVariableNames', 'units');

        featureStruct = table2struct(featureTable);
        featureMap = containers.Map({featureStruct.key}.', num2cell(featureStruct));
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

