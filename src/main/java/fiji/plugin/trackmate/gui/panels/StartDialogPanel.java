package fiji.plugin.trackmate.gui.panels;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TEXTFIELD_DIMENSION;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;
import fiji.util.NumberParser;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;

public class StartDialogPanel extends ActionListenablePanel
{

	private static final long serialVersionUID = -1L;

	private static final String TOOLTIP = "<html>" +
			"Pressing this button will make the current <br>" +
			"ImagePlus the source for TrackMate. If the <br>" +
			"image has a ROI, it will be used to set the <br>" +
			"crop rectangle as well.</html>";

	/** ActionEvent fired when the user press the refresh button. */
	private final ActionEvent IMAGEPLUS_REFRESHED = new ActionEvent( this, 0, "ImagePlus refreshed" );

	private final JLabel jLabelImageName;

	private final JNumericTextField jTextFieldXStart;

	private final JNumericTextField jTextFieldXEnd;

	private final JNumericTextField jTextFieldYStart;

	private final JNumericTextField jTextFieldYEnd;

	private final JNumericTextField jTextFieldZStart;

	private final JNumericTextField jTextFieldZEnd;

	private final JNumericTextField jTextFieldTEnd;

	private final JNumericTextField jTextFieldTStart;

	private final JButton jButtonRefresh;

	private final JLabel jLabelUnits4;

	private final JLabel jLabelUnits3;

	private final JLabel jLabelUnits2;

	private final JLabel jLabelUnits1;

	private final JNumericTextField jTextFieldPixelWidth;

	private final JNumericTextField jTextFieldVoxelDepth;

	private final JNumericTextField jTextFieldPixelHeight;

	private final JNumericTextField jTextFieldTimeInterval;

	private ImagePlus imp;

	private boolean impValid = false;

	public StartDialogPanel()
	{
		this.setPreferredSize( new java.awt.Dimension( 266, 476 ) );
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, 1.0, 1.0 };
		setLayout( gridBagLayout );

		final JLabel lblCitation = new JLabel( "<html>"
				+ "Please note that TrackMate is available through Fiji, "
				+ "and is based on a publication. If you use it successfully "
				+ "for your research please be so kind to cite our work:"
				+ "<p>"
				+ "<b>Tinevez, JY.; Perry, N. & Schindelin, J. et al. (2017), "
				+ "<i>TrackMate: An open and extensible platform for single-particle "
				+ "tracking.</i></b> Methods 115: 80-90."
				+ "</html>" );
		lblCitation.setFont( SMALL_FONT );
		final GridBagConstraints gbc_lblCitation = new GridBagConstraints();
		gbc_lblCitation.fill = GridBagConstraints.BOTH;
		gbc_lblCitation.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblCitation.gridwidth = 4;
		gbc_lblCitation.gridx = 0;
		gbc_lblCitation.gridy = 0;
		add( lblCitation, gbc_lblCitation );

		final JLabel lblLinkPubMed = new JLabel( "<html><a href=https://www.ncbi.nlm.nih.gov/pubmed/27713081>on PubMed (PMID 27713081)</a></html>" );
		lblLinkPubMed.setFont( SMALL_FONT );
		lblLinkPubMed.setCursor( new Cursor( Cursor.HAND_CURSOR ) );
		lblLinkPubMed.addMouseListener( new MouseAdapter()
		{
			@Override
			public void mouseClicked( final java.awt.event.MouseEvent e )
			{
				try
				{
					Desktop.getDesktop().browse( new URI( "https://www.ncbi.nlm.nih.gov/pubmed/27713081" ) );
				}
				catch ( URISyntaxException | IOException ex )
				{
					ex.printStackTrace();
				}
			}
		} );
		final GridBagConstraints gbc_lblLinkPubMed = new GridBagConstraints();
		gbc_lblLinkPubMed.anchor = GridBagConstraints.NORTH;
		gbc_lblLinkPubMed.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblLinkPubMed.gridwidth = 4;
		gbc_lblLinkPubMed.insets = new Insets( 0, 10, 5, 5 );
		gbc_lblLinkPubMed.gridx = 0;
		gbc_lblLinkPubMed.gridy = 1;
		add( lblLinkPubMed, gbc_lblLinkPubMed );

		jLabelImageName = new JLabel( "Select an image, and press refresh." );
		jLabelImageName.setFont( BIG_FONT );
		final GridBagConstraints gbc_jLabelImageName = new GridBagConstraints();
		gbc_jLabelImageName.anchor = GridBagConstraints.SOUTH;
		gbc_jLabelImageName.fill = GridBagConstraints.HORIZONTAL;
		gbc_jLabelImageName.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelImageName.gridwidth = 4;
		gbc_jLabelImageName.gridx = 0;
		gbc_jLabelImageName.gridy = 3;
		add( jLabelImageName, gbc_jLabelImageName );

		final JLabel jLabelCheckCalibration = new JLabel( "Calibration settings:" );
		jLabelCheckCalibration.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelCheckCalibration = new GridBagConstraints();
		gbc_jLabelCheckCalibration.anchor = GridBagConstraints.SOUTH;
		gbc_jLabelCheckCalibration.fill = GridBagConstraints.HORIZONTAL;
		gbc_jLabelCheckCalibration.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelCheckCalibration.gridwidth = 4;
		gbc_jLabelCheckCalibration.gridx = 0;
		gbc_jLabelCheckCalibration.gridy = 4;
		add( jLabelCheckCalibration, gbc_jLabelCheckCalibration );

		final JLabel jLabelPixelWidth = new JLabel( "Pixel width:" );
		jLabelPixelWidth.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelPixelWidth = new GridBagConstraints();
		gbc_jLabelPixelWidth.anchor = GridBagConstraints.EAST;
		gbc_jLabelPixelWidth.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelPixelWidth.gridwidth = 2;
		gbc_jLabelPixelWidth.gridx = 0;
		gbc_jLabelPixelWidth.gridy = 5;
		add( jLabelPixelWidth, gbc_jLabelPixelWidth );

		jTextFieldPixelWidth = new JNumericTextField();
		jTextFieldPixelWidth.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldPixelWidth.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldPixelWidth = new GridBagConstraints();
		gbc_jTextFieldPixelWidth.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldPixelWidth.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldPixelWidth.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldPixelWidth.gridx = 2;
		gbc_jTextFieldPixelWidth.gridy = 5;
		add( jTextFieldPixelWidth, gbc_jTextFieldPixelWidth );

		jLabelUnits1 = new JLabel();
		jLabelUnits1.setText( "units" );
		jLabelUnits1.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelUnits1 = new GridBagConstraints();
		gbc_jLabelUnits1.anchor = GridBagConstraints.WEST;
		gbc_jLabelUnits1.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelUnits1.gridx = 3;
		gbc_jLabelUnits1.gridy = 5;
		add( jLabelUnits1, gbc_jLabelUnits1 );

		final JLabel jLabelPixelHeight = new JLabel( "Pixel height:" );
		jLabelPixelHeight.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelPixelHeight = new GridBagConstraints();
		gbc_jLabelPixelHeight.anchor = GridBagConstraints.EAST;
		gbc_jLabelPixelHeight.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelPixelHeight.gridwidth = 2;
		gbc_jLabelPixelHeight.gridx = 0;
		gbc_jLabelPixelHeight.gridy = 6;
		add( jLabelPixelHeight, gbc_jLabelPixelHeight );

		jTextFieldPixelHeight = new JNumericTextField();
		jTextFieldPixelHeight.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldPixelHeight.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldPixelHeight = new GridBagConstraints();
		gbc_jTextFieldPixelHeight.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldPixelHeight.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldPixelHeight.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldPixelHeight.gridx = 2;
		gbc_jTextFieldPixelHeight.gridy = 6;
		add( jTextFieldPixelHeight, gbc_jTextFieldPixelHeight );

		final JLabel jLabelTimeInterval = new JLabel( "Time interval:" );
		jLabelTimeInterval.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelTimeInterval = new GridBagConstraints();
		gbc_jLabelTimeInterval.anchor = GridBagConstraints.EAST;
		gbc_jLabelTimeInterval.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelTimeInterval.gridwidth = 2;
		gbc_jLabelTimeInterval.gridx = 0;
		gbc_jLabelTimeInterval.gridy = 8;
		add( jLabelTimeInterval, gbc_jLabelTimeInterval );

		jLabelUnits2 = new JLabel();
		jLabelUnits2.setText( "units" );
		jLabelUnits2.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelUnits2 = new GridBagConstraints();
		gbc_jLabelUnits2.anchor = GridBagConstraints.WEST;
		gbc_jLabelUnits2.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelUnits2.gridx = 3;
		gbc_jLabelUnits2.gridy = 6;
		add( jLabelUnits2, gbc_jLabelUnits2 );

		jTextFieldVoxelDepth = new JNumericTextField();
		jTextFieldVoxelDepth.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldVoxelDepth.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldVoxelDepth = new GridBagConstraints();
		gbc_jTextFieldVoxelDepth.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldVoxelDepth.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldVoxelDepth.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldVoxelDepth.gridx = 2;
		gbc_jTextFieldVoxelDepth.gridy = 7;
		add( jTextFieldVoxelDepth, gbc_jTextFieldVoxelDepth );

		final JLabel jLabelVoxelDepth = new JLabel( "Voxel depth:" );
		jLabelVoxelDepth.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelVoxelDepth = new GridBagConstraints();
		gbc_jLabelVoxelDepth.anchor = GridBagConstraints.EAST;
		gbc_jLabelVoxelDepth.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelVoxelDepth.gridwidth = 2;
		gbc_jLabelVoxelDepth.gridx = 0;
		gbc_jLabelVoxelDepth.gridy = 7;
		add( jLabelVoxelDepth, gbc_jLabelVoxelDepth );

		jLabelUnits3 = new JLabel();
		jLabelUnits3.setText( "units" );
		jLabelUnits3.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelUnits3 = new GridBagConstraints();
		gbc_jLabelUnits3.anchor = GridBagConstraints.WEST;
		gbc_jLabelUnits3.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelUnits3.gridx = 3;
		gbc_jLabelUnits3.gridy = 7;
		add( jLabelUnits3, gbc_jLabelUnits3 );

		jLabelUnits4 = new JLabel();
		jLabelUnits4.setText( "units" );
		jLabelUnits4.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelUnits4 = new GridBagConstraints();
		gbc_jLabelUnits4.anchor = GridBagConstraints.WEST;
		gbc_jLabelUnits4.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelUnits4.gridx = 3;
		gbc_jLabelUnits4.gridy = 8;
		add( jLabelUnits4, gbc_jLabelUnits4 );

		jTextFieldTimeInterval = new JNumericTextField();
		jTextFieldTimeInterval.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldTimeInterval.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldTimeInterval = new GridBagConstraints();
		gbc_jTextFieldTimeInterval.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldTimeInterval.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldTimeInterval.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldTimeInterval.gridx = 2;
		gbc_jTextFieldTimeInterval.gridy = 8;
		add( jTextFieldTimeInterval, gbc_jTextFieldTimeInterval );

		final JLabel jLabelCropSetting = new JLabel( "Crop settings (in pixels, 0-based):" );
		jLabelCropSetting.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelCropSetting = new GridBagConstraints();
		gbc_jLabelCropSetting.anchor = GridBagConstraints.SOUTH;
		gbc_jLabelCropSetting.fill = GridBagConstraints.HORIZONTAL;
		gbc_jLabelCropSetting.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelCropSetting.gridwidth = 4;
		gbc_jLabelCropSetting.gridx = 0;
		gbc_jLabelCropSetting.gridy = 9;
		add( jLabelCropSetting, gbc_jLabelCropSetting );

		jTextFieldXStart = new JNumericTextField();
		jTextFieldXStart.setFormat( "%.0f" );
		jTextFieldXStart.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldXStart.setPreferredSize( TEXTFIELD_DIMENSION );
		jTextFieldXStart.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldXStart = new GridBagConstraints();
		gbc_jTextFieldXStart.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldXStart.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldXStart.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldXStart.gridx = 1;
		gbc_jTextFieldXStart.gridy = 10;
		add( jTextFieldXStart, gbc_jTextFieldXStart );

		jTextFieldXEnd = new JNumericTextField();
		jTextFieldXEnd.setFormat( "%.0f" );
		jTextFieldXEnd.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldXEnd.setPreferredSize( TEXTFIELD_DIMENSION );
		jTextFieldXEnd.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldXEnd = new GridBagConstraints();
		gbc_jTextFieldXEnd.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldXEnd.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldXEnd.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldXEnd.gridx = 3;
		gbc_jTextFieldXEnd.gridy = 10;
		add( jTextFieldXEnd, gbc_jTextFieldXEnd );

		final JLabel jLabelX = new JLabel( "X" );
		jLabelX.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelX = new GridBagConstraints();
		gbc_jLabelX.anchor = GridBagConstraints.EAST;
		gbc_jLabelX.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelX.gridx = 0;
		gbc_jLabelX.gridy = 10;
		add( jLabelX, gbc_jLabelX );

		final JLabel jLabelTo1 = new JLabel( "to" );
		jLabelTo1.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelTo1 = new GridBagConstraints();
		gbc_jLabelTo1.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelTo1.gridx = 2;
		gbc_jLabelTo1.gridy = 10;
		add( jLabelTo1, gbc_jLabelTo1 );

		final JLabel jLabelY = new JLabel( "Y" );
		jLabelY.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelY = new GridBagConstraints();
		gbc_jLabelY.anchor = GridBagConstraints.EAST;
		gbc_jLabelY.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelY.gridx = 0;
		gbc_jLabelY.gridy = 11;
		add( jLabelY, gbc_jLabelY );

		final JLabel jLabelTo3 = new JLabel( "to" );
		jLabelTo3.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelTo3 = new GridBagConstraints();
		gbc_jLabelTo3.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelTo3.gridx = 2;
		gbc_jLabelTo3.gridy = 12;
		add( jLabelTo3, gbc_jLabelTo3 );

		jTextFieldYStart = new JNumericTextField();
		jTextFieldYStart.setFormat( "%.0f" );
		jTextFieldYStart.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldYStart.setPreferredSize( TEXTFIELD_DIMENSION );
		jTextFieldYStart.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldYStart = new GridBagConstraints();
		gbc_jTextFieldYStart.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldYStart.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldYStart.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldYStart.gridx = 1;
		gbc_jTextFieldYStart.gridy = 11;
		add( jTextFieldYStart, gbc_jTextFieldYStart );

		final JLabel jLabelTo2 = new JLabel( "to" );
		jLabelTo2.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelTo2 = new GridBagConstraints();
		gbc_jLabelTo2.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelTo2.gridx = 2;
		gbc_jLabelTo2.gridy = 11;
		add( jLabelTo2, gbc_jLabelTo2 );

		final JLabel jLabelZ = new JLabel( "Z" );
		jLabelZ.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelZ = new GridBagConstraints();
		gbc_jLabelZ.anchor = GridBagConstraints.EAST;
		gbc_jLabelZ.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelZ.gridx = 0;
		gbc_jLabelZ.gridy = 12;
		add( jLabelZ, gbc_jLabelZ );

		jTextFieldYEnd = new JNumericTextField();
		jTextFieldYEnd.setFormat( "%.0f" );
		jTextFieldYEnd.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldYEnd.setPreferredSize( TEXTFIELD_DIMENSION );
		jTextFieldYEnd.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldYEnd = new GridBagConstraints();
		gbc_jTextFieldYEnd.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldYEnd.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldYEnd.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldYEnd.gridx = 3;
		gbc_jTextFieldYEnd.gridy = 11;
		add( jTextFieldYEnd, gbc_jTextFieldYEnd );

		jTextFieldZStart = new JNumericTextField();
		jTextFieldZStart.setFormat( "%.0f" );
		jTextFieldZStart.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldZStart.setPreferredSize( TEXTFIELD_DIMENSION );
		jTextFieldZStart.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldZStart = new GridBagConstraints();
		gbc_jTextFieldZStart.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldZStart.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldZStart.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldZStart.gridx = 1;
		gbc_jTextFieldZStart.gridy = 12;
		add( jTextFieldZStart, gbc_jTextFieldZStart );

		jTextFieldZEnd = new JNumericTextField();
		jTextFieldZEnd.setFormat( "%.0f" );
		jTextFieldZEnd.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldZEnd.setPreferredSize( TEXTFIELD_DIMENSION );
		jTextFieldZEnd.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldZEnd = new GridBagConstraints();
		gbc_jTextFieldZEnd.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldZEnd.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldZEnd.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldZEnd.gridx = 3;
		gbc_jTextFieldZEnd.gridy = 12;
		add( jTextFieldZEnd, gbc_jTextFieldZEnd );

		jTextFieldTStart = new JNumericTextField();
		jTextFieldTStart.setFormat( "%.0f" );
		jTextFieldTStart.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldTStart.setPreferredSize( TEXTFIELD_DIMENSION );
		jTextFieldTStart.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldTStart = new GridBagConstraints();
		gbc_jTextFieldTStart.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldTStart.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldTStart.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldTStart.gridx = 1;
		gbc_jTextFieldTStart.gridy = 13;
		add( jTextFieldTStart, gbc_jTextFieldTStart );

		final JLabel jLabelT = new JLabel( "T" );
		jLabelT.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelT = new GridBagConstraints();
		gbc_jLabelT.anchor = GridBagConstraints.EAST;
		gbc_jLabelT.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelT.gridx = 0;
		gbc_jLabelT.gridy = 13;
		add( jLabelT, gbc_jLabelT );

		jTextFieldTEnd = new JNumericTextField();
		jTextFieldTEnd.setFormat( "%.0f" );
		jTextFieldTEnd.setHorizontalAlignment( SwingConstants.CENTER );
		jTextFieldTEnd.setPreferredSize( TEXTFIELD_DIMENSION );
		jTextFieldTEnd.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jTextFieldTEnd = new GridBagConstraints();
		gbc_jTextFieldTEnd.fill = GridBagConstraints.HORIZONTAL;
		gbc_jTextFieldTEnd.anchor = GridBagConstraints.NORTH;
		gbc_jTextFieldTEnd.insets = new Insets( 5, 5, 5, 5 );
		gbc_jTextFieldTEnd.gridx = 3;
		gbc_jTextFieldTEnd.gridy = 13;
		add( jTextFieldTEnd, gbc_jTextFieldTEnd );

		final JLabel jLabelTo4 = new JLabel( "to" );
		jLabelTo4.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jLabelTo4 = new GridBagConstraints();
		gbc_jLabelTo4.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelTo4.gridx = 2;
		gbc_jLabelTo4.gridy = 13;
		add( jLabelTo4, gbc_jLabelTo4 );

		jButtonRefresh = new JButton( "Refresh source" );
		jButtonRefresh.setToolTipText( TOOLTIP );
		jButtonRefresh.setFont( SMALL_FONT );
		final GridBagConstraints gbc_jButtonRefresh = new GridBagConstraints();
		gbc_jButtonRefresh.anchor = GridBagConstraints.SOUTHWEST;
		gbc_jButtonRefresh.insets = new Insets( 5, 5, 5, 5 );
		gbc_jButtonRefresh.gridwidth = 4;
		gbc_jButtonRefresh.gridx = 0;
		gbc_jButtonRefresh.gridy = 14;
		add( jButtonRefresh, gbc_jButtonRefresh );
		jButtonRefresh.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				imp = WindowManager.getCurrentImage();
				getFrom( imp );
				fireAction( IMAGEPLUS_REFRESHED );
			}
		} );
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Returns <code>true</code> if the {@link ImagePlus} selected is valid and
	 * can be processed.
	 * 
	 * @return a boolean flag.
	 */
	public boolean isImpValid()
	{
		return impValid;
	}

	/**
	 * Update the specified settings object, with the parameters set in this
	 * panel.
	 * 
	 * @param settings
	 *            the Settings to update. Cannot be <code>null</code>.
	 */
	public void updateTo( final Model model, final Settings settings )
	{
		settings.imp = imp;
		// Crop cube
		settings.tstart = NumberParser.parseInteger( jTextFieldTStart.getText() );
		settings.tend = NumberParser.parseInteger( jTextFieldTEnd.getText() );
		settings.xstart = NumberParser.parseInteger( jTextFieldXStart.getText() );
		settings.xend = NumberParser.parseInteger( jTextFieldXEnd.getText() );
		settings.ystart = NumberParser.parseInteger( jTextFieldYStart.getText() );
		settings.yend = NumberParser.parseInteger( jTextFieldYEnd.getText() );
		settings.zstart = NumberParser.parseInteger( jTextFieldZStart.getText() );
		settings.zend = NumberParser.parseInteger( jTextFieldZEnd.getText() );
		// Image info
		settings.dx = NumberParser.parseDouble( jTextFieldPixelWidth.getText() );
		settings.dy = NumberParser.parseDouble( jTextFieldPixelHeight.getText() );
		settings.dz = NumberParser.parseDouble( jTextFieldVoxelDepth.getText() );
		settings.dt = NumberParser.parseDouble( jTextFieldTimeInterval.getText() );
		settings.width = imp.getWidth();
		settings.height = imp.getHeight();
		settings.nslices = imp.getNSlices();
		settings.nframes = imp.getNFrames();
		// Units
		model.setPhysicalUnits( jLabelUnits1.getText(), jLabelUnits4.getText() );
		// Roi
		final Roi roi = imp.getRoi();
		if ( null != roi )
		{
			settings.roi = roi;
			settings.polygon = roi.getPolygon();
		}
		// File info
		if ( null != imp.getOriginalFileInfo() )
		{
			settings.imageFileName = imp.getOriginalFileInfo().fileName;
			settings.imageFolder = imp.getOriginalFileInfo().directory;
		}
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Fill the text fields with the parameters grabbed in the {@link Settings}
	 * argument.
	 */
	public void echoSettings( final Model model, final Settings settings )
	{
		jLabelImageName.setText( settings.imp.getTitle() );
		jTextFieldPixelWidth.setText( "" + settings.dx );
		jTextFieldPixelHeight.setText( "" + settings.dy );
		jTextFieldVoxelDepth.setText( "" + settings.dz );
		jTextFieldTimeInterval.setText( "" + settings.dt );
		jLabelUnits1.setText( model.getSpaceUnits() );
		jLabelUnits2.setText( model.getSpaceUnits() );
		jLabelUnits3.setText( model.getSpaceUnits() );
		jLabelUnits4.setText( model.getTimeUnits() );
		jTextFieldXStart.setText( "" + settings.xstart );
		jTextFieldYStart.setText( "" + settings.ystart );
		jTextFieldXEnd.setText( "" + settings.xend );
		jTextFieldYEnd.setText( "" + settings.yend );
		jTextFieldZStart.setText( "" + settings.zstart );
		jTextFieldZEnd.setText( "" + settings.zend );
		jTextFieldTStart.setText( "" + settings.tstart );
		jTextFieldTEnd.setText( "" + settings.tend );
	}

	/**
	 * Fill the text fields with parameters grabbed from specified ImagePlus.
	 */
	public void getFrom( final ImagePlus lImp )
	{
		this.imp = lImp;
		if ( null == lImp )
		{
			jLabelImageName.setText( "No image selected." );
			impValid = false;
			return;
		}

		if ( lImp.getType() == ImagePlus.COLOR_RGB )
		{
			// We do not know how to process RGB images
			jLabelImageName.setText( lImp.getShortTitle() + " is RGB: invalid." );
			impValid = false;
			return;
		}

		jLabelImageName.setText( "Target: " + lImp.getShortTitle() );
		jTextFieldPixelWidth.setValue( lImp.getCalibration().pixelWidth );
		jTextFieldPixelHeight.setValue( lImp.getCalibration().pixelHeight );
		jTextFieldVoxelDepth.setValue( lImp.getCalibration().pixelDepth );
		if ( lImp.getCalibration().frameInterval == 0 )
		{
			jTextFieldTimeInterval.setValue( 1 );
			jLabelUnits4.setText( "frame" );
		}
		else
		{
			jTextFieldTimeInterval.setValue( lImp.getCalibration().frameInterval );
			jLabelUnits4.setText( lImp.getCalibration().getTimeUnit() );
		}
		jLabelUnits1.setText( lImp.getCalibration().getXUnit() );
		jLabelUnits2.setText( lImp.getCalibration().getYUnit() );
		jLabelUnits3.setText( lImp.getCalibration().getZUnit() );
		Roi roi = lImp.getRoi();
		if ( null == roi )
			roi = new Roi( 0, 0, lImp.getWidth(), lImp.getHeight() );
		final Rectangle boundingRect = roi.getBounds();
		jTextFieldXStart.setText( "" + ( boundingRect.x ) );
		jTextFieldYStart.setText( "" + ( boundingRect.y ) );
		jTextFieldXEnd.setText( "" + ( boundingRect.width + boundingRect.x - 1 ) );
		jTextFieldYEnd.setText( "" + ( boundingRect.height + boundingRect.y - 1 ) );
		jTextFieldZStart.setText( "" + 0 );
		jTextFieldZEnd.setText( "" + ( lImp.getNSlices() - 1 ) );
		jTextFieldTStart.setText( "" + 0 );
		jTextFieldTEnd.setText( "" + ( lImp.getNFrames() - 1 ) );

		impValid = true;
	}
}
