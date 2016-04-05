package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.util.mxGraphActions;

import fiji.plugin.trackmate.util.TrackNavigator;

public class TrackSchemeKeyboardHandler extends mxKeyboardHandler
{

	private final TrackNavigator navigator;

	public TrackSchemeKeyboardHandler( final TrackSchemeGraphComponent graphComponent, final TrackNavigator navigator )
	{
		super( graphComponent );
		this.navigator = navigator;
	}

	@Override
	protected InputMap getInputMap( final int condition )
	{
		InputMap map = null;

		if ( condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT )
		{
			map = ( InputMap ) UIManager.get( "ScrollPane.ancestorInputMap" );
		}
		else if ( condition == JComponent.WHEN_FOCUSED )
		{
			map = new InputMap();
		}

		map.put( KeyStroke.getKeyStroke( "F2" ), "edit" );
		map.put( KeyStroke.getKeyStroke( "DELETE" ), "delete" );

		map.put( KeyStroke.getKeyStroke( "HOME" ), "home" );
		map.put( KeyStroke.getKeyStroke( "END" ), "end" );

		map.put( KeyStroke.getKeyStroke( "ADD" ), "zoomIn" );
		map.put( KeyStroke.getKeyStroke( "EQUALS" ), "zoomIn" );
		map.put( KeyStroke.getKeyStroke( "EQUALS" ), "zoomIn" );
		map.put( KeyStroke.getKeyStroke( "SUBTRACT" ), "zoomOut" );
		map.put( KeyStroke.getKeyStroke( "MINUS" ), "zoomOut" );
		map.put( KeyStroke.getKeyStroke( "shift EQUALS" ), "resetZoom" );

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
	@Override
	protected ActionMap createActionMap()
	{
		final ActionMap map = ( ActionMap ) UIManager.get( "ScrollPane.actionMap" );

		map.put( "edit", TrackSchemeActions.getEditAction() );
		map.put( "delete", mxGraphActions.getDeleteAction() );

		map.put( "home", TrackSchemeActions.getHomeAction() );
		map.put( "end", TrackSchemeActions.getEndAction() );

		map.put( "zoomIn", TrackSchemeActions.getZoomInAction() );
		map.put( "zoomOut", TrackSchemeActions.getZoomOutAction() );
		map.put( "resetZoom", TrackSchemeActions.getResetZoomAction() );

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
