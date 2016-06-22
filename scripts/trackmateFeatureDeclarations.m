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
% Jean-Yves Tinevez - 2016


    %% Import the XPath classes.
    import javax.xml.xpath.*
    
    
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
    
    factory = XPathFactory.newInstance;
    xpath = factory.newXPath;
    
    %% Retrieve physical units.
    
    modelPath   =  xpath.compile('/TrackMate/Model');
    modelNode   = modelPath.evaluate(xmlRoot, XPathConstants.NODESET).item(0);
    spaceUnits  = char( modelNode.getAttribute( SPATIAL_UNITS_ATTRIBUTE ) );
    timeUnits   = char( modelNode.getAttribute( TIME_UNITS_ATTRIBUTE ) );
    
    %% XPath to retrieve spot feature declarations.
    
    spotFeatureFilter = xpath.compile('/TrackMate/Model/FeatureDeclarations/SpotFeatures/Feature');
    spotFeatureNodes = spotFeatureFilter.evaluate(xmlDoc, XPathConstants.NODESET);
    nSpotFeatureNodes = spotFeatureNodes.getLength();
    
    sf = containers.Map();
    for i = 1 : nSpotFeatureNodes
        f = readFeature( spotFeatureNodes.item( i-1 ), spaceUnits, timeUnits );
        sf( f.key ) = f;
    end
    
    %% XPath to retrieve edge feature declarations.
    
    edgeFeatureFilter = xpath.compile('/TrackMate/Model/FeatureDeclarations/EdgeFeatures/Feature');
    edgeFeatureNodes = edgeFeatureFilter.evaluate(xmlDoc, XPathConstants.NODESET);
    nEdgeFeatureNodes = edgeFeatureNodes.getLength();
    
    ef = containers.Map();
    for i = 1 : nEdgeFeatureNodes
        f = readFeature( edgeFeatureNodes.item( i-1 ), spaceUnits, timeUnits );
        ef( f.key ) = f;
    end
    
    %% XPath to retrieve track feature declarations.
    
    trackFeatureFilter = xpath.compile('/TrackMate/Model/FeatureDeclarations/TrackFeatures/Feature');
    trackFeatureNodes = trackFeatureFilter.evaluate(xmlDoc, XPathConstants.NODESET);
    nTrackFeatureNodes = trackFeatureNodes.getLength();
    
    tf = containers.Map();
    for i = 1 : nTrackFeatureNodes
        f = readFeature( trackFeatureNodes.item( i-1 ), spaceUnits, timeUnits );
        tf( f.key ) = f;
    end
    
    
    
    %% Subfunctions.
    
    function f = readFeature(featureNode, spaceUnits, timeUnits)
       
        key         = char( featureNode.getAttribute( FEATURE_KEY_ATTRIBUTE ) );
        name        = char( featureNode.getAttribute( FEATURE_NAME_ATTRIBUTE ) );
        shortName   = char( featureNode.getAttribute( FEATURE_SHORTNAME_ATTRIBUTE ) );
        dimension   = char( featureNode.getAttribute( FEATURE_DIMENSION_ATTRIBUTE ) );
        isInt       = strcmp( 'true', char( featureNode.getAttribute( FEATURE_ISINT_ATTRIBUTE ) ) );
        units       = determineUnits( dimension, spaceUnits, timeUnits );
        
        f = struct();
        f.key       = key;
        f.name      = name;
        f.shortName = shortName;
        f.dimension = dimension;
        f.isInt     = isInt;
        f.units     = units;
        
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