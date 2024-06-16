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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.Fonts;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BooleanElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.DoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.IntElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.ListElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StringElement;
import fiji.plugin.trackmate.util.FileChooser;
import fiji.plugin.trackmate.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.Argument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.ArgumentVisitor;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.ChoiceArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.DoubleArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.ExecutablePath;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.Flag;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.IntArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.PathArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.StringArgument;

public class CliGuiBuilder implements ArgumentVisitor
{

	private static final int tfCols = 4;

	private final JPanel panel;

	private final GridBagConstraints c;

	private CliGuiBuilder( final ExecutablePath executableArg )
	{
		this.panel = new JPanel();
		final GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[] { 0., 1., 0. };
		panel.setLayout( layout );
		panel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		this.c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;

		visit( executableArg );
	}

	@Override
	public void visit( final ExecutablePath arg )
	{
		final StringElement element = stringElement( arg.getName(), arg::getValue, arg::set );
		addToPathLayout(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				linkedTextField( element ) );

		panel.add( Box.createVerticalStrut( 10 ), c );
		final JSeparator separator = new JSeparator( JSeparator.HORIZONTAL );
		separator.setMinimumSize( new Dimension( 10, 10 ) );
		addToLayout( null, separator );
	}

	@Override
	public void visit( final Flag flag )
	{
		final BooleanElement element = booleanElement( flag.getName(), flag::isSet, flag::set );
		final JCheckBox checkbox = linkedCheckBox( element, "" );
		checkbox.setHorizontalAlignment( SwingConstants.LEADING );
		addToLayout(
				flag.getHelp(),
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
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				linkedSliderPanel( element, numberOfColumns ),
				arg.getUnits() );
	}

	@Override
	public void visit( final DoubleArgument arg )
	{
		if ( arg.isMinSet() && arg.isMaxSet() )
		{
			final BoundedDoubleElement element = boundedDoubleElement( arg.getName(),
					arg.getMin(), arg.getMax(), arg::getValue, arg::set );
			addToLayout(
					arg.getHelp(),
					new JLabel( element.getLabel() ),
					linkedSliderPanel( element, tfCols, 0.1 ),
					arg.getUnits() );
		}
		else
		{
			final DoubleElement element = doubleElement( arg.getName(), arg::getValue, arg::set );
			addToLayout(
					arg.getHelp(),
					new JLabel( element.getLabel() ),
					linkedFormattedTextField( element ),
					arg.getUnits() );
		}
	}

	@Override
	public void visit( final StringArgument arg )
	{
		final StringElement element = stringElement( arg.getName(), arg::getValue, arg::set );
		addToLayoutTwoLines(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				linkedTextField( element ) );
	}

	@Override
	public void visit( final PathArgument arg )
	{
		final StringElement element = stringElement( arg.getName(), arg::getValue, arg::set );
		addToPathLayout(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				linkedTextField( element ) );
	}

	@Override
	public void visit( final ChoiceArgument arg )
	{
		final ListElement< String > element = listElement( arg.getName(), arg.getChoices(), arg::getValue, arg::set );
		addToLayout(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				linkedComboBoxSelector( element ),
				arg.getUnits() );
	}

	private void addToLayoutTwoLines( final String help, final JLabel lbl, final JComponent comp )
	{
		lbl.setText( lbl.getText() + " " );
		lbl.setFont( Fonts.SMALL_FONT );
		c.insets = new Insets( 5, 0, 0, 0 );
		c.gridwidth = 3;
		panel.add( lbl, c );
		c.gridy++;
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets( 0, 0, 5, 0 );
		panel.add( comp, c );
		c.gridy++;

		if ( help != null )
		{
			lbl.setToolTipText( help );
			comp.setToolTipText( help );
		}
	}

	private void addToPathLayout( final String help, final JLabel lbl, final JTextField tf )
	{
		final JPanel p = new JPanel();
		final BoxLayout bl = new BoxLayout( p, BoxLayout.LINE_AXIS );
		p.setLayout( bl );

		lbl.setText( lbl.getText() + " " );
		lbl.setFont( Fonts.SMALL_FONT );

		final JButton browseButton = new JButton( "browse" );
		browseButton.setFont( Fonts.SMALL_FONT );
		browseButton.addActionListener( e -> {
			final File file = FileChooser.chooseFile( p, tf.getText(), DialogType.LOAD );
			if ( file == null )
				return;
			tf.setText( file.getAbsolutePath() );
		} );

		p.add( lbl );
		p.add( Box.createHorizontalGlue() );
		p.add( browseButton );

		c.insets = new Insets( 5, 0, 0, 0 );
		c.gridwidth = 3;
		panel.add( p, c );
		c.gridy++;
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets( 0, 0, 5, 0 );
		panel.add( tf, c );
		c.gridy++;

		if ( help != null )
		{
			lbl.setToolTipText( help );
			tf.setToolTipText( help );
			browseButton.setToolTipText( help );
		}
	}

	private void addToLayout( final String help, final JLabel lbl, final JComponent comp )
	{
		lbl.setText( lbl.getText() + " " );
		lbl.setFont( Fonts.SMALL_FONT );
		lbl.setHorizontalAlignment( JLabel.RIGHT );
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add( lbl, c );

		c.gridx++;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add( comp, c );

		c.gridx = 0;
		c.gridy++;
		c.insets = new Insets( 5, 0, 5, 0 );

		if ( help != null )
		{
			lbl.setToolTipText( help );
			comp.setToolTipText( help );
		}
	}

	private void addToLayout( final String help, final JLabel lbl, final JComponent comp, final String units )
	{
		if ( units == null )
		{
			addToLayout( help, lbl, comp );
			return;
		}

		lbl.setText( lbl.getText() + " " );
		lbl.setFont( Fonts.SMALL_FONT );
		lbl.setHorizontalAlignment( JLabel.RIGHT );
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add( lbl, c );

		c.gridx++;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add( comp, c );

		final JLabel lblUnits = new JLabel( " " + units );
		lblUnits.setFont( Fonts.SMALL_FONT );
		c.gridx++;
		c.insets = new Insets( 5, 0, 5, 0 );
		panel.add( lblUnits, c );

		c.gridx = 0;
		c.gridy++;

		if ( help != null )
		{
			lbl.setToolTipText( help );
			comp.setToolTipText( help );
			lblUnits.setText( help );
		}
	}

	private void addToLayout( final String help, final JComponent comp )
	{
		c.gridx = 0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets( 5, 0, 5, 0 );
		panel.add( comp, c );

		c.gridx = 0;
		c.gridy++;

		if ( help != null )
			comp.setToolTipText( help );
	}

	private void addLastRow()
	{
		c.gridx = 0;
		c.gridy++;
		c.weighty = 1.;
		panel.add( new JLabel(), c );
	}

	public static JPanel build( final CLIConfigurator cli )
	{
		final CliGuiBuilder builder = new CliGuiBuilder( cli.getExecutableArg() );
		cli.getArguments().stream()
				.filter( Argument::isVisible )
				.forEach( arg -> arg.accept( builder ) );
		builder.addLastRow();
		return builder.panel;
	}
}
