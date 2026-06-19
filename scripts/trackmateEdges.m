function  trackMap = trackmateEdges(filePath, featureList)
%%TRACKMATEEDGES Import edges from a TrackMate data file.
%
%   trackMap = TRACKMATEEDGES(file_path) imports the edges - or links -
%   contained in the TrackMate XML file file_path. TRACKMATEEDGES only
%   imports the edges of visible tracks.
%
%   trackMap = TRACKMATEEDGES(file_path, feature_list) where feature_list
%   is a cell array of string only imports the edge features whose names
%   are in the cell array.
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
%   The output is a collection of tracks. trackMap is a Map that links
%   track names to a MATLAB table containing the edges of this track. The
%   columns of the table depend on the feature_list specified as second
%   argument, but it always contains at least the SPOT_SOURCE_ID and
%   SPOT_TARGET_ID features, that store the IDs of the source and target
%   spots.
%
% EXAMPLE:
%
%   >> trackMap = trackmateEdges(file_path);   
%   >> trackNames = trackMap.keys;
%   >> trackNames{1}
%
%   ans =
%       Track_0
%
%   >> trackMap('Track_0')
% 
%   ans = 
%     SPOT_SOURCE_ID    SPOT_TARGET_ID    DISPLACEMENT    LINK_COST    VELOCITY
%     ______________    ______________    ____________    _________    ________
% 
%     14580             16501             4.7503          1            4.7503  
%     12683             14580             2.8316          1            2.8316  
%     10813             12683             8.1622          1            8.1622  
%      5295              7123              3.193          1             3.193  
%      1715              3487             4.3063          1            4.3063  
%      7123              8953             3.0804          1            3.0804  
%      8953             10813             3.3689          1            3.3689  
%         0              1715             6.2733          1            6.2733  
%      3487              5295             5.9587          1            5.9587 
% 


% __
% Jean-Yves Tinevez & contributors - 2026

    %% Constants definition.

    TRACK_ID_ATTRIBUTE          = "TRACK_ID";
    TRACK_NAME_ATTRIBUTE        = "name";
    SPOT_SOURCE_ID_ATTRIBUTE    = "SPOT_SOURCE_ID";
    SPOT_TARGET_ID_ATTRIBUTE    = "SPOT_TARGET_ID";
    ATTRIBUTE_SUFFIX            = "__";

    %% Open file

    % We'll call trackmateFeatureDeclarations() to fill in table properties
    % no matter what, so let's reuse that one's validation function.
    global isNotFirst modelStruct %#ok<GVMIS>
    if isNotFirst
        % Being called by other function
        willClear = false;
    else
        isNotFirst = true;
        willClear = true;
    end
    
    try
        [ ~, ef ] = trackmateFeatureDeclarations( filePath );
    catch ME
        rethrow(ME)
    end
    
    %% XPath to retrieve filtered track IDs.

    % Prepare a map: trackName -> edge table. 
    trackMap = containers.Map("KeyType", "char", "ValueType", "any");
    
    try
        filteredIDStruct = [modelStruct.FilteredTracks];
        filteredIDStruct = [filteredIDStruct.TrackID];
        fTrackIDs = [filteredIDStruct.( TRACK_ID_ATTRIBUTE+ATTRIBUTE_SUFFIX )];
        if ~isa(fTrackIDs, "double"); fTrackIDs = double(fTrackIDs); end
    catch ME
        switch ME.identifier
            case 'MATLAB:nonExistentField'
                % XPath points to 0 nodes
                fTrackIDs = [];
            otherwise
                rethrow(ME)
        end
    end
    
    if isempty(fTrackIDs)
        % No selected track, return empty map
        if willClear
            clear global isNotFirst modelStruct xmlDocFileName
        end
        return
    end
    
    %% XPath to retrieve track elements.

    try
        tracksStruct = [modelStruct.AllTracks]; % Can be multiple?
        tracksStruct = [tracksStruct.Track]; % Likely multiple
    catch ME
        switch ME.identifier
            case 'MATLAB:nonExistentField'
                % XPath points to 0 nodes
                if willClear
                    clear global isNotFirst modelStruct xmlDocFileName
                end
                return
            otherwise
                rethrow(ME)
        end
    end

    %% Retrieve edge feature list
    % Guess the attribute name from struct names
    % Valid XML name is a superset of MATLAB variable name, so MATLAB
    % may have modified them when importing into struct fields

    % Combine knowledge from FeatureDeclarations and user input
    if exist("featureList", "var")
        fList = union( featureList, keys(ef));
    else
        fList = keys(ef);
    end

    [fList_mod, havemodd1] = matlab.lang.makeValidName(fList);
    [fList_mod, havemodd2] = matlab.lang.makeUniqueStrings(fList_mod);
    fList_mod = append(fList_mod, ATTRIBUTE_SUFFIX);
    whichModified = havemodd1 | havemodd2;

    if nargin < 2 || isempty( featureList )
        % List of feature is all edge attributes. Look up the original name
        % if it's *known* to be non-trivially renamed.
        % Still, we will lose the original attribute name if it doesn't
        % appear in <FeatureDeclarations>

        featureList_mod = fieldnames(tracksStruct(1).Edge(1));
        % May contain a Text field for the node's text
        featureList_mod = featureList_mod(endsWith(featureList_mod, ATTRIBUTE_SUFFIX));
        % Add spot source and target, whether they are here or not.
        frontOfList = append([SPOT_SOURCE_ID_ATTRIBUTE; SPOT_TARGET_ID_ATTRIBUTE], ATTRIBUTE_SUFFIX);
        featureList_mod = union(frontOfList, featureList_mod, "stable");
        featureList = strings(size(featureList_mod));

        if any(whichModified)
            renameMap = containers.Map(fList_mod(whichModified), fList(whichModified));
            willLookup = iskey(renameMap, cellstr(featureList_mod));
            for k = 1:numel(featureList_mod)
                feature_mod = featureList_mod{k};
                if willLookup(k)
                    featureList{k} = renameMap(feature_mod);
                else
                    featureList{k} = extractBefore(feature_mod, ...
                        ATTRIBUTE_SUFFIX+textBoundary("end"));
                end
            end
        else
            featureList = extractBefore(featureList_mod, ...
                ATTRIBUTE_SUFFIX+textBoundary("end"));
        end

    else
        % List of feature is the input list. Still, the renaming is done
        % according to real attributes, so we look up the modified names.
        featureList = string(featureList(:));

        % Add spot source and target, whether they are here or not.
        frontOfList = [SPOT_SOURCE_ID_ATTRIBUTE; SPOT_TARGET_ID_ATTRIBUTE];
        featureList = union( frontOfList, featureList, "stable" );

        if any(whiwhModified)
            renameMapRev = containers.Map(fList(whichModified), fList_mod(whichModified));
            willLookup = iskey(renameMapRev, cellstr(featureList));
            featureList_mod = strings(size(featureList));
            for k = 1:numel(featureList)
                feature = featureList{k};
                if willLookup(k)
                    featureList_mod{k} = renameMapRev(feature);
                else
                    featureList_mod{k} = append(feature, ATTRIBUTE_SUFFIX);
                end
            end
        else
            featureList_mod = append(featureList, ATTRIBUTE_SUFFIX);
        end
    end

    %% XPath to retrieve filtered track elements.

    tracksID = [tracksStruct.(TRACK_ID_ATTRIBUTE+ATTRIBUTE_SUFFIX)];
    if ~isa(tracksID, "double"); tracksID = double(tracksID); end

    % Find the selected Track IDs and cache the result.
    whichSel = ismember(tracksID, fTrackIDs);
 
    % Prepare metadata once
    if ~isempty(whichSel)
        nVNames = numel( featureList );
        vDescriptions = strings( nVNames, 1);
        vUnits        = strings( nVNames, 1);

        for l = 1 : nVNames
            vn = featureList{ l };
            vDescriptions{ l }  = ef( vn ).name;
            vUnits{ l }         = ef( vn ).units;
        end
    else
        % None of the selected tracks were found, return empty map.
        if willClear
            clear global isNotFirst modelStruct xmlDocFileName
        end
        return
    end

    trackNames = [tracksStruct.(TRACK_NAME_ATTRIBUTE+ATTRIBUTE_SUFFIX)];
    if ~isstring(trackNames); trackNames = string(trackNames); end

    for iTracks = reshape(find(whichSel), 1, [])
        edgeTable = struct2table([tracksStruct(iTracks).Edge], ...
            "AsArray", true, "DimensionNames", {'Edge', 'Feature'});

        [~,iAdd,iRemove] = setxor(featureList_mod, edgeTable.Properties.VariableNames);
        edgeTable = removevars(edgeTable, iRemove);
        edgeTable{:, featureList_mod(iAdd)} = NaN;
        edgeTable = convertvars(edgeTable, @isstring, "double");

        edgeTable = edgeTable(:, featureList_mod);
        edgeTable = renamevars(edgeTable, featureList_mod, featureList);
        % Set table metadata.
        edgeTable.Properties.VariableDescriptions   = vDescriptions;
        edgeTable.Properties.VariableUnits          = vUnits;

        trackMap( trackNames{iTracks} ) = edgeTable;
    end
    
end