#@ ImagePlus imp
#@ double (label = "Spot radius", stepSize=0.1) radius
#@ double (label = "Quality threshold") threshold
#@ int (label = "Max frame gap") frameGap
#@ double (label = "Linking max distance") linkingMax
#@ double (label = "Gap-closing max distance") closingMax


# This Python/ImageJ2 script shows how to use TrackMate for multi-channel
# analysis. It is derived from a Groovy script by Jan Eglinger, and uses
# the ImageJ2 scripting framework to offer a basic UI / LCI interface 
# for the user.


import fiji.plugin.trackmate.Spot as Spot
import fiji.plugin.trackmate.Spot.frameComparator as frameComparator
import fiji.plugin.trackmate.Model as Model
import fiji.plugin.trackmate.Settings as Settings
import fiji.plugin.trackmate.TrackMate as TrackMate

import fiji.plugin.trackmate.detection.LogDetectorFactory as LogDetectorFactory

import fiji.plugin.trackmate.tracking.jaqaman.SparseLAPTrackerFactory as SparseLAPTrackerFactory
import fiji.plugin.trackmate.features.spot.SpotIntensityMultiCAnalyzerFactory as SpotIntensityMultiCAnalyzerFactory

import ij. IJ as IJ
import java.io.File as File
import java.util.ArrayList as ArrayList

# Swap Z and T dimensions if T=1
dims = imp.getDimensions() # default order: XYCZT
if (dims[4] == 1):
	imp.setDimensions( dims[2,4,3] )

# Get the number of channels 
nChannels = imp.getNChannels()

# Setup settings for TrackMate
settings = Settings(imp)
settings.dt = 0.05

# Spot analyzer: we want the multi-C intensity analyzer.
settings.addSpotAnalyzerFactory( SpotIntensityMultiCAnalyzerFactory() )

# Spot detector.
settings.detectorFactory = LogDetectorFactory()
settings.detectorSettings = settings.detectorFactory.getDefaultSettings()
settings.detectorSettings['RADIUS'] = radius
settings.detectorSettings['THRESHOLD'] = threshold

# Spot tracker.
settings.trackerFactory = SparseLAPTrackerFactory()
settings.trackerSettings = settings.trackerFactory.getDefaultSettings()
settings.trackerSettings['MAX_FRAME_GAP']  = frameGap
settings.trackerSettings['LINKING_MAX_DISTANCE']  = linkingMax
settings.trackerSettings['GAP_CLOSING_MAX_DISTANCE']  = closingMax

# Run TrackMate and store data into Model.
model = Model()
trackmate = TrackMate(model, settings)

if not trackmate.checkInput() or not trackmate.process():
	IJ.log('Could not execute TrackMate: ' + str( trackmate.getErrorMessage() ) )
else:
	IJ.log('TrackMate completed successfully.' )
	IJ.log( 'Found %d spots in %d tracks.' % ( model.getSpots().getNSpots( True ) , model.getTrackModel().nTracks( True ) ) )

	# Print results in the console.
	headerStr = '%10s %10s %10s %10s %10s %10s' % ( 'Spot_ID', 'Track_ID', 'Frame', 'X', 'Y', 'Z' )
	rowStr = '%10d %10d %10d %10.1f %10.1f %10.1f'
	for i in range( nChannels ):
		headerStr += ( ' %10s'  % ( 'C' + str(i+1) ) )
		rowStr += ( ' %10.1f' )
	
	IJ.log('\n')
	IJ.log( headerStr)
	tm = model.getTrackModel()
	trackIDs = tm.trackIDs( True )
	for trackID in trackIDs:
		
		spots = tm.trackSpots( trackID )
		# Sort spots by frame.
		sorted_spots = sorted( spots, key=lambda s : s.getFeature(Spot.FRAME))
		
		for spot in sorted_spots:
			values = [  spot.ID(), trackID, spot.getFeature('FRAME'), \
				spot.getFeature('POSITION_X'), spot.getFeature('POSITION_Y'), spot.getFeature('POSITION_Z') ]
			for i in range( nChannels ):
				values.append( spot.getFeature( 'MEAN_INTENSITY_CH%d' % (i+1) ) )
				
			IJ.log( rowStr % tuple( values ) ) 
	