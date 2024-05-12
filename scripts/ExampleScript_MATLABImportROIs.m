
close all
clear 
clc

% Read movie file
movie_path = 'c:/Users/tinevez/Desktop/MAX_Merged-1.tif'; 
I = tiffreadVolume( movie_path );
w = size(I, 1);
h = size(I, 2);

% Pixel size is manually specified.
pixel_size = 0.1984666072986889; % microns

% Read tracks.
file_path = 'c:/Users/tinevez/Desktop/MAX_Merged.xml';
[S, idmap, rois] = trackmateSpots( file_path );

n_frames = max(S.FRAME)+1;
colors = turbo(n_frames);

n_spots = height(S);

%%

F(n_frames) = struct('cdata',[],'colormap',[]);

figure
hold on

for t = 1 : n_frames
   
    % Clear axes.
    cla
    
    % Image data of current frame.
    If = I( :, :, t );
    imshow( If, [], ...
        'XData', [ 0 pixel_size * h ], ...
        'YData', [ 0 pixel_size * w ] )
    
    % Spot data of current frame.
    frame = t-1;
    log_index = S.FRAME == frame;
    Sf = S(log_index, :);
    index = find( log_index );
    
    nsf = height( Sf );
    for i = 1 : nsf
        x   = Sf.POSITION_X(i);
        y   = Sf.POSITION_Y(i);
        roi = rois{ index(i) };
        roi(:,1) = roi(:,1) + x;
        roi(:,2) = roi(:,2) + y;
        
        patch( 'XData', roi(:,1), 'YData', roi(:,2), ...
            'FaceColor', 'None', ...
            'EdgeColor', 'r', ...
            'LineWidth', 2, ...
            'Marker', 'none')
        
    end
    drawnow
    F(t) = getframe(gca);
    
end

cla
movie( F, 10, 3 )



%% 

figure
hold on
axis square

for i = 1 : n_spots
   
    x   = S.POSITION_X(i);
    y   = S.POSITION_Y(i);
    roi = rois{i};
    roi(:,1) = roi(:,1) + x;
    roi(:,2) = roi(:,2) + y;
        
    frame = S.FRAME(i)+1;
    col = colors( frame, : );
    
    patch( 'XData', roi(:,1), 'YData', roi(:,2), ...
        'FaceColor', 'None', ... 
        'EdgeColor', col )
  
    
end