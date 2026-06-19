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


    %% Constants definition.
    TRACKMATE_ELEMENT           = "TrackMate";
    SPATIAL_UNITS_ATTRIBUTE     = "spatialunits";
    TIME_UNITS_ATTRIBUTE        = "timeunits";
    FEATURE_KEY_ATTRIBUTE       = "feature";
    FEATURE_NAME_ATTRIBUTE      = "name";
    FEATURE_SHORTNAME_ATTRIBUTE = "shortname";
    FEATURE_DIMENSION_ATTRIBUTE = "dimension";
    FEATURE_ISINT_ATTRIBUTE     = "isint";
    ATTRIBUTE_SUFFIX            = "__";
        
    
    %% Open and check XML.
    global isNotFirst modelStruct xmlDocFileName %#ok<GVMIS>
    if isNotFirst
        % Being called by other function
        willClear = false;
    else
        isNotFirst = true;
        willClear = true;
    end

    % Either being called by user, or being called by other functions and
    % is the first run. Or somehow was used to work on another file.
    if willClear || isempty(modelStruct) || ~strcmp(xmlDocFileName, filePath)
        xPathExp = "/" + TRACKMATE_ELEMENT + "/Model";
        try
            modelStruct = readstruct(filePath, "FileType", "xml", ...
                "StructSelector", xPathExp, "ImportAttributes", true, ...
                "AttributeSuffix", ATTRIBUTE_SUFFIX);
        catch ME
            switch ME.identifier
                case 'MATLAB:UndefinedFunction'
                    error("Your MATLAB is too old (pre-R2020b) to run this script.")
                case 'MATLAB:io:xml:readstruct:NonexistentStructSelector'
                    % <TrackMate>/<Model> not found
                    error('MATLAB:trackMateGraph:BadXMLFile', ...
                        "File does not seem to be a proper TrackMate file.");
                otherwise
                    % case 'MATLAB:io:xml:common:InvalidXMLFile', etc.
                    error(ME.identifier, "Failed to read XML file %s.", filePath);
            end
        end
        xmlDocFileName = filePath;
    end
    
    %% Retrieve physical units.
    
    spaceUnits = char( modelStruct(1).(SPATIAL_UNITS_ATTRIBUTE + ATTRIBUTE_SUFFIX) );
    timeUnits = char( modelStruct(1).(TIME_UNITS_ATTRIBUTE + ATTRIBUTE_SUFFIX) );
    
    %% XPath to retrieve spot feature declarations.
    
    % /TrackMate/Model/FeatureDeclarations/SpotFeatures/Feature
    sf = makeFeatureMap("SpotFeatures", modelStruct);
    
    %% XPath to retrieve edge feature declarations.
    
    if nargout >= 2
        % /TrackMate/Model/FeatureDeclarations/EdgeFeatures/Feature
        ef = makeFeatureMap("EdgeFeatures", modelStruct);
    end
    
    %% XPath to retrieve track feature declarations.
    
    if nargout >= 3
        % /TrackMate/Model/FeatureDeclarations/TrackFeatures/Feature
        tf = makeFeatureMap("TrackFeatures", modelStruct);
    end
    
    if willClear
        clear global isNotFirst modelStruct xmlDocFileName
    end
    
    
    
    %% Subfunctions.
    
    function featureMap = makeFeatureMap(featName, modelStruct)
        attrs = append([FEATURE_KEY_ATTRIBUTE, FEATURE_NAME_ATTRIBUTE, ...
            FEATURE_SHORTNAME_ATTRIBUTE FEATURE_DIMENSION_ATTRIBUTE, ...
            FEATURE_ISINT_ATTRIBUTE], ...
            ATTRIBUTE_SUFFIX);
        fields = cell(size(attrs)+1);
        fields(1,:) = {'feature' 'name' 'shortName' 'dimension' 'isInt' 'units'};
        try
            featureStruct = [modelStruct.FeatureDeclarations];
            featureStruct = [featureStruct.(featName)];
            featureStruct = [featureStruct.Feature];
            for k = 1 : (numel(attrs)-1)
                fields{2,k} = cellstr(vertcat(featureStruct.(attrs{k})));
            end
            % isInt
            fields{2,k+1} = strcmp("true", vertcat(featureStruct.(attrs{k+1})));
            % units
            fields{2,k+2} = cellfun(@(str)determineUnits(str, spaceUnits, timeUnits), ...
                fields{2,4}, "UniformOutput", false);
            featureMap = containers.Map(fields{2,1}, num2cell(struct(fields{:})) );
        catch ME
            switch ME.identifier
                case 'MATLAB:nonExistentField'
                    featureMap = containers.Map("KeyType", "char", "ValueType", "any");
                otherwise
                    rethrow(ME)
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
end
