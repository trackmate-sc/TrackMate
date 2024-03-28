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
package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.tool.AbstractTool;
import fiji.tool.ToolWithOptions;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Toolbar;

public class SpotEditTool extends AbstractTool implements MouseMotionListener, MouseListener, KeyListener, ToolWithOptions
{

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

	/** The singleton instance. */
	private static SpotEditTool instance;

	/** Stores the editor possibly attached to each {@link ImagePlus}. */
	private final Map< ImagePlus, ModelEditActions > editorMap = new HashMap<>();

	/** Flag for the auto-linking mode. */
	private boolean autolinkingmode = false;

	private final SpotEditToolParams params = new SpotEditToolParams();

	private final Logger logger = new MyLogger();

	private final SpotEditToolConfigPanel configPanel;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Singleton
	 */
	private SpotEditTool()
	{
		// Config panel actions.
		this.configPanel = new SpotEditToolConfigPanel( params );
		configPanel.buttonSelectTrackDown.addActionListener( e -> selectTrackDownward() );
		configPanel.buttonSelectTrackUp.addActionListener( e -> selectTrackUpward() );
		configPanel.buttonSemiAutoTracking.addActionListener( e -> semiAutoTracking() );
		configPanel.buttonSelectTrack.addActionListener( e -> selectTrack() );

		// De-register on closing.
		ImagePlus.addImageListener( new ImageListener()
		{

			@Override
			public void imageUpdated( final ImagePlus imp )
			{}

			@Override
			public void imageOpened( final ImagePlus imp )
			{}

			@Override
			public void imageClosed( final ImagePlus imp )
			{
				editorMap.remove( imp );
			}
		} );
	}

	/**
	 * Returns the singleton instance for this tool. If it was not previously
	 * instantiated, this calls instantiates it.
	 * 
	 * @return the instance.
	 */
	public static SpotEditTool getInstance()
	{
		if ( null == instance )
			instance = new SpotEditTool();

		return instance;
	}

	/**
	 * Returns <code>true</code> if the tool is currently present in ImageJ
	 * toolbar.
	 * 
	 * @return <code>true</code> if the tool is currently present in ImageJ
	 *         toolbar.
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
	 * Registers the given {@link HyperStackDisplayer}. If this method is not
	 * called, the tool will not respond.
	 * 
	 * @param displayer
	 *            the displayer to register.
	 */
	public void register( final HyperStackDisplayer displayer )
	{
		final ImagePlus imp = displayer.getImp();
		final Model model = displayer.getModel();
		final SelectionModel selectionModel = displayer.getSelectionModel();
		final ModelEditActions actions = new ModelEditActions( imp, model, selectionModel, logger );
		editorMap.put( imp, actions );
	}

	/*
	 * MOUSE AND MOUSE MOTION
	 */

	@Override
	public void mouseClicked( final MouseEvent e )
	{
		final ImagePlus lImp = getImagePlus( e );
		final ModelEditActions actions = editorMap.get( lImp );
		if ( null == actions )
			return;

		final int addToSelectionMask = InputEvent.SHIFT_DOWN_MASK;
		final boolean addToSelection = ( e.getModifiersEx() & addToSelectionMask ) == addToSelectionMask;
		actions.select( e.getPoint(), addToSelection, !autolinkingmode );
	}

	@Override
	public void mousePressed( final MouseEvent e )
	{}

	@Override
	public void mouseReleased( final MouseEvent e )
	{
		final ImagePlus imp = getImagePlus( e );
		final ModelEditActions actions = editorMap.get( imp );
		if ( actions == null )
			return;

		actions.selectInRoi( e );
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
		final ImagePlus imp = getImagePlus( e );
		final ModelEditActions actions = editorMap.get( imp );
		if ( null == actions )
			return;

		actions.roiEdit( e );
	}

	@Override
	public void mouseMoved( final MouseEvent e )
	{
		final ImagePlus imp = getImagePlus( e );
		final ModelEditActions actions = editorMap.get( imp );
		if ( actions == null )
			return;

		actions.moveSpot( e.getPoint() );
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
		final ImagePlus imp = getImagePlus( e );
		final ModelEditActions actions = editorMap.get( imp );
		if ( null == actions )
			return;

		switch ( e.getKeyCode() )
		{

		// Track navigation actions.
		case KeyEvent.VK_UP:
		{
			actions.navigateToParent();
			e.consume();
			break;
		}
		case KeyEvent.VK_DOWN:
		{
			actions.navigateToChild();
			e.consume();
			break;
		}
		case KeyEvent.VK_LEFT:
		{
			actions.navigateToPreviousSibling();
			e.consume();
			break;
		}
		case KeyEvent.VK_RIGHT:
		{
			actions.navigateToNextSibling();
			e.consume();
			break;
		}
		case KeyEvent.VK_PAGE_DOWN:
		{
			actions.navigateToNextTrack();
			e.consume();
			break;
		}
		case KeyEvent.VK_PAGE_UP:
		{
			actions.navigateToPreviousTrack();
			e.consume();
			break;
		}
		
		// Delete currently edited spot
		case KeyEvent.VK_DELETE:
		{
			actions.deleteSpotSelection();
			e.consume();
			break;
		}

		// Quick add spot at mouse
		case KeyEvent.VK_A:
		{
			if ( e.isShiftDown() )
			{
				// Semi-auto tracking
				actions.semiAutoTracking( params.qualityThreshold, params.distanceTolerance, params.nFrames );
			}
			else
			{
				// Create and drop a new spot
				actions.addSpot( autolinkingmode );
			}
			e.consume();
			break;
		}

		// Quick delete spot under mouse
		case KeyEvent.VK_D:
		{
			actions.deleteSpot();
			e.consume();
			break;
		}

		// Quick move spot under the mouse
		case KeyEvent.VK_SPACE:
		{
			actions.startMoveSpot();
			e.consume();
			break;

		}

		// Quick change spot radius
		case KeyEvent.VK_Q:
		case KeyEvent.VK_E:
		{
			e.consume();
			actions.changeSpotRadius( e.getKeyCode() == KeyEvent.VK_E, e.isShiftDown() );
			break;
		}

		case KeyEvent.VK_L:
		{

			if ( e.isShiftDown() )
			{
				// Toggle auto-linking mode
				autolinkingmode = !autolinkingmode;
				logger.log( "Toggled auto-linking mode " + ( autolinkingmode ? "on.\n" : "off.\n" ) );

			}
			else
			{
				// Toggle a link between two spots.
				actions.toggleLink();
			}
			e.consume();
			break;

		}

		case KeyEvent.VK_G:
		case KeyEvent.VK_F:
		{
			actions.stepInTime( e.getKeyCode() == KeyEvent.VK_G, params.stepwiseTimeBrowsing );
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

	@Override
	public void keyReleased( final KeyEvent e )
	{
		switch ( e.getKeyCode() )
		{
		case KeyEvent.VK_SPACE:
		{
			final ImagePlus imp = getImagePlus( e );
			final ModelEditActions actions = editorMap.get( imp );
			if ( actions != null )
				actions.endMoveSpot();
			break;
		}
		}
	}

	@Override
	public void showOptionDialog()
	{
		configPanel.setLocation( toolbar.getLocationOnScreen() );
		configPanel.setVisible( true );
	}

	/*
	 * PRIVATE METHODS
	 */

	private void selectTrack()
	{
		final ImagePlus imp = WindowManager.getCurrentImage();
		final ModelEditActions actions = editorMap.get( imp );
		if ( null == actions )
			return;

		actions.selectTrack();
	}

	private void semiAutoTracking()
	{
		final ImagePlus imp = WindowManager.getCurrentImage();
		final ModelEditActions actions = editorMap.get( imp );
		if ( null == actions )
			return;

		actions.semiAutoTracking( params.qualityThreshold, params.distanceTolerance, params.nFrames );
	}

	private void selectTrackDownward()
	{
		final ImagePlus imp = WindowManager.getCurrentImage();
		final ModelEditActions actions = editorMap.get( imp );
		if ( null == actions )
			return;

		actions.selectTrackDownward();
	}

	private void selectTrackUpward()
	{

		final ImagePlus imp = WindowManager.getCurrentImage();
		final ModelEditActions actions = editorMap.get( imp );
		if ( null == actions )
			return;

		actions.selectTrackUpward();
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
		 * By how many frames to jump when we do step-wide time browsing.
		 */
		int stepwiseTimeBrowsing = 1;

		@Override
		public String toString()
		{
			return super.toString() + ": " + "QualityThreshold = " + qualityThreshold + ", DistanceTolerance = " + distanceTolerance + ", nFrames = " + nFrames;
		}
	}

	private class MyLogger extends Logger
	{

		private Logger logger()
		{
			if ( configPanel.isVisible() )
				return configPanel.getLogger();

			return Logger.IJTOOLBAR_LOGGER;
		}

		@Override
		public void log( final String message, final Color color )
		{
			logger().log( message, color );
		}

		@Override
		public void error( final String message )
		{
			logger().error( message );
		}

		@Override
		public void setProgress( final double val )
		{
			logger().setProgress( val );
		}

		@Override
		public void setStatus( final String status )
		{
			logger().setStatus( status );
		}
	}
}
