function cal = trackmateImageCalibration(path)
%%TRACKMATEIMAGECALIBRATION Reads the image calibration from a TrackMate file.
%
%   cal = TRACKMATEIMAGECALIBRATION(file_path) returns the physical image
%   calibration from a TrackMate file.
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
%   Calibration is returned as a struct with four fields: x, y, z and t.
%   Each of this field is a struct with the pixel size or frame interval
%   in physical units

% __
% Jean-Yves Tinevez & contributors - 2026

    %% Open XML file
    try
        tree = matlab.io.xml.dom.Parser().parseFile(path);
        root = tree.getDocumentElement;
    catch ME
        switch ME.identifier
            case 'MATLAB:UndefinedFunction'
                error('Your MATLAB is too old (pre-R2021a) to run this script.')
            otherwise
                rethrow(ME)
        end
    end

    %% Prepare dim strings
    
    dimensionNames      = { 'x',            'y',            'z',            't' };
    calibrationNames    = { 'pixelwidth',   'pixelheight',  'voxeldepth',   'timeinterval' };
    unitsNames          = { 'spatialunits', 'spatialunits', 'spatialunits', 'timeunits' };
    sizeNames           = { 'width',        'height',       'nslices',      'nframes' };
    
    %% Collect basic settings.
    
    % //Settings[1]//BasicSettings[1]
    settings = root.getElementsByTagName('Settings');
    settings = settings.item(0);
    bs = settings.getElementsByTagName('BasicSettings');
    bs = bs.item(0);
    
    %% Collect image settings.
    
    % //ImageData[1]
    id = root.getElementsByTagName('ImageData');
    id = id.item(0);
    
    %% Populate calibration structure with values.
    
    for i = 1 : numel(dimensionNames)
    
        dim = dimensionNames{i};
        
        if ~isempty( bs )
            cal.(dim).start = double(string(bs.getAttribute([dim 'start'])));
            cal.(dim).end   = double(string(bs.getAttribute([dim 'end'])));
            cal.(dim).size  = double(string(id.getAttribute(sizeNames{i})));
        end
        cal.(dim).value = double(string(id.getAttribute(calibrationNames{i})));

    end

    %% Get physical units from model element.
    
    % //Model[1]
    model = root.getElementsByTagName('Model');
    model = model.item(0);
    
    %% Populate calibration structure with values.
    
    for i = 1 : numel(dimensionNames)
    
        dim = dimensionNames{i};
        
        cal.(dim).units = model.getAttribute(unitsNames{i});

    end

end