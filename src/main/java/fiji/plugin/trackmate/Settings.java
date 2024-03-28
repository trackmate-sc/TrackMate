/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.features.FeatureAnalyzer;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.Spot2DMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.Spot3DMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileInfo;

/**
 * This class is used to store user settings for the {@link TrackMate}
 * trackmate. It is simply made of public fields
 */
public class Settings
{

	/**
	 * The ImagePlus to operate on. Will also be used by some
	 * {@link fiji.plugin.trackmate.visualization.TrackMateModelView} as a GUI
	 * target.
	 */
	public final ImagePlus imp;

	public double dt;

	public double dx;

	public double dy;

	public double dz;

	public int width;

	public int height;

	public int nslices;

	public int nframes;

	public String imageFolder;

	public String imageFileName;

	/**
	 * The region of interest (ROI). This will be used to crop the image and to
	 * discard found spots outside the ROI. If <code>null</code>, the whole
	 * image is considered.
	 */
	private Roi roi;

	// Crop cube
	/**
	 * The time-frame index, <b>0-based</b>, of the first time-point to process.
	 */
	public int tstart;

	/**
	 * The time-frame index, <b>0-based</b>, of the last time-point to process.
	 */
	public int tend;

	/**
	 * The lowest pixel Z position, <b>0-based</b>, of the volume to process.
	 */
	public int zstart;

	/**
	 * The lowest pixel Z position, <b>0-based</b>, of the volume to process.
	 */
	public int zend;

	/**
	 * The name of the detector factory to use. It will be used to generate
	 * {@link fiji.plugin.trackmate.detection.SpotDetector} for each target
	 * frame.
	 */
	public SpotDetectorFactoryBase< ? > detectorFactory;

	/** The the tracker to use. */
	public SpotTrackerFactory trackerFactory;

	/**
	 * Settings map for {@link fiji.plugin.trackmate.detection.SpotDetector}.
	 *
	 * @see fiji.plugin.trackmate.detection.DetectorKeys
	 */
	public Map< String, Object > detectorSettings = new HashMap<>();

	/**
	 * Settings map for {@link fiji.plugin.trackmate.tracking.SpotTracker}.
	 *
	 * @see fiji.plugin.trackmate.tracking.TrackerKeys
	 */
	public Map< String, Object > trackerSettings = new HashMap<>();

	// Filters

	/**
	 * The feature filter list.
	 */
	protected List< FeatureFilter > spotFilters = new ArrayList<>();

	/**
	 * The initial quality filter value that is used to clip spots of low
	 * quality from spots.
	 */
	public Double initialSpotFilterValue = Double.valueOf( 0 );

	/** The track filter list that is used to prune track and spots. */
	protected List< FeatureFilter > trackFilters = new ArrayList<>();

	protected String errorMessage;

	// Spot features

	/**
	 * The {@link SpotAnalyzerFactory}s that will be used to compute spot
	 * features. They are ordered in a {@link List} in case some analyzers
	 * requires the results of another analyzer to proceed.
	 */
	protected List< SpotAnalyzerFactoryBase< ? > > spotAnalyzerFactories = new ArrayList<>();

	// Edge features

	/**
	 * The {@link EdgeAnalyzer}s that will be used to compute edge features.
	 * They are ordered in a {@link List} in case some analyzers requires the
	 * results of another analyzer to proceed.
	 */
	protected List< EdgeAnalyzer > edgeAnalyzers = new ArrayList<>();

	// Track features

	/**
	 * The {@link TrackAnalyzer}s that will be used to compute track features.
	 * They are ordered in a {@link List} in case some analyzers requires the
	 * results of another analyzer to proceed.
	 */
	protected List< TrackAnalyzer > trackAnalyzers = new ArrayList<>();

	/*
	 * CONSTRUCTOR.
	 */

	public Settings()
	{
		this( null );
	}

	public Settings( final ImagePlus imp )
	{
		this.imp = imp;
		if ( null == imp )
		{
			this.dx = 1.;
			this.dy = 1.;
			this.dz = 1.;
			this.dt = 1.;
			this.width = 0;
			this.height = 0;
			this.imageFileName = "";
			this.imageFolder = "";
			this.nframes = 0;
			this.nslices = 0;
			return;
		}

		// File info
		final FileInfo fileInfo = imp.getOriginalFileInfo();
		if ( null != fileInfo )
		{
			this.imageFileName = fileInfo.fileName;
			this.imageFolder = fileInfo.directory;
		}
		else
		{
			this.imageFileName = imp.getShortTitle();
			this.imageFolder = "";

		}
		// Image size
		this.width = imp.getWidth();
		this.height = imp.getHeight();
		this.nslices = imp.getNSlices();
		this.nframes = imp.getNFrames();
		this.dx = imp.getCalibration().pixelWidth;
		this.dy = imp.getCalibration().pixelHeight;
		this.dz = imp.getCalibration().pixelDepth;
		final double ldt = imp.getCalibration().frameInterval;
		this.dt = ( ldt == 0 ) ? 1. : ldt;

		// Crop cube
		this.zstart = 0;
		this.zend = imp.getNSlices() - 1;
		this.tstart = 0;
		this.tend = imp.getNFrames() - 1;
		setRoi( imp.getRoi() );
		// The rest is left to the user
	}

	public Roi getRoi()
	{
		return roi;
	}

	public void setRoi( final Roi roi )
	{
		this.roi = roi;
	}

	public int getXstart()
	{
		return roi == null ? 0 : roi.getBounds().x;
	}

	public int getXend()
	{
		return roi == null ? width - 1 : roi.getBounds().x + roi.getBounds().width;
	}

	public int getYstart()
	{
		return roi == null ? 0 : roi.getBounds().y;
	}

	public int getYend()
	{
		return roi == null ? height - 1 : roi.getBounds().y + roi.getBounds().height;
	}

	/**
	 * Copy a settings objects based on this instance, but configured to run on
	 * the specified image. Detector and tracker factories and settings are
	 * copied, as well as filters, etc. The exception are analyzers: all the
	 * analyzers that are found at runtime are added, regardless of the content
	 * of the instance to copy.
	 *
	 * @param newImp
	 *            the image to copy the settings for.
	 * @return a new settings object.
	 */
	public Settings copyOn( final ImagePlus newImp )
	{
		final Settings newSettings = new Settings( newImp );
		if ( detectorFactory != null )
			newSettings.detectorFactory = detectorFactory.copy();
		newSettings.detectorSettings = new HashMap<>( detectorSettings );
		if ( trackerFactory != null )
			newSettings.trackerFactory = trackerFactory.copy();
		newSettings.trackerSettings = new HashMap<>( trackerSettings );
		newSettings.initialSpotFilterValue = initialSpotFilterValue;

		newSettings.spotFilters = new ArrayList<>();
		for ( final FeatureFilter filter : spotFilters )
			newSettings.spotFilters.add( new FeatureFilter( filter.feature, filter.value, filter.isAbove ) );

		newSettings.trackFilters = new ArrayList<>();
		for ( final FeatureFilter filter : trackFilters )
			newSettings.trackFilters.add( new FeatureFilter( filter.feature, filter.value, filter.isAbove ) );

		// Exception: we add all analyzers, regardless of the persistence.
		newSettings.addAllAnalyzers();
		return newSettings;
	}

	/*
	 * METHODS
	 */

	/**
	 * Returns a string description of the target image.
	 *
	 * @return a string representation of the target image.
	 */
	public String toStringImageInfo()
	{
		final StringBuilder str = new StringBuilder();

		str.append( "Image data:\n" );
		if ( null == imp )
		{
			str.append( "Source image not set.\n" );
		}
		else
		{
			str.append( "For the image named: " + imp.getTitle() + ".\n" );
		}
		if ( imageFileName == null || imageFileName == "" )
		{
			str.append( "Not matching any file.\n" );
		}
		else
		{
			str.append( "Matching file " + imageFileName + " " );
			if ( imageFolder == null || imageFolder == "" )
			{
				str.append( "in current folder.\n" );
			}
			else
			{
				str.append( "in folder: " + imageFolder + "\n" );
			}
		}

		str.append( "Geometry:\n" );
		str.append( String.format( "  X = %4d - %4d, dx = %g\n", getXstart(), getXend(), dx ) );
		str.append( String.format( "  Y = %4d - %4d, dy = %g\n", getYstart(), getYend(), dy ) );
		str.append( String.format( "  Z = %4d - %4d, dz = %g\n", zstart, zend, dz ) );
		str.append( String.format( "  T = %4d - %4d, dt = %g\n", tstart, tend, dt ) );

		return str.toString();
	}

	public String toStringFeatureAnalyzersInfo()
	{
		final StringBuilder str = new StringBuilder();

		if ( spotAnalyzerFactories.isEmpty() )
		{
			str.append( "No spot feature analyzers.\n" );
		}
		else
		{
			str.append( "Spot feature analyzers:\n" );
			prettyPrintFeatureAnalyzer( spotAnalyzerFactories, str );
		}

		if ( edgeAnalyzers.isEmpty() )
		{
			str.append( "No edge feature analyzers.\n" );
		}
		else
		{
			str.append( "Edge feature analyzers:\n" );
			prettyPrintFeatureAnalyzer( edgeAnalyzers, str );
		}

		if ( trackAnalyzers.isEmpty() )
		{
			str.append( "No track feature analyzers.\n" );
		}
		else
		{
			str.append( "Track feature analyzers:\n" );
			prettyPrintFeatureAnalyzer( trackAnalyzers, str );
		}

		return str.toString();
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder();

		str.append( toStringImageInfo() );

		str.append( '\n' );
		str.append( "Spot detection:\n" );
		if ( null == detectorFactory )
		{
			str.append( "No detector factory set.\n" );
		}
		else
		{
			str.append( "Detector: " + detectorFactory.toString() + ".\n" );
			if ( null == detectorSettings )
			{
				str.append( "No detector settings found.\n" );
			}
			else
			{
				str.append( "Detector settings:\n" );
				str.append( detectorSettings );
				str.append( '\n' );
			}
		}

		str.append( '\n' );
		str.append( toStringFeatureAnalyzersInfo() );

		str.append( '\n' );
		str.append( "Initial spot filter:\n" );
		if ( null == initialSpotFilterValue )
		{
			str.append( "No initial quality filter.\n" );
		}
		else
		{
			str.append( "Initial quality filter value: " + initialSpotFilterValue + ".\n" );
		}

		str.append( '\n' );
		str.append( "Spot feature filters:\n" );
		if ( spotFilters == null || spotFilters.size() == 0 )
		{
			str.append( "No spot feature filters.\n" );
		}
		else
		{
			str.append( "Set with " + spotFilters.size() + " spot feature filters:\n" );
			for ( final FeatureFilter featureFilter : spotFilters )
			{
				str.append( " - " + featureFilter + "\n" );
			}
		}

		str.append( '\n' );
		str.append( "Particle linking:\n" );
		if ( null == trackerFactory )
		{
			str.append( "No spot tracker set.\n" );
		}
		else
		{
			str.append( "Tracker: " + trackerFactory.toString() + ".\n" );
			if ( null == trackerSettings )
			{
				str.append( "No tracker settings found.\n" );
			}
			else
			{
				str.append( "Tracker settings:\n" );
				str.append( trackerSettings );
				str.append( '\n' );
			}
		}

		str.append( '\n' );
		str.append( "Track feature filters:\n" );
		if ( trackFilters == null || trackFilters.size() == 0 )
		{
			str.append( "No track feature filters.\n" );
		}
		else
		{
			str.append( "Set with " + trackFilters.size() + " track feature filters:\n" );
			for ( final FeatureFilter featureFilter : trackFilters )
			{
				str.append( " - " + featureFilter + "\n" );
			}
		}

		return str.toString();
	}

	public boolean checkValidity()
	{
		if ( null == imp )
		{
			errorMessage = "The source image is null.\n";
			return false;
		}
		if ( null == detectorFactory )
		{
			errorMessage = "The detector factory is null.\n";
			return false;
		}
		if ( null == detectorSettings )
		{
			errorMessage = "The detector settings is null.\n";
			return false;
		}
		if ( null == initialSpotFilterValue )
		{
			errorMessage = "Initial spot quality threshold is not set.\n";
			return false;
		}
		if ( null == trackerFactory )
		{
			errorMessage = "The tracker factory is null.\n";
			return false;
		}
		if ( !trackerFactory.checkSettingsValidity( trackerSettings ) )
		{
			errorMessage = "The tracker has invalid input:\n" + trackerFactory.getErrorMessage();
			return false;
		}
		return true;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	/*
	 * ALL FEATURES.
	 */

	/**
	 * All the spot analyzers, edge analyzers and track analyzers discovered at
	 * runtime. This method is a convenience for scripting, that simply
	 * discovers and adds all the analyzers it can find.
	 */
	public void addAllAnalyzers()
	{
		// Base spot analyzers.
		final SpotAnalyzerProvider spotAnalyzerProvider = new SpotAnalyzerProvider( imp == null ? 1 : imp.getNChannels() );
		final List< String > spotAnalyzerKeys = spotAnalyzerProvider.getKeys();
		for ( final String key : spotAnalyzerKeys )
			addSpotAnalyzerFactory( spotAnalyzerProvider.getFactory( key ) );

		// Shall we add 2D morphology analyzers?
		if ( imp != null && DetectionUtils.is2D( imp ) && detectorFactory != null && detectorFactory.has2Dsegmentation() )
		{
			final Spot2DMorphologyAnalyzerProvider spotMorphologyAnalyzerProvider = new Spot2DMorphologyAnalyzerProvider( imp.getNChannels() );
			final List< String > spotMorphologyAnaylyzerKeys = spotMorphologyAnalyzerProvider.getKeys();
			for ( final String key : spotMorphologyAnaylyzerKeys )
				addSpotAnalyzerFactory( spotMorphologyAnalyzerProvider.getFactory( key ) );
		}

		// Shall we add 3D morphology analyzers?
		if ( imp != null && !DetectionUtils.is2D( imp ) && detectorFactory != null && detectorFactory.has3Dsegmentation() )
		{
			final Spot3DMorphologyAnalyzerProvider spotMorphologyAnalyzerProvider = new Spot3DMorphologyAnalyzerProvider( imp.getNChannels() );
			final List< String > spotMorphologyAnaylyzerKeys = spotMorphologyAnalyzerProvider.getKeys();
			for ( final String key : spotMorphologyAnaylyzerKeys )
				addSpotAnalyzerFactory( spotMorphologyAnalyzerProvider.getFactory( key ) );
		}

		// Edge analyzers.
		final EdgeAnalyzerProvider edgeAnalyzerProvider = new EdgeAnalyzerProvider();
		final List< String > edgeAnalyzerKeys = edgeAnalyzerProvider.getKeys();
		for ( final String key : edgeAnalyzerKeys )
			addEdgeAnalyzer( edgeAnalyzerProvider.getFactory( key ) );

		// Track analyzers.
		final TrackAnalyzerProvider trackAnalyzerProvider = new TrackAnalyzerProvider();
		final List< String > trackAnalyzerKeys = trackAnalyzerProvider.getKeys();
		for ( final String key : trackAnalyzerKeys )
			addTrackAnalyzer( trackAnalyzerProvider.getFactory( key ) );
	}

	/*
	 * SPOT FEATURES
	 */

	/**
	 * Remove any {@link SpotAnalyzerFactory} to this object.
	 */
	public void clearSpotAnalyzerFactories()
	{
		spotAnalyzerFactories.clear();
	}

	/**
	 * Returns a copy of the list of {@link SpotAnalyzerFactory}s configured in
	 * this settings object. They are returned in an ordered list, to enforce
	 * processing order in case some analyzers requires the results of another
	 * analyzers to proceed.
	 *
	 * @return the list of {@link SpotAnalyzerFactory}s.
	 */
	public List< SpotAnalyzerFactoryBase< ? > > getSpotAnalyzerFactories()
	{
		return new ArrayList<>( spotAnalyzerFactories );
	}

	/**
	 * Adds a {@link SpotAnalyzerFactory} to the {@link List} of spot analyzers
	 * configured.
	 *
	 * @param spotAnalyzer
	 *            the {@link fiji.plugin.trackmate.features.spot.SpotAnalyzer}
	 *            to add, at the end of the list.
	 */
	public void addSpotAnalyzerFactory( final SpotAnalyzerFactoryBase< ? > spotAnalyzer )
	{
		if ( contains( spotAnalyzer ) )
			return;
		spotAnalyzerFactories.add( spotAnalyzer );
	}

	/**
	 * Adds a {@link SpotAnalyzerFactory} to the {@link List} of spot analyzers
	 * configured, at the specified index.
	 *
	 * @param index
	 *            index at which the analyzer is to be inserted.
	 * @param spotAnalyzer
	 *            the {@link fiji.plugin.trackmate.features.spot.SpotAnalyzer}
	 *            to add, at the specified index in the list.
	 */
	public void addSpotAnalyzerFactory( final int index, final SpotAnalyzerFactory< ? > spotAnalyzer )
	{
		if ( contains( spotAnalyzer ) )
			return;
		spotAnalyzerFactories.add( index, spotAnalyzer );
	}

	/**
	 * Removes the specified {@link SpotAnalyzerFactory} from the analyzers
	 * configured.
	 *
	 * @param spotAnalyzer
	 *            the {@link SpotAnalyzerFactory} to remove.
	 * @return true if the specified {@link SpotAnalyzerFactory} was in the list
	 *         and was removed.
	 */
	public boolean removeSpotAnalyzerFactory( final SpotAnalyzerFactory< ? > spotAnalyzer )
	{
		return spotAnalyzerFactories.remove( spotAnalyzer );
	}

	private boolean contains( final SpotAnalyzerFactoryBase< ? > spotAnalyzer )
	{
		for ( final SpotAnalyzerFactoryBase< ? > saf : spotAnalyzerFactories )
			if ( saf.getKey().equals( spotAnalyzer.getKey() ) )
				return true;

		return false;
	}

	/*
	 * EDGE FEATURE ANALYZERS
	 */

	/**
	 * Remove any {@link EdgeAnalyzer} to this object.
	 */
	public void clearEdgeAnalyzers()
	{
		edgeAnalyzers.clear();
	}

	/**
	 * Returns a copy of the list of {@link EdgeAnalyzer}s configured in this
	 * settings object. They are returned in an ordered list, to enforce
	 * processing order in case some analyzers requires the results of another
	 * analyzers to proceed.
	 *
	 * @return the list of {@link EdgeAnalyzer}s.
	 */
	public List< EdgeAnalyzer > getEdgeAnalyzers()
	{
		return new ArrayList<>( edgeAnalyzers );
	}

	/**
	 * Adds a {@link EdgeAnalyzer} to the {@link List} of edge analyzers
	 * configured.
	 *
	 * @param edgeAnalyzer
	 *            the {@link EdgeAnalyzer} to add, at the end of the list.
	 */
	public void addEdgeAnalyzer( final EdgeAnalyzer edgeAnalyzer )
	{
		if ( contains( edgeAnalyzer ) )
			return;
		edgeAnalyzers.add( edgeAnalyzer );
	}

	/**
	 * Adds a {@link EdgeAnalyzer} to the {@link List} of edge analyzers
	 * configured, at the specified index.
	 *
	 * @param index
	 *            index at which the analyzer is to be inserted.
	 *
	 * @param edgeAnalyzer
	 *            the {@link EdgeAnalyzer} to add, at the specified index in the
	 *            list.
	 */
	public void addEdgeAnalyzer( final int index, final EdgeAnalyzer edgeAnalyzer )
	{
		if ( contains( edgeAnalyzer ) )
			return;
		edgeAnalyzers.add( index, edgeAnalyzer );
	}

	/**
	 * Removes the specified {@link EdgeAnalyzer} from the analyzers configured.
	 *
	 * @param edgeAnalyzer
	 *            the {@link EdgeAnalyzer} to remove.
	 * @return true if the specified {@link EdgeAnalyzer} was in the list and
	 *         was removed.
	 */
	public boolean removeEdgeAnalyzer( final EdgeAnalyzer edgeAnalyzer )
	{
		return edgeAnalyzers.remove( edgeAnalyzer );
	}

	private boolean contains( final EdgeAnalyzer edgeAnalyzer )
	{
		for ( final EdgeAnalyzer ea : edgeAnalyzers )
			if ( ea.getKey().equals( edgeAnalyzer.getKey() ) )
				return true;

		return false;
	}

	/*
	 * TRACK FEATURE ANALYZERS
	 */

	/**
	 * Remove any {@link TrackAnalyzer} to this object.
	 */
	public void clearTrackAnalyzers()
	{
		trackAnalyzers.clear();
	}

	/**
	 * Returns a copy of the list of {@link TrackAnalyzer}s configured in this
	 * settings object. They are returned in an ordered list, to enforce
	 * processing order in case some analyzers requires the results of another
	 * analyzers to proceed.
	 *
	 * @return the list of {@link TrackAnalyzer}s.
	 */
	public List< TrackAnalyzer > getTrackAnalyzers()
	{
		return new ArrayList<>( trackAnalyzers );
	}

	/**
	 * Adds a {@link TrackAnalyzer} to the {@link List} of track analyzers
	 * configured.
	 *
	 * @param trackAnalyzer
	 *            the {@link TrackAnalyzer} to add, at the end of the list.
	 */
	public void addTrackAnalyzer( final TrackAnalyzer trackAnalyzer )
	{
		if ( contains( trackAnalyzer ) )
			return;
		trackAnalyzers.add( trackAnalyzer );
	}

	/**
	 * Adds a {@link TrackAnalyzer} to the {@link List} of track analyzers
	 * configured, at the specified index.
	 *
	 * @param index
	 *            index at which the analyzer is to be inserted.
	 *
	 * @param trackAnalyzer
	 *            the {@link TrackAnalyzer} to add, at the specified index in
	 *            the list.
	 */
	public void addTrackAnalyzer( final int index, final TrackAnalyzer trackAnalyzer )
	{
		if ( contains( trackAnalyzer ) )
			return;
		trackAnalyzers.add( index, trackAnalyzer );
	}

	/**
	 * Removes the specified {@link TrackAnalyzer} from the analyzers
	 * configured.
	 *
	 * @param trackAnalyzer
	 *            the {@link TrackAnalyzer} to remove.
	 * @return true if the specified {@link TrackAnalyzer} was in the list and
	 *         was removed.
	 */
	public boolean removeTrackAnalyzer( final TrackAnalyzer trackAnalyzer )
	{
		return trackAnalyzers.remove( trackAnalyzer );
	}

	private boolean contains( final TrackAnalyzer trackAnalyzer )
	{
		for ( final TrackAnalyzer ta : trackAnalyzers )
			if ( ta.getKey().equals( trackAnalyzer.getKey() ) )
				return true;

		return false;
	}

	/*
	 * FEATURE FILTERS
	 */

	/**
	 * Add a filter to the list of spot filters.
	 *
	 * @param filter
	 *            the filter to add.
	 */
	public void addSpotFilter( final FeatureFilter filter )
	{
		spotFilters.add( filter );
	}

	public void removeSpotFilter( final FeatureFilter filter )
	{
		spotFilters.remove( filter );
	}

	/** Remove all spot filters stored in this model. */
	public void clearSpotFilters()
	{
		spotFilters.clear();
	}

	public List< FeatureFilter > getSpotFilters()
	{
		return spotFilters;
	}

	public void setSpotFilters( final List< FeatureFilter > spotFilters )
	{
		this.spotFilters = spotFilters;
	}

	/**
	 * Add a filter to the list of track filters.
	 *
	 * @param filter
	 *            the filter to add.
	 */
	public void addTrackFilter( final FeatureFilter filter )
	{
		trackFilters.add( filter );
	}

	public void removeTrackFilter( final FeatureFilter filter )
	{
		trackFilters.remove( filter );
	}

	/** Remove all track filters stored in this model. */
	public void clearTrackFilters()
	{
		trackFilters.clear();
	}

	public List< FeatureFilter > getTrackFilters()
	{
		return trackFilters;
	}

	public void setTrackFilters( final List< FeatureFilter > trackFilters )
	{
		this.trackFilters = trackFilters;
	}

	/*
	 * PRIVATE METHODS
	 */

	public static final void prettyPrintFeatureAnalyzer( final List< ? extends FeatureAnalyzer > analyzers, final StringBuilder str )
	{
		for ( final FeatureAnalyzer analyzer : analyzers )
		{
			str.append( " - " + analyzer.getName() + " provides: " );
			for ( final String feature : analyzer.getFeatures() )
				str.append( analyzer.getFeatureShortNames().get( feature ) + ", " );

			str.deleteCharAt( str.length() - 1 );
			str.deleteCharAt( str.length() - 1 );
			// be precise
			if ( str.charAt( str.length() - 1 ) != '.' )
				str.append( '.' );

			// manual?
			if ( analyzer.isManualFeature() )
			{
				str.deleteCharAt( str.length() - 1 );
				str.append( "; is manual." );
			}
			str.append( '\n' );
		}
	}
}
