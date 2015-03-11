from fiji.plugin.trackmate.visualization.hyperstack import HyperStackDisplayer
from fiji.plugin.trackmate.io import TmXmlReader
from fiji.plugin.trackmate import Logger
from fiji.plugin.trackmate import Settings
from fiji.plugin.trackmate import SelectionModel
from fiji.plugin.trackmate.providers import DetectorProvider
from fiji.plugin.trackmate.providers import TrackerProvider
from fiji.plugin.trackmate.providers import SpotAnalyzerProvider
from fiji.plugin.trackmate.providers import EdgeAnalyzerProvider
from fiji.plugin.trackmate.providers import TrackAnalyzerProvider
from java.io import File
import sys
  
  
#----------------
# Setup variables
#----------------
  
# Put here the path to the TrackMate file you want to load
file = File('/Users/tinevez/Desktop/iconas/Data/FakeTracks.xml')
  
# We have to feed a logger to the reader.
logger = Logger.IJ_LOGGER
  
#-------------------
# Instantiate reader
#-------------------
  
reader = TmXmlReader(file)
if not reader.isReadingOk():
    sys.exit(reader.getErrorMessage())
#-----------------
# Get a full model
#-----------------
  
# This will return a fully working model, with everything
# stored in the file. Missing fields (e.g. tracks) will be 
# null or None in python
model = reader.getModel()
# model is a fiji.plugin.trackmate.Model
  
#----------------
# Display results
#----------------
  
# We can now plainly display the model. It will be shown on an
# empty image with default magnification.
sm = SelectionModel(model)
displayer =  HyperStackDisplayer(model, sm)
displayer.render()
  
#---------------------------------------------
# Get only part of the data stored in the file
#---------------------------------------------
  
# You might want to access only separate parts of the 
# model. 
  
spots = model.getSpots()
# spots is a fiji.plugin.trackmate.SpotCollection
  
logger.log(str(spots))
  
# If you want to get the tracks, it is a bit trickier. 
# Internally, the tracks are stored as a huge mathematical
# simple graph, which is what you retrieve from the file. 
# There are methods to rebuild the actual tracks, taking
# into account for everything, but frankly, if you want to 
# do that it is simpler to go through the model:
  
trackIDs = model.getTrackModel().trackIDs(True) # only filtered out ones
for id in trackIDs:
    logger.log(str(id) + ' - ' + str(model.getTrackModel().trackEdges(id)))
  
  
#---------------------------------------
# Building a settings object from a file
#---------------------------------------
  
# Reading the Settings object is actually currently complicated. The 
# reader wants to initialize properly everything you saved in the file,
# including the spot, edge, track analyzers, the filters, the detector,
# the tracker, etc...
# It can do that, but you must provide the reader with providers, that
# are able to instantiate the correct TrackMate Java classes from
# the XML data.
  
# We start by creating an empty settings object
settings = Settings()
  
# Then we create all the providers, and point them to the target model:
detectorProvider        = DetectorProvider()
trackerProvider         = TrackerProvider()
spotAnalyzerProvider    = SpotAnalyzerProvider()
edgeAnalyzerProvider    = EdgeAnalyzerProvider()
trackAnalyzerProvider   = TrackAnalyzerProvider()
  
# Ouf! now we can flesh out our settings object:
reader.readSettings(settings, detectorProvider, trackerProvider, spotAnalyzerProvider, edgeAnalyzerProvider, trackAnalyzerProvider)
  
logger.log(str('\n\nSETTINGS:'))
logger.log(str(settings))
  
# The settings object is also instantiated with the target image.
# Note that the XML file only stores a link to the image.
# If the link is not valid, the image will not be found.
imp = settings.imp
imp.show()
  
# With this, we can overlay the model and the source image:
displayer =  HyperStackDisplayer(model, sm, imp)
displayer.render()