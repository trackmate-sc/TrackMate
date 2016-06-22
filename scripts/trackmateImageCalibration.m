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
% Jean-Yves Tinevez - 2016

    %% Open XML file

    tree = xmlread(path);
    root = tree.getFirstChild();
    
    %% Prepare dim strings
    
    dimensionNames      = { 'x',            'y',            'z',            't' };
    calibrationNames    = { 'pixelwidth',   'pixelheight',  'voxeldepth',   'timeinterval' };
    unitsNames          = { 'spatialunits', 'spatialunits', 'spatialunits', 'timeunits' };
    sizeNames           = { 'width',        'height',       'nslices',      'nframes' };
    
    %% Collect basic settings.
    
    settings = root.getElementsByTagName('Settings');
    settings = settings.item(0);
    bs = settings.getElementsByTagName('BasicSettings');
    bs = bs.item(0);
    
    %% Collect image settings.
    
    id = root.getElementsByTagName('ImageData');
    id = id.item(0);
    
    %% Populate calibration structure with values.
    
    for i = 1 : numel(dimensionNames)
    
        dim = dimensionNames{i};
        
        if ~isempty( bs )
            cal.(dim).start = str2double(bs.getAttribute([dim 'start']));
            cal.(dim).end   = str2double(bs.getAttribute([dim 'end']));
            cal.(dim).size  = str2double(id.getAttribute(sizeNames{i}));
        end
        cal.(dim).value = str2double(id.getAttribute(calibrationNames{i}));

    end

    %% Get physical units from model element.
    
    model = root.getElementsByTagName('Model');
    model = model.item(0);
    
    %% Populate calibration structure with values.
    
    for i = 1 : numel(dimensionNames)
    
        dim = dimensionNames{i};
        
        cal.(dim).units = char(model.getAttribute(unitsNames{i}));

    end

end