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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

	public MeshSmootherPanel( final MeshSmootherModel model )
	{
		final BoundedDoubleElement smoothing = StyleElements.boundedDoubleElement( "Smoothing (%)", 0., 100., () -> model.getMu() * 100., v -> model.setSmoothing( v / 100. ) );
		final IntElement nIters1 = StyleElements.intElement( "N iterations", 1, 50, model::getNIters, model::setNIters );
		final BoundedDoubleElement mu = StyleElements.boundedDoubleElement( "µ", 0., 1., model::getMu, model::setMu );
		final BoundedDoubleElement lambda = StyleElements.boundedDoubleElement( "-λ", 0., 1., () -> -model.getLambda(), l -> model.setLambda( -l ) );
		final EnumElement< TaubinWeightType > weightType = StyleElements.enumElement( "weight type", TaubinWeightType.values(), model::getWeightType, model::setWeightType );
		final IntElement nIters2 = StyleElements.intElement( "N iterations", 1, 50, model::getNIters, model::setNIters );

		final List< StyleElement > simpleElements = Arrays.asList( smoothing, nIters1 );
		final List< StyleElement > advancedElements = Arrays.asList( mu, lambda, nIters2, weightType );

		setLayout( new BorderLayout( 0, 0 ) );

		final JPanel bottomPanel = new JPanel();
		add( bottomPanel, BorderLayout.SOUTH );
		bottomPanel.setLayout( new BoxLayout( bottomPanel, BoxLayout.Y_AXIS ) );

		final JPanel selectionPanel = new JPanel();
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

		final JTabbedPane mainPanel = new JTabbedPane( JTabbedPane.TOP );
		add( mainPanel, BorderLayout.CENTER );

		final JPanel panelSimple = new JPanel();
		panelSimple.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		final MyStyleElementVisitors simplePanelVisitor = new MyStyleElementVisitors( panelSimple );
		simpleElements.forEach( el -> el.accept( simplePanelVisitor ) );
		mainPanel.addTab( "Simple", null, panelSimple, null );

		final JPanel panelAdvanced = new JPanel();
		panelAdvanced.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		final MyStyleElementVisitors advancedPanelVisitor = new MyStyleElementVisitors( panelAdvanced );
		advancedElements.forEach( el -> el.accept( advancedPanelVisitor ) );
		mainPanel.addTab( "Advanced", null, panelAdvanced, null );

		final ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add( rdbtnAll );
		buttonGroup.add( rdbtnSelection );
		rdbtnSelection.setSelected( true );

		mainPanel.addChangeListener( new ChangeListener()
		{

			@Override
			public void stateChanged( final ChangeEvent e )
			{
				if ( mainPanel.getSelectedIndex() == 0 )
				{
					// Simple.
					model.setSmoothing( smoothing.get() );
					model.setNIters( nIters1.get() );
					model.setWeightType( TaubinWeightType.NAIVE );
				}
				else
				{
					// Advanced.
					model.setMu( mu.get() );
					model.setLambda( lambda.get() );
					model.setNIters( nIters2.get() );
					model.setWeightType( weightType.getValue() );
				}
			}
		} );
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
			panel.add( StyleElements.linkedSliderPanel( el, 3 ), gbcs );
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
			panel.add( StyleElements.linkedSliderPanel( el, 3 ), gbcs );
			gbcs.gridy++;
		}

		private Font getFont()
		{
			return panel.getFont();
		}
	}

	public static void main( final String[] args )
	{
		final MeshSmootherPanel panel = new MeshSmootherPanel( new MeshSmootherModel() );

		final JFrame frame = new JFrame( "Smoothing params" );
		frame.getContentPane().add( panel );
		frame.setSize( 400, 300 );
		frame.setLocationRelativeTo( null );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setVisible( true );
	}
}
