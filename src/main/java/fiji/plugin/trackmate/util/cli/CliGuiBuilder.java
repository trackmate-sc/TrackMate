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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElement;
import fiji.plugin.trackmate.util.FileChooser;
import fiji.plugin.trackmate.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.Argument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.ArgumentVisitor;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.ChoiceArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.Command;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.CondaEnvironmentCommand;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.DoubleArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.ExecutablePath;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.Flag;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.IntArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.PathArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.SelectableArguments;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.StringArgument;

public class CliGuiBuilder implements ArgumentVisitor
{

	private static final int tfCols = 4;

	private final CliConfigPanel panel;

	private final GridBagConstraints c;

	private ButtonGroup currentButtonGroup;

	private int topInset = 5;

	private int bottomInset = 5;

	private boolean selectedInCurrent = false;

	private SelectableArguments selectable;

	private final List< StyleElement > elements = new ArrayList<>();

	private CliGuiBuilder()
	{
		this.panel = new CliConfigPanel();
		final GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[] { 0., 1., 0. };
		panel.setLayout( layout );
		panel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		this.c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
	}

	private void setCurrentButtonGroup( final ButtonGroup buttonGroup )
	{
		if ( buttonGroup == null || ( this.currentButtonGroup != buttonGroup ) )
		{
			topInset = 5;
			bottomInset = 5;
		}
		else
		{
			topInset = 0;
			bottomInset = 0;
		}
		this.currentButtonGroup = buttonGroup;
	}

	private void setCurrentSelected( final boolean selectedInCurrent )
	{
		this.selectedInCurrent = selectedInCurrent;
	}

	private void setCurrentSelectable( final SelectableArguments selectable )
	{
		this.selectable = selectable;
	}

	/*
	 * ARGUMENT VISITOR.
	 */

	@Override
	public void visit( final ExecutablePath arg )
	{
		final StringElement element = stringElement( arg.getName(), arg::getValue, arg::set );
		elements.add( element );
		addPathToLayout(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				linkedTextField( element ),
				null );

		panel.add( Box.createVerticalStrut( 10 ), c );
		final JSeparator separator = new JSeparator( JSeparator.HORIZONTAL );
		separator.setMinimumSize( new Dimension( 10, 10 ) );
		addToLayout( null, separator );
	}

	@Override
	public void visit( final Flag flag )
	{
		final BooleanElement element = booleanElement( flag.getName(), flag::getValue, flag::set );
		elements.add( element );
		if ( !flag.isSet() )
			element.set( flag.getDefaultValue() );
		final JCheckBox checkbox = linkedCheckBox( element, "" );
		checkbox.setHorizontalAlignment( SwingConstants.LEADING );
		addToLayout(
				flag.getHelp(),
				new JLabel( element.getLabel() ),
				checkbox,
				flag );
	}

	@Override
	public void visit( final IntArgument arg )
	{
		final IntElement element = intElement( arg.getName(),
				arg.getMin(), arg.getMax(), arg::getValue, arg::set );
		elements.add( element );

		final int numberOfColumns;
		if ( arg.hasMin() && arg.hasMin() )
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
				arg.getUnits(),
				arg );
	}

	@Override
	public void visit( final DoubleArgument arg )
	{
		if ( arg.hasMin() && arg.hasMax() )
		{
			final BoundedDoubleElement element = boundedDoubleElement( arg.getName(),
					arg.getMin(), arg.getMax(), arg::getValue, arg::set );
			elements.add( element );
			if ( arg.isSet() )
				element.set( arg.getValue() );
			else if ( arg.hasDefaultValue() )
				element.set( arg.getDefaultValue() );
			addToLayout(
					arg.getHelp(),
					new JLabel( element.getLabel() ),
					linkedSliderPanel( element, tfCols, 0.1 ),
					arg.getUnits(),
					arg );
		}
		else
		{
			final DoubleElement element = doubleElement( arg.getName(), arg::getValue, arg::set );
			elements.add( element );
			if ( arg.isSet() )
				element.set( arg.getValue() );
			else if ( arg.hasDefaultValue() )
				element.set( arg.getDefaultValue() );
			addToLayout(
					arg.getHelp(),
					new JLabel( element.getLabel() ),
					linkedFormattedTextField( element ),
					arg.getUnits(),
					arg );
		}
	}

	@Override
	public void visit( final StringArgument arg )
	{
		final StringElement element = stringElement( arg.getName(), arg::getValue, arg::set );
		elements.add( element );
		addToLayoutTwoLines(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				linkedTextField( element ),
				arg );
	}

	@Override
	public void visit( final PathArgument arg )
	{
		final StringElement element = stringElement( arg.getName(), arg::getValue, arg::set );
		elements.add( element );
		if ( arg.isSet() )
			element.set( arg.getValue() );
		else if ( arg.hasDefaultValue() )
			element.set( arg.getDefaultValue() );
		addPathToLayout(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				linkedTextField( element ),
				arg );
	}

	@Override
	public void visit( final ChoiceArgument arg )
	{
		final ListElement< String > element = listElement( arg.getName(), arg.getChoices(), arg::getValue, arg::set );
		elements.add( element );
		final JComboBox< String > comboBox = linkedComboBoxSelector( element );
		if ( arg.isSet() )
			comboBox.setSelectedItem( arg.getValue() );
		else if ( arg.hasDefaultValue() )
			comboBox.setSelectedItem( arg.getDefaultValue() );
		addToLayout(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				comboBox,
				arg.getUnits(),
				arg );
	}

	@Override
	public void visit( final CondaEnvironmentCommand arg )
	{
		final ListElement< String > element = listElement( arg.getName(), arg.getEnvironment(), arg::getValue, arg::set );
		elements.add( element );
		final JComboBox< String > comboBox = linkedComboBoxSelector( element );
		if ( arg.isSet() )
			comboBox.setSelectedItem( arg.getValue() );
		else
			comboBox.setSelectedItem( 0 );
		addToLayout(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				comboBox,
				null );
	}

	/*
	 * UI STUFF.
	 */

	private void addToLayoutTwoLines( final String help, final JLabel lbl, final JComponent comp, final Argument< ? > arg )
	{
		lbl.setText( lbl.getText() + " " );
		lbl.setFont( Fonts.SMALL_FONT );
		comp.setFont( Fonts.SMALL_FONT );
		final JComponent item;
		if ( currentButtonGroup != null )
		{
			final JRadioButton rdbtn = new JRadioButton();
			currentButtonGroup.add( rdbtn );
			final SelectableArguments localSelectable = selectable;
			rdbtn.addItemListener( e -> {
				comp.setEnabled( rdbtn.isSelected() );
				if ( rdbtn.isSelected() )
					localSelectable.select( arg );
			} );
			rdbtn.setSelected( selectedInCurrent );
			comp.setEnabled( rdbtn.isSelected() );
			item = new JPanel();
			item.setLayout( new BoxLayout( item, BoxLayout.LINE_AXIS ) );
			item.add( rdbtn );
			item.add( Box.createHorizontalGlue() );
			item.add( lbl );
		}
		else
		{
			item = lbl;
		}
		c.insets = new Insets( 5, 0, 0, 0 );
		c.gridwidth = 3;
		panel.add( item, c );
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

	private void addPathToLayout( final String help, final JLabel lbl, final JTextField tf, final Argument< ? > arg )
	{
		final JPanel p = new JPanel();
		final BoxLayout bl = new BoxLayout( p, BoxLayout.LINE_AXIS );
		p.setLayout( bl );

		lbl.setText( lbl.getText() + " " );
		lbl.setFont( Fonts.SMALL_FONT );
		tf.setFont( Fonts.SMALL_FONT );
		final JButton browseButton = new JButton( "browse" );
		browseButton.setFont( Fonts.SMALL_FONT );
		browseButton.addActionListener( e -> {
			final File file = FileChooser.chooseFile( p, tf.getText(), DialogType.LOAD );
			if ( file == null )
				return;
			tf.setText( file.getAbsolutePath() );
		} );

		if ( currentButtonGroup != null )
		{
			final JRadioButton rdbtn = new JRadioButton();
			currentButtonGroup.add( rdbtn );
			final SelectableArguments localSelectable = selectable;
			rdbtn.addItemListener( e -> {
				tf.setEnabled( rdbtn.isSelected() );
				browseButton.setEnabled( rdbtn.isSelected() );
				if ( rdbtn.isSelected() )
					localSelectable.select( arg );
			} );

			rdbtn.setSelected( selectedInCurrent );
			tf.setEnabled( rdbtn.isSelected() );
			browseButton.setEnabled( rdbtn.isSelected() );
			p.add( rdbtn );
		}
		p.add( lbl );
		p.add( Box.createHorizontalGlue() );
		p.add( browseButton );

		c.insets = new Insets( topInset, 0, 0, 0 );
		c.gridwidth = 3;
		panel.add( p, c );
		c.gridy++;
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets( 0, 0, bottomInset, 0 );
		panel.add( tf, c );
		c.gridy++;

		if ( help != null )
		{
			lbl.setToolTipText( help );
			tf.setToolTipText( help );
			browseButton.setToolTipText( help );
		}
	}

	private void addToLayout( final String help, final JLabel lbl, final JComponent comp, final Argument< ? > arg )
	{
		lbl.setText( lbl.getText() + " " );
		lbl.setFont( Fonts.SMALL_FONT );
		lbl.setHorizontalAlignment( JLabel.RIGHT );
		comp.setFont( Fonts.SMALL_FONT );

		final JComponent header;
		if ( arg != null && currentButtonGroup != null )
		{
			final JRadioButton rdbtn = new JRadioButton();
			currentButtonGroup.add( rdbtn );
			final SelectableArguments localSelectable = selectable;
			rdbtn.addItemListener( e -> {
				comp.setEnabled( rdbtn.isSelected() );
				if ( rdbtn.isSelected() )
					localSelectable.select( arg );
			} );
			rdbtn.setSelected( selectedInCurrent );
			comp.setEnabled( rdbtn.isSelected() );
			header = new JPanel();
			header.setLayout( new BoxLayout( header, BoxLayout.LINE_AXIS ) );
			header.add( rdbtn );
			header.add( Box.createHorizontalGlue() );
			header.add( lbl );
		}
		else
		{
			header = lbl;
		}

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add( header, c );

		c.gridx++;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add( comp, c );

		c.gridx = 0;
		c.gridy++;
		c.insets = new Insets( topInset, 0, bottomInset, 0 );

		if ( help != null )
		{
			lbl.setToolTipText( help );
			comp.setToolTipText( help );
		}
	}

	private void addToLayout( final String help, final JLabel lbl, final JComponent comp, final String units, final Argument< ? > arg )
	{
		if ( units == null )
		{
			addToLayout( help, lbl, comp, arg );
			return;
		}

		lbl.setText( lbl.getText() + " " );
		lbl.setFont( Fonts.SMALL_FONT );
		lbl.setHorizontalAlignment( JLabel.RIGHT );
		comp.setFont( Fonts.SMALL_FONT );

		final JComponent header;
		if ( currentButtonGroup != null )
		{
			final JRadioButton rdbtn = new JRadioButton();
			currentButtonGroup.add( rdbtn );
			rdbtn.addItemListener( e -> comp.setEnabled( rdbtn.isSelected() ) );
			rdbtn.setSelected( selectedInCurrent );
			header = new JPanel();
			header.setLayout( new BoxLayout( header, BoxLayout.LINE_AXIS ) );
			header.add( rdbtn );
			header.add( Box.createHorizontalGlue() );
			header.add( lbl );
		}
		else
		{
			header = lbl;
		}

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.LINE_END;
		panel.add( header, c );

		c.gridx++;
		c.anchor = GridBagConstraints.LINE_START;
		panel.add( comp, c );

		final JLabel lblUnits = new JLabel( " " + units );
		lblUnits.setFont( Fonts.SMALL_FONT );
		c.gridx++;
		c.insets = new Insets( topInset, 0, bottomInset, 0 );
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
		final JComponent header;
		if ( currentButtonGroup != null )
		{
			final JRadioButton rdbtn = new JRadioButton();
			currentButtonGroup.add( rdbtn );
			rdbtn.addItemListener( e -> comp.setEnabled( rdbtn.isSelected() ) );
			rdbtn.setSelected( selectedInCurrent );
			header = new JPanel();
			header.setLayout( new BoxLayout( header, BoxLayout.LINE_AXIS ) );
			header.add( rdbtn );
			header.add( Box.createHorizontalGlue() );
			header.add( comp );
		}
		else
		{
			header = comp;
		}

		c.gridx = 0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets( topInset, 0, bottomInset, 0 );
		panel.add( header, c );

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

	public static CliConfigPanel build( final CLIConfigurator cli )
	{
		final CliGuiBuilder builder = new CliGuiBuilder();
		cli.getCommandArg().accept( builder );

		/*
		 * Iterate over CLI arguments.
		 */

		// Map a selectable group to a button group in the GUI
		final Map< SelectableArguments, ButtonGroup > buttonGroups = new HashMap<>();

		// Iterate over arguments, taking care of selectable group.
		for ( final Command< ? > arg : cli.getArguments() )
		{
			if ( !arg.isVisible() )
				continue;

			/*
			 * Assume we are not in a selectable group. In the case the current
			 * button group is null and we won't be adding a radio button.
			 */
			builder.setCurrentButtonGroup( null );
			builder.setCurrentSelected( false );
			builder.setCurrentSelectable( null );
			for ( final SelectableArguments selectable : cli.getSelectables() )
			{
				if ( selectable.getArguments().contains( arg ) )
				{
					// We are in a selectable group. We will add a radio button
					// to the current button group.
					final ButtonGroup buttonGroup = buttonGroups.computeIfAbsent( selectable, k -> new ButtonGroup() );
					builder.setCurrentButtonGroup( buttonGroup );
					builder.setCurrentSelectable( selectable );
					if ( selectable.getSelection().equals( arg ) )
					{
						// We are the selected one.
						builder.setCurrentSelected( true );
					}

					break;
				}
			}
			arg.accept( builder );
		}

		/*
		 * Last row.
		 */

		builder.addLastRow();
		return builder.panel;
	}


	public class CliConfigPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		public void refresh()
		{
			elements.forEach( e -> e.update() );
		}
	}
}
