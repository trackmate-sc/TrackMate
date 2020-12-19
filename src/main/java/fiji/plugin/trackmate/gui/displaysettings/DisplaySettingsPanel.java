package fiji.plugin.trackmate.gui.displaysettings;

import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.booleanElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.doubleElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.intElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedCheckBox;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedSliderPanel;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.separator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BooleanElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.DoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.IntElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.Separator;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElementVisitor;

public class DisplaySettingsPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private static final int tfCols = 4;

	private final List< StyleElement > styleElements;

	public DisplaySettingsPanel( final DisplaySettings style )
	{
		super( new GridBagLayout() );

		styleElements = styleElements( style );

		style.listeners().add( () -> {
			styleElements.forEach( StyleElement::update );
			repaint();
		} );

		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets( 0, 5, 0, 5 );
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;

		styleElements.forEach( element -> element.accept(
				new StyleElementVisitor()
				{
					@Override
					public void visit( final Separator element )
					{
						add( Box.createVerticalStrut( 10 ), c );
						++c.gridy;
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
					public void visit( final DoubleElement element )
					{
						addToLayout(
								linkedSliderPanel( element, tfCols ),
								new JLabel( element.getLabel() ) );
					}

					@Override
					public void visit( final IntElement element )
					{
						addToLayout(
								linkedSliderPanel( element, tfCols ),
								new JLabel( element.getLabel() ) );
					}

					private void addToLayout( final JComponent comp1, final JComponent comp2 )
					{
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
				} ) );
	}

	private List< StyleElement > styleElements( final DisplaySettings style )
	{
		return Arrays.asList(
				booleanElement( "anti-aliasing", style::getUseAntialiasing, style::setUseAntialiasing ),

				separator(),

				booleanElement( "draw links", style::getDrawLinks, style::setDrawLinks ),
				intElement( "time range for links", 0, 100, style::getTimeLimit, style::setTimeLimit ),
				booleanElement( "gradients for links", style::getUseGradient, style::setUseGradient ),

				separator(),

				booleanElement( "draw spots", style::getDrawSpots, style::setDrawSpots ),
				booleanElement( "ellipsoid intersection", style::getDrawEllipsoidSliceIntersection, style::setDrawEllipsoidSliceIntersection ),
				booleanElement( "ellipsoid projection", style::getDrawEllipsoidSliceProjection, style::setDrawEllipsoidSliceProjection ),
				booleanElement( "draw spot centers", style::getDrawSpotCenters, style::setDrawSpotCenters ),
				booleanElement( "draw spot centers for ellipses", style::getDrawSpotCentersForEllipses, style::setDrawSpotCentersForEllipses ),
				booleanElement( "draw spot labels", style::getDrawSpotLabels, style::setDrawSpotLabels ),

				separator(),

				doubleElement( "focus limit (max dist to view plane)", 0, 2000, style::getFocusLimit, style::setFocusLimit ),
				booleanElement( "view relative focus limit", style::getFocusLimitViewRelative, style::setFocusLimitViewRelative ),

				separator(),
				doubleElement( "ellipsoid fade depth", 0, 1, style::getEllipsoidFadeDepth, style::setEllipsoidFadeDepth ),
				doubleElement( "center point fade depth", 0, 1, style::getPointFadeDepth, style::setPointFadeDepth )
		);
	}
}
