package fiji.plugin.trackmate.gui.panels.components;

import fiji.util.NumberParser;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.text.Document;

public class JNumericTextField extends JTextField
{

	/*
	 * FIELDS
	 */

	private static final long serialVersionUID = 1L;

	private static final Border BORDER_FOCUSED = new LineBorder( new Color( 252, 117, 0 ), 1, true );

	private static final Border BORDER_UNFOCUSED = new LineBorder( new Color( 150, 150, 150 ), 1, true );
	
	private static final String DEFAULT_FORMAT = "%.3f";

	private final ActionListener al = new ActionListener()
	{

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			checkInput();
		}
	};

	private double value = 0;

	private double oldValue = 0;

	private String format = DEFAULT_FORMAT;

	/*
	 * CONSTRUCTORS
	 */

	public JNumericTextField( final Document doc, final String text, final int columns )
	{
		super( doc, text, columns );
		setBorder( BORDER_UNFOCUSED );
		if ( text != null )
		{
			try
			{
				value = NumberParser.parseDouble( text );
				oldValue = value;
			}
			catch ( final NumberFormatException nfe )
			{
				oldValue = 0;
				value = 0;
			}
		}
		addActionListener( al );
		addFocusListener( new FocusListener()
		{
			@Override
			public void focusLost( final FocusEvent e )
			{
				checkInput();
				setBorder( BORDER_UNFOCUSED );
			}

			@Override
			public void focusGained( final FocusEvent e )
			{
				setBorder( BORDER_FOCUSED );
			}
		} );
	}

	public JNumericTextField( final int columns )
	{
		this( null, null, columns );
	}

	public JNumericTextField( final String text, final int columns )
	{
		this( null, text, columns );
	}

	public JNumericTextField( final String text )
	{
		this( null, text, 0 );
	}

	public JNumericTextField()
	{
		this( null, null, 0 );
	}

	public JNumericTextField( final double value )
	{
		this( String.format( DEFAULT_FORMAT, value ) );
	}

	/*
	 * METHODS
	 */

	public double getValue()
	{
		checkInput();
		return value;
	}

	public void setValue( final double value )
	{
		setText( String.format( format, value ) );
	}

	public String getFormat()
	{
		return format;
	}

	public void setFormat( final String format )
	{
		this.format = format;
		checkInput();
	}

	private void checkInput()
	{
		final String str = getText();
		try
		{
			value = NumberParser.parseDouble( str );
			oldValue = value;
			setText( String.format( format, value ) );
		}
		catch ( final NumberFormatException nfe )
		{
			value = oldValue;
			setText( String.format( format, value ) );
		}

	}

}
