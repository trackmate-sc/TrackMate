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
    TRACKMATE_ELEMENT           = 'TrackMate';
    SPATIAL_UNITS_ATTRIBUTE     = 'spatialunits';
    TIME_UNITS_ATTRIBUTE        = 'timeunits';
    FEATURE_KEY_ATTRIBUTE       = 'feature';
    FEATURE_NAME_ATTRIBUTE      = 'name';
    FEATURE_SHORTNAME_ATTRIBUTE = 'shortname';
    FEATURE_DIMENSION_ATTRIBUTE = 'dimension';
    FEATURE_ISINT_ATTRIBUTE     = 'isint';
        
    
    %% Open and check XML.
    % Parsing a large file takes time. Cache the document until return.
    global TRACKMATEISNOTENTRY TRACKMATEXMLDOC TRACKMATEDOCNAME %#ok<GVMIS>
    if isempty(TRACKMATEISNOTENTRY)
        TRACKMATEISNOTENTRY = true;
        willClear = onCleanup(@()clear('global', 'TRACKMATEISNOTENTRY', 'TRACKMATEXMLDOC', 'TRACKMATEDOCNAME'));
    else
        willClear = onCleanup.empty;
    end
    
    % Either being called by user, or being called by other functions and
    % is the first run. Or somehow was used to work on another file.
    if ~isempty(willClear) || isempty(TRACKMATEXMLDOC) || ~strcmp(TRACKMATEDOCNAME, filePath)
        try
            TRACKMATEXMLDOC = matlab.io.xml.dom.Parser().parseFile( filePath );
        catch ME
            switch ME.identifier
                case 'MATLAB:UndefinedFunction'
                    error('Your MATLAB is too old (pre-R2021a) to run this script.')
                otherwise
                    % Attach the error to facilitate debugging.
                    error(ME.identifier, 'Failed to read XML file %s.',filePath);
            end
        end
        TRACKMATEDOCNAME = filePath;
    end

    rootNode = TRACKMATEXMLDOC.getDocumentElement;
    if isempty(rootNode) || ~strcmp(TRACKMATE_ELEMENT, rootNode.TagName)
        error('MATLAB:trackMateGraph:BadXMLFile', ...
            'File does not seem to be a proper TrackMate file.')
    end
    
    modelNode = rootNode.getFirstElementChild;
    modelFound = false;
    while ~isempty(modelNode)
        if strcmp('Model', modelNode.TagName)
            modelFound = true;
            break
        end
        modelNode = modelNode.getNextElementSibling;
    end

    if ~modelFound
        error('MATLAB:trackMateGraph:BadXMLFile', ...
            'File does not seem to contain a valid Model element.')
    end
    
    %% Retrieve physical units.
    
    % matlab.io.xml.dom.Element.getAttribute() returns character arrays
    spaceUnits  = modelNode.getAttribute( SPATIAL_UNITS_ATTRIBUTE );
    timeUnits   = modelNode.getAttribute( TIME_UNITS_ATTRIBUTE );
    
    %% XPath to retrieve spot feature declarations.
    
    % /TrackMate/Model/FeatureDeclarations/SpotFeatures/Feature
    sf = makeFeatureTable('SpotFeatures', modelNode);
    sf = transformFeatureTable(sf, spaceUnits, timeUnits);
    
    %% XPath to retrieve edge feature declarations.
    
    if nargout >= 2
        % /TrackMate/Model/FeatureDeclarations/EdgeFeatures/Feature
        ef = makeFeatureTable('EdgeFeatures', modelNode);
        ef = transformFeatureTable(ef, spaceUnits, timeUnits);
    end
    
    %% XPath to retrieve track feature declarations.
    
    if nargout >= 3
        % /TrackMate/Model/FeatureDeclarations/TrackFeatures/Feature
        tf = makeFeatureTable('TrackFeatures', modelNode);
        tf = transformFeatureTable(tf, spaceUnits, timeUnits);
    end
    
    
    
    %% Subfunctions.
    
    function ft = makeFeatureTable(featName, modelNode)
        ft = table( 'Size', [0 5], ...
            'VariableNames', {'key' 'name' 'shortName' 'dimension' 'isInt'}, ...
            'VariableTypes', {'string' 'string' 'string' 'string' 'logical'});

        declNode = modelNode.getFirstElementChild;
        while ~isempty(declNode)
        if strcmp('FeatureDeclarations', declNode.TagName)
            % FeatureDeclarations level
            fDNode = declNode.getFirstElementChild;
            while ~isempty(fDNode)
            if strcmp(featName, fDNode.TagName)
                % featName level
                neFeatures = fDNode.getChildElementCount;
                key = strings(neFeatures, 1);
                name = strings(neFeatures, 1);
                shortName = strings(neFeatures, 1);
                dimension = strings(neFeatures, 1);
                isInt = false(neFeatures, 1);

                iFeat = 0;
                featNode = fDNode.getFirstElementChild;
                while ~isempty(featNode)
                if strcmp('Feature', featNode.TagName)
                    % Feature node
                    iFeat = iFeat+1;
                    key{iFeat} = featNode.getAttribute(FEATURE_KEY_ATTRIBUTE);
                    name{iFeat} = featNode.getAttribute(FEATURE_NAME_ATTRIBUTE);
                    shortName{iFeat} = featNode.getAttribute(FEATURE_SHORTNAME_ATTRIBUTE);
                    dimension{iFeat} = featNode.getAttribute(FEATURE_DIMENSION_ATTRIBUTE);
                    isInt(iFeat) = strcmp('true', featNode.getAttribute(FEATURE_ISINT_ATTRIBUTE));
                end
                featNode = featNode.getNextElementSibling;
                end

                t = table(key, name, shortName, dimension, isInt);
                if iFeat < neFeatures
                    t(iFeat+1:end, :) = [];
                end
                ft = vertcat(ft, t); %#ok<AGROW>
            end
            fDNode = fDNode.getNextElementSibling;
            end
        end
        declNode = declNode.getNextElementSibling;
        end

        ft.(1) = cellstr(ft.(1));
        ft.(2) = cellstr(ft.(2));
        ft.(3) = cellstr(ft.(3));
        ft.(4) = cellstr(ft.(4));
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
end
