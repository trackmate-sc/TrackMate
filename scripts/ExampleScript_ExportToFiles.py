from fiji.plugin.trackmate.visualization.hyperstack import HyperStackDisplayer
from fiji.plugin.trackmate.io import TmXmlReader
from fiji.plugin.trackmate.io import TmXmlWriter
from fiji.plugin.trackmate.io import CSVExporter
from fiji.plugin.trackmate.visualization.table import TrackTableView
from fiji.plugin.trackmate.action import ExportTracksToXML
from fiji.plugin.trackmate import Logger
from java.io import File
import sys

# We have to do the following to avoid errors with UTF8 chars generated in 
# TrackMate that will mess with our Fiji Jython.
reload(sys)
sys.setdefaultencoding('utf-8')


# This script demonstrates several ways by which TrackMate data
# can be exported to files. Mainly: 1/ to a TrackMate XML file,
# 2/ & 3/ to CSV files, 4/ to a simplified XML file, for linear tracks.


#----------------------------------
# Loading an example tracking data.
#----------------------------------

# For this script to work, you need to edit the path to the XML below.
# It can be any TrackMate file, that we will re-export in the second
# part of the script.

# Put here the path to the TrackMate file you want to load
input_filename = '/Users/tinevez/Desktop/FakeTracks.xml'
input_file = File( input_filename )

# We have to feed a logger to the reader.
logger = Logger.IJ_LOGGER

reader = TmXmlReader( input_file )
if not reader.isReadingOk():
    sys.exit( reader.getErrorMessage() )

# Load the model.
model = reader.getModel()
# Load the image and tracking settings.
imp = reader.readImage()
settings = reader.readSettings(imp)
# Load the display settings.
ds = reader.getDisplaySettings()
# Load the log.
log = reader.getLog()
log = """Hey, I have read this TrackMate file in a Jython 
script and modified it before resaving it.
Here is the original log:
"""  + log


#-------------------------------
# 1/ Resave to a TrackMate file.
#-------------------------------

# The following will generate a TrackMate XML file.
# This is the file type you will be able to load with
# the GUI, using the command 'Plugins > Tracking > Load a TrackMate file'
# in Fiji.

target_xml_filename = input_filename.replace( '.xml', '-resaved.xml' )
target_xml_file = File( target_xml_filename )
writer = TmXmlWriter( target_xml_file, logger )

# Append content. Only the model is mandatory.
writer.appendLog( log )
writer.appendModel( model )
writer.appendSettings( settings )
writer.appendDisplaySettings( ds )

# We want TrackMate to show the view config panel when 
# reopening this file.
writer.appendGUIState( 'ConfigureViews' )

# Actually write the file.
writer.writeToFile()



#-------------------------------------------------------
# 2/ Export spots data to a CSV file in a headless mode.
#-------------------------------------------------------

# This will export a CSV table containing the spots data. The table will
# include all spot features, their ID, the track they belong to, name etc.
# But it will not include the edge and track features. Also if you have
# splitting and merging events in your data, the content of the CSV file
# will not be enough to reconstruct the tracks. 

# Nonetheless, the advantage of using this snippet, with the 'CSVExporter'
# is that it can work in headless mode. It does not depend on Fiji GUI
# being launched. So you can use it a 'headless' script, called from the 
# command line. See this page for more information:
# https://imagej.net/scripting/headless

out_file_csv = input_filename.replace( '.xml', '.csv' )
only_visible = True # Export only visible tracks
# If you set this flag to False, it will include all the spots,
# the ones not in tracks, and the ones not visible.
CSVExporter.exportSpots( out_file_csv, model, only_visible )



#----------------------------------------------------
# 3/ Export spots, edges and track data to CSV files.
#----------------------------------------------------

# The following uses the tables that are displayed in the TrackMate
# GUI. As a consequence the snippet cannot be used in 'headless' mode.
# If you launch the script from the Fiji script editor, we won't
# have a problem.

# Spot table. Will contain only the spots that are in visible tracks.
spot_table = TrackTableView.createSpotTable( model, ds )
spot_table_csv_file = File( input_filename.replace( '.xml', '-spots.csv' ) )
spot_table.exportToCsv( spot_table_csv_file )

# Edge table.
edge_table = TrackTableView.createEdgeTable( model, ds )
edge_table_csv_file = File( input_filename.replace( '.xml', '-edges.csv' ) )
edge_table.exportToCsv( edge_table_csv_file )

# Track table.
track_table = TrackTableView.createTrackTable( model, ds )
track_table_csv_file = File( input_filename.replace( '.xml', '-tracks.csv' ) )
track_table.exportToCsv( track_table_csv_file )



#------------------------------------
# 4/ Export to a simplified XML file.
#------------------------------------

# During the ISBI Single-Particle Tracking challenge the organizers used
# a special file format, in a XML fie, to store tracks. Because of the 
# scope of the challenge, this works well ONLY for linear tracks. That is:
# tracks that have no merging or splitting events. 

# The file looks like this:
# <?xml version="1.0" encoding="UTF-8"?>
# <Tracks nTracks="7" spaceUnits="pixel" frameInterval="1.0" timeUnits="sec" generationDateTime="Tue, 16 Apr 2024 18:36:11" from="TrackMate v7.12.2-SNAPSHOT-4a56a0a4e34f1590f1acc341368f2fcf336e1c80">
#   <particle nSpots="49">
#     <detection t="0" x="116.25803433315897" y="118.01058828304035" z="0.0" />
#     <detection t="1" x="116.35642718798508" y="117.70622315532961" z="0.0" />
#     <detection t="2" x="116.46312406173281" y="117.69830578342241" z="0.0" />
#     <detection t="3" x="116.3916284518453" y="117.58156664808513" z="0.0" />
# etc.

# In this folder, the MATLAB script 'importTrackMateTracks.m' can open such a file
# in MATLAB. But of course, it is not a TrackMate file that TrackMate can open.

simple_xml_file = File( input_filename.replace( '.xml', '-simple-tracks.xml' ) )
ExportTracksToXML.export( model, settings, simple_xml_file )

