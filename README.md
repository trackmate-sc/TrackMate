[![](https://travis-ci.org/fiji/TrackMate.svg?branch=master)](https://travis-ci.org/fiji/TrackMate)

![TrackMate logo](http://imagej.net/_images/0/0c/TrackMate-Logo85x50-color-300p.png)


TrackMate
=========

TrackMate is your buddy for your everyday tracking.


Citation
--------

Please note that TrackMate is available through Fiji, and is based on a publication. If you use it successfully for your research please be so kind to cite our work:

Jean-Yves Tinevez, Nick Perry, Johannes Schindelin, Genevieve M. Hoopes, Gregory D. Reynolds, Emmanuel Laplantine, Sebastian Y. Bednarek, Spencer L. Shorte, Kevin W. Eliceiri, __TrackMate: An open and extensible platform for single-particle tracking__, Methods, Available online 3 October 2016, ISSN 1046-2023, http://dx.doi.org/10.1016/j.ymeth.2016.09.016. (http://www.sciencedirect.com/science/article/pii/S1046202316303346)


Single Particle Tracking
------------------------

TrackMate provides the tools to perform single particle tracking (SPT). SPT is
an image analysis challenge where the goal is to segment and follow over time
some labelled, spot-like structures. Each spot is segmented in multiple frames
and its trajectory is reconstructed by assigning it an identity over these
frames, in the shape of a track. These tracks can then be either visualized or
yield further analysis results such as velocity, total displacement, diffusion
characteristics, division events, etc...

TrackMate can deal with single particles, or spot-like objects. They are bright
objects over a dark background for which the object contour shape is not
important, but for which the main information can be extracted from the X,Y,Z
coordinates over time. Examples include sub-resolution fluorescent spots,
labelled traffic vesicles, nuclei or cells imaged at low resolution.

Though these objects are solely represented by a X,Y,Z,T coordinates array,
TrackMate can compute numerical features for each spot, given its coordinates
and a radius. For instance, the mean, max, min and median intensity will be
computed, as well as the estimated radius and orientation for each spot,
allowing to follow how these feature evolves over time for one object.


TrackMate goals
---------------

Its development focuses on achieving two concomitant goals:

### For users ###

TrackMate aims at offering a generic solution that works out of the box,
through a simple and sensible user interface.

The tracking process is divided in a series of steps, through which you will be
guided thanks to a wizard-like GUI. It privileges tracking schemes where the
segmentation step is decoupled from the particle-linking step.

The segmentation / filtering / particle-linking processes and results are
visualized immediately in 2D or 3D, allowing to judge their efficiency and
adjust their control parameters. The visualization tools are the one shipped
with Fiji and interact nicely with others plugin.

Several automated segmentation and linking algorithms are provided. But you are
also offered to edit the results manually, or even to completely skip the
automatic steps, and perform fully manual segmentation and/or linking.

Some tools for track and spot analysis are included. Various plots can be made
directly from the plugin and for instance used to derive numerical results from
the tracks. If they are not enough, functions are provided to export the whole
results to other analysis software such as MATLAB.

TrackMate relies on several different libraries and plugins for data
manipulation, analysis and visualization. This can be a pitfall when
distributing a complex plugin, but this is where the Fiji magic comes into
play. All dependencies are dealt with by through the Fiji updater. Installing
TrackMate is easy as calling the Fiji Updater, and the plugin must work out of
the box. If this does not work for you, then it is a bug and we commit to fix
it.

A strong emphasis is made on performance, and TrackMate will take advantage of
multi-cores hardware.

### For developers ###

Have you ever wanted to develop your own segmentation and/or particle-linking
algorithm, but wanted to avoid the painful burden to also write a GUI, several
visualization tools, analysis tools and exporting facilities? Then TrackMate is
for you.

We spent a considerable amount of time making TrackMate extensible in every
aspect. It has a very modular design, that makes it easy to extend. You can for
instance develop your own segmentation algorithm, extend TrackMate to include
it, and benefit from the visualization tools and the GUI already there. Here is
a list of the components you can extend and customize:

* detection algorithms
* particle-linking algorithms
* numerical features for spots (such as mean intensity, etc..)
* numerical features for links (such as velocity, orientation, etc..)
* numerical features for tracks (total displacement, length, etc...)
* visualization tools
* post-processing actions (exporting, data massaging, etc...)

You can even modify the GUI, and remove, edit or insert new steps in the
wizard. This can be useful for instance if you want to implement a tracking
scheme that solves simultaneously the segmentation part and the particle
linking part, but still want to take advantage of TrackMate components.

Do you want to make your new algorithms usable by the reviewers of your
submitted paper? Upload your extended version of TrackMate to a private update
site, as explained here, then send the link to the reviewers. Now that the
paper has been accepted (congratulations), you want to make it accessible to
anyone? Just put the link to the update site in the article. All of this can
happen without us even noticing.

TrackMate was developed to serve as a tool for Life-Science image analysis
community, so that new tracking tools can be developed more easily and quickly,
and so that end-users can use them to perform their own research. We will
support you if need help to reuse it.

For details, please see http://fiji.sc/TrackMate
