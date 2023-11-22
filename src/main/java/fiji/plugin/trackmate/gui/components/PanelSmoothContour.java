package fiji.plugin.trackmate.gui.components;

import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fiji.plugin.trackmate.gui.displaysettings.SliderPanelDouble;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;

public class PanelSmoothContour extends JPanel
{

	private static final long serialVersionUID = 1L;

	private double scale;

	private double previousValue;

	private final SliderPanelDouble sliderPanel;

	private final JCheckBox chckbxSmooth;

	public PanelSmoothContour( final double scale, final String units )
	{
		this.scale = scale;
		setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );

		chckbxSmooth = new JCheckBox( "Smooth" );
		chckbxSmooth.setFont( SMALL_FONT );
		add( chckbxSmooth );
		add( Box.createHorizontalGlue() );

		final DoubleSupplier getter = () -> getScale();
		final Consumer< Double > setter = v -> setScalePrivate( v );
		final BoundedDoubleElement scaleEl = StyleElements.boundedDoubleElement( "scale", 0., 20., getter, setter );
		sliderPanel = StyleElements.linkedSliderPanel( scaleEl, 2 );
		sliderPanel.setFont( SMALL_FONT );

		add( sliderPanel );
		add( Box.createHorizontalStrut( 5 ) );
		final JLabel lblUnits = new JLabel( units );
		lblUnits.setFont( SMALL_FONT );
		
		chckbxSmooth.addActionListener( e -> refresh() );
		refresh();
	}

	private void refresh()
	{
		final boolean selected = chckbxSmooth.isSelected();
		if ( !selected )
		{
			previousValue = this.scale;
			this.scale = -.1;
		}
		else
		{
			this.scale = previousValue;
		}
		sliderPanel.setEnabled( selected );
	}

	private void setScalePrivate( final double scale )
	{
		this.scale = scale;
	}

	public void setScale( final double scale )
	{
		setScalePrivate( scale );
		refresh();
	}

	public double getScale()
	{
		return scale;
	}
}
