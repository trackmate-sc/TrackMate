/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2025 TrackMate developers.
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

import org.apache.commons.lang3.function.BooleanConsumer;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Actions;

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

	private final JPanel brushOptionsPanel;

	private final ButtonGroup group = new ButtonGroup();

	private Mode mode = ignore -> {};

	private final JToggleButton moveBtn;

	private final JToggleButton drawBtn;

	private final JToggleButton floodFillBtn;

	private final JToggleButton eraseBtn;

	private final JToggleButton floodEraseBtn;

	private final JToggleButton selectLabelBtn;

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
		this.brushOptionsPanel = initBrushOptionPanel();

		// Setup layout - horizontal with two areas: buttons and brush options
		setLayout( new MigLayout( "insets 0, gap 4", "[shrink][grow, fill]", "[]" ) );
	    setBorder( BorderFactory.createEmptyBorder( 0, 0, 4, 0 ) );

		// Create tool buttons panel (horizontal layout)
		final JPanel toolsPanel = new JPanel( new MigLayout( "insets 0, gap 2", "", "" ) );
	    toolsPanel.add( moveBtn );
		toolsPanel.add( drawBtn );
		toolsPanel.add( eraseBtn );
	    toolsPanel.add( floodFillBtn );
	    toolsPanel.add( floodEraseBtn );
	    toolsPanel.add( selectLabelBtn );
		// Optionally add the planar mode button here
		// toolsPanel.add( initPlanarModeButton() );

		// Initialize and setup brush options panel

		// Add both panels to the main panel
		add( toolsPanel, "aligny top" );
		add( brushOptionsPanel, "hidemode 3, wmax 220" );


		// Set initial state - move tool selected, brush panel hidden
		moveBtn.doClick();
	}

	private void setVisibility( final boolean brushVisible )
	{
		if ( brushOptionsPanel != null )
		{
			brushOptionsPanel.setVisible( brushVisible );
			revalidate();
			repaint();
		}
	}

	private JPanel initBrushOptionPanel()
	{
		final JPanel panel = new JPanel();
		panel.setLayout( new MigLayout( "insets 0 8 0 4, gap 4", "", "[center]" ) );

		final JLabel label = new JLabel( "Brush size:" );
		final JSlider brushSizeSlider = initBrushSizeSlider();
		final JLabel valueLabel = initSliderValueLabel( brushSizeSlider );

		panel.add( label );
		panel.add( brushSizeSlider, "width 100:150:200" );
		panel.add( valueLabel, "width 20:25:30" );

		panel.setBackground( OPTIONS_BACKGROUND );
		panel.setMaximumSize( new java.awt.Dimension( 300, 40 ) );
		panel.setBorder( BorderFactory.createLineBorder( OPTIONS_BORDER ) );

		return panel;
	}

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
				setVisibility( visibility );
			}
		} );
		group.add( button );
		return button;
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
