/*-
f * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
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
import fiji.plugin.trackmate.gui.displaysettings.BoundedValue;
import fiji.plugin.trackmate.gui.displaysettings.BoundedValue.UpdateListener;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BooleanElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.DoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.IntElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.ListElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StringElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElement;
import fiji.plugin.trackmate.util.FileChooser;
import fiji.plugin.trackmate.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.cli.CommandCLIConfigurator.ExecutablePath;
import fiji.plugin.trackmate.util.cli.CondaCLIConfigurator.CondaEnvironmentCommand;
import fiji.plugin.trackmate.util.cli.Configurator.Argument;
import fiji.plugin.trackmate.util.cli.Configurator.ArgumentVisitor;
import fiji.plugin.trackmate.util.cli.Configurator.ChoiceArgument;
import fiji.plugin.trackmate.util.cli.Configurator.DoubleArgument;
import fiji.plugin.trackmate.util.cli.Configurator.Flag;
import fiji.plugin.trackmate.util.cli.Configurator.IntArgument;
import fiji.plugin.trackmate.util.cli.Configurator.PathArgument;
import fiji.plugin.trackmate.util.cli.Configurator.SelectableArguments;
import fiji.plugin.trackmate.util.cli.Configurator.StringArgument;

public class ConfigGuiBuilder implements ArgumentVisitor
{

	private static final int tfCols = 4;

	private final ConfigPanel panel;

	private final GridBagConstraints c;

	private int topInset = 5;

	private int bottomInset = 5;

	private final Map< Argument< ?, ? >, Function< ?, ? > > forwardUITranslators;

	private final Map< Argument< ?, ? >, Function< ?, ? > > backwardUITranslators;

	private ConfigGuiBuilder(
			final Map< Argument< ?, ? >, Function< ?, ? > > forwardUITranslators,
			final Map< Argument< ?, ? >, Function< ?, ? > > backwardUITranslators )
	{
		this.forwardUITranslators = forwardUITranslators;
		this.backwardUITranslators = backwardUITranslators;
		this.panel = new ConfigPanel();
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


	private void setCurrentRadioButton( final JRadioButton radioButton )
	{
		if ( radioButton == null || ( panel.rdbtn != radioButton ) )
		{
			topInset = 5;
			bottomInset = 5;
		}
		else
		{
			topInset = 0;
			bottomInset = 0;
		}
		panel.rdbtn = radioButton;
	}

	/*
	 * ARGUMENT VISITOR.
	 */

	@Override
	public void visit( final ExecutablePath arg )
	{
		if ( !arg.isSet() )
		{
			if ( !arg.hasDefaultValue() )
				throw new IllegalArgumentException( "The GUI builder requires all arguments and commands "
						+ "to have a value or a default value. The argument '" + arg.getName() + "' misses both." );
			arg.set( arg.getDefaultValue() );
		}

		final StringElement element = stringElement( arg.getName(), arg::getValue, arg::set );
		panel.elements.put( arg.getKey(), element );
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
		if ( !flag.isSet() )
		{
			if ( !flag.hasDefaultValue() )
				throw new IllegalArgumentException( "The GUI builder requires all arguments and commands "
						+ "to have a value or a default value. The argument '" + flag.getName() + "' misses both." );
			flag.set( flag.getDefaultValue() );
		}

		final BooleanElement element = booleanElement( flag.getName(), flag::getValue, flag::set );
		panel.elements.put( flag.getKey(), element );
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
		if ( !arg.isSet() )
		{
			if ( !arg.hasDefaultValue() )
				throw new IllegalArgumentException( "The GUI builder requires all arguments and commands "
						+ "to have a value or a default value. The argument '" + arg.getName() + "' misses both." );
			arg.set( arg.getDefaultValue() );
		}

		// Translate
		@SuppressWarnings( "unchecked" )
		final Function< Integer, Integer > forward = ( Function< Integer, Integer > ) forwardUITranslators.getOrDefault( arg, v -> v );
		@SuppressWarnings( "unchecked" )
		final Function< Integer, Integer > backward = ( Function< Integer, Integer > ) backwardUITranslators.getOrDefault( arg, v -> v );
		final IntSupplier valueGetter = () -> {
			final int value = arg.getValue();
			return forward.apply( value );
		};
		final Consumer< Integer > valueSetter = ( v ) -> {
			final int value = backward.apply( v );
			arg.set( value );
		};
		final int min = forward.apply( arg.getMin() );
		final int max = forward.apply( arg.getMax() );

		final IntElement element = intElement( arg.getName(), min, max, valueGetter, valueSetter );
		panel.elements.put( arg.getKey(), element );

		final int numberOfColumns;
		if ( arg.hasMin() && arg.hasMin() )
		{
			final int largest = Math.max( Math.abs( min ), Math.abs( max ) );
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
		if ( !arg.isSet() )
		{
			if ( !arg.hasDefaultValue() )
				throw new IllegalArgumentException( "The GUI builder requires all arguments and commands "
						+ "to have a value or a default value. The argument '" + arg.getName() + "' misses both." );
			arg.set( arg.getDefaultValue() );
		}

		// Translate
		@SuppressWarnings( "unchecked" )
		final Function< Double, Double > forward = ( Function< Double, Double > ) forwardUITranslators.getOrDefault( arg, v -> v );
		@SuppressWarnings( "unchecked" )
		final Function< Double, Double > backward = ( Function< Double, Double > ) backwardUITranslators.getOrDefault( arg, v -> v );
		final DoubleSupplier valueGetter = () -> {
			final double value = arg.getValue();
			return forward.apply( value );
		};
		final Consumer< Double > valueSetter = ( v ) -> {
			final double value = backward.apply( v );
			arg.set( value );
		};

		if ( arg.hasMin() && arg.hasMax() )
		{
			final double min = forward.apply( arg.getMin() );
			final double max = forward.apply( arg.getMax() );
			final BoundedDoubleElement element = boundedDoubleElement( arg.getName(),
					min, max, valueGetter, valueSetter );
			panel.elements.put( arg.getKey(), element );
			addToLayout(
					arg.getHelp(),
					new JLabel( element.getLabel() ),
					linkedSliderPanel( element, tfCols, arg.getMax() / 50 ),
					arg.getUnits(),
					arg );
		}
		else
		{
			final DoubleElement element = doubleElement( arg.getName(), valueGetter, valueSetter );
			panel.elements.put( arg.getKey(), element );
			if ( arg.isSet() )
				element.set( forward.apply( arg.getValue() ) );
			else if ( arg.hasDefaultValue() )
				element.set( forward.apply( arg.getDefaultValue() ) );
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
		if ( !arg.isSet() )
		{
			if ( !arg.hasDefaultValue() )
				throw new IllegalArgumentException( "The GUI builder requires all arguments and commands "
						+ "to have a value or a default value. The argument '" + arg.getName() + "' misses both." );
			arg.set( arg.getDefaultValue() );
		}

		// Translate
		@SuppressWarnings( "unchecked" )
		final Function< String, String > forward = ( Function< String, String > ) forwardUITranslators.getOrDefault( arg, v -> v );
		@SuppressWarnings( "unchecked" )
		final Function< String, String > backward = ( Function< String, String > ) backwardUITranslators.getOrDefault( arg, v -> v );
		final Supplier< String > valueGetter = () -> {
			final String value = arg.getValue();
			return forward.apply( value );
		};
		final Consumer< String > valueSetter = ( v ) -> {
			final String value = backward.apply( v );
			arg.set( value );
		};

		final StringElement element = stringElement( arg.getName(), valueGetter, valueSetter );
		panel.elements.put( arg.getKey(), element );
		addToLayoutTwoLines(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				linkedTextField( element ),
				arg );
	}

	@Override
	public void visit( final PathArgument arg )
	{
		if ( !arg.isSet() )
		{
			if ( !arg.hasDefaultValue() )
				throw new IllegalArgumentException( "The GUI builder requires all arguments and commands "
						+ "to have a value or a default value. The argument '" + arg.getName() + "' misses both." );
			arg.set( arg.getDefaultValue() );
		}

		// Translate
		@SuppressWarnings( "unchecked" )
		final Function< String, String > forward = ( Function< String, String > ) forwardUITranslators.getOrDefault( arg, v -> v );
		@SuppressWarnings( "unchecked" )
		final Function< String, String > backward = ( Function< String, String > ) backwardUITranslators.getOrDefault( arg, v -> v );
		final Supplier< String > valueGetter = () -> {
			final String value = arg.getValue();
			return forward.apply( value );
		};
		final Consumer< String > valueSetter = ( v ) -> {
			final String value = backward.apply( v );
			arg.set( value );
		};

		final StringElement element = stringElement( arg.getName(), valueGetter, valueSetter );
		panel.elements.put( arg.getKey(), element );
		addPathToLayout(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				linkedTextField( element ),
				arg );
	}

	@Override
	public void visit( final ChoiceArgument arg )
	{
		if ( !arg.isSet() )
		{
			if ( !arg.hasDefaultValue() )
				throw new IllegalArgumentException( "The GUI builder requires all arguments and commands "
						+ "to have a value or a default value. The argument '" + arg.getName() + "' misses both." );
			arg.set( arg.getDefaultValue() );
		}

		final List< String > displays = arg.getDisplays();
		final Supplier< String > supplier = () -> {
			return displays.get( arg.getSelectedIndex() );
		};
		final Consumer< String > consumer = ( s ) -> arg.set( displays.indexOf( s ) );

		final ListElement< String > element = listElement( arg.getName(), displays, supplier, consumer );
		panel.elements.put( arg.getKey(), element );
		final JComboBox< String > comboBox = linkedComboBoxSelector( element );
		comboBox.setSelectedIndex( arg.getSelectedIndex() );
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
		if ( arg.getEnvironments().isEmpty() )
		{
			// No environment found. Tell the user.
			final JLabel lbl = new JLabel( "<html>There was an error retrieving the "
					+ "list of conda environments. "
					+ "<p>"
					+ "Did you configure Conda for TrackMate? "
					+ "<p>"
					+ "(Edit > Options > Configure TrackMate Conda path...)</html>" );
			lbl.setFont( Fonts.SMALL_FONT );
			lbl.setForeground( Color.RED );
			lbl.setPreferredSize( new Dimension( 200, 40 ) );
			addToLayout( arg.getHelp(), lbl );
			return;
		}

		if ( !arg.isSet() )
		{
			if ( !arg.hasDefaultValue() )
				throw new IllegalArgumentException( "The GUI builder requires all arguments and commands "
						+ "to have a value or a default value. The argument '" + arg.getName() + "' misses both." );
			arg.set( arg.getDefaultValue() );
		}

		final ListElement< String > element = listElement( arg.getName(), arg.getEnvironments(), arg::getValue, arg::set );
		panel.elements.put( arg.getKey(), element );
		final JComboBox< String > comboBox = linkedComboBoxSelector( element );
		comboBox.setSelectedItem( arg.getValue() );
		addToLayout(
				arg.getHelp(),
				new JLabel( element.getLabel() ),
				comboBox,
				null );
	}

	/*
	 * UI STUFF.
	 */

	private void addToLayoutTwoLines( final String help, final JLabel lbl, final JComponent comp, final Argument< ?, ? > arg )
	{
		lbl.setText( lbl.getText() + " " );
		lbl.setFont( Fonts.SMALL_FONT );
		comp.setFont( Fonts.SMALL_FONT );
		final JComponent item;
		if ( panel.rdbtn != null )
		{
			final JRadioButton btn = panel.rdbtn;
			btn.addItemListener( e -> comp.setEnabled( btn.isSelected() ) );
			comp.setEnabled( btn.isSelected() );
			item = new JPanel();
			item.setLayout( new BoxLayout( item, BoxLayout.LINE_AXIS ) );
			item.add( btn );
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

	private void addPathToLayout( final String help, final JLabel lbl, final JTextField tf, final Argument< ?, ? > arg )
	{
		final JPanel p = new JPanel();
		final BoxLayout bl = new BoxLayout( p, BoxLayout.LINE_AXIS );
		p.setLayout( bl );

		tf.setColumns( 10 ); // Avoid long paths deforming new panels.

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
			tf.postActionEvent();
		} );

		if ( panel.rdbtn != null )
		{
			final JRadioButton btn = panel.rdbtn;
			btn.addItemListener( e -> {
				tf.setEnabled( btn.isSelected() );
				browseButton.setEnabled( btn.isSelected() );
			} );

			tf.setEnabled( btn.isSelected() );
			browseButton.setEnabled( btn.isSelected() );
			p.add( btn );
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

	private void addToLayout( final String help, final JLabel lbl, final JComponent comp, final Argument< ?, ? > arg )
	{
		lbl.setText( lbl.getText() + " " );
		lbl.setFont( Fonts.SMALL_FONT );
		lbl.setHorizontalAlignment( JLabel.RIGHT );
		comp.setFont( Fonts.SMALL_FONT );

		final JComponent header;
		if ( arg != null && panel.rdbtn != null )
		{
			final JRadioButton btn = panel.rdbtn;
			btn.addItemListener( e -> comp.setEnabled( btn.isSelected() ) );
			comp.setEnabled( btn.isSelected() );
			header = new JPanel();
			header.setLayout( new BoxLayout( header, BoxLayout.LINE_AXIS ) );
			header.add( btn );
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

	private void addToLayout( final String help, final JLabel lbl, final JComponent comp, final String units, final Argument< ?, ? > arg )
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
		if ( panel.rdbtn != null )
		{
			final JRadioButton btn = panel.rdbtn;
			btn.addItemListener( e -> comp.setEnabled( btn.isSelected() ) );
			header = new JPanel();
			header.setLayout( new BoxLayout( header, BoxLayout.LINE_AXIS ) );
			header.add( btn );
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
			lblUnits.setToolTipText( help );
		}
	}

	private void addToLayout( final String help, final JComponent comp )
	{
		final JComponent header;
		if ( panel.rdbtn != null )
		{
			final JRadioButton btn = panel.rdbtn;
			btn.addItemListener( e -> comp.setEnabled( btn.isSelected() ) );
			header = new JPanel();
			header.setLayout( new BoxLayout( header, BoxLayout.LINE_AXIS ) );
			header.add( btn );
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

	public static ConfigPanel build( final Configurator config )
	{
		final ConfigGuiBuilder builder = createBuilder( config );
		// Could we make something more elegant than this?
		if ( config instanceof CLIConfigurator )
			( ( CLIConfigurator ) config ).getCommandArg().accept( builder );
		return build( config, builder );
	}

	private static ConfigGuiBuilder createBuilder( final Configurator config )
	{
		return new ConfigGuiBuilder(
				config.forwardUITranslators,
				config.backwardUITranslators );
	}

	private static ConfigPanel build( final Configurator config, final ConfigGuiBuilder builder )
	{
		/*
		 * Iterate over arguments.
		 */

		// Map a selectable group to a button group in the GUI
		final Map< Argument< ?, ? >, JRadioButton > buttons = new HashMap<>();
		for ( final SelectableArguments selectable : config.getSelectables() )
		{
			final List< Argument< ?, ? > > args = selectable.getArguments();
			final int nItems = args.size();
			final String label = selectable.getKey();
			final IntSupplier get = selectable::getSelected;
			final Consumer< Integer > set = selectable::select;
			final IntElement element = intElement( label, 0, nItems - 1, get, set );
			builder.panel.elements.put( selectable.getKey(), element );
			final ButtonGroup buttonGroup = linkedButtonGroup( element );
			// Link radio buttons to arguments.
			final Enumeration< AbstractButton > enumeration = buttonGroup.getElements();
			final Iterator< Argument< ?, ? > > it = args.iterator();
			while ( enumeration.hasMoreElements() )
			{
				final JRadioButton btn = ( JRadioButton ) enumeration.nextElement();
				final Argument< ?, ? > arg = it.next();
				buttons.put( arg, btn );
				btn.setSelected( selectable.getSelection().equals( arg ) );
			}
		}

		// Iterate over arguments, taking care of selectable group.
		for ( final Argument< ?, ? > arg : config.getArguments() )
		{
			if ( !arg.isVisible() )
				continue;

			builder.setCurrentRadioButton( buttons.get( arg ) );
			arg.accept( builder );
		}

		/*
		 * Last row.
		 */

		builder.addLastRow();
		return builder.panel;
	}

	private static ButtonGroup linkedButtonGroup( final IntElement element )
	{
		final BoundedValue value = element.getValue();
		final ButtonGroup buttonGroup = new ButtonGroup();
		final List< JRadioButton > buttons = new ArrayList<>();
		for ( int i = 0; i <= value.getRangeMax(); i++ )
		{
			final JRadioButton btn = new JRadioButton();
			buttons.add( btn );
			final int selected = i;
			btn.addItemListener( new ItemListener()
			{

				@Override
				public void itemStateChanged( final ItemEvent e )
				{
					if (btn.isSelected())
						element.set( selected );
				}
			} );
			buttonGroup.add( btn );
		}
		element.getValue().setUpdateListener( new UpdateListener()
		{

			@Override
			public void update()
			{
				final int selected = element.get();
				buttons.get( selected ).setSelected( true );
			}
		} );
		return buttonGroup;
	}

	public class ConfigPanel extends JPanel
	{

		/**
		 * Map of StyleElements that are created by this builder. The keys are
		 * the corresponding argument keys.
		 */
		final Map< String, StyleElement > elements = new LinkedHashMap<>();

		private JRadioButton rdbtn;

		private static final long serialVersionUID = 1L;

		public void refresh()
		{
			elements.values().forEach( e -> e.update() );
		}
	}
}
