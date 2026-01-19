/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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

package fiji.plugin.trackmate.gui.editor.labkit.component;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_CONTEXT;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ItemEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

import org.apache.commons.lang3.function.BooleanConsumer;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;

import fiji.plugin.trackmate.gui.editor.labkit.component.TMFloodFillController.FloodEraseMode;
import fiji.plugin.trackmate.gui.editor.labkit.component.TMFloodFillController.FloodFillMode;
import fiji.plugin.trackmate.gui.editor.labkit.component.TMLabelBrushController.EraseBrushMode;
import fiji.plugin.trackmate.gui.editor.labkit.component.TMLabelBrushController.PaintBrushMode;
import net.miginfocom.swing.MigLayout;

/**
 * Panel with the tool buttons for brush, flood fill, etc... Activates and
 * deactivates mouse behaviours.
 */
public class TMLabelToolsPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final Color OPTIONS_BORDER = new Color( 220, 220, 220 );

	private static final Color OPTIONS_BACKGROUND = new Color( 230, 230, 230 );

	private static final String MOVE_TOOL_TIP = "<html><b>Move</b><br>" +
			"<small>Keyboard shortcuts:<br>" +
			"- <b>Left Click</b> on the image and drag, to rotate the image.<br>" +
			"- <b>Right Click</b> on the image, to move around.<br>" +
			"- <b>Mouse Wheel</b> or <b>+ / -</b> to zoom in and out.<br>" +
			"- <b>← / →</b> or <b>F / G</b> to move through time.<br>" +
			"- <b>Shift + R</b> to reset the view." +
			"</small></html>";

	private static final String DRAW_TOOL_TIP = "<html><b>Add</b><br>" +
			"<small>Keyboard shortcuts:<br>" +
			"- Hold down the <b>A</b> key and <b>Left Click</b> on the image to draw the selected label.<br>" +
			"- Hold down the <b>A</b> key and use the <b>Mouse Wheel</b> to change the brush diameter.<br>" +
			"- You can also change the brush diameter with the <b>Q /E</b> (shift to go faster).<br>" +
			"</small></html>";

	private static final String ERASE_TOOL_TIP = "<html><b>Delete</b><br>" +
			"<small>Keyboard shortcuts:<br>" +
			"- Hold down the <b>D</b> key and <b>Left Click</b> on the image to delete the selected label.<br>" +
			"- Hold down the <b>D</b> key and use the <b>Mouse Wheel</b> to change the brush diameter.<br>" +
			"</small></html>";

	private static final String FLOOD_FILL_TOOL_TIP = "<html><b>Flood Fill</b><br>" +
			"<small>Keyboard shortcuts:<br>" +
			"- Hold down the <b>L</b> key and <b>Left Click</b> on the image to flood fill with the selected label.<br>" +
			"</small></html>";

	private static final String FLOOD_ERASE_TOOL_TIP = "<html><b>Remove Connected Component</b><br>" +
			"<small>Keyboard shortcuts:<br>" +
			"- Hold down the <b>R</b> key and <b>Left Click</b> on the image to remove connected component.<br>" +
			"</small></html>";

	private static final String SELECT_LABEL_TOOL_TIP = "<html><b>Select Label</b><br>" +
			"<small>Keyboard shortcuts:<br>" +
			"- Hold down the <b>Shift</b> key and <b>Left Click</b> on the image<br>" +
			"  to select the label under the cursor.</small></html>";

	private final TMFloodFillController floodFillController;

	private final TMLabelBrushController brushController;

	private final TMSelectLabelController selectLabelController;

	private final ButtonGroup group = new ButtonGroup();

	private Mode mode = ignore -> {};

	private final JToggleButton moveBtn;

	private final JToggleButton drawBtn;

	private final JToggleButton floodFillBtn;

	private final JToggleButton eraseBtn;

	private final JToggleButton floodEraseBtn;

	private final JToggleButton selectLabelBtn;

	private final JPanel brushOptionsPanel;

	private final JPanel paintModePanel;

	private final JPanel eraseModePanel;

	private final JPanel floodFillModePanel;

	private final JPanel floodEraseModePanel;

	private JComboBox< TMLabelBrushController.PaintBrushMode > paintModeCombo;

	private JComboBox< TMLabelBrushController.EraseBrushMode > eraseModeCombo;

	private JComboBox< TMFloodFillController.FloodFillMode > floodFillModeCombo;

	private JComboBox< TMFloodFillController.FloodEraseMode > floodEraseModeCombo;

	public TMLabelToolsPanel(
			final TMLabelBrushController brushController,
			final TMFloodFillController floodFillController,
			final TMSelectLabelController selectLabelController )
	{
		this.brushController = brushController;
		this.floodFillController = floodFillController;
		this.selectLabelController = selectLabelController;

		// Create buttons first
		this.moveBtn = addActionButton( MOVE_TOOL_TIP, ignore -> {}, false, "/images/move.png" );
		this.drawBtn = addActionButton( DRAW_TOOL_TIP, brushController::setBrushActive, true, "/images/draw.png" );
		this.floodFillBtn = addActionButton( FLOOD_FILL_TOOL_TIP, floodFillController::setFloodFillActive, false, "/images/fill.png" );
		this.eraseBtn = addActionButton( ERASE_TOOL_TIP, brushController::setEraserActive, true, "/images/erase.png" );
		this.floodEraseBtn = addActionButton( FLOOD_ERASE_TOOL_TIP, floodFillController::setRemoveBlobActive, false, "/images/flooderase.png" );
		this.selectLabelBtn = addActionButton( SELECT_LABEL_TOOL_TIP, selectLabelController::setActive, false, "/images/pipette.png" );

		// Initialize all panels
		this.brushOptionsPanel = initBrushSizePanel();
		this.paintModePanel = initPaintModePanel();
		this.eraseModePanel = initEraseModePanel();
		this.floodFillModePanel = initFloodFillModePanel();
		this.floodEraseModePanel = initFloodEraseModePanel();

		// Setup layout
		setLayout( new MigLayout( "insets 0, gap 4", "", "[]" ) );
		setBorder( BorderFactory.createEmptyBorder( 0, 0, 4, 0 ) );

		// Create tool buttons panel
		final JPanel toolsPanel = new JPanel( new MigLayout( "insets 0, gap 2", "", "" ) );
		toolsPanel.add( moveBtn );
		toolsPanel.add( drawBtn );
		toolsPanel.add( eraseBtn );
		toolsPanel.add( floodFillBtn );
		toolsPanel.add( floodEraseBtn );
		toolsPanel.add( selectLabelBtn );

		// Add all panels
		add( toolsPanel );
		add( brushOptionsPanel, "hidemode 3, h 32!" );
		add( paintModePanel, "hidemode 3, h 32!" );
		add( eraseModePanel, "hidemode 3, h 32!" );
		add( floodFillModePanel, "hidemode 3, h 32!" );
		add( floodEraseModePanel, "hidemode 3, h 32!" );

		// Set initial state
		moveBtn.doClick();
	}

	private JPanel initFloodFillModePanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new MigLayout( "insets 4 8 4 8, gap 4", "", "[center]" ) );

		final JLabel floodModeLabel = new JLabel( "Flood mode:" );
		floodFillModeCombo = new JComboBox<>( TMFloodFillController.FloodFillMode.values() );
		floodFillModeCombo.setSelectedItem( TMFloodFillController.FloodFillMode.REPLACE );
		floodFillModeCombo.setFocusable( false );
		floodFillModeCombo.setPreferredSize( new java.awt.Dimension( 130, 20 ) );

		floodFillModeCombo.addActionListener( e -> {
			final FloodFillMode mode = ( FloodFillMode ) floodFillModeCombo.getSelectedItem();
			if ( mode != null )
			{
				floodFillController.setFloodFillMode( mode );
				panel.setToolTipText( mode.getTooltip() );
				floodFillModeCombo.setToolTipText( mode.getTooltip() );
			}
		} );
		floodFillModeCombo.setSelectedItem( floodFillController.getFloodFillMode() );

		panel.add( floodModeLabel, "aligny center" );
		panel.add( floodFillModeCombo, "aligny center" );

		panel.setBackground( OPTIONS_BACKGROUND );
		panel.setBorder( BorderFactory.createLineBorder( OPTIONS_BORDER ) );

		return panel;
	}

	private JPanel initFloodEraseModePanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new MigLayout( "insets 4 8 4 8, gap 4", "", "[center]" ) );

		final JLabel floodEraseModeLabel = new JLabel( "Flood erase:" );
		floodEraseModeCombo = new JComboBox<>( TMFloodFillController.FloodEraseMode.values() );
		floodEraseModeCombo.setSelectedItem( TMFloodFillController.FloodEraseMode.REMOVE_ALL );
		floodEraseModeCombo.setFocusable( false );
		floodEraseModeCombo.setPreferredSize( new java.awt.Dimension( 130, 20 ) );

		floodEraseModeCombo.addActionListener( e -> {
			final FloodEraseMode mode = ( FloodEraseMode ) floodEraseModeCombo.getSelectedItem();
			if ( mode != null )
			{
				floodFillController.setFloodEraseMode( mode );
				panel.setToolTipText( mode.getTooltip() );
				floodEraseModeCombo.setToolTipText( mode.getTooltip() );
			}
		} );
		floodEraseModeCombo.setSelectedItem( floodFillController.getFloodEraseMode() );

		panel.add( floodEraseModeLabel, "aligny center" );
		panel.add( floodEraseModeCombo, "aligny center" );
		panel.setBackground( OPTIONS_BACKGROUND );
		panel.setBorder( BorderFactory.createLineBorder( OPTIONS_BORDER ) );
		return panel;
	}

	// Update the addActionButton method to handle showing/hiding all panels
	private JToggleButton addActionButton( final String toolTipText, final Mode mode, final boolean visibility, final String iconPath )
	{
		final JToggleButton button = new JToggleButton();
		button.setIcon( getIcon( iconPath ) );
		button.setToolTipText( toolTipText );
		button.setMargin( new Insets( 0, 0, 0, 0 ) );
		button.setFocusable( false );
		button.addItemListener( ev -> {
			if ( ev.getStateChange() == ItemEvent.SELECTED )
			{
				setMode( mode );
				if ( brushOptionsPanel != null )
					brushOptionsPanel.setVisible( visibility );

				// Show/hide appropriate mode panels
				if ( paintModePanel != null && eraseModePanel != null &&
						floodFillModePanel != null && floodEraseModePanel != null )
				{
					// Hide all mode panels first
					paintModePanel.setVisible( false );
					eraseModePanel.setVisible( false );
					floodFillModePanel.setVisible( false );
					floodEraseModePanel.setVisible( false );

					// Show the appropriate one
					if ( button == drawBtn )
					{
						paintModePanel.setVisible( true );
					}
					else if ( button == eraseBtn )
					{
						eraseModePanel.setVisible( true );
					}
					else if ( button == floodFillBtn )
					{
						floodFillModePanel.setVisible( true );
					}
					else if ( button == floodEraseBtn )
					{
						floodEraseModePanel.setVisible( true );
					}
				}
			}
		} );
		group.add( button );
		return button;
	}

	private JPanel initBrushSizePanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new MigLayout( "insets 4 8 4 8, gap 4, aligny center", "", "" ) );

		final JLabel label = new JLabel( "Brush size:" );
		final JSlider brushSizeSlider = initBrushSizeSlider();
		final JLabel valueLabel = initSliderValueLabel( brushSizeSlider );

		panel.add( label, "aligny center" );
		panel.add( brushSizeSlider, "width 100:150:200, aligny center" );
		panel.add( valueLabel, "width 20:25:30, aligny center" );

		panel.setBackground( OPTIONS_BACKGROUND );
		panel.setBorder( BorderFactory.createLineBorder( OPTIONS_BORDER ) );

		return panel;
	}

	private JPanel initPaintModePanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new MigLayout( "insets 4 8 4 8, gap 4", "", "[center]" ) );

		final JLabel paintModeLabel = new JLabel( "Paint mode:" );
		paintModeCombo = new JComboBox< PaintBrushMode >( PaintBrushMode.values() );
		paintModeCombo.setToolTipText( "" );
		paintModeCombo.setFocusable( false );
		paintModeCombo.setMaximumRowCount( 5 );
		paintModeCombo.addActionListener( e -> {
			final PaintBrushMode mode = ( PaintBrushMode ) paintModeCombo.getSelectedItem();
			if ( mode != null )
			{
				brushController.setPaintBrushMode( mode );
				panel.setToolTipText( mode.getTooltip() );
				paintModeCombo.setToolTipText( mode.getTooltip() );
			}
		} );
		paintModeCombo.setSelectedItem( brushController.getPaintBrushMode() );

		panel.add( paintModeLabel );
		panel.add( paintModeCombo, "width 100:120:150" );
		panel.setBackground( OPTIONS_BACKGROUND );
		panel.setBorder( BorderFactory.createLineBorder( OPTIONS_BORDER ) );
		return panel;
	}

	private JPanel initEraseModePanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new MigLayout( "insets 4 8 4 8, gap 4", "", "[center]" ) );

		final JLabel eraseModeLabel = new JLabel( "Erase mode:" );
		eraseModeCombo = new JComboBox<>( EraseBrushMode.values() );
		eraseModeCombo.setFocusable( false );
		eraseModeCombo.setMaximumRowCount( 5 );
		eraseModeCombo.addActionListener( e -> {
			final EraseBrushMode mode = ( EraseBrushMode ) eraseModeCombo.getSelectedItem();
			if ( mode != null )
			{
				brushController.setEraseBrushMode( ( EraseBrushMode ) eraseModeCombo.getSelectedItem() );
				panel.setToolTipText( mode.getTooltip() );
				eraseModeCombo.setToolTipText( mode.getTooltip() );
			}
		} );
		eraseModeCombo.setSelectedItem( brushController.getEraseBrushMode() );

		panel.add( eraseModeLabel );
		panel.add( eraseModeCombo, "width 100:120:150" );
		panel.setBackground( OPTIONS_BACKGROUND );
		panel.setBorder( BorderFactory.createLineBorder( OPTIONS_BORDER ) );
		return panel;
	}

	private void setMode( final Mode mode )
	{
		this.mode.setActive( false );
		this.mode = mode;
		this.mode.setActive( true );
	}

	private ImageIcon getIcon( final String iconPath )
	{
		return new ImageIcon( this.getClass().getResource( iconPath ) );
	}

	private JSlider initBrushSizeSlider()
	{
		final JSlider brushSize = new JSlider( 1, 50, ( int ) brushController.getBrushDiameter() );
		brushSize.setFocusable( false );
		brushSize.setPaintTrack( true );
		brushSize.addChangeListener( e -> {
			brushController.setBrushDiameter( brushSize.getValue() );
		} );
		brushSize.setOpaque( false );
		brushController.brushDiameterListeners().addListener( () -> {
			final double diameter = brushController.getBrushDiameter();
			brushSize.setValue( ( int ) diameter );
		} );
		return brushSize;
	}

	private JLabel initSliderValueLabel( final JSlider brushSize )
	{
		final JLabel valLabel = new JLabel( String.valueOf( brushSize.getValue() ) );
		brushSize.addChangeListener( e -> {
			valLabel.setText( String.valueOf( brushSize.getValue() ) );
		} );
		return valLabel;
	}

	private interface Mode
	{

		void setActive( boolean active );
	}

	public void install( final Actions actions )
	{
		final Mode panActive = createMode( ignore -> {}, moveBtn );
		final Mode brushActive = createMode( brushController::setBrushActive, drawBtn );
		final Mode eraserActive = createMode( brushController::setEraserActive, eraseBtn );
		final Mode floodFillActive = createMode( floodFillController::setFloodFillActive, floodFillBtn );
		final Mode fillEraseActive = createMode( floodFillController::setRemoveBlobActive, floodEraseBtn );
		final Mode selectLabelActive = createMode( selectLabelController::setActive, selectLabelBtn );

		actions.runnableAction( () -> setMode( panActive ), SELECT_PAN_TOOL, SELECT_PAN_TOOL_KEYS );
		actions.runnableAction( () -> setMode( brushActive ), SELECT_DRAW_TOOL, SELECT_DRAW_TOOL_KEYS );
		actions.runnableAction( () -> setMode( eraserActive ), SELECT_ERASE_TOOL, SELECT_ERASE_TOOL_KEYS );
		actions.runnableAction( () -> setMode( floodFillActive ), SELECT_FLOOD_FILL_TOOL, SELECT_FLOOD_FILL_TOOL_KEYS );
		actions.runnableAction( () -> setMode( fillEraseActive ), SELECT_ERASE_FILL_TOOL, SELECT_ERASE_FILL_TOOL_KEYS );
		actions.runnableAction( () -> setMode( selectLabelActive ), SELECT_LABEL_TOOL, SELECT_LABEL_KEYS );
	}

	private Mode createMode( final BooleanConsumer runnable, final JToggleButton btn )
	{
		return new Mode()
		{
			@Override
			public void setActive( final boolean active )
			{
				runnable.accept( active );
				btn.setSelected( active );
			}
		};
	}

	private static final String SELECT_PAN_TOOL = "pan tool";
	private static final String SELECT_DRAW_TOOL = "paint tool";
	private static final String SELECT_FLOOD_FILL_TOOL = "flood fill tool";
	private static final String SELECT_ERASE_TOOL = "erase tool";
	private static final String SELECT_ERASE_FILL_TOOL = "erase fill tool";
	private static final String SELECT_LABEL_TOOL = "select label tool";

	private static final String[] SELECT_PAN_TOOL_KEYS = new String[] { "F1" };
	private static final String[] SELECT_DRAW_TOOL_KEYS = new String[] { "F2" };
	private static final String[] SELECT_ERASE_TOOL_KEYS = new String[] { "F3" };
	private static final String[] SELECT_FLOOD_FILL_TOOL_KEYS = new String[] { "F4" };
	private static final String[] SELECT_ERASE_FILL_TOOL_KEYS = new String[] { "F5" };
	private static final String[] SELECT_LABEL_KEYS = new String[] { "F6" };

	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{

		public Descriptions()
		{
			super( KEY_CONFIG_SCOPE, KEY_CONFIG_CONTEXT );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( SELECT_PAN_TOOL, SELECT_PAN_TOOL_KEYS, "Make the pan tool active." );
			descriptions.add( SELECT_DRAW_TOOL, SELECT_DRAW_TOOL_KEYS, "Make the draw tool active." );
			descriptions.add( SELECT_FLOOD_FILL_TOOL, SELECT_FLOOD_FILL_TOOL_KEYS, "Make the flood fill tool active." );
			descriptions.add( SELECT_ERASE_TOOL, SELECT_ERASE_TOOL_KEYS, "Make the erase tool active." );
			descriptions.add( SELECT_ERASE_FILL_TOOL, SELECT_ERASE_FILL_TOOL_KEYS, "Make the erase fill tool active." );
			descriptions.add( SELECT_LABEL_TOOL, SELECT_LABEL_KEYS, "Make the select label tool active." );
		}
	}
}
