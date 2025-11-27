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

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.ui.keymap.Keymap;
import bdv.ui.splitpanel.SplitPanel;
import bdv.util.BdvHandle;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.DisplayMode;
import bdv.viewer.NavigationActions;
import bdv.viewer.ViewerPanel;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.gui.editor.labkit.component.TMFloodFillController.FloodEraseMode;
import fiji.plugin.trackmate.gui.editor.labkit.component.TMFloodFillController.FloodFillMode;
import net.miginfocom.swing.MigLayout;
import sc.fiji.labkit.ui.bdv.BdvAutoContrast;
import sc.fiji.labkit.ui.bdv.BdvLayer;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.LabelsLayer;
import sc.fiji.labkit.ui.models.Holder;
import sc.fiji.labkit.ui.models.ImageLabelingModel;
import sc.fiji.labkit.ui.models.LabelingModel;

/**
 * A copy of BasicLabelingComponent to enable using custom components.
 */
public class TMBasicLabelingComponent extends JPanel implements AutoCloseable
{

	private static final long serialVersionUID = 1L;

	private final Holder< BdvStackSource< ? > > imageSource;

	private BdvHandle bdvHandle;

	private final JFrame dialogBoxOwner;

	private final ImageLabelingModel model;

	private JSlider zSlider;

	private TMLabelBrushController brushController;

	private TMFloodFillController floodFillController;

	private TMSelectLabelController selectLabelController;

	private TMLabelToolsPanel toolsPanel;

	public TMBasicLabelingComponent(
			final JFrame dialogBoxOwner,
			final ImageLabelingModel model,
			final BdvOptions options )
	{
		this.model = model;
		this.dialogBoxOwner = dialogBoxOwner;

		initBdv( options );
		this.imageSource = initImageLayer();
		initLabelsLayer();
		initPanel();
		this.model.transformationModel().initialize( bdvHandle.getViewerPanel() );
	}

	// Give focus to BDV when activated
	@Override
	public void addNotify()
	{
		super.addNotify();
		final Window window = SwingUtilities.getWindowAncestor( this );
		if ( window != null )
		{
			window.addWindowListener( new WindowAdapter()
			{
				@Override
				public void windowActivated( final WindowEvent e )
				{
					// Request focus for the panel when the window is activated
					bdvHandle.getViewerPanel().requestFocusInWindow();
				}
			} );
		}
	}

	private void initBdv( final BdvOptions options )
	{
		bdvHandle = new BdvHandlePanel( dialogBoxOwner, options );
		final ViewerPanel viewer = bdvHandle.getViewerPanel();
		viewer.setDisplayMode( DisplayMode.FUSED );

		/*
		 * Re-add the navigation actions, but make them listen to updates in the
		 * keymap. Otherwise the BDV handle only receives the new key bindings
		 * after restart.
		 */
		final InputTriggerConfig inputTriggerConfig = bdvHandle.getViewerPanel().getInputTriggerConfig();
		final Actions navigationActions = new Actions( inputTriggerConfig, "bdv", "navigation" );
		final InputActionBindings keybindings = bdvHandle.getKeybindings();
		navigationActions.install( keybindings, "navigation" );
		NavigationActions.install( navigationActions, viewer, options.values.is2D() );

		final Keymap keymap = bdvHandle.getKeymapManager().getForwardSelectedKeymap();
		keymap.updateListeners().add( () -> {
			navigationActions.updateKeyConfig( keymap.getConfig() );
		} );
	}

	public BdvHandle getBdvHandle()
	{
		return bdvHandle;
	}

	private void initPanel()
	{
		setLayout( new MigLayout( "", "[grow]", "[][grow]" ) );
		zSlider = new JSlider( Adjustable.VERTICAL );
		zSlider.setFocusable( false );
		add( initToolsPanel(), "wrap, growx" );
		final JPanel bdvAndZSlider = new JPanel();
		bdvAndZSlider.setLayout( new BorderLayout() );
		bdvAndZSlider.add( bdvHandle.getSplitPanel() );
		bdvAndZSlider.add( zSlider, BorderLayout.LINE_END );
		add( bdvAndZSlider, "grow" );
	}

	private Holder< BdvStackSource< ? > > initImageLayer()
	{
		return addBdvLayer( new BdvLayer.FinalLayer( model.showable(), "Image", model
				.imageVisibility() ) );
	}

	private void initLabelsLayer()
	{
		addBdvLayer( new LabelsLayer( model ) );
	}

	public Holder< BdvStackSource< ? > > addBdvLayer( final BdvLayer layer )
	{
		return new TMBdvLayerLink( layer, bdvHandle );
	}

	private JPanel initToolsPanel()
	{
		this.brushController = new TMLabelBrushController( bdvHandle, model );
		this.floodFillController = new TMFloodFillController( bdvHandle, model );
		this.selectLabelController = new TMSelectLabelController( bdvHandle, model );

		this.toolsPanel = new TMLabelToolsPanel( brushController, floodFillController, selectLabelController );
		// Hide the zSlider toggle button if we are 2D
		final boolean is2D = DetectionUtils.is2D( model.imageForSegmentation().get() );
		if ( is2D )
			zSlider.setVisible( false );

		// To edit TrackMate spots, fill and erase replace existing labels.
		floodFillController.setFloodEraseMode( FloodEraseMode.REMOVE_ALL );
		floodFillController.setFloodFillMode( FloodFillMode.REPLACE );

		return toolsPanel;
	}

	public void install( final Actions actions, final Behaviours behaviours )
	{
		toolsPanel.install( actions );
		brushController.install( actions, behaviours );
		floodFillController.install( behaviours );
		selectLabelController.install( behaviours );
		MyChangeLabelAction.install( actions, model );
	}

	@Override
	public void close()
	{
		bdvHandle.close();
	}

	public void autoContrast()
	{
		BdvAutoContrast.autoContrast( imageSource.get() );
	}

	public void toggleContrastSettings()
	{
		final SplitPanel splitPanel = bdvHandle.getSplitPanel();
		splitPanel.setCollapsed( !splitPanel.isCollapsed() );
	}

	public static class MyChangeLabelAction extends AbstractNamedAction
	{

		private static final long serialVersionUID = 1L;

		private static final String NEXT_LABEL = "next label";
		private static final String PREVIOUS_LABEL = "previous label";

		private static final String[] NEXT_LABEL_KEYS = new String[] { "N" };
		private static final String[] PREVIOUS_LABEL_KEYS = new String[] { "P" };

		private final LabelingModel model;

		private final boolean next;

		public MyChangeLabelAction( final LabelingModel model, final boolean next )
		{
			super( next ? NEXT_LABEL : PREVIOUS_LABEL );
			this.model = model;
			this.next = next;
		}

		@Override
		public void actionPerformed( final ActionEvent actionEvent )
		{
			final List< Label > labels = model.labeling().get().getLabels();
			final Label nextLabel = next
					? next( labels, model.selectedLabel().get() )
					: previous( labels, model.selectedLabel().get() );
			if ( nextLabel != null )
				model.selectedLabel().set( nextLabel );
		}

		private Label next( final List< Label > labels, final Label currentLabel )
		{
			if ( labels.isEmpty() )
				return null;
			int index = labels.indexOf( currentLabel ) + 1;
			if ( index >= labels.size() )
				index = 0;
			return labels.get( index );
		}

		private Label previous( final List< Label > labels, final Label currentLabel )
		{
			if ( labels.isEmpty() )
				return null;
			int index = labels.indexOf( currentLabel ) - 1;
			if ( index < 0 )
				index = labels.size() - 1;
			return labels.get( index );
		}

		public static void install( final Actions actions, final LabelingModel model )
		{
			actions.namedAction( new MyChangeLabelAction( model, true ), NEXT_LABEL_KEYS );
			actions.namedAction( new MyChangeLabelAction( model, false ), PREVIOUS_LABEL_KEYS );
		}

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
				descriptions.add( NEXT_LABEL, NEXT_LABEL_KEYS, "Select the next label in the list." );
				descriptions.add( PREVIOUS_LABEL, PREVIOUS_LABEL_KEYS, "Select the previous label in the list." );
			}
		}
	}
}
