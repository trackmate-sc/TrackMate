package fiji.plugin.trackmate.util.cli;

import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.booleanElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.boundedDoubleElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.doubleElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.intElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedCheckBox;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedComboBoxSelector;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedFormattedTextField;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedSliderPanel;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.linkedTextField;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.listElement;
import static fiji.plugin.trackmate.gui.displaysettings.StyleElements.stringElement;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.Fonts;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BooleanElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.DoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.IntElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.ListElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StringElement;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.ArgumentVisitor;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.ChoiceArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.DoubleArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.Flag;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.IntArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.StringArgument;

public class CliGuiBuilder implements ArgumentVisitor
{

	private static final int tfCols = 4;

	private final JPanel panel;

	private final GridBagConstraints c;

	private CliGuiBuilder()
	{
		this.panel = new JPanel( new GridBagLayout() );
		panel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		this.c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;

	}

	@Override
	public void visit( final Flag flag )
	{
		final BooleanElement element = booleanElement( flag.getName(), flag::isSet, flag::set );
		final JCheckBox checkbox = linkedCheckBox( element, "" );
		checkbox.setHorizontalAlignment( SwingConstants.LEADING );
		addToLayout(
				new JLabel( element.getLabel() ),
				checkbox );
	}

	@Override
	public void visit( final IntArgument arg )
	{
		final IntElement element = intElement( arg.getName(),
				arg.getMin(), arg.getMax(), arg::getValue, arg::set );

		final int numberOfColumns;
		if ( arg.isMinSet() && arg.isMinSet() )
		{
			final int largest = Math.max( Math.abs( arg.getMin() ), Math.abs( arg.getMax() ) );
			final String numberString = String.valueOf( largest );
			numberOfColumns = numberString.length() + 1;
		}
		else
		{
			numberOfColumns = tfCols;
		}
		addToLayout(
				new JLabel( element.getLabel() ),
				linkedSliderPanel( element, numberOfColumns ) );
	}

	@Override
	public void visit( final DoubleArgument arg )
	{
		if ( arg.isMinSet() && arg.isMaxSet() )
		{
			final BoundedDoubleElement element = boundedDoubleElement( arg.getName(),
					arg.getMin(), arg.getMax(), arg::getValue, arg::set );
			addToLayout(
					new JLabel( element.getLabel() ),
					linkedSliderPanel( element, tfCols, 0.1 ) );
		}
		else
		{
			final DoubleElement element = doubleElement( arg.getName(), arg::getValue, arg::set );
			addToLayout(
					new JLabel( element.getLabel() ),
					linkedFormattedTextField( element ) );
		}
	}

	@Override
	public void visit( final StringArgument arg )
	{
		final StringElement element = stringElement( arg.getName(), arg::getValue, arg::set );
		addToLayoutTwoLines(
				new JLabel( element.getLabel() ),
				linkedTextField( element ) );
	}

	@Override
	public void visit( final ChoiceArgument arg )
	{
		final ListElement< String > element = listElement( arg.getName(), arg.getChoices(), arg::getValue, arg::set );
		addToLayout(
				new JLabel( element.getLabel() ),
				linkedComboBoxSelector( element ) );
	}

	private void addToLayoutTwoLines( final JLabel lbl, final JComponent comp )
	{
		lbl.setText( lbl.getText() + ": " );
		lbl.setFont( Fonts.SMALL_FONT );
		c.insets = new Insets( 5, 0, 0, 0 );
		c.gridwidth = 2;
		panel.add( lbl, c );
		c.gridy++;
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets( 0, 0, 5, 0 );
		panel.add( comp, c );
		c.gridy++;
	}

	private void addToLayout( final JLabel lbl, final JComponent comp )
	{
		lbl.setText( lbl.getText() + ": " );
		lbl.setFont( Fonts.SMALL_FONT );
		lbl.setHorizontalAlignment( JLabel.RIGHT );
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add( lbl, c );

		c.gridx++;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add( comp, c );

		c.weightx = 0.0;
		c.gridx = 0;
		c.gridy++;
		c.insets = new Insets( 5, 0, 5, 0 );
	}

	public static JPanel build( final CLIConfigurator cli )
	{
		final CliGuiBuilder builder = new CliGuiBuilder();
		cli.getArguments().forEach( arg -> arg.accept( builder ) );
		return builder.panel;
	}
}
