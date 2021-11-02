%% TrackMate in MATLAB example.
% We run a full analysis in MATLAB, calling the Java classes.

%% Some remarks.
% For this script to work you need to prepare a bit your Fiji installation
% and its connection to MATLAB.
%
% 1.
% In your Fiji, please install the 'ImageJ-MATLAB' site. This is explained
% here: https://imagej.net/scripting/matlab. Then restart Fiji.
%
% 2.
% Add the /path/to/your/Fiji.app/scripts to the MATLAB path. Either use the
% path tool in MATLAB or use the command:
% >> addpath( '/path/to/your/Fiji.app/scripts' )
%
% 3.
% In MATLAB, first launch ImageJ-MATLAB:
% >> ImageJ
%
% 4.
% You can now run this script.



%% The import lines, like in Python and Java

import java.lang.Integer

import ij.IJ

import fiji.plugin.trackmate.TrackMate
import fiji.plugin.trackmate.Model
import fiji.plugin.trackmate.Settings
import fiji.plugin.trackmate.SelectionModel
import fiji.plugin.trackmate.Logger
import fiji.plugin.trackmate.features.FeatureFilter
import fiji.plugin.trackmate.detection.LogDetectorFactory
import fiji.plugin.trackmate.tracking.sparselap.SparseLAPTrackerFactory
import fiji.plugin.trackmate.tracking.LAPUtils
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer


%% The script itself.

% Get currently selected image
imp = IJ.openImage('https://fiji.sc/samples/FakeTracks.tif');
% imp = ij.ImagePlus('/Users/tinevez/Desktop/Data/FakeTracks.tif');
imp.show()


%----------------------------
% Create the model object now
%----------------------------
   
% Some of the parameters we configure below need to have
% a reference to the model at creation. So we create an
% empty model now.
model = Model();
   
% Send all messages to ImageJ log window.
model.setLogger( Logger.IJ_LOGGER )
      
%------------------------
% Prepare settings object
%------------------------
      
settings = Settings( imp );
      
% Configure detector - We use a java map
settings.detectorFactory = LogDetectorFactory();
map = java.util.HashMap();
map.put('DO_SUBPIXEL_LOCALIZATION', true);
map.put('RADIUS', 2.5);
map.put('TARGET_CHANNEL', Integer.valueOf(1)); % Needs to be an integer, otherwise TrackMate complaints.
map.put('THRESHOLD', 0);
map.put('DO_MEDIAN_FILTERING', false);
settings.detectorSettings = map;
   
% Configure spot filters - Classical filter on quality.
% All the spurious spots have a quality lower than 50 so we can add:
filter1 = FeatureFilter('QUALITY', 50., true);
settings.addSpotFilter(filter1)
    
% Configure tracker - We want to allow splits and fusions
settings.trackerFactory  = SparseLAPTrackerFactory();
settings.trackerSettings = LAPUtils.getDefaultLAPSettingsMap(); % almost good enough
settings.trackerSettings.put('ALLOW_TRACK_SPLITTING', true)
settings.trackerSettings.put('ALLOW_TRACK_MERGING', true)
   
% Configure track analyzers - Later on we want to filter out tracks 
% based on their displacement, so we need to state that we want 
% track displacement to be calculated. By default, out of the GUI, 
% not features are calculated. 

% Let's add all analyzers we know of.
settings.addAllAnalyzers()
   
% Configure track filters - We want to get rid of the two immobile spots at 
% the bottom right of the image. Track displacement must be above 10 pixels.
filter2 = FeatureFilter('TRACK_DISPLACEMENT', 10.0, true);
settings.addTrackFilter(filter2)
   
   
%-------------------
% Instantiate plugin
%-------------------
   
trackmate = TrackMate(model, settings);
      
%--------
% Process
%--------
   
ok = trackmate.checkInput();
if ~ok
    display(trackmate.getErrorMessage())
end

ok = trackmate.process();
if ~ok
    display(trackmate.getErrorMessage())
end
      
%----------------
% Display results
%----------------

% Read the user default display setttings.
ds = DisplaySettingsIO.readUserDefault();

% Big lines.
ds.setLineThickness( 3. )

selectionModel = SelectionModel( model );
displayer = HyperStackDisplayer( model, selectionModel, imp, ds );
displayer.render()
displayer.refresh()
   
% Echo results
display( model.toString() )

