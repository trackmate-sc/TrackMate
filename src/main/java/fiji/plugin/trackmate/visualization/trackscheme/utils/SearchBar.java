package fiji.plugin.trackmate.visualization.trackscheme.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JTextField;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

@SuppressWarnings( "unchecked" )
public class SearchBar extends JTextField
{
	private static final long serialVersionUID = 1L;

	private final static Font NORMAL_FONT = TrackMateWizard.FONT.deriveFont( 10f );

	private final static Font NOTFOUND_FONT;
	static
	{
		@SuppressWarnings( "rawtypes" )
		final Map attributes = NORMAL_FONT.getAttributes();
		attributes.put( TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON );
		attributes.put( TextAttribute.FOREGROUND, Color.RED.darker() );
		NOTFOUND_FONT = new Font( attributes );
	}

	private final PropertyChangeSupport observer = new PropertyChangeSupport( this );

	private final Model model;

	private final TrackMateModelView view;


	/**
	 * Creates new form SearchBox
	 * 
	 * @param model
	 *            the model to search in.
	 * @param view
	 *            the view to update when a spot is found.
	 */
	public SearchBar( final Model model, final TrackMateModelView view )
	{
		this.model = model;
		this.view = view;
		putClientProperty( "JTextField.variant", "search" );
		putClientProperty( "JTextField.Search.Prompt", "Search" );
		setPreferredSize( new Dimension( 80, 25 ) );
		setFont( NORMAL_FONT );

		addFocusListener( new java.awt.event.FocusAdapter()
		{
			@Override
			public void focusGained( final java.awt.event.FocusEvent evt )
			{
				searchBoxFocusGained( evt );
			}

			@Override
			public void focusLost( final java.awt.event.FocusEvent evt )
			{
				searchBoxFocusLost( evt );
			}
		} );
		addKeyListener( new KeyAdapter()
		{
			@Override
			public void keyReleased( final KeyEvent e )
			{
				searchBoxKey( e );
			}
		} );
		observer.addPropertyChangeListener( new SearchAction() );
	}

	private void searchBoxKey( final KeyEvent e )
	{
		setFont( NORMAL_FONT );
		if ( getText().length() > 1 || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			observer.firePropertyChange( "Searching started", null, getText() );
		}
	}

	/**
	 * @param evt  
	 */
	private void searchBoxFocusGained( final java.awt.event.FocusEvent evt )
	{
		setFont( NORMAL_FONT );
		setFont( getFont().deriveFont( Font.PLAIN ) );
//		setText( null );
	}

	/**
	 * @param evt  
	 */
	private void searchBoxFocusLost( final java.awt.event.FocusEvent evt )
	{
		setFont( NORMAL_FONT );
		setFont( getFont().deriveFont( Font.ITALIC ) );
//		setText( "Search" );
	}

	private class SearchAction implements PropertyChangeListener, Iterator< Spot >
	{

		private Iterator< Spot > iterator;

		private Iterator< Integer > trackIterator;

		public SearchAction()
		{
			trackIterator = model.getTrackModel().trackIDs( true ).iterator();
			if ( trackIterator.hasNext() )
			{
				final Integer currentTrackID = trackIterator.next();
				final Spot trackStart = firstSpotOf( currentTrackID );
				iterator = model.getTrackModel().getSortedDepthFirstIterator( trackStart, Spot.nameComparator, false );
			}
			else
			{
				iterator = Collections.EMPTY_LIST.iterator();
			}
		}

		@Override
		public void propertyChange( final PropertyChangeEvent evt )
		{
			final String text = ( String ) evt.getNewValue();
			if ( !text.isEmpty() )
			{
				search( text );
			}
		}

		private void search( final String text )
		{
			Spot start = null;
			Spot spot;
			while ( ( spot = next() ) != start )
			{
				if ( start == null )
				{
					start = spot;
				}
				if ( spot.getName().contains( text ) )
				{
					view.centerViewOn( spot );
					return;
				}
			}
			setFont( NOTFOUND_FONT );
		}

		@Override
		public boolean hasNext()
		{
			return true;
		}

		@Override
		public Spot next()
		{
			if ( null == iterator || !iterator.hasNext() )
			{
				if ( null == trackIterator || !trackIterator.hasNext() )
				{
					trackIterator = model.getTrackModel().trackIDs( true ).iterator();
				}
				final Integer currentTrackID = trackIterator.next();
				final Spot trackStart = firstSpotOf( currentTrackID );
				iterator = model.getTrackModel().getSortedDepthFirstIterator( trackStart, Spot.nameComparator, false );
			}
			return iterator.next();
		}

		private Spot firstSpotOf( final Integer trackID )
		{
			final List< Spot > trackSpots = new ArrayList<>( model.getTrackModel().trackSpots( trackID ) );
			Collections.sort( trackSpots, Spot.frameComparator );
			return trackSpots.get( 0 );
		}

		@Override
		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}
}
