package fiji.plugin.trackmate.gui.displaysettings;

import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.booleanElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.boundedDoubleElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.colorElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.colormapElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.doubleElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.enumElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.featureElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.fontElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.intElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.label;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedCheckBox;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedColorButton;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedColormapChooser;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedFeatureSelector;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedFontButton;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedFormattedTextField;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedSliderPanel;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedSpinnerEnumSelector;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.separator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import com.itextpdf.text.Font;

import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackDisplayMode;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BooleanElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.ColorElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.ColormapElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.DoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.EnumElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.FeatureElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.FontElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.IntElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.LabelElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.Separator;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElementVisitor;

public class DisplaySettingsPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private static final int tfCols = 4;

	private final JColorChooser colorChooser;

	private final List< StyleElement > styleElements;

	public DisplaySettingsPanel( final DisplaySettings ds )
	{
		super( new GridBagLayout() );

		colorChooser = new JColorChooser();
		styleElements = styleElements( ds );

		ds.listeners().add( () -> {
			styleElements.forEach( StyleElement::update );
			repaint();
		} );

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;

		c.insets = new Insets( 2, 5, 2, 5 );

		styleElements.forEach( element -> element.accept(
				new StyleElementVisitor()
				{
					@Override
					public void visit( final Separator element )
					{
						add( Box.createVerticalStrut( 10 ), c );
						++c.gridy;
						addToLayout( new JSeparator( JSeparator.HORIZONTAL ) );
					}

					@Override
					public void visit( final LabelElement element )
					{
						final JLabel label = new JLabel( element.getLabel() );
						label.setFont( getFont().deriveFont( Font.BOLD ).deriveFont( getFont().getSize() + 2f ) );
						addToLayout( label );
					}

					@Override
					public void visit( final BooleanElement element )
					{
						final JCheckBox checkbox = linkedCheckBox( element, "" );
						checkbox.setHorizontalAlignment( SwingConstants.TRAILING );
						addToLayout(
								checkbox,
								new JLabel( element.getLabel() ) );
					}

					@Override
					public void visit( final BoundedDoubleElement element )
					{
						addToLayout(
								linkedSliderPanel( element, tfCols, 0.1 ),
								new JLabel( element.getLabel() ) );
					}

					@Override
					public void visit( final DoubleElement element )
					{
						addToLayout(
								linkedFormattedTextField( element ),
								new JLabel( element.getLabel() ) );
					}

					@Override
					public void visit( final IntElement element )
					{
						addToLayout(
								linkedSliderPanel( element, tfCols ),
								new JLabel( element.getLabel() ) );
					}

					@Override
					public void visit( final ColorElement element )
					{
						addToLayoutFlushRight(
								linkedColorButton( element, colorChooser ),
								new JLabel( element.getLabel() ) );
					}

					@Override
					public void visit( final FeatureElement element )
					{
						addToLayout(
								linkedFeatureSelector( element ),
								new JLabel( element.getLabel() ) );
					}

					@Override
					public < E > void visit( final EnumElement< E > element )
					{
						addToLayout(
								linkedSpinnerEnumSelector( element ),
								new JLabel( element.getLabel() ) );
					}

					@Override
					public void visit( final ColormapElement element )
					{
						addToLayout(
								linkedColormapChooser( element ),
								new JLabel( element.getLabel() ) );
					}

					@Override
					public void visit( final FontElement element )
					{
						addToLayout(
								linkedFontButton( element ),
								new JLabel( element.getLabel() ) );
					}

					private void addToLayout( final JComponent comp1, final JComponent comp2 )
					{
						c.gridwidth = 1;
						c.anchor = GridBagConstraints.LINE_END;
						add( comp1, c );
						c.gridx++;
						c.weightx = 0.0;
						c.anchor = GridBagConstraints.LINE_START;
						add( comp2, c );
						c.gridx = 0;
						c.weightx = 1.0;
						c.gridy++;
					}

					private void addToLayoutFlushRight( final JComponent comp1, final JComponent comp2 )
					{
						c.fill =
								c.gridwidth = 1;
						c.fill = GridBagConstraints.NONE;
						c.anchor = GridBagConstraints.EAST;
						add( comp1, c );
						c.gridx++;
						c.weightx = 0.0;
						c.fill = GridBagConstraints.HORIZONTAL;
						c.anchor = GridBagConstraints.LINE_START;
						add( comp2, c );
						c.gridx = 0;
						c.weightx = 1.0;
						c.gridy++;
					}

					private void addToLayout( final JComponent comp )
					{
						c.gridwidth = 2;
						c.anchor = GridBagConstraints.LINE_START;
						c.gridx = 0;
						c.weightx = 1.0;
						add( comp, c );
						c.gridy++;
					}
				} ) );
	}


	private List< StyleElement > styleElements( final DisplaySettings ds )
	{
		return Arrays.asList(

				separator(),

				label( "Spots" ),
				booleanElement( "draw spots", ds::isSpotVisible, ds::setSpotVisible ),
				booleanElement( "draw spots as ROIs", ds::isSpotDisplayedAsRoi, ds::setSpotDisplayedAsRoi ),
				booleanElement( "draw spots filled", ds::isSpotFilled, ds::setSpotFilled ),
				boundedDoubleElement( "spot alpha transparency", 0., 1., ds::getSpotTransparencyAlpha, ds::setSpotTransparencyAlpha ),
				booleanElement( "show spot names", ds::isSpotShowName, ds::setSpotShowName ),
				boundedDoubleElement( "spot radius ratio", 0., 20., ds::getSpotDisplayRadius, ds::setSpotDisplayRadius ),
				featureElement( "spot color", ds::getSpotColorByType, ds::getSpotColorByFeature, ( type, feature ) -> ds.setSpotColorBy( type, feature ) ),
				doubleElement( "spot display min", ds::getSpotMin, m -> ds.setSpotMinMax( m, ds.getSpotMax() ) ),
				doubleElement( "spot display max", ds::getSpotMax, m -> ds.setSpotMinMax( ds.getSpotMin(), m ) ),
				colorElement( "spot uniform color", ds::getSpotUniformColor, ds::setSpotUniformColor ),

				separator(),

				label( "Tracks" ),
				booleanElement( "draw tracks", ds::isTrackVisible, ds::setTrackVisible ),
				enumElement( "track display mode", TrackDisplayMode.values(), ds::getTrackDisplayMode, ds::setTrackDisplayMode ),
				featureElement( "track color", ds::getTrackColorByType, ds::getTrackColorByFeature, ( type, feature ) -> ds.setTrackColorBy( type, feature ) ),
				doubleElement( "track display min", ds::getTrackMin, m -> ds.setTrackMinMax( m, ds.getTrackMax() ) ),
				doubleElement( "track display max", ds::getTrackMax, m -> ds.setTrackMinMax( ds.getTrackMin(), m ) ),
				colorElement( "track uniform color", ds::getTrackUniformColor, ds::setTrackUniformColor ),
				booleanElement( "fade track in time", ds::isFadeTracks, ds::setFadeTracks ),
				intElement( "track fade range", 0, 500, ds::getFadeTrackRange, ds::setFadeTrackRange ),

				separator(),

				label( "General" ),

				colormapElement( "colormap", ds::getColormap, ds::setColormap ),

				booleanElement( "limit Z-depth", ds::isZDrawingDepthLimited, ds::setZDrawingDepthLimited ),
				boundedDoubleElement( "drawing Z-depth", 0., 1000., ds::getZDrawingDepth, ds::setZDrawingDepth ),

				colorElement( "selection color", ds::getHighlightColor, ds::setHighlightColor ),
				colorElement( "color for missing values", ds::getMissingValueColor, ds::setMissingValueColor ),
				colorElement( "color for undefined values", ds::getUndefinedValueColor, ds::setUndefinedValueColor ),

				boundedDoubleElement( "line thickness", 0., 10., ds::getLineThickness, ds::setLineThickness ),
				boundedDoubleElement( "selection line thickness", 0., 10., ds::getSelectionLineThickness, ds::setSelectionLineThickness ),
				fontElement( "font", ds::getFont, ds::setFont ),

				booleanElement( "anti-aliasing", ds::getUseAntialiasing, ds::setUseAntialiasing ),

				separator(),

				label( "TrackScheme" ),

				colorElement( "foreground color", ds::getTrackSchemeForegroundColor, ds::setTrackSchemeForegroundColor ),
				colorElement( "background color 1", ds::getTrackSchemeBackgroundColor1, ds::setTrackSchemeBackgroundColor1 ),
				colorElement( "background color 2", ds::getTrackSchemeBackgroundColor2, ds::setTrackSchemeBackgroundColor2 ),
				colorElement( "decoration color", ds::getTrackSchemeDecorationColor, ds::setTrackSchemeDecorationColor ),
				booleanElement( "fill box", ds::isTrackSchemeFillBox, ds::setTrackschemeFillBox ),

				separator() );
	}
}
