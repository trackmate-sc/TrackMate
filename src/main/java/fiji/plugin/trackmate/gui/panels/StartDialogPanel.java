package fiji.plugin.trackmate.gui.panels;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TEXTFIELD_DIMENSION;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
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
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.GuiUtils;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;

public class StartDialogPanel extends ActionListenablePanel
{

	private static final long serialVersionUID = -1L;

	private static final NumberFormat DOUBLE_FORMAT = new DecimalFormat( "#.###" );

	private static final String TOOLTIP = "<html>" +
			"Pressing this button will make the current <br>" +
			"ImagePlus the source for TrackMate. If the <br>" +
			"image has a ROI, it will be used to set the <br>" +
			"crop rectangle as well.</html>";

	/** ActionEvent fired when the user press the refresh button. */
	private final ActionEvent IMAGEPLUS_REFRESHED = new ActionEvent( this, 0, "ImagePlus refreshed" );

	private final JLabel lblImageName;

	private final JFormattedTextField tfXStart;

	private final JFormattedTextField tfXEnd;

	private final JFormattedTextField tfYStart;

	private final JFormattedTextField tfYEnd;

	private final JFormattedTextField tfZStart;

	private final JFormattedTextField tfZEnd;

	private final JFormattedTextField tfTEnd;

	private final JFormattedTextField tfTStart;

	private final JLabel lblTimeUnits;

	private final JLabel lblSpatialUnits3;

	private final JLabel lblSpatialUnits2;

	private final JLabel lblSpatialUnits1;

	private final JFormattedTextField tfPixelWidth;

	private final JFormattedTextField tfVoxelDepth;

	private final JFormattedTextField tfPixelHeight;

	private final JFormattedTextField tfTimeInterval;

	private ImagePlus imp;

	private boolean impValid = false;

	public StartDialogPanel()
	{
		this.setPreferredSize( new Dimension( 291, 491 ) );
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

		final GridBagConstraints gbcLblCitation = new GridBagConstraints();
		gbcLblCitation.fill = GridBagConstraints.BOTH;
		gbcLblCitation.insets = new Insets( 5, 5, 5, 5 );
		gbcLblCitation.gridwidth = 4;
		gbcLblCitation.gridx = 0;
		gbcLblCitation.gridy = 0;
		add( lblCitation, gbcLblCitation );

		final JLabel lblLinkPubMed = new JLabel( "<html>"
				+ "<a href=https://www.ncbi.nlm.nih.gov/pubmed/27713081>on PubMed (PMID 27713081)</a></html>" );
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
		final GridBagConstraints gbcLblLinkPubMed = new GridBagConstraints();
		gbcLblLinkPubMed.anchor = GridBagConstraints.NORTH;
		gbcLblLinkPubMed.fill = GridBagConstraints.HORIZONTAL;
		gbcLblLinkPubMed.gridwidth = 4;
		gbcLblLinkPubMed.insets = new Insets( 0, 10, 5, 5 );
		gbcLblLinkPubMed.gridx = 0;
		gbcLblLinkPubMed.gridy = 1;
		add( lblLinkPubMed, gbcLblLinkPubMed );

		lblImageName = new JLabel( "Select an image, and press refresh." );
		lblImageName.setFont( BIG_FONT );
		final GridBagConstraints gbcLabelImageName = new GridBagConstraints();
		gbcLabelImageName.anchor = GridBagConstraints.SOUTH;
		gbcLabelImageName.fill = GridBagConstraints.HORIZONTAL;
		gbcLabelImageName.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelImageName.gridwidth = 4;
		gbcLabelImageName.gridx = 0;
		gbcLabelImageName.gridy = 3;
		add( lblImageName, gbcLabelImageName );

		final JLabel jLabelCheckCalibration = new JLabel( "Calibration settings:" );
		jLabelCheckCalibration.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelCheckCalibration = new GridBagConstraints();
		gbcLabelCheckCalibration.anchor = GridBagConstraints.SOUTH;
		gbcLabelCheckCalibration.fill = GridBagConstraints.HORIZONTAL;
		gbcLabelCheckCalibration.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelCheckCalibration.gridwidth = 4;
		gbcLabelCheckCalibration.gridx = 0;
		gbcLabelCheckCalibration.gridy = 4;
		add( jLabelCheckCalibration, gbcLabelCheckCalibration );

		final JLabel jLabelPixelWidth = new JLabel( "Pixel width:" );
		jLabelPixelWidth.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelPixelWidth = new GridBagConstraints();
		gbcLabelPixelWidth.anchor = GridBagConstraints.EAST;
		gbcLabelPixelWidth.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelPixelWidth.gridwidth = 2;
		gbcLabelPixelWidth.gridx = 0;
		gbcLabelPixelWidth.gridy = 5;
		add( jLabelPixelWidth, gbcLabelPixelWidth );

		tfPixelWidth = new JFormattedTextField( DOUBLE_FORMAT );
		GuiUtils.selectAllOnFocus( tfPixelWidth );
		tfPixelWidth.setHorizontalAlignment( SwingConstants.CENTER );
		tfPixelWidth.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldPixelWidth = new GridBagConstraints();
		gbcTextFieldPixelWidth.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldPixelWidth.anchor = GridBagConstraints.NORTH;
		gbcTextFieldPixelWidth.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldPixelWidth.gridx = 2;
		gbcTextFieldPixelWidth.gridy = 5;
		add( tfPixelWidth, gbcTextFieldPixelWidth );

		lblSpatialUnits1 = new JLabel();
		lblSpatialUnits1.setText( "units" );
		lblSpatialUnits1.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelSpatialUnits = new GridBagConstraints();
		gbcLabelSpatialUnits.anchor = GridBagConstraints.WEST;
		gbcLabelSpatialUnits.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelSpatialUnits.gridx = 3;
		gbcLabelSpatialUnits.gridy = 5;
		add( lblSpatialUnits1, gbcLabelSpatialUnits );

		final JLabel jLabelPixelHeight = new JLabel( "Pixel height:" );
		jLabelPixelHeight.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelPixelHeight = new GridBagConstraints();
		gbcLabelPixelHeight.anchor = GridBagConstraints.EAST;
		gbcLabelPixelHeight.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelPixelHeight.gridwidth = 2;
		gbcLabelPixelHeight.gridx = 0;
		gbcLabelPixelHeight.gridy = 6;
		add( jLabelPixelHeight, gbcLabelPixelHeight );

		tfPixelHeight = new JFormattedTextField( DOUBLE_FORMAT );
		GuiUtils.selectAllOnFocus( tfPixelHeight );
		tfPixelHeight.setHorizontalAlignment( SwingConstants.CENTER );
		tfPixelHeight.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldPixelHeight = new GridBagConstraints();
		gbcTextFieldPixelHeight.anchor = GridBagConstraints.NORTH;
		gbcTextFieldPixelHeight.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldPixelHeight.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldPixelHeight.gridx = 2;
		gbcTextFieldPixelHeight.gridy = 6;
		add( tfPixelHeight, gbcTextFieldPixelHeight );

		final JLabel jLabelTimeInterval = new JLabel( "Time interval:" );
		jLabelTimeInterval.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelTimeInterval = new GridBagConstraints();
		gbcLabelTimeInterval.anchor = GridBagConstraints.EAST;
		gbcLabelTimeInterval.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelTimeInterval.gridwidth = 2;
		gbcLabelTimeInterval.gridx = 0;
		gbcLabelTimeInterval.gridy = 8;
		add( jLabelTimeInterval, gbcLabelTimeInterval );

		lblSpatialUnits2 = new JLabel();
		lblSpatialUnits2.setText( "units" );
		lblSpatialUnits2.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelTimeUnits = new GridBagConstraints();
		gbcLabelTimeUnits.anchor = GridBagConstraints.WEST;
		gbcLabelTimeUnits.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelTimeUnits.gridx = 3;
		gbcLabelTimeUnits.gridy = 6;
		add( lblSpatialUnits2, gbcLabelTimeUnits );

		tfVoxelDepth = new JFormattedTextField( DOUBLE_FORMAT );
		GuiUtils.selectAllOnFocus( tfVoxelDepth );
		tfVoxelDepth.setHorizontalAlignment( SwingConstants.CENTER );
		tfVoxelDepth.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldVoxelDepth = new GridBagConstraints();
		gbcTextFieldVoxelDepth.anchor = GridBagConstraints.NORTH;
		gbcTextFieldVoxelDepth.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldVoxelDepth.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldVoxelDepth.gridx = 2;
		gbcTextFieldVoxelDepth.gridy = 7;
		add( tfVoxelDepth, gbcTextFieldVoxelDepth );

		final JLabel jLabelVoxelDepth = new JLabel( "Voxel depth:" );
		jLabelVoxelDepth.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelVoxelDepth = new GridBagConstraints();
		gbcLabelVoxelDepth.anchor = GridBagConstraints.EAST;
		gbcLabelVoxelDepth.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelVoxelDepth.gridwidth = 2;
		gbcLabelVoxelDepth.gridx = 0;
		gbcLabelVoxelDepth.gridy = 7;
		add( jLabelVoxelDepth, gbcLabelVoxelDepth );

		lblSpatialUnits3 = new JLabel();
		lblSpatialUnits3.setText( "units" );
		lblSpatialUnits3.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelUnits3 = new GridBagConstraints();
		gbcLabelUnits3.anchor = GridBagConstraints.WEST;
		gbcLabelUnits3.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelUnits3.gridx = 3;
		gbcLabelUnits3.gridy = 7;
		add( lblSpatialUnits3, gbcLabelUnits3 );

		lblTimeUnits = new JLabel();
		lblTimeUnits.setText( "units" );
		lblTimeUnits.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelUnits4 = new GridBagConstraints();
		gbcLabelUnits4.anchor = GridBagConstraints.WEST;
		gbcLabelUnits4.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelUnits4.gridx = 3;
		gbcLabelUnits4.gridy = 8;
		add( lblTimeUnits, gbcLabelUnits4 );

		tfTimeInterval = new JFormattedTextField( DOUBLE_FORMAT );
		GuiUtils.selectAllOnFocus( tfTimeInterval );
		tfTimeInterval.setHorizontalAlignment( SwingConstants.CENTER );
		tfTimeInterval.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldTimeInterval = new GridBagConstraints();
		gbcTextFieldTimeInterval.anchor = GridBagConstraints.NORTH;
		gbcTextFieldTimeInterval.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldTimeInterval.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldTimeInterval.gridx = 2;
		gbcTextFieldTimeInterval.gridy = 8;
		add( tfTimeInterval, gbcTextFieldTimeInterval );

		final JLabel jLabelCropSetting = new JLabel( "Crop settings (in pixels, 0-based):" );
		jLabelCropSetting.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelCropSetting = new GridBagConstraints();
		gbcLabelCropSetting.anchor = GridBagConstraints.SOUTH;
		gbcLabelCropSetting.fill = GridBagConstraints.HORIZONTAL;
		gbcLabelCropSetting.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelCropSetting.gridwidth = 4;
		gbcLabelCropSetting.gridx = 0;
		gbcLabelCropSetting.gridy = 9;
		add( jLabelCropSetting, gbcLabelCropSetting );

		tfXStart = new JFormattedTextField( Integer.valueOf( 0 ) );
		GuiUtils.selectAllOnFocus( tfXStart );
		tfXStart.setHorizontalAlignment( SwingConstants.CENTER );
		tfXStart.setPreferredSize( TEXTFIELD_DIMENSION );
		tfXStart.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldXStart = new GridBagConstraints();
		gbcTextFieldXStart.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldXStart.anchor = GridBagConstraints.NORTH;
		gbcTextFieldXStart.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldXStart.gridx = 1;
		gbcTextFieldXStart.gridy = 10;
		add( tfXStart, gbcTextFieldXStart );

		tfXEnd = new JFormattedTextField( Integer.valueOf( 0 ) );
		GuiUtils.selectAllOnFocus( tfXEnd );
		tfXEnd.setHorizontalAlignment( SwingConstants.CENTER );
		tfXEnd.setPreferredSize( TEXTFIELD_DIMENSION );
		tfXEnd.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldXEnd = new GridBagConstraints();
		gbcTextFieldXEnd.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldXEnd.anchor = GridBagConstraints.NORTH;
		gbcTextFieldXEnd.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldXEnd.gridx = 3;
		gbcTextFieldXEnd.gridy = 10;
		add( tfXEnd, gbcTextFieldXEnd );

		final JLabel jLabelX = new JLabel( "X" );
		jLabelX.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelX = new GridBagConstraints();
		gbcLabelX.anchor = GridBagConstraints.EAST;
		gbcLabelX.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelX.gridx = 0;
		gbcLabelX.gridy = 10;
		add( jLabelX, gbcLabelX );

		final JLabel jLabelTo1 = new JLabel( "to" );
		jLabelTo1.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelTo1 = new GridBagConstraints();
		gbcLabelTo1.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelTo1.gridx = 2;
		gbcLabelTo1.gridy = 10;
		add( jLabelTo1, gbcLabelTo1 );

		final JLabel jLabelY = new JLabel( "Y" );
		jLabelY.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelY = new GridBagConstraints();
		gbcLabelY.anchor = GridBagConstraints.EAST;
		gbcLabelY.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelY.gridx = 0;
		gbcLabelY.gridy = 11;
		add( jLabelY, gbcLabelY );

		final JLabel jLabelTo3 = new JLabel( "to" );
		jLabelTo3.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelTo3 = new GridBagConstraints();
		gbcLabelTo3.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelTo3.gridx = 2;
		gbcLabelTo3.gridy = 12;
		add( jLabelTo3, gbcLabelTo3 );

		tfYStart = new JFormattedTextField( Integer.valueOf( 0 ) );
		GuiUtils.selectAllOnFocus( tfYStart );
		tfYStart.setHorizontalAlignment( SwingConstants.CENTER );
		tfYStart.setPreferredSize( TEXTFIELD_DIMENSION );
		tfYStart.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldYStart = new GridBagConstraints();
		gbcTextFieldYStart.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldYStart.anchor = GridBagConstraints.NORTH;
		gbcTextFieldYStart.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldYStart.gridx = 1;
		gbcTextFieldYStart.gridy = 11;
		add( tfYStart, gbcTextFieldYStart );

		final JLabel jLabelTo2 = new JLabel( "to" );
		jLabelTo2.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelTo2 = new GridBagConstraints();
		gbcLabelTo2.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelTo2.gridx = 2;
		gbcLabelTo2.gridy = 11;
		add( jLabelTo2, gbcLabelTo2 );

		final JLabel jLabelZ = new JLabel( "Z" );
		jLabelZ.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelZ = new GridBagConstraints();
		gbcLabelZ.anchor = GridBagConstraints.EAST;
		gbcLabelZ.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelZ.gridx = 0;
		gbcLabelZ.gridy = 12;
		add( jLabelZ, gbcLabelZ );

		tfYEnd = new JFormattedTextField( Integer.valueOf( 0 ) );
		GuiUtils.selectAllOnFocus( tfYEnd );
		tfYEnd.setHorizontalAlignment( SwingConstants.CENTER );
		tfYEnd.setPreferredSize( TEXTFIELD_DIMENSION );
		tfYEnd.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldYEnd = new GridBagConstraints();
		gbcTextFieldYEnd.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldYEnd.anchor = GridBagConstraints.NORTH;
		gbcTextFieldYEnd.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldYEnd.gridx = 3;
		gbcTextFieldYEnd.gridy = 11;
		add( tfYEnd, gbcTextFieldYEnd );

		tfZStart = new JFormattedTextField( Integer.valueOf( 0 ) );
		GuiUtils.selectAllOnFocus( tfZStart );
		tfZStart.setHorizontalAlignment( SwingConstants.CENTER );
		tfZStart.setPreferredSize( TEXTFIELD_DIMENSION );
		tfZStart.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldZStart = new GridBagConstraints();
		gbcTextFieldZStart.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldZStart.anchor = GridBagConstraints.NORTH;
		gbcTextFieldZStart.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldZStart.gridx = 1;
		gbcTextFieldZStart.gridy = 12;
		add( tfZStart, gbcTextFieldZStart );

		tfZEnd = new JFormattedTextField( Integer.valueOf( 0 ) );
		GuiUtils.selectAllOnFocus( tfZEnd );
		tfZEnd.setHorizontalAlignment( SwingConstants.CENTER );
		tfZEnd.setPreferredSize( TEXTFIELD_DIMENSION );
		tfZEnd.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldZEnd = new GridBagConstraints();
		gbcTextFieldZEnd.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldZEnd.anchor = GridBagConstraints.NORTH;
		gbcTextFieldZEnd.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldZEnd.gridx = 3;
		gbcTextFieldZEnd.gridy = 12;
		add( tfZEnd, gbcTextFieldZEnd );

		tfTStart = new JFormattedTextField( Integer.valueOf( 0 ) );
		GuiUtils.selectAllOnFocus( tfTStart );
		tfTStart.setHorizontalAlignment( SwingConstants.CENTER );
		tfTStart.setPreferredSize( TEXTFIELD_DIMENSION );
		tfTStart.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldTStart = new GridBagConstraints();
		gbcTextFieldTStart.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldTStart.anchor = GridBagConstraints.NORTH;
		gbcTextFieldTStart.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldTStart.gridx = 1;
		gbcTextFieldTStart.gridy = 13;
		add( tfTStart, gbcTextFieldTStart );

		final JLabel jLabelT = new JLabel( "T" );
		jLabelT.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelT = new GridBagConstraints();
		gbcLabelT.anchor = GridBagConstraints.EAST;
		gbcLabelT.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelT.gridx = 0;
		gbcLabelT.gridy = 13;
		add( jLabelT, gbcLabelT );

		tfTEnd = new JFormattedTextField( Integer.valueOf( 0 ) );
		GuiUtils.selectAllOnFocus( tfTEnd );
		tfTEnd.setHorizontalAlignment( SwingConstants.CENTER );
		tfTEnd.setPreferredSize( TEXTFIELD_DIMENSION );
		tfTEnd.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextFieldTEnd = new GridBagConstraints();
		gbcTextFieldTEnd.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldTEnd.anchor = GridBagConstraints.NORTH;
		gbcTextFieldTEnd.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldTEnd.gridx = 3;
		gbcTextFieldTEnd.gridy = 13;
		add( tfTEnd, gbcTextFieldTEnd );

		final JLabel jLabelTo4 = new JLabel( "to" );
		jLabelTo4.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelTo4 = new GridBagConstraints();
		gbcLabelTo4.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelTo4.gridx = 2;
		gbcLabelTo4.gridy = 13;
		add( jLabelTo4, gbcLabelTo4 );

		final JButton jButtonRefresh = new JButton( "Refresh source" );
		jButtonRefresh.setToolTipText( TOOLTIP );
		jButtonRefresh.setFont( SMALL_FONT );
		final GridBagConstraints gbcButtonRefresh = new GridBagConstraints();
		gbcButtonRefresh.anchor = GridBagConstraints.SOUTHWEST;
		gbcButtonRefresh.insets = new Insets( 5, 5, 5, 5 );
		gbcButtonRefresh.gridwidth = 4;
		gbcButtonRefresh.gridx = 0;
		gbcButtonRefresh.gridy = 14;
		add( jButtonRefresh, gbcButtonRefresh );
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
		settings.tstart = ( ( Number ) tfTStart.getValue() ).intValue();
		settings.tend = ( ( Number ) tfTEnd.getValue() ).intValue();
		settings.xstart = ( ( Number ) tfXStart.getValue() ).intValue();
		settings.xend = ( ( Number ) tfXEnd.getValue() ).intValue();
		settings.ystart = ( ( Number ) tfYStart.getValue() ).intValue();
		settings.yend = ( ( Number ) tfYEnd.getValue() ).intValue();
		settings.zstart = ( ( Number ) tfZStart.getValue() ).intValue();
		settings.zend = ( ( Number ) tfZEnd.getValue() ).intValue();
		// Image info
		settings.dx = ( ( Number ) tfPixelWidth.getValue() ).doubleValue();
		settings.dy = ( ( Number ) tfPixelHeight.getValue() ).doubleValue();
		settings.dz = ( ( Number ) tfVoxelDepth.getValue() ).doubleValue();
		settings.dt = ( ( Number ) tfTimeInterval.getValue() ).doubleValue();
		settings.width = imp.getWidth();
		settings.height = imp.getHeight();
		settings.nslices = imp.getNSlices();
		settings.nframes = imp.getNFrames();
		// Units
		model.setPhysicalUnits( lblSpatialUnits1.getText(), lblTimeUnits.getText() );
		// Roi
		settings.roi = imp.getRoi();

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
		lblImageName.setText( settings.imp.getTitle() );
		tfPixelWidth.setValue( Double.valueOf( settings.dx ) );
		tfPixelHeight.setValue( Double.valueOf( settings.dy ) );
		tfVoxelDepth.setValue( Double.valueOf( settings.dz ) );
		tfTimeInterval.setValue( Double.valueOf( settings.dt ) );
		lblSpatialUnits1.setText( model.getSpaceUnits() );
		lblSpatialUnits2.setText( model.getSpaceUnits() );
		lblSpatialUnits3.setText( model.getSpaceUnits() );
		lblTimeUnits.setText( model.getTimeUnits() );
		tfXStart.setValue( Integer.valueOf( settings.xstart ) );
		tfYStart.setValue( Integer.valueOf( settings.ystart ) );
		tfXEnd.setValue( Integer.valueOf( settings.xend ) );
		tfYEnd.setValue( Integer.valueOf( settings.yend ) );
		tfZStart.setValue( Integer.valueOf( settings.zstart ) );
		tfZEnd.setValue( Integer.valueOf( settings.zend ) );
		tfTStart.setValue( Integer.valueOf( settings.tstart ) );
		tfTEnd.setValue( Integer.valueOf( settings.tend ) );
	}

	/**
	 * Fill the text fields with parameters grabbed from specified ImagePlus.
	 */
	public void getFrom( final ImagePlus lImp )
	{
		this.imp = lImp;
		if ( null == lImp )
		{
			lblImageName.setText( "No image selected." );
			impValid = false;
			return;
		}

		if ( lImp.getType() == ImagePlus.COLOR_RGB )
		{
			// We do not know how to process RGB images
			lblImageName.setText( lImp.getShortTitle() + " is RGB: invalid." );
			impValid = false;
			return;
		}

		lblImageName.setText( "Target: " + lImp.getShortTitle() );
		tfPixelWidth.setValue( lImp.getCalibration().pixelWidth );
		tfPixelHeight.setValue( lImp.getCalibration().pixelHeight );
		tfVoxelDepth.setValue( lImp.getCalibration().pixelDepth );

		if ( lImp.getCalibration().frameInterval == 0 )
		{
			tfTimeInterval.setValue( 1 );
			lblTimeUnits.setText( "frame" );
		}
		else
		{
			tfTimeInterval.setValue( lImp.getCalibration().frameInterval );
			lblTimeUnits.setText( lImp.getCalibration().getTimeUnit() );
		}
		lblSpatialUnits1.setText( lImp.getCalibration().getXUnit() );
		lblSpatialUnits2.setText( lImp.getCalibration().getYUnit() );
		lblSpatialUnits3.setText( lImp.getCalibration().getZUnit() );

		Roi roi = lImp.getRoi();
		if ( null == roi )
			roi = new Roi( 0, 0, lImp.getWidth(), lImp.getHeight() );

		final Rectangle boundingRect = roi.getBounds();
		tfXStart.setValue( Integer.valueOf( boundingRect.x ) );
		tfYStart.setValue( Integer.valueOf( boundingRect.y ) );
		tfXEnd.setValue( Integer.valueOf( boundingRect.width + boundingRect.x - 1 ) );
		tfYEnd.setValue( Integer.valueOf( boundingRect.height + boundingRect.y - 1 ) );
		tfZStart.setValue( Integer.valueOf( 0 ) );
		tfZEnd.setValue( Integer.valueOf( lImp.getNSlices() - 1 ) );
		tfTStart.setValue( Integer.valueOf( 0 ) );
		tfTEnd.setValue( Integer.valueOf( lImp.getNFrames() - 1 ) );

		impValid = true;
	}
}
