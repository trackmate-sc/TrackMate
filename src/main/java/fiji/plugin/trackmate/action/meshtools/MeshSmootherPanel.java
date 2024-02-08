/*-
 * #%L
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
package fiji.plugin.trackmate.action.meshtools;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;

import fiji.plugin.trackmate.gui.displaysettings.SliderPanel;
import fiji.plugin.trackmate.gui.displaysettings.SliderPanelDouble;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.EnumElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.IntElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElementVisitor;
import net.imglib2.mesh.alg.TaubinSmoothing.TaubinWeightType;

public class MeshSmootherPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	final JButton btnRun;

	final JButton btnUndo;

	final JRadioButton rdbtnSelection;

	final JRadioButton rdbtnAll;

	private final MeshSmootherModel modelBasic;

	private final MeshSmootherModel modelSimple;

	private final MeshSmootherModel modelAdvanced;

	private final JTabbedPane mainPanel;

	public MeshSmootherPanel()
	{
		this.modelBasic = new MeshSmootherModel();
		modelBasic.setMu( 1. );
		modelBasic.setLambda( 0. );
		modelBasic.setWeightType( TaubinWeightType.NAIVE );
		final IntElement nItersBasic = StyleElements.intElement( "N iterations", 1, 50, modelBasic::getNIters, modelBasic::setNIters );
		final List< StyleElement > superSimpleElements = Arrays.asList( nItersBasic );

		this.modelSimple = new MeshSmootherModel();
		final BoundedDoubleElement smoothing = StyleElements.boundedDoubleElement( "Smoothing (%)", 0., 100., () -> modelSimple.getMu() * 100., v -> modelSimple.setSmoothing( v / 100. ) );
		final IntElement nItersSimple = StyleElements.intElement( "N iterations", 1, 50, modelSimple::getNIters, modelSimple::setNIters );
		final List< StyleElement > simpleElements = Arrays.asList( smoothing, nItersSimple );

		this.modelAdvanced = new MeshSmootherModel();
		final BoundedDoubleElement mu = StyleElements.boundedDoubleElement( "µ", 0., 1., modelAdvanced::getMu, modelAdvanced::setMu );
		final BoundedDoubleElement lambda = StyleElements.boundedDoubleElement( "-λ", 0., 1., () -> -modelAdvanced.getLambda(), l -> modelAdvanced.setLambda( -l ) );
		final EnumElement< TaubinWeightType > weightType = StyleElements.enumElement( "weight type", TaubinWeightType.values(), modelAdvanced::getWeightType, modelAdvanced::setWeightType );
		final IntElement nItersAdvanced = StyleElements.intElement( "N iterations", 1, 50, modelAdvanced::getNIters, modelAdvanced::setNIters );
		final List< StyleElement > advancedElements = Arrays.asList( mu, lambda, nItersAdvanced, weightType );

		setLayout( new BorderLayout( 0, 0 ) );

		final JPanel bottomPanel = new JPanel();
		add( bottomPanel, BorderLayout.SOUTH );
		bottomPanel.setLayout( new BoxLayout( bottomPanel, BoxLayout.Y_AXIS ) );

		final JPanel selectionPanel = new JPanel();
		selectionPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		bottomPanel.add( selectionPanel );
		selectionPanel.setLayout( new BoxLayout( selectionPanel, BoxLayout.X_AXIS ) );

		final JLabel lblRunOn = new JLabel( "Run on:" );
		selectionPanel.add( lblRunOn );

		selectionPanel.add( Box.createHorizontalGlue() );

		rdbtnSelection = new JRadioButton( "selection only" );
		selectionPanel.add( rdbtnSelection );

		rdbtnAll = new JRadioButton( "all visible spots" );
		selectionPanel.add( rdbtnAll );

		final JPanel buttonPanel = new JPanel();
		bottomPanel.add( buttonPanel );
		buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.X_AXIS ) );

		this.btnUndo = new JButton( "Undo" );
		buttonPanel.add( btnUndo );

		buttonPanel.add( Box.createHorizontalGlue() );

		this.btnRun = new JButton( "Run" );
		buttonPanel.add( btnRun );

		this.mainPanel = new JTabbedPane( JTabbedPane.TOP );
		add( mainPanel, BorderLayout.CENTER );

		final JPanel panelSuperSimple = new JPanel();
		panelSuperSimple.setOpaque( false );
		panelSuperSimple.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		final MyStyleElementVisitors superSimplePanelVisitor = new MyStyleElementVisitors( panelSuperSimple );
		superSimpleElements.forEach( el -> el.accept( superSimplePanelVisitor ) );
		mainPanel.addTab( "Basic", null, panelSuperSimple, null );

		final JPanel panelSimple = new JPanel();
		panelSimple.setOpaque( false );
		panelSimple.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		final MyStyleElementVisitors simplePanelVisitor = new MyStyleElementVisitors( panelSimple );
		simpleElements.forEach( el -> el.accept( simplePanelVisitor ) );
		mainPanel.addTab( "Simple", null, panelSimple, null );

		final JPanel panelAdvanced = new JPanel();
		panelAdvanced.setOpaque( false );
		panelAdvanced.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		final MyStyleElementVisitors advancedPanelVisitor = new MyStyleElementVisitors( panelAdvanced );
		advancedElements.forEach( el -> el.accept( advancedPanelVisitor ) );
		mainPanel.addTab( "Advanced", null, panelAdvanced, null );

		final ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add( rdbtnAll );
		buttonGroup.add( rdbtnSelection );
		rdbtnSelection.setSelected( true );
	}

	public MeshSmootherModel getModel()
	{
		switch ( mainPanel.getSelectedIndex() )
		{
		case 0:
			return modelBasic;
		case 1:
			return modelSimple;
		case 2:
			return modelAdvanced;
		}
		throw new IllegalStateException( "Cannot handle mesh smoothing settings type number " + ( mainPanel.getSelectedIndex() ) );
	}

	private static class MyStyleElementVisitors implements StyleElementVisitor
	{

		private final JPanel panel;

		private final GridBagConstraints gbcs;

		public MyStyleElementVisitors( final JPanel panel )
		{
			this.panel = panel;
			final GridBagLayout layout = new GridBagLayout();
			layout.columnWidths = new int[] { 0, 0, 0 };
			layout.rowHeights = new int[] { 40, 40, 40, 40 };
			layout.columnWeights = new double[] { 0., 1., Double.MIN_VALUE };
			layout.rowWeights = new double[] { 0., 0., 0., 0., 1. };
			panel.setLayout( layout );

			this.gbcs = new GridBagConstraints();
			gbcs.fill = GridBagConstraints.HORIZONTAL;
			gbcs.gridx = 0;
			gbcs.gridy = 0;
		}

		@Override
		public < E > void visit( final EnumElement< E > el )
		{
			gbcs.gridx = 0;
			final JLabel lbl = new JLabel( el.getLabel() );
			lbl.setHorizontalAlignment( JLabel.RIGHT );
			lbl.setFont( getFont().deriveFont( getFont().getSize2D() - 1f ) );
			panel.add( lbl, gbcs );
			gbcs.gridx++;
			panel.add( StyleElements.linkedComboBoxEnumSelector( el ), gbcs );
			gbcs.gridy++;
		}

		@Override
		public void visit( final BoundedDoubleElement el )
		{
			gbcs.gridx = 0;
			final JLabel lbl = new JLabel( el.getLabel() );
			lbl.setHorizontalAlignment( JLabel.RIGHT );
			lbl.setFont( getFont().deriveFont( getFont().getSize2D() - 1f ) );
			panel.add( lbl, gbcs );
			gbcs.gridx++;
			final SliderPanelDouble sliderPanel = StyleElements.linkedSliderPanel( el, 3 );
			sliderPanel.setOpaque( false );
			panel.add( sliderPanel, gbcs );
			gbcs.gridy++;
		}

		@Override
		public void visit( final IntElement el )
		{
			gbcs.gridx = 0;
			final JLabel lbl = new JLabel( el.getLabel() );
			lbl.setHorizontalAlignment( JLabel.RIGHT );
			lbl.setFont( getFont().deriveFont( getFont().getSize2D() - 1f ) );
			panel.add( lbl, gbcs );
			gbcs.gridx++;
			final SliderPanel sliderPanel = StyleElements.linkedSliderPanel( el, 3 );
			sliderPanel.setOpaque( false );
			panel.add( sliderPanel, gbcs );
			gbcs.gridy++;
		}

		private Font getFont()
		{
			return panel.getFont();
		}
	}

	public static void main( final String[] args )
	{
		final MeshSmootherPanel panel = new MeshSmootherPanel();

		final JFrame frame = new JFrame( "Smoothing params" );
		frame.getContentPane().add( panel );
		frame.setSize( 400, 300 );
		frame.setLocationRelativeTo( null );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setVisible( true );
	}
}
