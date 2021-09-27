from fiji.plugin.trackmate.visualization.hyperstack import HyperStackDisplayer
from fiji.plugin.trackmate.io import TmXmlReader
from fiji.plugin.trackmate import Logger
from fiji.plugin.trackmate import Settings
from fiji.plugin.trackmate import SelectionModel
from fiji.plugin.trackmate.gui.displaysettings import DisplaySettingsIO
from java.io import File
import sys

# We have to do the following to avoid errors with UTF8 chars generated in 
# TrackMate that will mess with our Fiji Jython.
reload(sys)
sys.setdefaultencoding('utf-8')


#----------------
# Setup variables
#----------------

# For this script to work, you need to edit the path to the XML below.
# And you must have the corresponding image saved as an ImageJ TIFF 
# under the name 'FakeTracks.tif' in the same folder.

# Put here the path to the TrackMate file you want to load
file = File( '/Users/tinevez/Desktop/FakeTracks.xml' )

# We have to feed a logger to the reader.
logger = Logger.IJ_LOGGER

#-------------------
# Instantiate reader
#-------------------

reader = TmXmlReader( file )
if not reader.isReadingOk():
    sys.exit( reader.getErrorMessage() )
    
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
# empty image with default magnification because we do not 
# specify an image to display it. We will see later how to 
# retrieve the image on which the data was generated.

# A selection.
sm = SelectionModel( model )

# Read the default display settings.
ds = DisplaySettingsIO.readUserDefault()

# The viewer.
displayer =  HyperStackDisplayer( model, sm, ds ) 
displayer.render()

#---------------------------------------------
# Get only part of the data stored in the file
#---------------------------------------------

# You might want to access only separate parts of the
# model.

spots = model.getSpots()
# spots is a fiji.plugin.trackmate.SpotCollection

logger.log( str(spots) )

# If you want to get the tracks, it is a bit trickier.
# Internally, the tracks are stored as a huge mathematical
# simple graph, which is what you retrieve from the file.
# There are methods to rebuild the actual tracks, taking
# into account for everything, but frankly, if you want to
# do that it is simpler to go through the model:

trackIDs = model.getTrackModel().trackIDs(True) # only filtered out ones
for id in trackIDs:
    logger.log( str(id) + ' - ' + str(model.getTrackModel().trackEdges(id)) )


#---------------------------------------
# Building a settings object from a file
#---------------------------------------

# The settings object can be  used to regenerate the full
# model from scratch. It contains all the parameters that 
# were configured when the GUI was used and the source image.

# First load the image. TrackMate will retrieve the image *PATH* 
# from the XML file and try to reopen it. It works properly only
# if the image was saved as an ImageJ TIFF.
imp = reader.readImage()
# Reading the image does not display it.


# Now we can read the settings objects. Notice the method 
# asks for the image. This is the settings object will be built
# with a link to the source image. 
settings = reader.readSettings( imp )

# Let's print the settings object.
logger.log(str('\n\nSETTINGS:'))
logger.log(str(settings))

# Display the image now.
imp.show()

# With this, we can overlay the model and the source image:
displayer =  HyperStackDisplayer(model, sm, imp, ds)
displayer.render()
