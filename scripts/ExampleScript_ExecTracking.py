import sys

from ij import IJ
from ij import WindowManager

from fiji.plugin.trackmate import Model
from fiji.plugin.trackmate import Settings
from fiji.plugin.trackmate import TrackMate
from fiji.plugin.trackmate import SelectionModel
from fiji.plugin.trackmate import Logger
from fiji.plugin.trackmate.detection import LogDetectorFactory
from fiji.plugin.trackmate.tracking.jaqaman import SparseLAPTrackerFactory
from fiji.plugin.trackmate.gui.displaysettings import DisplaySettingsIO
from fiji.plugin.trackmate.gui.displaysettings.DisplaySettings import TrackMateObject
from fiji.plugin.trackmate.features.track import TrackIndexAnalyzer

import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer as HyperStackDisplayer
import fiji.plugin.trackmate.features.FeatureFilter as FeatureFilter

# We have to do the following to avoid errors with UTF8 chars generated in 
# TrackMate that will mess with our Fiji Jython.
reload(sys)
sys.setdefaultencoding('utf-8')

# Get currently selected image
# imp = WindowManager.getCurrentImage()
imp = IJ.openImage('https://fiji.sc/samples/FakeTracks.tif')
imp.show()


#----------------------------
# Create the model object now
#----------------------------

# Some of the parameters we configure below need to have
# a reference to the model at creation. So we create an
# empty model now.
model = Model()

# Send all messages to ImageJ log window.
model.setLogger(Logger.IJ_LOGGER)


#------------------------
# Prepare settings object
#------------------------

settings = Settings(imp)

# Configure detector - We use the Strings for the keys
settings.detectorFactory = LogDetectorFactory()
settings.detectorSettings = {
    'DO_SUBPIXEL_LOCALIZATION' : True,
    'RADIUS' : 2.5,
    'TARGET_CHANNEL' : 1,
    'THRESHOLD' : 0.,
    'DO_MEDIAN_FILTERING' : False,
}  

# Configure spot filters - Classical filter on quality
filter1 = FeatureFilter('QUALITY', 30, True)
settings.addSpotFilter(filter1)

# Configure tracker - We want to allow merges and fusions
settings.trackerFactory = SparseLAPTrackerFactory()
settings.trackerSettings = settings.trackerFactory.getDefaultSettings() # almost good enough
settings.trackerSettings['ALLOW_TRACK_SPLITTING'] = True
settings.trackerSettings['ALLOW_TRACK_MERGING'] = True

# Add ALL the feature analyzers known to TrackMate. They will 
# yield numerical features for the results, such as speed, mean intensity etc.
settings.addAllAnalyzers()

# The line above is VERY IMPORTANT if you want to filter spots or tracks.
# By default the TrackMate settings do not include feature analyzers, and the 
# objects you will get from tracking will only include the very minimal 
# feature set needed for TrackMate to operate. This might be advantageous
# if you want to go fast. If you want however to compute specific features and / or
# filter spots based on these filters, you need to explicitely include the corresponding
# analyzer into the settings object. Filtering will not work without that. For instance,
# if you try for  to filter spots with a feature that was not calculated by an analyzer,
# you will have 0 spots after filtering.
# With `settings.addAllAnalyzers()` we simply add all the analyzers that TrackMate 
# can find. Since they are relatively fast, this is a convenient method.

# Configure track filters - We want to get rid of the two immobile spots at
# the bottom right of the image. Track displacement must be above 10 pixels.
filter2 = FeatureFilter('TRACK_DISPLACEMENT', 10, True)
settings.addTrackFilter(filter2)


#-------------------
# Instantiate plugin
#-------------------

trackmate = TrackMate(model, settings)

#--------
# Process
#--------

ok = trackmate.checkInput()
if not ok:
    sys.exit(str(trackmate.getErrorMessage()))

ok = trackmate.process()
if not ok:
    sys.exit(str(trackmate.getErrorMessage()))


#----------------
# Display results
#----------------

# A selection.
selectionModel = SelectionModel( model )

# Read the default display settings.
ds = DisplaySettingsIO.readUserDefault()
# Color by tracks.
ds.setTrackColorBy( TrackMateObject.TRACKS, TrackIndexAnalyzer.TRACK_INDEX )
ds.setSpotColorBy( TrackMateObject.TRACKS, TrackIndexAnalyzer.TRACK_INDEX )

displayer =  HyperStackDisplayer( model, selectionModel, imp, ds )
displayer.render()
displayer.refresh()

# Echo results with the logger we set at start:
model.getLogger().log( str( model ) )
