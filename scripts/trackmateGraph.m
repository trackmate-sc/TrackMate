function G = trackmateGraph(filePath, spotFeatureList, edgeFeatureList, verbose)
%%TRACKMATEGRAPH Import a TrackMate data file as a MATLAB directed graph.
%
%   G = TRACKMATEGRAPH(file_path) imports the TrackMate data stored in the
%   file file_path and returns it as a MATLAB directed graph.
%
%   G = TRACKMATEGRAPH(file_path, spot_feature_list, edge_feature_list)
%   where spot_feature_list and edge_feature_list are two cell arrays of
%   string only imports the spot and edge features whose names are in the
%   cell arrays. If the cell arrays are empty, all available features are
%   imported.
%
%   G = TRACKMATEGRAPH(file_path, sfl, efl, true) generates output in the
%   command window that log the current import progress.
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
%   The ouput G is a MATLAB directed graph, which allows for the
%   representation of tracks with possible split and merge events. The full
%   capability of MATLAB graph is listed in the digraph class
%   documentation.
%
%   G.Edges and G.Nodes are two MATLAB tables that list the spot and edges
%   feature values. The G.Edges.EndNodes N x 2 matrix lists the source and
%   target nodes row number in the G.Nodes table.
%
% EXAMPLE:
%
%   >> G = trackmateGraph(file_path, [], [], true);
%   >> x = G.Nodes.POSITION_X;
%   >> y = G.Nodes.POSITION_Y;
%   >> z = G.Nodes.POSITION_Z;
%   >> % MATLAB cannot plot graphs in 3D, so we ship gplot23D.
%   >> gplot23D( adjacency(G), [ x y z ], 'k.-' )
%   >> axis equal

% __
% Jean-Yves Tinevez - 2016


    %% Constants definition.
    
    SPOT_SOURCE_ID_ATTRIBUTE    = 'SPOT_SOURCE_ID';
    SPOT_TARGET_ID_ATTRIBUTE    = 'SPOT_TARGET_ID';

    %% Deal with inputs.
    
    if nargin < 4
        verbose = true;
        if nargin < 3
            edgeFeatureList = [];
            if nargin < 2
                spotFeatureList = [];
            end
        end
    end

    %% Import spot table.
    
    if verbose
        fprintf('Importing spot table. ')
        tic
    end
    
    [ spotTable, spotIDMap ] = trackmateSpots(filePath, spotFeatureList);
    
    if verbose
        fprintf('Done in %.1f s.\n', toc)
    end

    
    %% Import edge table.
    
    if verbose
        fprintf('Importing edge table. ')
        tic
    end
    
    trackMap = trackmateEdges(filePath, edgeFeatureList);
    
    if verbose
        fprintf('Done in %.1f s.\n', toc)
    end
    
    tmp = trackMap.values;
    edgeTable = vertcat( tmp{:} );
    
    %% Build graph.
    
    
    if verbose
        fprintf('Building graph. ')
        tic
    end
    
    sourceID = edgeTable.( SPOT_SOURCE_ID_ATTRIBUTE );
    targetID = edgeTable.( SPOT_TARGET_ID_ATTRIBUTE );
    
    s = cell2mat( values( spotIDMap, num2cell(sourceID) ) );
    t = cell2mat( values( spotIDMap, num2cell(targetID) ) );
    
    EndNodes = [ s t ];
    nodeTable = table( EndNodes );
    nt = horzcat( nodeTable, edgeTable );
    
    G = digraph( nt, spotTable );
    
    if verbose
        fprintf('Done in %.1f s.\n', toc)
    end
    

end
