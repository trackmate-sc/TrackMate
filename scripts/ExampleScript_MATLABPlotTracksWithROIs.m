%% Import the tracks of cells that divide from TrackMate and overlay them with their shape on a movie.
% 
% This example MATLAB script opens a TrackMate file and an image file, and
% create a movie that shows the cells and their track over time. The cells
% are painted with their shape. The tracks have divisions, and are drawn
% over the movie.
%
% The movie that we use is one of C.elegans early embryo, following 2 cells
% divisions. It is a MIP of a 3D movie where the embryo is labeled with
% H2B-eGFP. The cells have been tracked and manually curated in TrackMate.
% There are 4 tracks: 2 for the AB and P cells, and 2 for the polar bodies.
%
% Jean-Yves Tinevez - 2024

close all
clear 
clc

% Where to store the files?
root_folder = '.';
% Image file.
movie_path = fullfile( root_folder, 'MAX_Merged-1.tif' );
% TrackMate file.
file_path = fullfile( root_folder, 'MAX_Merged.xml' );

if ~exist( file_path, 'file' ) || ~exist( movie_path, 'file' )
    % Download and uncompress the files.
    url = 'https://samples.fiji.sc/tutorials/MATLABtuto.zip';
    zip_file_path = fullfile( root_folder, 'MATLABtuto.zip' );
    fprintf( 'Downloading tutorial files from %s\n', url )
    websave( zip_file_path, url );
    fprintf( 'Saved to %s\n', zip_file_path )
    unzip( zip_file_path, root_folder )
end


%% Read the movie file.

% You can change the image used in this tutorial, but it must be a
% 1-channel, 2D over time, movie. I use the tiffreadVolume function, that
% can read ImageJ TIFFs with no issue.

fprintf( 'Reading image %s\n', movie_path )
I = tiffreadVolume( movie_path );
w           = size(I, 1);
h           = size(I, 2);
n_frames    = size(I, 3);
fprintf( 'Image is %d x %d with %d frames.\n', w, h, n_frames )

% Auto adjust contrast
imin = prctile( I(:), 1 );
imax = prctile( I(:), 99 );

%% Read the TrackMate file.

% Read tracks as a directed graph in MATLAB.
fprintf( 'Reading TrackMate file %s\n', file_path )
[G, rois] = trackmateGraph( file_path );
% The 'rois' cell array contains the polygon of all cells, if they have
% one.

% Directed graph with opposite edge direction (we will need it to paint
% tracks from current time-point to the start).
Greversed = flipedge( G );

% The spot data is stored in the the nodes of the graph as a table.
S = G.Nodes;
n_spots = height(S);
fprintf( 'Found %d spots.\n', n_spots )

% Pixel size is stored in the TrackMate file.
cal = trackmateImageCalibration( file_path );
pixel_size = cal.x.value; % microns
fprintf( 'Pixel size is %.2f %s.\n', pixel_size, cal.x.units )


%% Rebuild the tracks.

% We don't have the individual track with this. We have to rebuild them
% from the connected component of the graph.
spot_track = conncomp( G, 'Type', 'weak' );
% This contains the index of the track for all spots.
n_tracks = max( spot_track );
fprintf( 'Found %d tracks.\n', n_tracks )

%% Display image and tracks, export to movie.

% To color the tracks.
col = jet( n_tracks );

% Structure used to store the frame of the MATLAB movie.
F( n_frames ) = struct('cdata',[],'colormap',[]);

% Make a file name for the video.
export_file = strrep( file_path, '.xml', '' );

% Display first frame to trigger proper axes limits. Zoom to 200%.
figure
If = I( :, :, 1 );
imshow( If, [ imin imax ], ...
    'XData', [ 0 pixel_size * h ], ...
    'YData', [ 0 pixel_size * w ], ...
    'InitialMagnification', 200)
drawnow

% Prepare the writer for the MP4 file.
v = VideoWriter( export_file, 'MPEG-4' );
v.FrameRate = 10;
v.Quality = 100;
open(v);

for t = 1 : n_frames
   
    % Clear axes.
    cla
    
    % Image data of current frame.
    If = I( :, :, t );
    imshow( If, [ imin imax ], ...
        'XData', [ 0 pixel_size * h ], ...
        'YData', [ 0 pixel_size * w ], ...
        'InitialMagnification', 200)
    hold on
    
    % Spot data of current frame.
    frame = t-1; % 0-indexed in Java.
    log_index = S.FRAME == frame;
    Sf = S(log_index, :);
    index = find( log_index );
    
    nsf = height( Sf );
    for i = 1 : nsf

        % What is the spot row in the main table?
        spot_row = index( i );
        track = spot_track( spot_row );
        roi = rois{ spot_row };

        % Track of the current spot, backward in time.
        path = dfsearch( Greversed, spot_row );
        track_x = S.POSITION_X( path );
        track_y = S.POSITION_Y( path );

        % Paint the track.
        plot( track_x, track_y, ...
            'Color', col(track, :), ...
            'LineWidth', 2, ...
            'Marker', '.')

        % Spot polygon coordinates.
        x = Sf.POSITION_X( i );
        y = Sf.POSITION_Y( i );
        roi(:,1) = roi(:,1) + x;
        roi(:,2) = roi(:,2) + y;
        
        % Paint current spot polygon, colored by track.
        patch( 'XData', roi(:,1), 'YData', roi(:,2), ...
            'FaceColor', 'None', ...
            'EdgeColor', col( track, :), ...
            'LineWidth', 4, ...
            'Marker', 'none')
        
    end
    drawnow
    frame = getframe(gca);
    F(t) = frame;
    writeVideo(v,frame);
    
end
close(v)

%% Replay the movie.

movie( F, 1, 5 )
