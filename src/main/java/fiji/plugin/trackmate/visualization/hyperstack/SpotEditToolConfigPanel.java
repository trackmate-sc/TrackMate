package fiji.plugin.trackmate.visualization.hyperstack;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Icons.SELECT_TRACK_ICON;
import static fiji.plugin.trackmate.gui.Icons.SELECT_TRACK_ICON_DOWNWARDS;
import static fiji.plugin.trackmate.gui.Icons.SELECT_TRACK_ICON_UPWARDS;
import static fiji.plugin.trackmate.gui.Icons.SPOT_ICON_64x64;
import static fiji.plugin.trackmate.gui.Icons.TRACK_ICON;
import static fiji.plugin.trackmate.gui.Icons.TRACK_ICON_64x64;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.util.ModelTools;
import ij.ImagePlus;

public class SpotEditToolConfigPanel extends JFrame
{
	private static final long serialVersionUID = 1L;

	private final Logger logger;

	private final JFormattedTextField jNFQualityThreshold;

	private final JFormattedTextField jNFDistanceTolerance;

	private final JFormattedTextField jNFNFrames;

	private final SpotEditTool parent;

	private final JFormattedTextField jNFNStepwiseTime;


	public SpotEditToolConfigPanel( final SpotEditTool parent )
	{
		this.parent = parent;

		/*
		 * Listeners
		 */

		final ActionListener al = new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				updateParamsFromTextFields();
			}
		};
		final FocusListener fl = new FocusListener()
		{
			@Override
			public void focusLost( final FocusEvent arg0 )
			{
				updateParamsFromTextFields();
			}

			@Override
			public void focusGained( final FocusEvent arg0 )
			{}
		};

		/*
		 * GUI
		 */

		setTitle( "TrackMate tools" );
		setIconImage( TRACK_ICON.getImage() );
		setResizable( false );
		getContentPane().setLayout( new BoxLayout( getContentPane(), BoxLayout.X_AXIS ) );

		final JPanel mainPanel = new JPanel();
		getContentPane().add( mainPanel );
		mainPanel.setLayout( null );

		final JLabel lblTitle = new JLabel( "TrackMate tools" );
		lblTitle.setBounds( 6, 6, 395, 33 );
		lblTitle.setFont( BIG_FONT );
		lblTitle.setIcon( TRACK_ICON_64x64 );
		mainPanel.add( lblTitle );

		final JPanel panelSemiAutoParams = new JPanel();
		panelSemiAutoParams.setBorder( new LineBorder( new Color( 252, 117, 0 ), 1, false ) );
		panelSemiAutoParams.setBounds( 6, 51, 192, 142 );
		mainPanel.add( panelSemiAutoParams );
		panelSemiAutoParams.setLayout( null );

		final JLabel lblSemiAutoTracking = new JLabel( "Semi-automatic tracking" );
		lblSemiAutoTracking.setBounds( 6, 6, 180, 16 );
		lblSemiAutoTracking.setFont( FONT.deriveFont( Font.BOLD ) );
		panelSemiAutoParams.add( lblSemiAutoTracking );

		final JLabel lblQualityThreshold = new JLabel( "Quality threshold" );
		lblQualityThreshold.setToolTipText( "<html>" +
				"The fraction of the initial spot quality <br>" +
				"found spots must have to be considered for linking. <br>" +
				"The higher, the more stringent.</html>" );
		lblQualityThreshold.setBounds( 6, 66, 119, 16 );
		lblQualityThreshold.setFont( SMALL_FONT );
		panelSemiAutoParams.add( lblQualityThreshold );

		jNFQualityThreshold = new JFormattedTextField( parent.params.qualityThreshold );
		jNFQualityThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		jNFQualityThreshold.setFont( SMALL_FONT );
		jNFQualityThreshold.setBounds( 137, 64, 49, 18 );
		jNFQualityThreshold.addActionListener( al );
		jNFQualityThreshold.addFocusListener( fl );

		panelSemiAutoParams.add( jNFQualityThreshold );

		final JLabel lblDistanceTolerance = new JLabel( "Distance tolerance" );
		lblDistanceTolerance.setToolTipText( "<html>" +
				"The maximal distance above which found spots are rejected, <br>" +
				"expressed in units of the initial spot radius.</html>" );
		lblDistanceTolerance.setBounds( 6, 86, 119, 16 );
		lblDistanceTolerance.setFont( SMALL_FONT );
		panelSemiAutoParams.add( lblDistanceTolerance );

		jNFDistanceTolerance = new JFormattedTextField( parent.params.distanceTolerance );
		jNFDistanceTolerance.setHorizontalAlignment( SwingConstants.CENTER );
		jNFDistanceTolerance.setFont( SMALL_FONT );
		jNFDistanceTolerance.setBounds( 137, 84, 49, 18 );
		jNFDistanceTolerance.addActionListener( al );
		jNFDistanceTolerance.addFocusListener( fl );
		panelSemiAutoParams.add( jNFDistanceTolerance );

		final JLabel lblNFrames = new JLabel( "Max nFrames" );
		lblNFrames.setToolTipText( "<html>How many frames to process at max. <br/>Make it 0 or negative for no limit.</html>" );
		lblNFrames.setBounds( 6, 104, 119, 16 );
		lblNFrames.setFont( SMALL_FONT );
		panelSemiAutoParams.add( lblNFrames );

		jNFNFrames = new JFormattedTextField( parent.params.nFrames );
		jNFNFrames.setBounds( 137, 104, 49, 18 );
		jNFNFrames.setHorizontalAlignment( SwingConstants.CENTER );
		jNFNFrames.setFont( SMALL_FONT );
		jNFNFrames.addActionListener( al );
		jNFNFrames.addFocusListener( fl );
		panelSemiAutoParams.add( jNFNFrames );

		final JButton buttonSemiAutoTracking = new JButton( SPOT_ICON_64x64 );
		buttonSemiAutoTracking.setBounds( 6, 31, 33, 23 );
		panelSemiAutoParams.add( buttonSemiAutoTracking );
		buttonSemiAutoTracking.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				semiAutoTracking();
			}
		} );

		final JLabel labelSemiAutoTracking = new JLabel( "Semi-automatic tracking" );
		labelSemiAutoTracking.setToolTipText( "Launch semi-automatic tracking on selected spots." );
		labelSemiAutoTracking.setFont( SMALL_FONT );
		labelSemiAutoTracking.setBounds( 49, 31, 137, 23 );
		panelSemiAutoParams.add( labelSemiAutoTracking );


		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		scrollPane.setBounds( 210, 51, 264, 328 );
		mainPanel.add( scrollPane );

		final JTextPane textPane = new JTextPane();
		textPane.setFont( SMALL_FONT );
		textPane.setEditable( false );
		textPane.setBackground( this.getBackground() );
		scrollPane.setViewportView( textPane );

		final JPanel panelButtons = new JPanel();
		panelButtons.setBounds( 6, 262, 192, 117 );
		panelButtons.setBorder( new LineBorder( new Color( 252, 117, 0 ), 1, false ) );
		mainPanel.add( panelButtons );
		panelButtons.setLayout( null );

		final JLabel lblSelectionTools = new JLabel( "Selection tools" );
		lblSelectionTools.setFont( FONT.deriveFont( Font.BOLD ) );
		lblSelectionTools.setBounds( 6, 11, 172, 14 );
		panelButtons.add( lblSelectionTools );

		final JButton buttonSelectTrack = new JButton( SELECT_TRACK_ICON );
		buttonSelectTrack.setBounds( 10, 36, 33, 23 );
		buttonSelectTrack.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				selectTrack();
			}
		} );
		panelButtons.add( buttonSelectTrack );

		final JLabel lblSelectTrack = new JLabel( "Select track" );
		lblSelectTrack.setBounds( 53, 36, 129, 23 );
		lblSelectTrack.setFont( SMALL_FONT );
		lblSelectTrack.setToolTipText( "Select the whole tracks selected spots belong to." );
		panelButtons.add( lblSelectTrack );

		final JButton buttonSelectTrackUp = new JButton( SELECT_TRACK_ICON_UPWARDS );
		buttonSelectTrackUp.setBounds( 10, 61, 33, 23 );
		buttonSelectTrackUp.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				selectTrackUpward();
			}
		} );
		panelButtons.add( buttonSelectTrackUp );

		final JLabel lblSelectTrackUpward = new JLabel( "Select track upward" );
		lblSelectTrackUpward.setBounds( 53, 61, 129, 23 );
		lblSelectTrackUpward.setFont( SMALL_FONT );
		lblSelectTrackUpward.setToolTipText( "<html>" +
				"Select the whole tracks selected spots <br>" +
				"belong to, backward in time.</html>" );
		panelButtons.add( lblSelectTrackUpward );

		final JButton buttonSelectTrackDown = new JButton( SELECT_TRACK_ICON_DOWNWARDS );
		buttonSelectTrackDown.setBounds( 10, 86, 33, 23 );
		buttonSelectTrackDown.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				selectTrackDownward();
			}
		} );
		panelButtons.add( buttonSelectTrackDown );

		final JLabel lblSelectTrackDown = new JLabel( "Select track downward" );
		lblSelectTrackDown.setBounds( 53, 86, 129, 23 );
		lblSelectTrackDown.setFont( SMALL_FONT );
		lblSelectTrackDown.setToolTipText( "<html>" +
				"Select the whole tracks selected spots <br>" +
				"belong to, forward in time.</html>" );
		panelButtons.add( lblSelectTrackDown );

		final JPanel panel = new JPanel();
		panel.setBorder( new LineBorder( new Color( 252, 117, 0 ) ) );
		panel.setBounds( 6, 201, 192, 53 );
		mainPanel.add( panel );
		panel.setLayout( null );

		final JLabel lblNavigationTools = new JLabel( "Navigation tools" );
		lblNavigationTools.setBounds( 6, 6, 172, 14 );
		lblNavigationTools.setFont( FONT.deriveFont( Font.BOLD ) );
		panel.add( lblNavigationTools );

		jNFNStepwiseTime = new JFormattedTextField( parent.params.stepwiseTimeBrowsing );
		jNFNStepwiseTime.setBounds( 137, 26, 49, 18 );
		jNFNStepwiseTime.setHorizontalAlignment( SwingConstants.CENTER );
		jNFNStepwiseTime.setFont( SMALL_FONT );
		jNFNStepwiseTime.addActionListener( al );
		jNFNStepwiseTime.addFocusListener( fl );
		panel.add( jNFNStepwiseTime );

		final JLabel lblJumpByb = new JLabel( "Stepwise time browsing" );
		lblJumpByb.setBounds( 10, 29, 120, 14 );
		lblJumpByb.setFont( SMALL_FONT );
		panel.add( lblJumpByb );

		logger = new Logger()
		{

			@Override
			public void error( final String message )
			{
				log( message, Logger.ERROR_COLOR );
			}

			@Override
			public void log( final String message, final Color color )
			{
				SwingUtilities.invokeLater( new Runnable()
				{
					@Override
					public void run()
					{
						textPane.setEditable( true );
						final StyleContext sc = StyleContext.getDefaultStyleContext();
						final AttributeSet aset = sc.addAttribute( SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color );
						final int len = textPane.getDocument().getLength();
						textPane.setCaretPosition( len );
						textPane.setCharacterAttributes( aset, false );
						textPane.replaceSelection( message );
						textPane.setEditable( false );
					}
				} );
			}

			@Override
			public void setStatus( final String status )
			{
				log( status, Logger.GREEN_COLOR );
			}

			@Override
			public void setProgress( final double val )
			{}
		};

		setSize( 487, 418 );
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		setVisible( true );
	}

	private void selectTrackDownward()
	{
		final ImagePlus imp = parent.imp;
		if ( imp == null )
			return;
		final HyperStackDisplayer displayer = parent.displayers.get( imp );
		if ( null == displayer )
			return;
		final SelectionModel selectionModel = displayer.getSelectionModel();
		ModelTools.selectTrackDownward( selectionModel );
	}

	private void selectTrackUpward()
	{
		final ImagePlus imp = parent.imp;
		if ( imp == null )
			return;
		final HyperStackDisplayer displayer = parent.displayers.get( imp );
		if ( null == displayer )
			return;
		final SelectionModel selectionModel = displayer.getSelectionModel();
		ModelTools.selectTrackUpward( selectionModel );
	}

	private void selectTrack()
	{
		final ImagePlus imp = parent.imp;
		if ( imp == null )
			return;
		final HyperStackDisplayer displayer = parent.displayers.get( imp );
		if ( null == displayer )
			return;
		final SelectionModel selectionModel = displayer.getSelectionModel();
		ModelTools.selectTrack( selectionModel );
	}

	/**
	 * Returns the {@link Logger} that outputs on this config panel.
	 *
	 * @return the {@link Logger} instance of this panel.
	 */
	public Logger getLogger()
	{
		return logger;
	}

	private void updateParamsFromTextFields()
	{
		parent.params.distanceTolerance = ( ( Number ) jNFDistanceTolerance.getValue() ).doubleValue();
		parent.params.qualityThreshold = ( ( Number ) jNFQualityThreshold.getValue() ).doubleValue();
		parent.params.nFrames = ( ( Number ) jNFNFrames.getValue() ).intValue();
		parent.params.stepwiseTimeBrowsing = ( ( Number ) jNFNStepwiseTime.getValue() ).intValue();
	}

	private void semiAutoTracking()
	{
		final ImagePlus imp = parent.imp;
		if ( imp == null )
			return;
		final HyperStackDisplayer displayer = parent.displayers.get( imp );
		if ( null == displayer )
			return;
		final Model model = displayer.getModel();
		final SelectionModel selectionModel = displayer.getSelectionModel();
		parent.semiAutoTracking( model, selectionModel, imp );
	}
}
