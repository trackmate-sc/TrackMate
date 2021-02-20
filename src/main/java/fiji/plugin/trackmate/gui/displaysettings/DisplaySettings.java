package fiji.plugin.trackmate.gui.displaysettings;

import static fiji.plugin.trackmate.features.FeatureUtils.USE_UNIFORM_COLOR_KEY;

import java.awt.Color;
import java.awt.Font;
import java.util.Objects;

import org.scijava.listeners.Listeners;

import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;

public class DisplaySettings
{

	/** The display settings name. */
	private String name;

	/** The uniform color for spots. */
	private Color spotUniformColor;

	private TrackMateObject spotColorByType;

	private String spotColorByFeature;

	private double spotDisplayRadius;

	private boolean spotDisplayedAsRoi;

	private double spotMin;

	private double spotMax;

	private boolean spotShowName;

	private double trackMin;

	private double trackMax;

	private TrackMateObject trackColorByType;

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
	private Colormap colormap;

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

	private Font font;

	private double lineThickness;

	private double selectionLineThickness;

	private Color trackschemeBackgroundColor1;

	private Color trackschemeBackgroundColor2;

	private Color trackschemeForegroundColor;

	private Color trackschemeDecorationColor;

	private boolean trackschemeFillBox;

	private boolean spotFilled;

	private double spotTransparencyAlpha;

	private final transient Listeners.List< UpdateListener > updateListeners;


	public DisplaySettings()
	{
		this.updateListeners = new Listeners.SynchronizedList<>();
		set( df );
	}

	private DisplaySettings( final String name )
	{
		this.name = name;
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

	synchronized void set( final DisplaySettings ds )
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
		spotFilled = ds.spotFilled;
		spotTransparencyAlpha = ds.spotTransparencyAlpha;

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

		font = ds.font;
		lineThickness = ds.lineThickness;
		selectionLineThickness = ds.selectionLineThickness;
		trackschemeBackgroundColor1 = ds.trackschemeBackgroundColor1;
		trackschemeBackgroundColor2 = ds.trackschemeBackgroundColor2;
		trackschemeDecorationColor = ds.trackschemeDecorationColor;
		trackschemeForegroundColor = ds.trackschemeForegroundColor;
		trackschemeFillBox = ds.trackschemeFillBox;

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

	public Colormap getColormap()
	{
		return colormap;
	}

	public synchronized void setColormap( final Colormap colormap )
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

	public TrackMateObject getSpotColorByType()
	{
		return spotColorByType;
	}

	public synchronized void setSpotColorBy( final TrackMateObject spotColorByType, final String spotColorByFeature )
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
		final double smin = Math.min( spotMin, spotMax );
		final double smax = Math.max( spotMin, spotMax );

		boolean notify = false;
		if ( this.spotMin != smin )
		{
			this.spotMin = smin;
			notify = true;
		}
		if ( this.spotMax != smax )
		{
			this.spotMax = smax;
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

	public TrackMateObject getTrackColorByType()
	{
		return trackColorByType;
	}

	public synchronized void setTrackColorBy( final TrackMateObject trackColorByType, final String trackColorByFeature )
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
		final double tmin = Math.min( trackMin, trackMax );
		final double tmax = Math.max( trackMin, trackMax );

		boolean notify = false;
		if ( this.trackMin != tmin )
		{
			this.trackMin = tmin;
			notify = true;
		}
		if ( this.trackMax != tmax )
		{
			this.trackMax = tmax;
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

	public Font getFont()
	{
		return font;
	}

	public synchronized void setFont( final Font font )
	{
		if ( this.font != font )
		{
			this.font = font;
			notifyListeners();
		}
	}

	public double getLineThickness()
	{
		return lineThickness;
	}

	public synchronized void setLineThickness( final double lineThickness )
	{
		if ( this.lineThickness != lineThickness )
		{
			this.lineThickness = lineThickness;
			notifyListeners();
		}
	}

	public double getSelectionLineThickness()
	{
		return selectionLineThickness;
	}

	public synchronized void setSelectionLineThickness( final double selectionLineThickness )
	{
		if ( this.selectionLineThickness != selectionLineThickness )
		{
			this.selectionLineThickness = selectionLineThickness;
			notifyListeners();
		}
	}

	public boolean isSpotFilled()
	{
		return spotFilled;
	}

	public synchronized void setSpotFilled( final boolean isSpotFilled )
	{
		if ( this.spotFilled != isSpotFilled )
		{
			this.spotFilled = isSpotFilled;
			notifyListeners();
		}
	}

	public double getSpotTransparencyAlpha()
	{
		return spotTransparencyAlpha;
	}

	public synchronized void setSpotTransparencyAlpha( final double spotTransparencyAlpha )
	{
		if ( this.spotTransparencyAlpha != spotTransparencyAlpha )
		{
			this.spotTransparencyAlpha = spotTransparencyAlpha;
			notifyListeners();
		}
	}

	public Color getTrackSchemeBackgroundColor1()
	{
		return trackschemeBackgroundColor1;
	}

	public synchronized void setTrackSchemeBackgroundColor1( final Color trackschemeBackgroundColor1 )
	{
		if ( this.trackschemeBackgroundColor1 != trackschemeBackgroundColor1 )
		{
			this.trackschemeBackgroundColor1 = trackschemeBackgroundColor1;
			notifyListeners();
		}
	}

	public Color getTrackSchemeBackgroundColor2()
	{
		return trackschemeBackgroundColor2;
	}

	public synchronized void setTrackSchemeBackgroundColor2( final Color trackschemeBackgroundColor2 )
	{
		if ( this.trackschemeBackgroundColor2 != trackschemeBackgroundColor2 )
		{
			this.trackschemeBackgroundColor2 = trackschemeBackgroundColor2;
			notifyListeners();
		}
	}

	public Color getTrackSchemeDecorationColor()
	{
		return trackschemeDecorationColor;
	}

	public synchronized void setTrackSchemeDecorationColor( final Color trackschemeDecorationColor )
	{
		if ( this.trackschemeDecorationColor != trackschemeDecorationColor )
		{
			this.trackschemeDecorationColor = trackschemeDecorationColor;
			notifyListeners();
		}
	}

	public Color getTrackSchemeForegroundColor()
	{
		return trackschemeForegroundColor;
	}

	public synchronized void setTrackSchemeForegroundColor( final Color trackschemeForegroundColor )
	{
		if ( this.trackschemeForegroundColor != trackschemeForegroundColor )
		{
			this.trackschemeForegroundColor = trackschemeForegroundColor;
			notifyListeners();
		}
	}

	public boolean isTrackSchemeFillBox()
	{
		return trackschemeFillBox;
	}

	public synchronized void setTrackschemeFillBox( final boolean trackschemeFillBox )
	{
		if ( this.trackschemeFillBox != trackschemeFillBox )
		{
			this.trackschemeFillBox = trackschemeFillBox;
			notifyListeners();
		}
	}

	/*
	 * Other methods.
	 */

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

	public enum TrackMateObject
	{
		DEFAULT( "Default" ), SPOTS( "spots" ), EDGES( "edges" ), TRACKS( "tracks" );

		private String name;

		private TrackMateObject( final String name )
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
		df = new DisplaySettings( "Default" );
		df.useAntialiasing = true;
		df.colormap = Colormap.Jet;
		df.limitZDrawingDepth = false;
		df.drawingZDepth = 10.;
		df.fadeTracks = true;
		df.fadeTrackRange = 30;
		df.highlightColor = new Color( 0.2f, 0.9f, 0.2f );
		df.missingValueColor = Color.GRAY.darker();
		df.spotColorByFeature = USE_UNIFORM_COLOR_KEY;
		df.spotColorByType = TrackMateObject.DEFAULT;
		df.spotDisplayedAsRoi = true;
		df.spotDisplayRadius = 1.;
		df.spotFilled = false;
		df.spotMin = 0.;
		df.spotMax = 10.;
		df.spotShowName = false;
		df.spotTransparencyAlpha = 1.;
		df.spotUniformColor = new Color( 0.8f, 0.2f, 0.8f );
		df.spotVisible = true;
		df.trackDisplayMode = TrackDisplayMode.FULL;
		df.trackColorByFeature = TrackIndexAnalyzer.TRACK_INDEX;
		df.trackColorByType = TrackMateObject.DEFAULT;
		df.trackUniformColor = new Color( 0.8f, 0.8f, 0.2f );
		df.trackMin = 0.;
		df.trackMax = 10.;
		df.trackVisible = true;
		df.undefinedValueColor = Color.BLACK;

		df.font = new Font( "Arial", Font.BOLD, 12 );
		df.lineThickness = 1.0f;
		df.selectionLineThickness = 4.0f;

		df.trackschemeBackgroundColor1 = Color.GRAY;
		df.trackschemeBackgroundColor2 = Color.LIGHT_GRAY;
		df.trackschemeForegroundColor = Color.BLACK;
		df.trackschemeDecorationColor = Color.BLACK;
		df.trackschemeFillBox = false;
	}

	public static DisplaySettings defaultStyle()
	{
		return df;
	}

	@Override
	public String toString()
	{
		return DisplaySettingsIO.toJson( this );
	}

	public static void main( final String[] args )
	{
		System.out.println( df.toString() );
	}
}
