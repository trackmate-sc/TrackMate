/*-
 * #%L
 * The Labkit image segmentation tool for Fiji.
 * %%
 * Copyright (C) 2017 - 2024 Matthias Arzt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
import javax.swing.JCheckBox;
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
import sc.fiji.labkit.ui.brush.PlanarModeController;

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
			"- <b>Ctrl + Shift + Mouse Wheel</b> or <b>+ / -</b> to zoom in and out.<br>" +
			"- <b>← / →</b> or <b>F / G</b> to move through time.<br>" +
			"- <b>Shift + R</b> to reset the view." +
			 "</small></html>";

	private static final String DRAW_TOOL_TIP = "<html><b>Add</b><br>" +
			"<small>Keyboard shortcuts:<br>" +
			"- Hold down the <b>A</b> key and <b>Left Click</b> on the image to draw the selected label.<br>" +
			"- Hold down the <b>A</b> key and use the <b>Mouse Wheel</b> to change the brush diameter.<br>" +
			"</small></html>";

	private static final String ERASE_TOOL_TIP = "<html><b>Delete</b><br>" +
			"<small>Keyboard shortcuts:<br>" +
			"- Hold down the <b>D</b> key and <b>Left Click</b> on the image to delete the selected label.<br>" +
			"- Hold down the <b>D</b> key and use the <b>Mouse Wheel</b> to change the brush diameter.<br>" +
			"</small></html>";

	private static final String FLOOD_FILL_TOOL_TIP = "<html><b>Flood Fill</b><br>" +
			"<small>Keyboard shortcuts:<br>" +
			"- Hold down the <b>Q</b> key and <b>Left Click</b> on the image to flood fill with the selected label.<br>" +
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

	private final PlanarModeController planarModeController;

	private JPanel brushOptionsPanel;

	private final ButtonGroup group = new ButtonGroup();

	private Mode mode = ignore -> {};

	private JToggleButton moveBtn;

	private JToggleButton drawBtn;

	private JToggleButton floodFillBtn;

	private JToggleButton eraseBtn;

	private JToggleButton floodEraseBtn;

	private JToggleButton selectLabelBtn;

	public TMLabelToolsPanel(
			final TMLabelBrushController brushController,
			final TMFloodFillController floodFillController,
			final TMSelectLabelController selectLabelController,
			final PlanarModeController planarModeController )
	{
		this.brushController = brushController;
		this.floodFillController = floodFillController;
		this.selectLabelController = selectLabelController;
		this.planarModeController = planarModeController;

		setLayout( new MigLayout( "flowy, insets 0, gap 4pt, top", "[][][][][]",
				"[]push" ) );
		setBorder( BorderFactory.createEmptyBorder( 0, 0, 4, 0 ) );
		initActionButtons();
		add( initOptionPanel(), "wrap, growy" );
		add( initPlanarModeButton(), "growy" );
	}

	private void setMode( final Mode mode )
	{
		this.mode.setActive( false );
		this.mode = mode;
		this.mode.setActive( true );
	}

	private void setVisibility( final boolean brushVisible )
	{
		if ( brushOptionsPanel != null )
			brushOptionsPanel.setVisible( brushVisible );
	}

	private void initActionButtons()
	{
		this.moveBtn = addActionButton( MOVE_TOOL_TIP, ignore -> {}, false, "/images/move.png" );
		this.drawBtn = addActionButton( DRAW_TOOL_TIP, brushController::setBrushActive, true, "/images/draw.png" );
		this.floodFillBtn = addActionButton( FLOOD_FILL_TOOL_TIP, floodFillController::setFloodFillActive, false, "/images/fill.png" );
		this.eraseBtn = addActionButton( ERASE_TOOL_TIP, brushController::setEraserActive, true, "/images/erase.png" );
		this.floodEraseBtn = addActionButton( FLOOD_ERASE_TOOL_TIP, floodFillController::setRemoveBlobActive, false, "/images/flooderase.png" );
		this.selectLabelBtn = addActionButton( SELECT_LABEL_TOOL_TIP, selectLabelController::setActive, false, "/images/pipette.png" );
		moveBtn.doClick();
	}

	private JPanel initOptionPanel()
	{
		final JPanel optionPane = new JPanel();
		optionPane.setLayout( new MigLayout( "insets 0" ) );
		optionPane.add( initOverrideCheckBox() );
		optionPane.add( initBrushOptionPanel(), "al left" );
		optionPane.setBackground( OPTIONS_BACKGROUND );
		optionPane.setBorder( BorderFactory.createLineBorder( OPTIONS_BORDER ) );
		return optionPane;
	}

	private JToggleButton initPlanarModeButton()
	{
		final JToggleButton button = new JToggleButton();
		final ImageIcon rotateIcon = getIcon( "/images/rotate.png" );
		final ImageIcon planarIcon = getIcon( "/images/planes.png" );
		button.setIcon( rotateIcon );
		button.setFocusable( false );
		final String ENABLE_TEXT = "Click to: Enable slice by slice editing of 3d images.";
		final String DISABLE_TEXT = "Click to: Disable slice by slice editing and freely rotate 3d images.";
		button.addActionListener( ignore -> {
			final boolean selected = button.isSelected();
			button.setIcon( selected ? planarIcon : rotateIcon );
			button.setToolTipText( selected ? DISABLE_TEXT : ENABLE_TEXT );
			planarModeController.setActive( selected );
			floodFillController.setPlanarMode( selected );
			brushController.setPlanarMode( selected );
		} );
		button.setToolTipText( ENABLE_TEXT );
		return button;
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
		add( button, "wrap, top" );
		return button;
	}

	private ImageIcon getIcon( final String iconPath )
	{
		return new ImageIcon( this.getClass().getResource( iconPath ) );
	}

	private JPanel initBrushOptionPanel()
	{
		brushOptionsPanel = new JPanel();
		brushOptionsPanel.setLayout( new MigLayout( "insets 4pt, gap 2pt, wmax 300" ) );
		brushOptionsPanel.setOpaque( false );
		brushOptionsPanel.add( new JLabel( "Brush size:" ), "grow" );
		final JSlider brushSizeSlider = initBrushSizeSlider();
		brushOptionsPanel.add( brushSizeSlider, "grow" );
		brushOptionsPanel.add( initSliderValueLabel( brushSizeSlider ), "right" );
		return brushOptionsPanel;
	}

	private JCheckBox initOverrideCheckBox()
	{
		final JCheckBox checkBox = new JCheckBox( "allow overlapping labels" );
		checkBox.setOpaque( false );
		checkBox.addItemListener( action -> {
			final boolean overlapping = action.getStateChange() == ItemEvent.SELECTED;
			brushController.setOverlapping( overlapping );
			floodFillController.setOverlapping( overlapping );
		} );
		return checkBox;
	}

	private JSlider initBrushSizeSlider()
	{
		final JSlider brushSize = new JSlider( 1, 50, ( int ) brushController
				.getBrushDiameter() );
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
	private static final String[] SELECT_FLOOD_FILL_TOOL_KEYS = new String[] { "F3" };
	private static final String[] SELECT_ERASE_TOOL_KEYS = new String[] { "F4" };
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
