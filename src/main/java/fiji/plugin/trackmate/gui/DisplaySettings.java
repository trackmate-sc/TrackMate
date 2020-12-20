package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.features.FeatureUtils.USE_UNIFORM_COLOR_KEY;

import java.awt.Color;
import java.util.Objects;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.scijava.listeners.Listeners;

import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;

public class DisplaySettings
{

	/** The display settings name. */
	private String name;

	/** The uniform color for spots. */
	private Color spotUniformColor;

	private ObjectType spotColorByType;

	private String spotColorByFeature;

	private double spotDisplayRadius;

	private boolean spotDisplayedAsRoi;

	private double spotMin;

	private double spotMax;

	private boolean spotShowName;

	private double trackMin;

	private double trackMax;

	private ObjectType trackColorByType;

	private String trackColorByFeature;

	/** The uniform track color. */
	private Color trackUniformColor;

	/**
	 * The color to use to paint objects for which a feature is undefined.
	 * <i>E.g.</i> the numerical feature for this object calculated
	 * automatically, but its returned value is {@link Double#NaN}.
	 */
	private Color undefinedValueColor;

	/**
	 * The color to use to paint objects with a manual feature that has not been
	 * assigned yet. <i>E.g</i> the coloring uses a manual feature, but this
	 * object did not receive a value yet.
	 */
	private Color missingValueColor;

	/** The color for highlighting. */
	private Color highlightColor;

	/** The track display mode. */
	private TrackDisplayMode trackDisplayMode;

	/** The color map. */
	private InterpolatePaintScale colormap;

	/** Whether we should limit the Z drawing depth. */
	private boolean limitZDrawingDepth;

	/** The Z drawing depth, in image units. */
	private double drawingZDepth;

	/** Whether we should limit time range when drawing tracks. */
	private boolean fadeTracks;

	/** The frame limit. */
	private int fadeTrackRange;

	/** Whether to use antialiasing (for drawing everything). */
	private boolean useAntialiasing;

	private boolean spotVisible;

	private boolean trackVisible;

	private final transient Listeners.List< UpdateListener > updateListeners;

	private DisplaySettings()
	{
		this.updateListeners = new Listeners.SynchronizedList<>();
	}

	/**
	 * Returns a new display settings, copied from this instance.
	 *
	 * @param name
	 *            the name for the copied render settings.
	 * @return a new {@link RenderSettings} instance.
	 */
	public DisplaySettings copy( final String name )
	{
		final DisplaySettings rs = new DisplaySettings();
		rs.set( this );
		if ( name != null )
			rs.setName( name );
		return rs;
	}

	public DisplaySettings copy()
	{
		return copy( null );
	}

	public synchronized void set( final DisplaySettings ds )
	{
		name = ds.name;

		spotVisible = ds.spotVisible;
		spotDisplayedAsRoi = ds.spotDisplayedAsRoi;
		spotShowName = ds.spotShowName;
		spotDisplayRadius = ds.spotDisplayRadius;
		spotColorByType = ds.spotColorByType;
		spotColorByFeature = ds.spotColorByFeature;
		spotMin = ds.spotMin;
		spotMax = ds.spotMax;
		spotUniformColor = ds.spotUniformColor;

		trackVisible = ds.trackVisible;
		trackColorByFeature = ds.trackColorByFeature;
		trackColorByType = ds.trackColorByType;
		trackDisplayMode = ds.trackDisplayMode;
		trackMin = ds.trackMin;
		trackMax = ds.trackMax;
		fadeTracks = ds.fadeTracks;
		fadeTrackRange = ds.fadeTrackRange;
		trackUniformColor = ds.trackUniformColor;

		colormap = ds.colormap;
		limitZDrawingDepth = ds.limitZDrawingDepth;
		drawingZDepth = ds.drawingZDepth;
		highlightColor = ds.highlightColor;
		missingValueColor = ds.missingValueColor;
		undefinedValueColor = ds.undefinedValueColor;
		useAntialiasing = ds.useAntialiasing;

		notifyListeners();
	}

	public String getName()
	{
		return name;
	}

	public synchronized void setName( final String name )
	{
		if ( !Objects.equals( this.name, name ) )
		{
			this.name = name;
			notifyListeners();
		}
	}

	public InterpolatePaintScale getColormap()
	{
		return colormap;
	}

	public synchronized void setColormap( final InterpolatePaintScale colormap )
	{
		if ( this.colormap != colormap )
		{
			this.colormap = colormap;
			notifyListeners();
		}
	}

	public boolean isZDrawingDepthLimited()
	{
		return limitZDrawingDepth;
	}

	public synchronized void setZDrawingDepthLimited( final boolean limitZDrawingDepth )
	{
		if ( this.limitZDrawingDepth != limitZDrawingDepth )
		{
			this.limitZDrawingDepth = limitZDrawingDepth;
			notifyListeners();
		}
	}

	public double getZDrawingDepth()
	{
		return drawingZDepth;
	}

	public synchronized void setZDrawingDepth( final double drawingZDepth )
	{
		if ( this.drawingZDepth != drawingZDepth )
		{
			this.drawingZDepth = drawingZDepth;
			notifyListeners();
		}
	}

	public Color getHighlightColor()
	{
		return highlightColor;
	}

	public synchronized void setHighlightColor( final Color highlightColor )
	{
		if ( this.highlightColor != highlightColor )
		{
			this.highlightColor = highlightColor;
			notifyListeners();
		}
	}

	public boolean isFadeTracks()
	{
		return fadeTracks;
	}

	public synchronized void setFadeTracks( final boolean fadeTracks )
	{
		if ( this.fadeTracks != fadeTracks )
		{
			this.fadeTracks = fadeTracks;
			notifyListeners();
		}
	}

	public Color getMissingValueColor()
	{
		return missingValueColor;
	}

	public synchronized void setMissingValueColor( final Color missingValueColor )
	{
		if ( this.missingValueColor != missingValueColor )
		{
			this.missingValueColor = missingValueColor;
			notifyListeners();
		}
	}

	public String getSpotColorByFeature()
	{
		return spotColorByFeature;
	}

	public ObjectType getSpotColorByType()
	{
		return spotColorByType;
	}

	public synchronized void setSpotColorBy( final ObjectType spotColorByType, final String spotColorByFeature )
	{
		boolean fire = false;
		if ( this.spotColorByType != spotColorByType )
		{
			this.spotColorByType = spotColorByType;
			fire = true;
		}
		if ( this.spotColorByFeature != spotColorByFeature )
		{
			this.spotColorByFeature = spotColorByFeature;
			fire = true;
		}
		if ( fire )
			notifyListeners();
	}

	public boolean isSpotDisplayedAsRoi()
	{
		return spotDisplayedAsRoi;
	}

	public synchronized void setSpotDisplayedAsRoi( final boolean spotDisplayedAsRoi )
	{
		if ( this.spotDisplayedAsRoi != spotDisplayedAsRoi )
		{
			this.spotDisplayedAsRoi = spotDisplayedAsRoi;
			notifyListeners();
		}
	}

	public double getSpotDisplayRadius()
	{
		return spotDisplayRadius;
	}

	public synchronized void setSpotDisplayRadius( final double spotDisplayRadius )
	{
		if ( this.spotDisplayRadius != spotDisplayRadius )
		{
			this.spotDisplayRadius = spotDisplayRadius;
			notifyListeners();
		}
	}

	public double getSpotMax()
	{
		return spotMax;
	}

	public double getSpotMin()
	{
		return spotMin;
	}

	public synchronized void setSpotMinMax( final double spotMin, final double spotMax )
	{
		boolean notify = false;
		if ( this.spotMin != spotMin )
		{
			this.spotMin = spotMin;
			notify = true;
		}
		if ( this.spotMax != spotMax )
		{
			this.spotMax = spotMax;
			notify = true;
		}
		if ( notify )
			notifyListeners();
	}

	public boolean isSpotShowName()
	{
		return spotShowName;
	}

	public synchronized void setSpotShowName( final boolean spotShowName )
	{
		if ( this.spotShowName != spotShowName )
		{
			this.spotShowName = spotShowName;
			notifyListeners();
		}
	}

	public boolean isSpotVisible()
	{
		return spotVisible;
	}

	public synchronized void setSpotVisible( final boolean spotsVisible )
	{
		if ( this.spotVisible != spotsVisible )
		{
			this.spotVisible = spotsVisible;
			notifyListeners();
		}
	}

	public Color getSpotUniformColor()
	{
		return spotUniformColor;
	}

	public synchronized void setSpotUniformColor( final Color spotUniformColor )
	{
		if ( this.spotUniformColor != spotUniformColor )
		{
			this.spotUniformColor = spotUniformColor;
			notifyListeners();
		}
	}

	public int getFadeTrackRange()
	{
		return fadeTrackRange;
	}

	public synchronized void setFadeTrackRange( final int fadeTrackRange )
	{
		if ( this.fadeTrackRange != fadeTrackRange )
		{
			this.fadeTrackRange = fadeTrackRange;
			notifyListeners();
		}
	}

	public TrackDisplayMode getTrackDisplayMode()
	{
		return trackDisplayMode;
	}

	public String getTrackColorByFeature()
	{
		return trackColorByFeature;
	}

	public ObjectType getTrackColorByType()
	{
		return trackColorByType;
	}

	public synchronized void setTrackColorBy( final ObjectType trackColorByType, final String trackColorByFeature )
	{
		boolean fire = false;
		if ( this.trackColorByType != trackColorByType )
		{
			this.trackColorByType = trackColorByType;
			fire = true;
		}
		if ( this.trackColorByFeature != trackColorByFeature )
		{
			this.trackColorByFeature = trackColorByFeature;
			fire = true;
		}
		if ( fire )
			notifyListeners();
	}

	public double getTrackMax()
	{
		return trackMax;
	}

	public double getTrackMin()
	{
		return trackMin;
	}

	public synchronized void setTrackMinMax( final double trackMin, final double trackMax )
	{
		boolean notify = false;
		if ( this.trackMin != trackMin )
		{
			this.trackMin = trackMin;
			notify = true;
		}
		if ( this.trackMax != trackMax )
		{
			this.trackMax = trackMax;
			notify = true;
		}
		if ( notify )
			notifyListeners();
	}

	public synchronized void setTrackDisplayMode( final TrackDisplayMode trackDisplayMode )
	{
		if ( this.trackDisplayMode != trackDisplayMode )
		{
			this.trackDisplayMode = trackDisplayMode;
			notifyListeners();
		}
	}

	public boolean isTrackVisible()
	{
		return trackVisible;
	}

	public synchronized void setTrackVisible( final boolean trackVisible )
	{
		if ( this.trackVisible != trackVisible )
		{
			this.trackVisible = trackVisible;
			notifyListeners();
		}
	}

	public Color getTrackUniformColor()
	{
		return trackUniformColor;
	}

	public synchronized void setTrackUniformColor( final Color trackUniformColor )
	{
		if ( this.trackUniformColor != trackUniformColor )
		{
			this.trackUniformColor = trackUniformColor;
			notifyListeners();
		}
	}

	public Color getUndefinedValueColor()
	{
		return undefinedValueColor;
	}

	public synchronized void setUndefinedValueColor( final Color undefinedValueColor )
	{
		if ( this.undefinedValueColor != undefinedValueColor )
		{
			this.undefinedValueColor = undefinedValueColor;
			notifyListeners();
		}
	}

	/**
	 * Get the antialiasing setting.
	 *
	 * @return {@code true} if antialiasing is used.
	 */
	public boolean getUseAntialiasing()
	{
		return useAntialiasing;
	}

	/**
	 * Sets whether to use anti-aliasing for drawing.
	 *
	 * @param useAntialiasing
	 *            whether to use use anti-aliasing.
	 */
	public synchronized void setUseAntialiasing( final boolean useAntialiasing )
	{
		if ( this.useAntialiasing != useAntialiasing )
		{
			this.useAntialiasing = useAntialiasing;
			notifyListeners();
		}
	}

	private void notifyListeners()
	{
		for ( final UpdateListener l : updateListeners.list )
			l.displaySettingsChanged();
	}

	public enum TrackDisplayMode
	{
		/**
		 * Track display mode where the full tracks are drawn.
		 */
		FULL( "Show entire tracks" ),

		/**
		 * Track display mode where the only part of the tracks close to the
		 * current time-point are drawn backward and forward. Edges away from
		 * current time point are faded in the background. How much can be seen
		 * is defined by the value of the frame-display-depth.
		 */
		LOCAL( "Show tracks local in time" ),

		/**
		 * Track display mode where the only part of the tracks close to the
		 * current time-point are drawn, backward only. How much can be seen is
		 * defined by the value of the frame-display-depth.
		 */
		LOCAL_BACKWARD( "Show tracks backward in time" ),

		/**
		 * Track display mode where the only part of the tracks close to the
		 * current time-point are drawn, forward only. How much can be seen is
		 * defined by the value of the frame-display-depth.
		 */
		LOCAL_FORWARD( "Show tracks forward in time" ),

		/**
		 * Track display mode where only the content of the current selection is
		 * displayed.
		 */
		SELECTION_ONLY( "Show selection only" );

		private final String name;

		TrackDisplayMode( final String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	public enum ObjectType
	{
		DEFAULT( "Default" ), SPOTS( "spots" ), EDGES( "edges" ), TRACKS( "tracks" );

		private String name;

		private ObjectType( final String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	public interface UpdateListener
	{
		public void displaySettingsChanged();
	}

	public Listeners.List< UpdateListener > listeners()
	{
		return updateListeners;
	}

	/*
	 * DEFAULTS DISPLAY SETTINGS LIBRARY.
	 */

	private static final DisplaySettings df;
	static
	{
		df = new DisplaySettings();
		df.useAntialiasing = true;
		df.colormap = InterpolatePaintScale.Jet;
		df.limitZDrawingDepth = false;
		df.drawingZDepth = 10.;
		df.fadeTracks = true;
		df.fadeTrackRange = 30;
		df.highlightColor = new Color( 0.2f, 0.9f, 0.2f );
		df.missingValueColor = Color.GRAY.darker();
		df.spotColorByFeature = USE_UNIFORM_COLOR_KEY;
		df.spotColorByType = ObjectType.DEFAULT;
		df.spotDisplayedAsRoi = true;
		df.spotDisplayRadius = 1.;
		df.spotMin = 0.;
		df.spotMax = 10.;
		df.spotShowName = false;
		df.spotUniformColor = new Color( 0.8f, 0.2f, 0.8f );
		df.spotVisible = true;
		df.trackDisplayMode = TrackDisplayMode.FULL;
		df.trackColorByFeature = TrackIndexAnalyzer.TRACK_INDEX;
		df.trackColorByType = ObjectType.DEFAULT;
		df.trackUniformColor = new Color( 0.8f, 0.8f, 0.2f );
		df.trackMin = 0.;
		df.trackMax = 10.;
		df.trackVisible = true;
		df.undefinedValueColor = Color.BLACK;
		df.name = "Default";
	}

	public static DisplaySettings defaultStyle()
	{
		return df;
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder( super.toString() );
		str.append( String.format( "\n%20s: %s", "name", name ) );

		str.append( String.format( "\n%20s: %s", "spotVisible", "" + spotVisible ) );
		str.append( String.format( "\n%20s: %s", "spotDisplayedAsRoi", "" + spotDisplayedAsRoi ) );
		str.append( String.format( "\n%20s: %s", "spotShowName", "" + spotShowName ) );
		str.append( String.format( "\n%20s: %s", "spotDisplayRadius", "" + spotDisplayRadius ) );
		str.append( String.format( "\n%20s: %s", "spotColorByType", spotColorByType ) );
		str.append( String.format( "\n%20s: %s", "spotColorByFeature", spotColorByFeature ) );
		str.append( String.format( "\n%20s: %s", "spotMin", "" + spotMin ) );
		str.append( String.format( "\n%20s: %s", "spotMax", "" + spotMax ) );
		str.append( String.format( "\n%20s: %s", "spotUniformColor", "" + spotUniformColor ) );

		str.append( String.format( "\n%20s: %s", "trackVisible", "" + trackVisible ) );
		str.append( String.format( "\n%20s: %s", "trackDisplayMode", trackDisplayMode ) );
		str.append( String.format( "\n%20s: %s", "trackColorByType", trackColorByType ) );
		str.append( String.format( "\n%20s: %s", "trackColorByFeature", trackColorByFeature ) );
		str.append( String.format( "\n%20s: %s", "trackMin", "" + trackMin ) );
		str.append( String.format( "\n%20s: %s", "trackMax", "" + trackMax ) );
		str.append( String.format( "\n%20s: %s", "trackUniformColor", "" + trackUniformColor ) );
		str.append( String.format( "\n%20s: %s", "fadeTracks", "" + fadeTracks ) );
		str.append( String.format( "\n%20s: %s", "fadeTrackRange", "" + fadeTrackRange ) );

		str.append( String.format( "\n%20s: %s", "colormap", colormap.getName() ) );
		str.append( String.format( "\n%20s: %s", "limitZDrawingDepth", "" + limitZDrawingDepth ) );
		str.append( String.format( "\n%20s: %s", "drawingZDepth", "" + drawingZDepth ) );
		str.append( String.format( "\n%20s: %s", "highlightColor", "" + highlightColor ) );
		str.append( String.format( "\n%20s: %s", "missingValueColor", "" + missingValueColor ) );
		str.append( String.format( "\n%20s: %s", "undefinedValueColor", "" + undefinedValueColor ) );
		str.append( String.format( "\n%20s: %s", "useAntialiasing", "" + useAntialiasing ) );

		return str.toString();
	}
}
