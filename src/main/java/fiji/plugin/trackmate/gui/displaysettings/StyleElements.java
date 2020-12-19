package fiji.plugin.trackmate.gui.displaysettings;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class StyleElements
{
	public static Separator separator()
	{
		return new Separator();
	}

	public static LabelElement label( final String label )
	{
		return new LabelElement( label );
	}

	public static BooleanElement booleanElement( final String label, final BooleanSupplier get, final Consumer< Boolean > set )
	{
		return new BooleanElement( label )
		{
			@Override
			public boolean get()
			{
				return get.getAsBoolean();
			}

			@Override
			public void set( final boolean b )
			{
				set.accept( b );
			}
		};
	}

	public static ColorElement colorElement( final String label, final Supplier< Color > get, final Consumer< Color > set )
	{
		return new ColorElement( label )
		{
			@Override
			public Color getColor()
			{
				return get.get();
			}

			@Override
			public void setColor( final Color c )
			{
				set.accept( c );
			}
		};
	}

	public static DoubleElement doubleElement( final String label, final double rangeMin, final double rangeMax, final DoubleSupplier get, final Consumer< Double > set )
	{
		return new DoubleElement( label, rangeMin, rangeMax )
		{
			@Override
			public double get()
			{
				return get.getAsDouble();
			}

			@Override
			public void set( final double v )
			{
				set.accept( v );
			}
		};
	}

	public static IntElement intElement( final String label, final int rangeMin, final int rangeMax, final IntSupplier get, final Consumer< Integer > set )
	{
		return new IntElement( label, rangeMin, rangeMax )
		{
			@Override
			public int get()
			{
				return get.getAsInt();
			}

			@Override
			public void set( final int v )
			{
				set.accept( v );
			}
		};
	}

	public interface StyleElementVisitor
	{
		public default void visit( final Separator element )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final LabelElement label )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final ColorElement colorElement )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final BooleanElement booleanElement )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final DoubleElement doubleElement )
		{
			throw new UnsupportedOperationException();
		}

		public default void visit( final IntElement intElement )
		{
			throw new UnsupportedOperationException();
		}
	}

	/*
	 *
	 * ===============================================================
	 *
	 */

	public interface StyleElement
	{
		public default void update()
		{
		}

		public void accept( StyleElementVisitor visitor );
	}

	public static class Separator implements StyleElement
	{
		@Override
		public void accept( final StyleElementVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	public static class LabelElement implements StyleElement
	{
		private final String label;

		public LabelElement( final String label )
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}

		@Override
		public void accept( final StyleElementVisitor visitor )
		{
			visitor.visit( this );
		}
	}

	public static abstract class ColorElement implements StyleElement
	{
		private final ArrayList< Consumer< Color > > onSet = new ArrayList<>();

		private final String label;

		public ColorElement( final String label )
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}

		@Override
		public void accept( final StyleElementVisitor visitor )
		{
			visitor.visit( this );
		}

		public void onSet( final Consumer< Color > set )
		{
			onSet.add( set );
		}

		@Override
		public void update()
		{
			onSet.forEach( c -> c.accept( getColor() ) );
		}

		public abstract Color getColor();

		public abstract void setColor( Color c );
	}

	public static abstract class BooleanElement implements StyleElement
	{
		private final String label;

		private final ArrayList< Consumer< Boolean > > onSet = new ArrayList<>();

		public BooleanElement( final String label )
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}

		@Override
		public void accept( final StyleElementVisitor visitor )
		{
			visitor.visit( this );
		}

		public void onSet( final Consumer< Boolean > set )
		{
			onSet.add( set );
		}

		@Override
		public void update()
		{
			onSet.forEach( c -> c.accept( get() ) );
		}

		public abstract boolean get();

		public abstract void set( boolean b );
	}

	public static abstract class DoubleElement implements StyleElement
	{
		private final BoundedValueDouble value;

		private final String label;

		public DoubleElement( final String label, final double rangeMin, final double rangeMax )
		{
			final double currentValue = Math.max( rangeMin, Math.min( rangeMax, get() ) );
			value = new BoundedValueDouble( rangeMin, rangeMax, currentValue )
			{
				@Override
				public void setCurrentValue( final double value )
				{
					super.setCurrentValue( value );
					if ( get() != getCurrentValue() )
						set( getCurrentValue() );
				}
			};
			this.label = label;
		}

		public BoundedValueDouble getValue()
		{
			return value;
		}

		public String getLabel()
		{
			return label;
		}

		@Override
		public void accept( final StyleElementVisitor visitor )
		{
			visitor.visit( this );
		}

		public abstract double get();

		public abstract void set( double v );

		@Override
		public void update()
		{
			if ( get() != value.getCurrentValue() )
				value.setCurrentValue( get() );
		}
	}

	public static abstract class IntElement implements StyleElement
	{
		private final BoundedValue value;

		private final String label;

		public IntElement( final String label, final int rangeMin, final int rangeMax )
		{
			final int currentValue = Math.max( rangeMin, Math.min( rangeMax, get() ) );
			value = new BoundedValue( rangeMin, rangeMax, currentValue )
			{
				@Override
				public void setCurrentValue( final int value )
				{
					super.setCurrentValue( value );
					if ( get() != getCurrentValue() )
						set( getCurrentValue() );
				}
			};
			this.label = label;
		}

		public BoundedValue getValue()
		{
			return value;
		}

		public String getLabel()
		{
			return label;
		}

		@Override
		public void accept( final StyleElementVisitor visitor )
		{
			visitor.visit( this );
		}

		public abstract int get();

		public abstract void set( int v );

		@Override
		public void update()
		{
			if ( get() != value.getCurrentValue() )
				value.setCurrentValue( get() );
		}
	}

	/*
	 *
	 * ===============================================================
	 *
	 */

	public static JLabel linkedLabel( final LabelElement element )
	{
		return new JLabel( element.getLabel() );
	}

	public static JCheckBox linkedCheckBox( final BooleanElement element, final String label )
	{
		final JCheckBox checkbox = new JCheckBox( label, element.get() );
		checkbox.setFocusable( false );
		checkbox.addActionListener( ( e ) -> element.set( checkbox.isSelected() ) );
		element.onSet( b -> {
			if ( b != checkbox.isSelected() )
				checkbox.setSelected( b );
		} );
		return checkbox;
	}

	public static JButton linkedColorButton( final ColorElement element, final String label, final JColorChooser colorChooser )
	{
		final ColorIcon icon = new ColorIcon( element.getColor(), 16, 2 );
		final JButton button = new JButton( label, icon );
		button.setOpaque( false );
		button.setContentAreaFilled( false );
		button.setBorderPainted( false );
		button.setFont( new JButton().getFont() );
		button.setMargin( new Insets( 0, 0, 0, 0 ) );
		button.setBorder( new EmptyBorder( 2, 5, 2, 2 ) );
		button.setHorizontalAlignment( SwingConstants.LEFT );
		button.setFocusable( false );
		button.addActionListener( e -> {
			colorChooser.setColor( element.getColor() );
			final JDialog d = JColorChooser.createDialog( button, "Choose a color", true, colorChooser, new ActionListener()
			{
				@Override
				public void actionPerformed( final ActionEvent arg0 )
				{
					final Color c = colorChooser.getColor();
					if ( c != null )
					{
						icon.setColor( c );
						button.repaint();
						element.setColor( c );
					}
				}
			}, null );
			d.setVisible( true );
		} );
		element.onSet( icon::setColor );
		return button;
	}

	public static SliderPanel linkedSliderPanel( final IntElement element, final int tfCols )
	{
		final SliderPanel slider = new SliderPanel( null, element.getValue(), 1 );
		slider.setNumColummns( tfCols );
		slider.setBorder( new EmptyBorder( 0, 0, 0, 6 ) );
		return slider;
	}

	public static SliderPanelDouble linkedSliderPanel( final DoubleElement element, final int tfCols )
	{
		final SliderPanelDouble slider = new SliderPanelDouble( null, element.getValue(), 1 );
		slider.setDecimalFormat( "0.####" );
		slider.setNumColummns( tfCols );
		slider.setBorder( new EmptyBorder( 0, 0, 0, 6 ) );
		return slider;
	}
}
