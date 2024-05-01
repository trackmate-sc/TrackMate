import sys
import math

from ij.gui import NewImage
from ij.plugin import Animator

from fiji.plugin.trackmate import Settings
from fiji.plugin.trackmate import Model
from fiji.plugin.trackmate import Logger
from fiji.plugin.trackmate import Spot
from fiji.plugin.trackmate import SelectionModel
from fiji.plugin.trackmate import TrackMate
from fiji.plugin.trackmate.visualization.hyperstack import HyperStackDisplayer
from fiji.plugin.trackmate.visualization.trackscheme import TrackScheme
from fiji.plugin.trackmate.gui.displaysettings import DisplaySettingsIO
from fiji.plugin.trackmate.gui.displaysettings.DisplaySettings import TrackMateObject
from fiji.plugin.trackmate.gui.displaysettings.DisplaySettings import TrackDisplayMode

# We just need a model for this script. Nothing else, since
# we will do everything manually.
model = Model()
model.setLogger(Logger.IJ_LOGGER)

# Well actually, we still need a bit:
# We want to color-code the tracks by their feature, for instance
# with the track index. But for this, we need to compute the
# features themselves.
#
# Manually, this is done by declaring what features interest you
# in a settings object, and creating a ModelFeatureUpdater that
# will listen to changes in the model, and compute the feautures
# on the fly.
settings = Settings()
settings.addAllAnalyzers()


# Every manual edit to the model must be made
# between a model.beginUpdate() and a model.endUpdate()
# call, otherwise you will mess with the event signalling
# and feature calculation.
model.beginUpdate()

# 1.

s1 = None
for t in range(0, 5):
    x = 10 + t * 10
    if s1 is None:

        # When you create a spot, you always have to specify its x, y, z
        # coordinates (even if z=0 in 2D images), AND its radius, AND its
        # quality. We enforce these 5 values so as to avoid any bad surprise
        # in other TrackMate component.
        # Typically, we use negative quality values to tag spots created
        # manually.
        s1 = Spot(x, 10, 0, 1, -1)
        model.addSpotTo(s1, t)
        continue


    s2 = Spot(x, 10, 0, 1, -1)
    model.addSpotTo(s2, t)
    # You need to specify an edge cost for the link you create between two spots.
    # Again, we use negative costs to tag edges created manually.
    model.addEdge(s1, s2, -1)
    s1 = s2

# So that's how you manually build a model from scratch.
# The next lines just do more of this, to build something enjoyable.

middle = s2
s1 = s2
for t in range(0, 4):
    x = 60 + t * 10
    s2 = Spot(x, 10, 0, 1, -1)
    model.addSpotTo(s2, t + 5)
    model.addEdge(s1, s2, -1)
    s1 = s2

s1 = middle
for t in range(0, 16):
    y = 20 + t * 6
    s2 = Spot(50, y, 0, 1, -1)
    model.addSpotTo(s2, t + 5)
    model.addEdge(s1, s2, -1)
    s1 = s2


# 2.

s1 = None
for t in range(0, 21):
    if s1 is None:
        s1 = Spot(110, 10, 0, 1, -1)
        model.addSpotTo(s1, t)
        start = s1
        continue

    y = 10 + t * 5
    s2 = Spot(110, y, 0, 1, -1)
    model.addSpotTo(s2, t)
    model.addEdge(s1, s2, -1)

    if t == 10:
        middle = s2

    s1 = s2

s1 = start
for t in range(1, 20):
    theta = math.pi - t * math.pi / 20
    x = 110 + 40 * math.sin(theta)
    y = 35 + 25 * math.cos(theta)
    s2 = Spot(x, y, 0, 1, -1)   
    model.addSpotTo(s2, t)
    model.addEdge(s1, s2, -1)
    s1 = s2

s1 = middle
for t in range(1, 11):
    x = 110 + t * 5
    y = 60 + t * 5
    s2 = Spot(x, y, 0, 1, -1)   
    model.addSpotTo(s2, t+10)
    model.addEdge(s1, s2, -1)
    s1 = s2


# 3.

s1 = None
for t in range(0, 21):
    if s1 is None:
        s1 = Spot(170, 110, 0, 1, -1)
        model.addSpotTo(s1, t)
        continue

    x = 170 + t * 4
    if t < 10:
        y = 110 - t * 10
    else:
        y = 10 + (t-10) * 10
    s2 = Spot(x, y, 0, 1, -1)   
    model.addSpotTo(s2, t)
    model.addEdge(s1, s2, -1)
    s1 = s2

    if t == 5:
        start = s2
    if t == 15:
        end = s2

s1 = start
for t in range(6, 15):
    x = 194 + (t-5) * 3.5
    s2 = Spot(x, 60, 0, 1, -1)
    model.addSpotTo(s2, t)
    model.addEdge(s1, s2, -1)
    s1 = s2

model.addEdge(s2, end, -1)


# 4.
# A nice partial squircle

n = 4
a = 30
b = 50
s1 = None
for t in range(0, 21):
    theta = math.pi/10.0 + t/20.0 * (2 * math.pi - math.pi/5.0)

    ct = math.cos(theta)
    if  ct > 0:
        sgn = +1 # copysign is not available to us :(
    else:
        sgn = -1
    x = 290 + math.pow(math.fabs( ct ) , (2.0/n)) * a * sgn

    st = math.sin(theta)
    if  st > 0:
        sgn = +1
    else:
        sgn = -1
    y = 60 + math.pow(math.fabs( st ) , (2.0/n)) * b * sgn

    s2 = Spot(x, y, 0, 1, -1)
    model.addSpotTo(s2, t)

    if s1 is None:
        s1 = s2
        continue

    model.addEdge(s1, s2, -1)
    s1 = s2


#5.

s1 = None
s3 = None
for t in range(0, 21):
    x1 = 340
    y1 = 10 + t * 5
    y2 = y1

    s2 = Spot(x1, y1, 0, 1, -1)
    model.addSpotTo(s2, t)

    if t == 10:
        s4 = s2
    else:
        if t < 10:
            x2 = 400 - t *  6
        else:
            x2 = 340 + (t-10) * 6
        s4 = Spot(x2, y1, 0, 1, -1)

    model.addSpotTo(s4, t)

    if s1 is None:
        s1 = s2
        s3 = s4
        continue

    model.addEdge(s1, s2, -1)
    model.addEdge(s3, s4, -1)
    s1 = s2
    s3 = s4


# 6.
s1 = None
s3 = None
for t in range(0, 6):
    y = 40 - t * 6
    x1 = 450 - t * 8
    x2 = 450 + t * 8

    s2 = Spot(x1, y, 0, 1, -1)
    model.addSpotTo(s2, t)
    if s1 is None:
        s1 = s2
        s3 = s2
        continue

    s4 = Spot(x2, y, 0, 1, -1)
    model.addSpotTo(s4, t)
    model.addEdge(s1, s2, -1)
    model.addEdge(s3, s4, -1)
    s1 = s2
    s3 = s4

# If, at this stage, you start thinking I have too much free time,
# know that I don't.

for t in range(6, 21):
    x1 = 410
    x2 = 490
    y = 10 + (t-6) * 7
    s2 = Spot(x1, y, 0, 1, -1)
    s4 = Spot(x2, y, 0, 1, -1)
    model.addSpotTo(s2, t)
    model.addSpotTo(s4, t)
    model.addEdge(s1, s2, -1)
    model.addEdge(s3, s4, -1)
    s1 = s2
    s3 = s4


# 7.
# The power of copy-paste

s1 = None
for t in range(0, 21):
    if s1 is None:
        s1 = Spot(510, 110, 0, 1, -1)
        model.addSpotTo(s1, t)
        continue

    x = 510+ t * 4
    if t < 10:
        y = 110 - t * 10
    else:
        y = 10 + (t-10) * 10
    s2 = Spot(x, y, 0, 1, -1)   
    model.addSpotTo(s2, t)
    model.addEdge(s1, s2, -1)
    s1 = s2

    if t == 5:
        start = s2
    if t == 15:
        end = s2

s1 = start
for t in range(6, 15):
    x = 534 + (t-5) * 3.5
    s2 = Spot(x, 60, 0, 1, -1)
    model.addSpotTo(s2, t)
    model.addEdge(s1, s2, -1)
    s1 = s2

model.addEdge(s2, end, -1)


# 8.
# The power of copy-paste
# At this stage I wish Nick named this plugin Tatata.

s1 = None
for t in range(0, 5):
    x = 590 + t * 10
    if s1 is None:
        s1 = Spot(x, 10, 0, 1, -1)
        model.addSpotTo(s1, t)
        continue


    s2 = Spot(x, 10, 0, 1, -1)
    model.addSpotTo(s2, t)
    model.addEdge(s1, s2, -1)
    s1 = s2


middle = s2

s1 = s2
for t in range(0, 4):
    x = 640 + t * 10
    s2 = Spot(x, 10, 0, 1, -1)
    model.addSpotTo(s2, t + 5)
    model.addEdge(s1, s2, -1)
    s1 = s2

s1 = middle
for t in range(0, 16):
    y = 20 + t * 6
    s2 = Spot(630, y, 0, 1, -1)
    model.addSpotTo(s2, t + 5)
    model.addEdge(s1, s2, -1)
    s1 = s2


# 9.
s1 = None
for t in range(0, 6):
    y = 60
    x = 720 - t * 8
    s2 = Spot(x, y, 0, 1, -1)
    model.addSpotTo(s2, t)

    if s1 is None:
        s1 = s2
        continue

    model.addEdge(s1, s2, -1)
    s1 = s2

s3 = s1
for t in range(6, 11):
    x = 680
    y1 = 60 + (t-5) * 10
    y2 = 60 - (t-5) * 10
    s2 = Spot(x, y1, 0, 1, -1)
    s4 = Spot(x, y2, 0, 1, -1)
    model.addSpotTo(s2, t)
    model.addSpotTo(s4, t)
    model.addEdge(s1, s2, -1)
    model.addEdge(s3, s4, -1)
    s1 = s2
    s3 = s4

for t in range(11, 21):
    y1 = 110
    y2 = 10
    x = 680 + (t-10) * 5
    s2 = Spot(x, y1, 0, 1, -1)
    s4 = Spot(x, y2, 0, 1, -1)
    model.addSpotTo(s2, t)
    model.addSpotTo(s4, t)
    model.addEdge(s1, s2, -1)
    model.addEdge(s3, s4, -1)
    s1 = s2
    s3 = s4

# Change the radius of the last one, so that we create some blank
# space around the model for display.
s2.putFeature('RADIUS', 20)


# Commit all of this.
model.endUpdate()
# This actually triggers the features to be recalculated.


# Prepare display.
sm = SelectionModel(model)

# We will edit the display settings for some eye candy.
# Read the default display settings.
ds = DisplaySettingsIO.readUserDefault()

# With the line below, we state that we want to color tracks using
# a numerical feature defined for TRACKS, and that has they key 'TRACK_INDEX'.
ds.setTrackColorBy( TrackMateObject.TRACKS, 'TRACK_INDEX' )

# With the line below, we state that we want to color spots using
# a numerical feature defined for TRACKS, and that has they key 'TRACK_INDEX'.
# Basically we stated that we want to color spots in the same way that we
# colored tracks 6 lines above.
ds.setSpotColorBy( TrackMateObject.TRACKS, 'TRACK_INDEX' )

# Now we want to display tracks as comets or 'dragon tails'. That is:
# tracks should fade in time.
ds.setTrackDisplayMode( TrackDisplayMode.LOCAL_BACKWARD )

# Big lines.
ds.setLineThickness( 3. )

# The TrackScheme view is a bit hard to interpret.
trackscheme = TrackScheme( model, sm, ds )
trackscheme.render()

# You can create an hyperstack viewer without specifying any ImagePlus.
# It will then create a dummy one tuned to display the model content.
view = HyperStackDisplayer(model, sm, ds )
view.render()

# Animate it a bit
imp = view.getImp()
imp.getCalibration().fps = 30
# Set to loop back and forth
imp.getCalibration().setLoopBackAndForth( True )
Animator().run('start')
