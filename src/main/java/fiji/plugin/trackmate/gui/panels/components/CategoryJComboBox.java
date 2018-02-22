package fiji.plugin.trackmate.gui.panels.components;

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;

import com.itextpdf.text.Font;

/**
 * A JcomboBox that displays categories, and return the category the selected
 * item belong to.
 * <p>
 * We have to extends <code>JComboBox&lt;Object&gt;</code>, because we want to
 * be able to insert both objects of type <code>K</code> and <code>V</code>.
 *
 *
 * @author Jean-Yves Tinevez, adapted from
 *         http://java-swing-tips.blogspot.fr/2010
 *         /03/non-selectable-jcombobox-items.html
 *
 * @param <K>
 *            the type of the category objects
 * @param <V>
 *            the type of the items
 */
public class CategoryJComboBox< K, V > extends JComboBox< Object >
{

	private static final long serialVersionUID = 1L;

	protected static final String INDENT = "  ";

	/** Indices of items that should be displayed as a category name. */
	private final HashSet< Integer > categoryIndexSet = new HashSet< >();

	private boolean isCategoryIndex = false;

	private final Map< V, String > itemNames;

	private final HashMap< V, K > invertMap;

	private final Map< K, String > categoryNames;

	/*
	 * CONSTRUCTOR
	 */

	public CategoryJComboBox( final Map< K, Collection< V >> items, final Map< V, String > itemNames, final Map< K, String > categoryNames )
	{
		super();
		this.invertMap = new HashMap< >();
		this.itemNames = itemNames;
		this.categoryNames = categoryNames;
		init();

		// Feed the combo box
		for ( final K category : items.keySet() )
		{
			addItem( category, true );

			final Collection< V > categoryItems = items.get( category );
			for ( final V item : categoryItems )
			{
				addItem( item, false );
				invertMap.put( item, category );
			}
		}
		if ( items.size() > 0 )
			setSelectedIndex( 1 );
	}

	/*
	 * METHODS
	 */

	public K getSelectedCategory()
	{
		final Object obj = getSelectedItem();
		return invertMap.get( obj );
	}

	public void setDisableIndex( final HashSet< Integer > set )
	{
		categoryIndexSet.clear();
		for ( final Integer i : set )
		{
			categoryIndexSet.add( i );
		}
	}

	@Override
	public void setPopupVisible( final boolean v )
	{
		if ( !v && isCategoryIndex )
		{
			isCategoryIndex = false;
		}
		else
		{
			super.setPopupVisible( v );
		}
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public V getSelectedItem()
	{
		return ( V ) super.getSelectedItem();
	}

	@Override
	public void setSelectedIndex( final int index )
	{
		if ( categoryIndexSet.contains( index ) )
		{
			isCategoryIndex = true;
		}
		else
		{
			super.setSelectedIndex( index );
		}
	}

	/*
	 * PRIVATE METHODS
	 */

	private void addItem( final Object anObject, final boolean isCategoryName )
	{
		super.addItem( anObject );
		if ( isCategoryName )
		{
			categoryIndexSet.add( getItemCount() - 1 );
		}
	}

	/**
	 * Called at instantiation: prepare the {@link JComboBox} with correct
	 * listeners and logic for categories.
	 */
	private void init()
	{
		setFont( SMALL_FONT );
		final ListCellRenderer< Object > r = getRenderer();
		setRenderer( new ListCellRenderer< Object >()
		{
			@Override
			public Component getListCellRendererComponent( final JList< ? extends Object > list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus )
			{
				JLabel c;
				if ( categoryIndexSet.contains( index ) )
				{
					c = ( JLabel ) r.getListCellRendererComponent( list, value, index, false, false );
					c.setEnabled( false );
					c.setFont( SMALL_FONT.deriveFont( Font.BOLD ) );
					c.setText( categoryNames.get( value ) );
				}
				else
				{
					c = ( JLabel ) r.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus );
					c.setEnabled( true );
					c.setFont( SMALL_FONT );
					c.setText( INDENT + itemNames.get( value ) );
				}
				return c;
			}
		} );

		final Action up = new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final int si = getSelectedIndex();
				for ( int i = si - 1; i >= 0; i-- )
				{
					if ( !categoryIndexSet.contains( i ) )
					{
						setSelectedIndex( i );
						break;
					}
				}
			}
		};
		final Action down = new AbstractAction()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final int si = getSelectedIndex();
				for ( int i = si + 1; i < getModel().getSize(); i++ )
				{
					if ( !categoryIndexSet.contains( i ) )
					{
						setSelectedIndex( i );
						break;
					}
				}
			}
		};

		final ActionMap am = getActionMap();
		am.put( "selectPrevious", up );
		am.put( "selectNext", down );
		final InputMap im = getInputMap();
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_UP, 0 ), "selectPrevious" );
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_KP_UP, 0 ), "selectPrevious" );
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_DOWN, 0 ), "selectNext" );
		im.put( KeyStroke.getKeyStroke( KeyEvent.VK_KP_DOWN, 0 ), "selectNext" );
	}

	/**
	 * Demo
	 */
	public static void main( final String[] args )
	{
		//
		final List< String > fruits = new ArrayList< >( 5 );
		fruits.add( "Apple" );
		fruits.add( "Pear" );
		fruits.add( "Orange" );
		fruits.add( "Strawberry" );
		//
		final List< String > cars = new ArrayList< >( 3 );
		cars.add( "Peugeot" );
		cars.add( "Ferrari" );
		cars.add( "Ford" );
		//
		final List< String > computers = new ArrayList< >( 2 );
		computers.add( "PC" );
		computers.add( "Mac" );
		//
		final LinkedHashMap< String, Collection< String >> items = new LinkedHashMap<>( 3 );
		items.put( "Fruits", fruits );
		items.put( "Cars", cars );
		items.put( "Computers", computers );
		//
		final Map< String, String > itemNames = new HashMap< >();
		for ( final String key : items.keySet() )
		{
			for ( final String string : items.get( key ) )
			{
				itemNames.put( string, string );
			}
		}
		//
		final Map< String, String > categoryNames = new HashMap< >();
		for ( final String key : items.keySet() )
		{
			categoryNames.put( key, key );
		}
		// Ouf!

		final CategoryJComboBox< String, String > cb = new CategoryJComboBox< >( items, itemNames, categoryNames );
		cb.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				System.out.println( "Selected " + cb.getSelectedItem() + " in category " + cb.getSelectedCategory() );
			}
		} );

		final JFrame frame = new JFrame();
		frame.getContentPane().add( cb );
		frame.setVisible( true );

	}
}
