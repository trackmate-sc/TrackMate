%% Import spot ROIs from a TrackMate file.
% 
% This example MATLAB script shows how to use the `trackmateSpot.m`
% function to open a TrackMate file that contains spots with ROIs (their
% shape as polygon). They are then painted in a common graph, colored by
% frame.
% 
% The `trackmateSpot.m` does not return the track information, and we
% cannot retrieve the tracks solely with this function. See the other
% example `ExampleScript_MATLABImportROIs.m` in this folder for a more
% involved example that does this.
%
% Jean-Yves Tinevez - 2024

close all
clear 
clc

% Where to store the files?
root_folder = '.';
% TrackMate file.
file_path = fullfile( root_folder, 'MAX_Merged.xml' );
if ~exist( file_path, 'file' )
    % Download and uncompress the files.
    url = 'https://samples.fiji.sc/tutorials/MATLABtuto.zip';
    zip_file_path = fullfile( root_folder, 'MATLABtuto.zip' );
    fprintf( 'Downloading tutorial files from %s\n', url )
    websave( zip_file_path, url );
    fprintf( 'Saved to %s\n', zip_file_path )
    unzip( zip_file_path, root_folder )
end

% Read tracks.
[S, idmap, rois] = trackmateSpots( file_path );

n_frames = max(S.FRAME)+1;
colors = turbo(n_frames);

n_spots = height(S);

%% Plot the spot contours.

figure
hold on
axis square

for i = 1 : n_spots
   
    % Spot center.
    x   = S.POSITION_X(i);
    y   = S.POSITION_Y(i);
    % Spot contour (centered on 0).
    roi = rois{i};
    % Put the contours with respect to the spot center.
    roi(:,1) = roi(:,1) + x;
    roi(:,2) = roi(:,2) + y;

    % Color by frame.
    frame = S.FRAME(i)+1;
    col = colors( frame, : );
    
    patch( 'XData', roi(:,1), 'YData', roi(:,2), ...
        'FaceColor', 'None', ... 
        'EdgeColor', col )
    plot( x, y, 'Color', col, 'Marker', 'x' )
    
end
xlabel( 'X (um)' )
ylabel( 'Y (um)' )
set(gca, 'YDir', 'reverse' )
