/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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
package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.detection.semiauto.SemiAutoTracker;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.tool.AbstractTool;
import fiji.tool.ToolWithOptions;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.FreehandRoi;
import ij.gui.ImageCanvas;
import ij.gui.Toolbar;

public class SpotEditTool extends AbstractTool implements MouseMotionListener, MouseListener, KeyListener, ToolWithOptions
{

	private static final double COARSE_STEP = 2;

	private static final double FINE_STEP = 0.2f;

	private static final String TOOL_NAME = "Spot edit tool";

	private static final String TOOL_ICON = "CeacD70Cd8bD80"
			+ "D71Cc69D81CfefD91"
			+ "CdbcD72Cb9bD82"
			+ "Cd9bD73Cc8aD83CfefD93"
			+ "CdddD54CbaaD64Cb69D74Cb59D84Cb9aD94CdddDa4"
			+ "CfefD25Cd9bD35Cb8aD45CaaaD55CcccD65CfdeL7585CdccD95CaaaDa5Cb8aDb5Cd7aDc5CfceDd5"
			+ "CfeeD26Cc69D36Cc8aD46CdacDb6Cb59Dc6CecdDd6"
			+ "Cb9aD37CdcdD47CeeeDb7Ca89Dc7"
			+ "CfefD28Cc7aD38Cd9cD48CecdDb8Cb79Dc8CfdeDd8"
			+ "CcabD29Cb59D39Cb69D49CedeD59CeacDb9Cc59Dc9CebdDd9"
			+ "CfdeD0aCc7aD1aCb8aD2aCedeD3aCcbcD4aCb7aD5aCe9cD6aCeeeDbaCa89DcaCfefDda"
			+ "CebdD0bCc59D1bCebdD2bCfefD4bCc7aL5b6bCeceDbbCb79DcbCfdeDdb"
			+ "CfeeD0cCa89D1cCfefD2cCcabL5c6cCc9bDbcCc59DccCdabDdc"
			+ "CedeD0dCb79D1dCedeD2dCc9bL5d6dCecdD9dCc8aDadCb9aDbdCdbcDcdCb8aDddCd8bDedCfceDfd"
			+ "CebdD0eCc59D1eCebdD2eCfeeD4eCc7aD5eCc6aD6eCfeeD7eCd9bD9eCc59DaeCfdeDbeCebdDdeCc59DeeCeacDfe"
			+ "CfefD0fCdbcD1fCdddD4fCdcdL5f6fCdddD7fCfdeD9fCdbdDafCebdDefCfefDff";

	/**
	 * Fall back default radius when the settings does not give a default radius
	 * to use.
	 */
	private static final double FALL_BACK_RADIUS = 5.;

	/** The singleton instance. */
	private static SpotEditTool instance;

	/** Stores the view possible attached to each {@link ImagePlus}. */
	HashMap< ImagePlus, HyperStackDisplayer > displayers = new HashMap<>();

	/** The radius of the previously edited spot. */
	private double previousRadius = FALL_BACK_RADIUS;

	private Spot quickEditedSpot;

	/** Flag for the auto-linking mode. */
	private boolean autolinkingmode = false;

	SpotEditToolParams params = new SpotEditToolParams();

	private Logger logger = Logger.IJTOOLBAR_LOGGER;

	private SpotEditToolConfigPanel configPanel;

	/**
	 * The last {@link ImagePlus} on which an action happened.
	 */
	ImagePlus imp;

	private FreehandRoi roiedit;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Singleton
	 */
	private SpotEditTool()
	{}

	/**
	 * Return the singleton instance for this tool. If it was not previously
	 * instantiated, this calls instantiates it.
	 */
	public static SpotEditTool getInstance()
	{
		if ( null == instance )
			instance = new SpotEditTool();

		return instance;
	}

	/**
	 * Return true if the tool is currently present in ImageJ toolbar.
	 */
	public static boolean isLaunched()
	{
		final Toolbar toolbar = Toolbar.getInstance();
		if ( null != toolbar && toolbar.getToolId( TOOL_NAME ) >= 0 )
			return true;
		return false;
	}

	/*
	 * METHODS
	 */

	@Override
	public String getToolName()
	{
		return TOOL_NAME;
	}

	@Override
	public String getToolIcon()
	{
		return TOOL_ICON;
	}

	/**
	 * Overridden so that we can keep track of the last ImagePlus actions are
	 * taken on. Very much like ImageJ.
	 */
	@Override
	public ImagePlus getImagePlus( final ComponentEvent e )
	{
		imp = super.getImagePlus( e );
		return imp;
	}

	@Override
	protected void registerTool( final ImageCanvas canvas )
	{
		/*
		 * Double check! Since TrackMate v7 there the following bug:
		 * 
		 * Sometimes the listeners of this tool get added to the target image
		 * canvas TWICE. This causes an unspeakable mess where all events are
		 * triggered twice for e.g. a single click. For instance you cannot
		 * shift-click on a spot to add it to the selection, because the event
		 * is fired TWICE, which results in the spot being de-selected
		 * immediately after being selected.
		 * 
		 * But the double registration seems to happen randomly. Sometimes the
		 * listeners are added only once, *sometimes* (more often) twice.
		 * 
		 * To work around this mess, we overload the registerTool(ImageCanvas)
		 * method and skip the registration if we find that the mouse listener
		 * has already been added to the canvas. It fixes the issue, regardless
		 * of the occurrence of the double call to this method or not.
		 */

		final MouseListener[] listeners = canvas.getMouseListeners();
		for ( final MouseListener listener : listeners )
		{
			if ( listener == this.mouseProxy )
				return;
		}

		super.registerTool( canvas );
	}

	/**
	 * Register the given {@link HyperStackDisplayer}. If this method id not
	 * called, the tool will not respond.
	 */
	public void register( final ImagePlus lImp, final HyperStackDisplayer displayer )
	{
		if ( displayers.containsKey( lImp ) )
			unregisterTool( lImp );

		displayers.put( lImp, displayer );
	}

	/*
	 * MOUSE AND MOUSE MOTION
	 */

	@Override
	public void mouseClicked( final MouseEvent e )
	{
		final ImagePlus lImp = getImagePlus( e );
		final HyperStackDisplayer displayer = displayers.get( lImp );
		if ( null == displayer )
			return;

		final Spot clickLocation = makeSpot( lImp, displayer, getImageCanvas( e ), e.getPoint() );
		final int frame = displayer.imp.getFrame() - 1;
		final Model model = displayer.getModel();
		final Spot target = model.getSpots().getSpotAt( clickLocation, frame, true );
		final SelectionModel selectionModel = displayer.getSelectionModel();

		// Change selection

		// If no target, we clear selection
		if ( null == target )
		{
			if ( !autolinkingmode )
			{
				selectionModel.clearSelection();
				logger.log( "Cleared selection.\n" );
			}
			roiedit = null;
			lImp.setRoi( roiedit );
		}
		else
		{
			updateStatusBar( target, lImp.getCalibration().getUnits() );
			final int addToSelectionMask = InputEvent.SHIFT_DOWN_MASK;
			if ( ( e.getModifiersEx() & addToSelectionMask ) == addToSelectionMask )
			{
				if ( selectionModel.getSpotSelection().contains( target ) )
					selectionModel.removeSpotFromSelection( target );
				else
					selectionModel.addSpotToSelection( target );
			}
			else
			{
				selectionModel.clearSpotSelection();
				selectionModel.addSpotToSelection( target );
			}
		}
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{}

	@Override
	public void mouseReleased( final MouseEvent e )
	{
		if ( null != roiedit )
		{
			new Thread( "SpotEditTool roiedit processing" )
			{
				@Override
				public void run()
				{
					roiedit.mouseReleased( e );
					final ImagePlus lImp = getImagePlus( e );
					final HyperStackDisplayer displayer = displayers.get( lImp );
					final int frame = displayer.imp.getFrame() - 1;
					final Model model = displayer.getModel();
					final SelectionModel selectionModel = displayer.getSelectionModel();

					final Iterator< Spot > it;
					if ( IJ.shiftKeyDown() )
						it = model.getSpots().iterator( true );
					else
						it = model.getSpots().iterator( frame, true );

					final Collection< Spot > added = new ArrayList<>();
					final double calibration[] = TMUtils.getSpatialCalibration( lImp );

					while ( it.hasNext() )
					{
						final Spot spot = it.next();
						final double x = spot.getFeature( Spot.POSITION_X );
						final double y = spot.getFeature( Spot.POSITION_Y );
						// In pixel units
						final int xp = ( int ) ( x / calibration[ 0 ] + 0.5f );
						final int yp = ( int ) ( y / calibration[ 1 ] + 0.5f );

						if ( null != roiedit && roiedit.contains( xp, yp ) )
							added.add( spot );
					}

					if ( !added.isEmpty() )
					{
						selectionModel.addSpotToSelection( added );
						if ( added.size() == 1 )
							logger.log( "Added one spot to selection.\n" );
						else
							logger.log( "Added " + added.size() + " spots to selection.\n" );
					}
					roiedit = null;
				}
			}.start();
		}
	}

	@Override
	public void mouseEntered( final MouseEvent e )
	{}

	@Override
	public void mouseExited( final MouseEvent e )
	{}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		final ImagePlus lImp = getImagePlus( e );
		final HyperStackDisplayer displayer = displayers.get( lImp );
		if ( null == displayer )
			return;

		if ( null == roiedit )
		{
			if ( !IJ.spaceBarDown() )
			{
				roiedit = new FreehandRoi( e.getX(), e.getY(), lImp )
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void handleMouseUp( final int screenX, final int screenY )
					{
						type = FREEROI;
						super.handleMouseUp( screenX, screenY );
					}
				};
				lImp.setRoi( roiedit );
			}
		}
		else
		{
			roiedit.mouseDragged( e );
		}
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{
		if ( quickEditedSpot == null )
			return;
		final ImagePlus lImp = getImagePlus( e );
		final double[] calibration = TMUtils.getSpatialCalibration( lImp );
		final HyperStackDisplayer displayer = displayers.get( lImp );
		if ( null == displayer )
			return;

		final Point mouseLocation = e.getPoint();
		final ImageCanvas canvas = getImageCanvas( e );
		final double x = ( -0.5 + canvas.offScreenXD( mouseLocation.x ) ) * calibration[ 0 ];
		final double y = ( -0.5 + canvas.offScreenYD( mouseLocation.y ) ) * calibration[ 1 ];
		final double z = ( lImp.getSlice() - 1 ) * calibration[ 2 ];

		quickEditedSpot.putFeature( Spot.POSITION_X, x );
		quickEditedSpot.putFeature( Spot.POSITION_Y, y );
		quickEditedSpot.putFeature( Spot.POSITION_Z, z );
		displayer.imp.updateAndDraw();
	}

	/*
	 * KEYLISTENER
	 */

	@Override
	public void keyTyped( final KeyEvent e )
	{}

	@Override
	public void keyPressed( final KeyEvent e )
	{
		final ImagePlus lImp = getImagePlus( e );
		if ( lImp == null )
			return;
		final HyperStackDisplayer displayer = displayers.get( lImp );
		if ( null == displayer )
			return;

		final Model model = displayer.getModel();
		final SelectionModel selectionModel = displayer.getSelectionModel();
		final ImageCanvas canvas = lImp.getCanvas();

		final int keycode = e.getKeyCode();

		switch ( keycode )
		{

		// Delete currently edited spot
		case KeyEvent.VK_DELETE:
		{
			final ArrayList< Spot > spotSelection = new ArrayList<>( selectionModel.getSpotSelection() );
			final ArrayList< DefaultWeightedEdge > edgeSelection = new ArrayList<>( selectionModel.getEdgeSelection() );
			model.beginUpdate();
			try
			{
				selectionModel.clearSelection();
				for ( final DefaultWeightedEdge edge : edgeSelection )
				{
					model.removeEdge( edge );
					logger.log( "Removed edge " + edge + ".\n" );
				}
				for ( final Spot spot : spotSelection )
				{
					model.removeSpot( spot );
					logger.log( "Removed spot " + spot + ".\n" );
				}
			}
			finally
			{
				model.endUpdate();
			}

			lImp.updateAndDraw();
			e.consume();
			break;
		}

		// Quick add spot at mouse
		case KeyEvent.VK_A:
		{
			if ( e.isShiftDown() )
			{
				logger.log( "Semi-automatic tracking.\n" );
				// Semi-auto tracking
				semiAutoTracking( model, selectionModel, lImp );

			}
			else
			{
				// Create and drop a new spot
				final double radius = previousRadius;
				final Spot newSpot = makeSpot( lImp, displayer, canvas, null );
				final double dt = lImp.getCalibration().frameInterval;
				final int frame = displayer.imp.getFrame() - 1;
				newSpot.putFeature( Spot.POSITION_T, frame * dt );
				newSpot.putFeature( Spot.FRAME, Double.valueOf( frame ) );
				newSpot.putFeature( Spot.RADIUS, radius );
				newSpot.putFeature( Spot.QUALITY, -1d );

				model.beginUpdate();
				try
				{
					model.addSpotTo( newSpot, frame );
					logger.log( "Added spot " + newSpot + " to frame " + frame + ".\n" );
				}
				finally
				{
					model.endUpdate();
				}

				/*
				 * If we are in auto-link mode, we create an edge with spot in
				 * selection, if there is just one and if it is in a previous
				 * frame
				 */
				if ( autolinkingmode )
				{
					final Set< Spot > spotSelection = selectionModel.getSpotSelection();
					if ( spotSelection.size() == 1 )
					{
						final Spot source = spotSelection.iterator().next();
						if ( newSpot.diffTo( source, Spot.FRAME ) != 0 )
						{
							model.beginUpdate();
							try
							{
								model.addEdge( source, newSpot, -1 );
								logger.log( "Created a link between " + source + " and " + newSpot + ".\n" );
							}
							finally
							{
								model.endUpdate();
							}
						}
					}
					selectionModel.clearSpotSelection();
					selectionModel.addSpotToSelection( newSpot );
				}

				lImp.updateAndDraw();
				e.consume();
			}

			break;
		}

		// Quick delete spot under mouse
		case KeyEvent.VK_D:
		{
			final int frame = displayer.imp.getFrame() - 1;
			final Spot clickLocation = makeSpot( lImp, displayer, canvas, null );
			final Spot target = model.getSpots().getSpotAt( clickLocation, frame, true );
			if ( null == target )
			{
				// Consume it anyway, so that we are not bothered by IJ
				e.consume();
				return;
			}

			selectionModel.removeSpotFromSelection( target );
			model.beginUpdate();
			try
			{
				model.removeSpot( target );
				logger.log( "Removed spot " + target + ".\n" );
			}
			finally
			{
				model.endUpdate();
			}

			lImp.updateAndDraw();

			e.consume();
			break;
		}

		// Quick move spot under the mouse
		case KeyEvent.VK_SPACE:
		{

			if ( null == quickEditedSpot )
			{
				final int frame = displayer.imp.getFrame() - 1;
				final Spot clickLocation = makeSpot( lImp, displayer, canvas, null );
				quickEditedSpot = model.getSpots().getSpotAt( clickLocation, frame, true );
				if ( null == quickEditedSpot )
					return;
			}
			e.consume();
			break;

		}

		// Quick change spot radius
		case KeyEvent.VK_Q:
		case KeyEvent.VK_E:
		{

			e.consume();
			final int frame = displayer.imp.getFrame() - 1;
			final Spot clickLocation = makeSpot( lImp, displayer, canvas, null );
			final Spot target = model.getSpots().getSpotAt( clickLocation, frame, true );
			if ( null == target )
				return;

			final double radius = target.getFeature( Spot.RADIUS );
			final int factor = ( e.getKeyCode() == KeyEvent.VK_Q ) ? -1 : 1;
			final double dx = lImp.getCalibration().pixelWidth;

			final double newRadius = ( e.isShiftDown() )
					? radius + factor * dx * COARSE_STEP
					: radius + factor * dx * FINE_STEP;

			if ( newRadius <= dx )
				return;

			// Store new value of radius for next spot creation.
			previousRadius = newRadius;

			final SpotRoi roi = target.getRoi();
			if ( null == roi )
			{
				target.putFeature( Spot.RADIUS, newRadius );
			}
			else
			{
				final double alpha = newRadius / radius;
				roi.scale( alpha );
				target.putFeature( Spot.RADIUS, roi.radius() );
			}

			model.beginUpdate();
			try
			{
				model.updateFeatures( target );
				logger.log( String.format( Locale.US, "Changed spot " + target + " radius to %.1f " + model.getSpaceUnits() + ".\n", radius ) );
			}
			finally
			{
				model.endUpdate();
			}
			lImp.updateAndDraw();
			break;
		}

		case KeyEvent.VK_L:
		{

			if ( e.isShiftDown() )
			{
				/*
				 * Toggle auto-linking mode
				 */
				autolinkingmode = !autolinkingmode;
				logger.log( "Toggled auto-linking mode " + ( autolinkingmode ? "on.\n" : "off.\n" ) );

			}
			else
			{
				/*
				 * Toggle a link between two spots.
				 */
				final Set< Spot > selectedSpots = selectionModel.getSpotSelection();
				if ( selectedSpots.size() == 2 )
				{
					final Iterator< Spot > it = selectedSpots.iterator();
					final Spot sourceTmp = it.next();
					final Spot targetTmp = it.next();

					final Spot source = sourceTmp.diffTo( targetTmp, Spot.FRAME ) < 0 ? sourceTmp : targetTmp;
					final Spot target = sourceTmp.diffTo( targetTmp, Spot.FRAME ) < 0 ? targetTmp : sourceTmp;

					if ( model.getTrackModel().containsEdge( source, target ) )
					{
						/*
						 * Remove it
						 */
						model.beginUpdate();
						try
						{
							model.removeEdge( source, target );
							logger.log( "Removed edge between " + source + " and " + target + ".\n" );
						}
						finally
						{
							model.endUpdate();
						}

					}
					else
					{
						/*
						 * Create a new link
						 */
						final int ts = source.getFeature( Spot.FRAME ).intValue();
						final int tt = target.getFeature( Spot.FRAME ).intValue();

						if ( tt != ts )
						{
							model.beginUpdate();
							try
							{
								model.addEdge( source, target, -1 );
								logger.log( "Created an edge between " + source + " and " + target + ".\n" );
							}
							finally
							{
								model.endUpdate();
							}
							/*
							 * To emulate a kind of automatic linking, we put
							 * the last spot to the selection, so several spots
							 * can be tracked in a row without having to
							 * de-select one
							 */
							final Spot single = ( tt > ts ) ? target : source;
							selectionModel.clearSpotSelection();
							selectionModel.addSpotToSelection( single );
						}
						else
						{
							logger.error( "Cannot create an edge between two spots belonging to the same frame.\n" );
						}
					}

				}
				else
				{
					logger.error( "Expected selection to contain 2 spots, found " + selectedSpots.size() + ".\n" );
				}

			}
			e.consume();
			break;

		}

		case KeyEvent.VK_G:
		case KeyEvent.VK_F:
		{
			// Stepwise time browsing.
			final int currentT = lImp.getT() - 1;
			final int prevStep = ( currentT / params.stepwiseTimeBrowsing ) * params.stepwiseTimeBrowsing;
			int tp;
			if ( keycode == KeyEvent.VK_G )
			{
				tp = prevStep + params.stepwiseTimeBrowsing;
			}
			else
			{
				if ( currentT == prevStep )
					tp = currentT - params.stepwiseTimeBrowsing;
				else
					tp = prevStep;
			}
			lImp.setT( tp + 1 );

			e.consume();
			break;
		}

		case KeyEvent.VK_W:
		{
			e.consume(); // consume it: we do not want IJ to close the window
			break;
		}

		}

	}

	private Spot makeSpot( final ImagePlus lImp, HyperStackDisplayer displayer, final ImageCanvas canvas, Point mouseLocation )
	{
		if ( displayer == null )
		{
			displayer = displayers.get( lImp );
		}
		if ( mouseLocation == null )
		{
			mouseLocation = MouseInfo.getPointerInfo().getLocation();
			SwingUtilities.convertPointFromScreen( mouseLocation, canvas );
		}
		final double[] calibration = TMUtils.getSpatialCalibration( lImp );
		return new Spot(
				( -0.5d + canvas.offScreenXD( mouseLocation.x ) ) * calibration[ 0 ],
				( -0.5d + canvas.offScreenYD( mouseLocation.y ) ) * calibration[ 1 ],
				( lImp.getSlice() - 1 ) * calibration[ 2 ],
				FALL_BACK_RADIUS,
				-1d );
	}

	@Override
	public void keyReleased( final KeyEvent e )
	{
		switch ( e.getKeyCode() )
		{
		case KeyEvent.VK_SPACE:
		{
			if ( null == quickEditedSpot )
				return;
			final ImagePlus lImp = getImagePlus( e );
			if ( lImp == null )
				return;
			final HyperStackDisplayer displayer = displayers.get( lImp );
			if ( null == displayer )
				return;
			final Model model = displayer.getModel();
			model.beginUpdate();
			try
			{
				model.updateFeatures( quickEditedSpot );
			}
			finally
			{
				model.endUpdate();
			}
			quickEditedSpot = null;
			break;
		}
		}

	}

	/*
	 * PRIVATE METHODS
	 */

	private void updateStatusBar( final Spot spot, final String units )
	{
		if ( null == spot )
			return;
		String statusString = "";
		if ( null == spot.getName() || spot.getName().equals( "" ) )
			statusString = String.format( Locale.US, "Spot ID%d, x = %.1f, y = %.1f, z = %.1f, r = %.1f %s", spot.ID(), spot.getFeature( Spot.POSITION_X ), spot.getFeature( Spot.POSITION_Y ), spot.getFeature( Spot.POSITION_Z ), spot.getFeature( Spot.RADIUS ), units );
		else
			statusString = String.format( Locale.US, "Spot %s, x = %.1f, y = %.1f, z = %.1f, r = %.1f %s", spot.getName(), spot.getFeature( Spot.POSITION_X ), spot.getFeature( Spot.POSITION_Y ), spot.getFeature( Spot.POSITION_Z ), spot.getFeature( Spot.RADIUS ), units );
		IJ.showStatus( statusString );
	}

	void semiAutoTracking( final Model model, final SelectionModel selectionModel, final ImagePlus lImp )
	{
		@SuppressWarnings( "rawtypes" )
		final SemiAutoTracker autotracker = new SemiAutoTracker( model, selectionModel, lImp, logger );
		autotracker.setParameters( params.qualityThreshold, params.distanceTolerance, params.nFrames );
		autotracker.setNumThreads( 4 );
		new Thread( "TrackMate semi-automated tracking thread" )
		{
			@Override
			public void run()
			{
				final boolean ok = autotracker.checkInput() && autotracker.process();
				if ( !ok )
					logger.error( autotracker.getErrorMessage() );
			}
		}.start();
	}

	@Override
	public void showOptionDialog()
	{
		if ( null == configPanel )
		{
			configPanel = new SpotEditToolConfigPanel( this );
			configPanel.addWindowListener( new WindowAdapter()
			{
				@Override
				public void windowClosing( final WindowEvent e )
				{
					logger = Logger.IJTOOLBAR_LOGGER;
				}
			} );
		}
		configPanel.setLocation( toolbar.getLocationOnScreen() );
		configPanel.setVisible( true );
		logger = configPanel.getLogger();
	}

	/*
	 * INNER CLASSES
	 */

	static class SpotEditToolParams
	{

		/*
		 * Semi-auto tracking parameters
		 */
		/**
		 * The fraction of the initial quality above which we keep new spots.
		 * The highest, the more intolerant.
		 */
		double qualityThreshold = 0.5;

		/**
		 * How close must be the new spot found to be accepted, in radius units.
		 */
		double distanceTolerance = 2d;

		/**
		 * We process at most nFrames. Make it 0 or negative to have no bounds.
		 */
		int nFrames = 10;

		/**
		 * By how many frames to jymp when we do step-wide time browsing.
		 */
		int stepwiseTimeBrowsing = 5;

		@Override
		public String toString()
		{
			return super.toString() + ": " + "QualityThreshold = " + qualityThreshold + ", DistanceTolerance = " + distanceTolerance + ", nFrames = " + nFrames;
		}
	}
}
