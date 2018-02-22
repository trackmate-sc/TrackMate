package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.mxgraph.swing.util.mxGraphActions;

import fiji.plugin.trackmate.util.TrackNavigator;

public class TrackSchemeKeyboardHandler
{

	private final TrackNavigator navigator;

	private final TrackSchemeGraphComponent graphComponent;

	public TrackSchemeKeyboardHandler( final TrackSchemeGraphComponent graphComponent, final TrackNavigator navigator )
	{
		this.graphComponent = graphComponent;
		this.navigator = navigator;
	}

	public void installKeyboardActions( final JComponent component )
	{
		final InputMap inputMap = getInputMap( JComponent.WHEN_FOCUSED );
		SwingUtilities.replaceUIInputMap( component,
				JComponent.WHEN_FOCUSED, inputMap );
		SwingUtilities.replaceUIActionMap( component, createActionMap() );
	}

	protected InputMap getInputMap( final int condition )
	{
		final InputMap map;
		if ( condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT )
			map = ( InputMap ) UIManager.get( "ScrollPane.ancestorInputMap" );
		else 
			map = new InputMap();

		map.put( KeyStroke.getKeyStroke( "F2" ), "edit" );
		map.put( KeyStroke.getKeyStroke( "DELETE" ), "delete" );

		map.put( KeyStroke.getKeyStroke( "HOME" ), "home" );
		map.put( KeyStroke.getKeyStroke( "END" ), "end" );

		map.put( KeyStroke.getKeyStroke( "ADD" ), "zoomIn" );
		map.put( KeyStroke.getKeyStroke( "EQUALS" ), "zoomIn" );
		map.put( KeyStroke.getKeyStroke( "SUBTRACT" ), "zoomOut" );
		map.put( KeyStroke.getKeyStroke( "MINUS" ), "zoomOut" );
		map.put( KeyStroke.getKeyStroke( "shift EQUALS" ), "resetZoom" );

		map.put( KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD4, 0 ), "panLeft" );
		map.put( KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD6, 0 ), "panRight" );
		map.put( KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD8, 0 ), "panUp" );
		map.put( KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD2, 0 ), "panDown" );
		map.put( KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD9, 0 ), "panUpRight" );
		map.put( KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD3, 0 ), "panDownRight" );
		map.put( KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD1, 0 ), "panDownLeft" );
		map.put( KeyStroke.getKeyStroke( KeyEvent.VK_NUMPAD7, 0 ), "panUpLeft" );

		map.put( KeyStroke.getKeyStroke( "control A" ), "selectAll" );
		map.put( KeyStroke.getKeyStroke( "control shift A" ), "selectNone" );

		map.put( KeyStroke.getKeyStroke( "UP" ), "selectPreviousInTime" );
		map.put( KeyStroke.getKeyStroke( "DOWN" ), "selectNextInTime" );
		map.put( KeyStroke.getKeyStroke( "RIGHT" ), "selectNextSibling" );
		map.put( KeyStroke.getKeyStroke( "LEFT" ), "selectPreviousSibling" );
		map.put( KeyStroke.getKeyStroke( "PAGE_DOWN" ), "selectNextTrack" );
		map.put( KeyStroke.getKeyStroke( "PAGE_UP" ), "selectPreviousTrack" );

		return map;
	}

	/**
	 * Return the mapping between JTree's input map and JGraph's actions.
	 */
	protected ActionMap createActionMap()
	{
		final ActionMap map = ( ActionMap ) UIManager.get( "ScrollPane.actionMap" );

		map.put( "edit", TrackSchemeActions.getEditAction( graphComponent ) );
		map.put( "delete", mxGraphActions.getDeleteAction() );

		map.put( "home", TrackSchemeActions.getHomeAction( graphComponent ) );
		map.put( "end", TrackSchemeActions.getEndAction( graphComponent ) );

		map.put( "zoomIn", TrackSchemeActions.getZoomInAction( graphComponent ) );
		map.put( "zoomOut", TrackSchemeActions.getZoomOutAction( graphComponent ) );
		map.put( "resetZoom", TrackSchemeActions.getResetZoomAction( graphComponent ) );

		map.put( "panUp", TrackSchemeActions.getPanUpAction( graphComponent ) );
		map.put( "panDown", TrackSchemeActions.getPanDownAction( graphComponent ) );
		map.put( "panLeft", TrackSchemeActions.getPanLeftAction( graphComponent ) );
		map.put( "panRight", TrackSchemeActions.getPanRightAction( graphComponent ) );
		map.put( "panUpLeft", TrackSchemeActions.getPanUpLeftAction( graphComponent ) );
		map.put( "panDownLeft", TrackSchemeActions.getPanDownLeftAction( graphComponent ) );
		map.put( "panUpRight", TrackSchemeActions.getPanUpRightAction( graphComponent ) );
		map.put( "panDownRight", TrackSchemeActions.getPanDownRightAction( graphComponent ) );

		map.put( "selectNone", TrackSchemeActions.getSelectNoneAction() );
		map.put( "selectAll", TrackSchemeActions.getSelectAllAction() );

		map.put( "selectPreviousInTime", new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				navigator.previousInTime();
			}
		} );
		map.put( "selectNextInTime", new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				navigator.nextInTime();
			}
		} );
		map.put( "selectNextSibling", new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				navigator.nextSibling();
			}
		} );
		map.put( "selectPreviousSibling", new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				navigator.previousSibling();
			}
		} );
		map.put( "selectNextTrack", new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				navigator.nextTrack();
			}
		} );
		map.put( "selectPreviousTrack", new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				navigator.previousTrack();
			}
		} );

		return map;
	}
}
