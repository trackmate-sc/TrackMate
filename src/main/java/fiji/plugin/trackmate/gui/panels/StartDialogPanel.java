package fiji.plugin.trackmate.gui.panels;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TEXTFIELD_DIMENSION;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;
import fiji.util.NumberParser;

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

	private JLabel jLabelCheckCalibration;

	private JNumericTextField jTextFieldPixelWidth;

	private JNumericTextField jTextFieldZStart;

	private JNumericTextField jTextFieldYStart;

	private JNumericTextField jTextFieldXStart;

	private JLabel jLabelCropSetting;

	private JButton jButtonRefresh;

	private JNumericTextField jTextFieldTEnd;

	private JLabel jLabelTo4;

	private JNumericTextField jTextFieldTStart;

	private JLabel jLabelT;

	private JNumericTextField jTextFieldZEnd;

	private JNumericTextField jTextFieldYEnd;

	private JNumericTextField jTextFieldXEnd;

	private JLabel jLabelTo3;

	private JLabel jLabelTo2;

	private JLabel jLabelTo1;

	private JLabel jLabelZ;

	private JLabel jLabelY;

	private JLabel jLabelX;

	private JLabel jLabelUnits3;

	private JLabel jLabelUnits2;

	private JLabel jLabelUnits1;

	private JNumericTextField jTextFieldVoxelDepth;

	private JNumericTextField jTextFieldPixelHeight;

	private JLabel jLabelVoxelDepth;

	private JLabel jLabelPixelHeight;

	private JLabel jLabelPixelWidth;

	private JLabel jLabelImageName;

	private JNumericTextField jTextFieldTimeInterval;

	private JLabel jLabelTimeInterval;

	private JLabel jLabelUnits4;

	private ImagePlus imp;

	private boolean impValid = false;

	public StartDialogPanel()
	{
		initGUI();
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
	public void getFrom( final ImagePlus imp )
	{

		this.imp = imp;

		if ( null == imp )
		{
			jLabelImageName.setText( "No image selected." );
			impValid = false;
			return;
		}

		if ( imp.getType() == ImagePlus.COLOR_RGB )
		{
			// We do not know how to process RGB images
			jLabelImageName.setText( imp.getShortTitle() + " is RGB: invalid." );
			impValid = false;
			return;
		}

		jLabelImageName.setText( "Target: " + imp.getShortTitle() );
		jTextFieldPixelWidth.setValue( imp.getCalibration().pixelWidth );
		jTextFieldPixelHeight.setValue( imp.getCalibration().pixelHeight );
		jTextFieldVoxelDepth.setValue( imp.getCalibration().pixelDepth );
		//		jTextFieldPixelWidth.setText(String.format("%g", imp.getCalibration().pixelWidth));
		//		jTextFieldPixelHeight.setText(String.format("%g", imp.getCalibration().pixelHeight));
		//		jTextFieldVoxelDepth.setText(String.format("%g", imp.getCalibration().pixelDepth));
		if ( imp.getCalibration().frameInterval == 0 )
		{
			jTextFieldTimeInterval.setValue( 1 );
			jLabelUnits4.setText( "frame" );
		}
		else
		{
			jTextFieldTimeInterval.setValue( imp.getCalibration().frameInterval );
			jLabelUnits4.setText( imp.getCalibration().getTimeUnit() );
		}
		jLabelUnits1.setText( imp.getCalibration().getXUnit() );
		jLabelUnits2.setText( imp.getCalibration().getYUnit() );
		jLabelUnits3.setText( imp.getCalibration().getZUnit() );
		Roi roi = imp.getRoi();
		if ( null == roi )
			roi = new Roi( 0, 0, imp.getWidth(), imp.getHeight() );
		final Rectangle boundingRect = roi.getBounds();
		jTextFieldXStart.setText( "" + ( boundingRect.x ) );
		jTextFieldYStart.setText( "" + ( boundingRect.y ) );
		jTextFieldXEnd.setText( "" + ( boundingRect.width + boundingRect.x - 1 ) );
		jTextFieldYEnd.setText( "" + ( boundingRect.height + boundingRect.y - 1 ) );
		jTextFieldZStart.setText( "" + 0 );
		jTextFieldZEnd.setText( "" + ( imp.getNSlices() - 1 ) );
		jTextFieldTStart.setText( "" + 0 );
		jTextFieldTEnd.setText( "" + ( imp.getNFrames() - 1 ) );

		impValid = true;
	}

	private void initGUI()
	{
		try
		{
			this.setPreferredSize( new java.awt.Dimension( 266, 476 ) );
			{
				jLabelImageName = new JLabel();
				jLabelImageName.setText( "Select an image, and press refresh." );
				jLabelImageName.setFont( BIG_FONT );
			}
			{
				jLabelCheckCalibration = new JLabel();
				jLabelCheckCalibration.setText( "Calibration settings:" );
				jLabelCheckCalibration.setFont( SMALL_FONT );
			}
			{
				jLabelPixelWidth = new JLabel();
				jLabelPixelWidth.setText( "Pixel width:" );
				jLabelPixelWidth.setFont( SMALL_FONT );
			}
			{
				jLabelPixelHeight = new JLabel();
				jLabelPixelHeight.setText( "Pixel height:" );
				jLabelPixelHeight.setFont( SMALL_FONT );
			}
			{
				jLabelVoxelDepth = new JLabel();
				jLabelVoxelDepth.setText( "Voxel depth:" );
				jLabelVoxelDepth.setFont( SMALL_FONT );
			}
			{
				jLabelTimeInterval = new JLabel();
				jLabelTimeInterval.setText( "Time interval:" );
				jLabelTimeInterval.setFont( SMALL_FONT );
			}
			{
				jTextFieldPixelWidth = new JNumericTextField();
				jTextFieldPixelWidth.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldPixelWidth.setFont( SMALL_FONT );
			}
			{
				jTextFieldPixelHeight = new JNumericTextField();
				jTextFieldPixelHeight.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldPixelHeight.setFont( SMALL_FONT );
			}
			{
				jTextFieldVoxelDepth = new JNumericTextField();
				jTextFieldVoxelDepth.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldVoxelDepth.setFont( SMALL_FONT );
			}
			{
				jTextFieldTimeInterval = new JNumericTextField();
				jTextFieldTimeInterval.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldTimeInterval.setFont( SMALL_FONT );
			}
			{
				jLabelUnits1 = new JLabel();
				jLabelUnits1.setText( "units" );
				jLabelUnits1.setFont( SMALL_FONT );
			}
			{
				jLabelUnits2 = new JLabel();
				jLabelUnits2.setText( "units" );
				jLabelUnits2.setFont( SMALL_FONT );
			}
			{
				jLabelUnits3 = new JLabel();
				jLabelUnits3.setText( "units" );
				jLabelUnits3.setFont( SMALL_FONT );
			}
			{
				jLabelUnits4 = new JLabel();
				jLabelUnits4.setText( "units" );
				jLabelUnits4.setFont( SMALL_FONT );
			}
			{
				jLabelCropSetting = new JLabel();
				jLabelCropSetting.setText( "Crop settings (in pixels, 0-based):" );
				jLabelCropSetting.setFont( SMALL_FONT );
			}
			{
				jLabelX = new JLabel();
				jLabelX.setText( "X" );
				jLabelX.setFont( SMALL_FONT );
			}
			{
				jLabelY = new JLabel();
				jLabelY.setText( "Y" );
				jLabelY.setFont( SMALL_FONT );
			}
			{
				jLabelZ = new JLabel();
				jLabelZ.setText( "Z" );
				jLabelZ.setFont( SMALL_FONT );
			}
			{
				jTextFieldXStart = new JNumericTextField();
				jTextFieldXStart.setFormat( "%.0f" );
				jTextFieldXStart.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldXStart.setPreferredSize( TEXTFIELD_DIMENSION );
				jTextFieldXStart.setFont( SMALL_FONT );
			}
			{
				jTextFieldYStart = new JNumericTextField();
				jTextFieldYStart.setFormat( "%.0f" );
				jTextFieldYStart.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldYStart.setPreferredSize( TEXTFIELD_DIMENSION );
				jTextFieldYStart.setFont( SMALL_FONT );
			}
			{
				jTextFieldZStart = new JNumericTextField();
				jTextFieldZStart.setFormat( "%.0f" );
				jTextFieldZStart.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldZStart.setPreferredSize( TEXTFIELD_DIMENSION );
				jTextFieldZStart.setFont( SMALL_FONT );
			}
			{
				jLabelTo1 = new JLabel();
				jLabelTo1.setText( "to" );
				jLabelTo1.setFont( SMALL_FONT );
			}
			{
				jLabelTo2 = new JLabel();
				jLabelTo2.setText( "to" );
				jLabelTo2.setFont( SMALL_FONT );
			}
			{
				jLabelTo3 = new JLabel();
				jLabelTo3.setText( "to" );
				jLabelTo3.setFont( SMALL_FONT );
			}
			{
				jTextFieldXEnd = new JNumericTextField();
				jTextFieldXEnd.setFormat( "%.0f" );
				jTextFieldXEnd.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldXEnd.setPreferredSize( TEXTFIELD_DIMENSION );
				jTextFieldXEnd.setFont( SMALL_FONT );
			}
			{
				jTextFieldYEnd = new JNumericTextField();
				jTextFieldYEnd.setFormat( "%.0f" );
				jTextFieldYEnd.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldYEnd.setPreferredSize( TEXTFIELD_DIMENSION );
				jTextFieldYEnd.setFont( SMALL_FONT );
			}
			{
				jTextFieldZEnd = new JNumericTextField();
				jTextFieldZEnd.setFormat( "%.0f" );
				jTextFieldZEnd.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldZEnd.setPreferredSize( TEXTFIELD_DIMENSION );
				jTextFieldZEnd.setFont( SMALL_FONT );
			}
			{
				jLabelT = new JLabel();
				jLabelT.setText( "T" );
				jLabelT.setFont( SMALL_FONT );
			}
			{
				jTextFieldTStart = new JNumericTextField();
				jTextFieldTStart.setFormat( "%.0f" );
				jTextFieldTStart.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldTStart.setPreferredSize( TEXTFIELD_DIMENSION );
				jTextFieldTStart.setFont( SMALL_FONT );
			}
			{
				jLabelTo4 = new JLabel();
				jLabelTo4.setText( "to" );
				jLabelTo4.setFont( SMALL_FONT );
			}
			{
				jTextFieldTEnd = new JNumericTextField();
				jTextFieldTEnd.setFormat( "%.0f" );
				jTextFieldTEnd.setHorizontalAlignment( SwingConstants.CENTER );
				jTextFieldTEnd.setPreferredSize( TEXTFIELD_DIMENSION );
				jTextFieldTEnd.setFont( SMALL_FONT );
			}

			{
				jButtonRefresh = new JButton();
				jButtonRefresh.setText( "Refresh source" );
				jButtonRefresh.setToolTipText( TOOLTIP );
				jButtonRefresh.setFont( SMALL_FONT );
				final GroupLayout groupLayout = new GroupLayout( this );
				groupLayout.setHorizontalGroup(
						groupLayout.createParallelGroup( Alignment.LEADING )
								.addGroup( groupLayout.createSequentialGroup()
										.addGap( 10 )
										.addComponent( jLabelImageName, GroupLayout.PREFERRED_SIZE, 245, GroupLayout.PREFERRED_SIZE ) )
								.addGroup( groupLayout.createSequentialGroup()
										.addGap( 58 )
										.addComponent( jLabelPixelHeight, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE )
										.addGap( 10 )
										.addComponent( jTextFieldPixelHeight, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE )
										.addGap( 10 )
										.addComponent( jLabelUnits2, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE )
										.addGap( 11 ) )
								.addGroup( groupLayout.createSequentialGroup()
										.addGap( 58 )
										.addComponent( jLabelVoxelDepth, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE )
										.addGap( 10 )
										.addComponent( jTextFieldVoxelDepth, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE )
										.addGap( 10 )
										.addComponent( jLabelUnits3, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE )
										.addGap( 11 ) )
								.addGroup( Alignment.TRAILING, groupLayout.createSequentialGroup()
										.addGap( 58 )
										.addComponent( jLabelX )
										.addGap( 13 )
										.addComponent( jTextFieldXStart, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE )
										.addGap( 28 )
										.addComponent( jLabelTo1 )
										.addGap( 23 )
										.addComponent( jTextFieldXEnd, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE )
										.addGap( 48 ) )
								.addGroup( Alignment.TRAILING, groupLayout.createSequentialGroup()
										.addGap( 58 )
										.addComponent( jLabelY )
										.addGap( 13 )
										.addComponent( jTextFieldYStart, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE )
										.addGap( 28 )
										.addComponent( jLabelTo2 )
										.addGap( 23 )
										.addComponent( jTextFieldYEnd, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE )
										.addGap( 48 ) )
								.addGroup( Alignment.TRAILING, groupLayout.createSequentialGroup()
										.addGap( 58 )
										.addComponent( jLabelZ )
										.addGap( 14 )
										.addComponent( jTextFieldZStart, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE )
										.addGap( 28 )
										.addComponent( jLabelTo3 )
										.addGap( 23 )
										.addComponent( jTextFieldZEnd, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE )
										.addGap( 48 ) )
								.addGroup( groupLayout.createSequentialGroup()
										.addGap( 58 )
										.addComponent( jLabelT, GroupLayout.PREFERRED_SIZE, 7, GroupLayout.PREFERRED_SIZE )
										.addGap( 13 )
										.addComponent( jTextFieldTStart, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE )
										.addGap( 28 )
										.addComponent( jLabelTo4 )
										.addGap( 23 )
										.addComponent( jTextFieldTEnd, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE )
										.addGap( 48 ) )
								.addGroup( groupLayout.createSequentialGroup()
										.addGap( 10 )
										.addComponent( jButtonRefresh, GroupLayout.PREFERRED_SIZE, 108, Short.MAX_VALUE )
										.addGap( 148 ) )
								.addGroup( groupLayout.createSequentialGroup()
										.addGroup( groupLayout.createParallelGroup( Alignment.TRAILING )
												.addGroup( Alignment.LEADING, groupLayout.createSequentialGroup()
														.addGap( 10 )
														.addComponent( jLabelCropSetting, GroupLayout.PREFERRED_SIZE, 62, Short.MAX_VALUE ) )
												.addGroup( Alignment.LEADING, groupLayout.createSequentialGroup()
														.addGap( 52 )
														.addComponent( jLabelTimeInterval, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE )
														.addGap( 10 )
														.addComponent( jTextFieldTimeInterval, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE )
														.addGap( 10 )
														.addComponent( jLabelUnits4, GroupLayout.PREFERRED_SIZE, 78, GroupLayout.PREFERRED_SIZE ) ) )
										.addGap( 10 ) )
								.addGroup( groupLayout.createSequentialGroup()
										.addGroup( groupLayout.createParallelGroup( Alignment.TRAILING )
												.addGroup( Alignment.LEADING, groupLayout.createSequentialGroup()
														.addGap( 10 )
														.addComponent( jLabelCheckCalibration, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE ) )
												.addGroup( Alignment.LEADING, groupLayout.createSequentialGroup()
														.addGap( 63 )
														.addComponent( jLabelPixelWidth, GroupLayout.PREFERRED_SIZE, 55, GroupLayout.PREFERRED_SIZE )
														.addGap( 10 )
														.addComponent( jTextFieldPixelWidth, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE )
														.addGap( 10 )
														.addComponent( jLabelUnits1, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE ) ) )
										.addGap( 11 ) )
						);
				groupLayout.setVerticalGroup(
						groupLayout.createParallelGroup( Alignment.LEADING )
								.addGroup( groupLayout.createSequentialGroup()
										.addGap( 14 )
										.addComponent( jLabelImageName )
										.addGap( 76 )
										.addComponent( jLabelCheckCalibration )
										.addGap( 10 )
										.addGroup( groupLayout.createParallelGroup( Alignment.LEADING )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 1 )
														.addComponent( jLabelPixelWidth ) )
												.addComponent( jTextFieldPixelWidth, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 1 )
														.addComponent( jLabelUnits1 ) ) )
										.addGap( 5 )
										.addGroup( groupLayout.createParallelGroup( Alignment.LEADING )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 1 )
														.addComponent( jLabelPixelHeight ) )
												.addComponent( jTextFieldPixelHeight, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 1 )
														.addComponent( jLabelUnits2 ) ) )
										.addGap( 5 )
										.addGroup( groupLayout.createParallelGroup( Alignment.LEADING )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 1 )
														.addComponent( jLabelVoxelDepth ) )
												.addComponent( jTextFieldVoxelDepth, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 1 )
														.addComponent( jLabelUnits3 ) ) )
										.addGap( 5 )
										.addGroup( groupLayout.createParallelGroup( Alignment.LEADING )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 1 )
														.addComponent( jLabelTimeInterval ) )
												.addComponent( jTextFieldTimeInterval, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 1 )
														.addComponent( jLabelUnits4 ) ) )
										.addGap( 32 )
										.addComponent( jLabelCropSetting )
										.addGap( 10 )
										.addGroup( groupLayout.createParallelGroup( Alignment.LEADING )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 2 )
														.addComponent( jLabelX ) )
												.addComponent( jTextFieldXStart, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 2 )
														.addComponent( jLabelTo1 ) )
												.addComponent( jTextFieldXEnd, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE ) )
										.addGap( 5 )
										.addGroup( groupLayout.createParallelGroup( Alignment.LEADING )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 2 )
														.addComponent( jLabelY ) )
												.addComponent( jTextFieldYStart, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 2 )
														.addComponent( jLabelTo2 ) )
												.addComponent( jTextFieldYEnd, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE ) )
										.addGap( 5 )
										.addGroup( groupLayout.createParallelGroup( Alignment.LEADING )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 2 )
														.addComponent( jLabelZ ) )
												.addComponent( jTextFieldZStart, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 2 )
														.addComponent( jLabelTo3 ) )
												.addComponent( jTextFieldZEnd, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE ) )
										.addGap( 6 )
										.addGroup( groupLayout.createParallelGroup( Alignment.LEADING )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 2 )
														.addComponent( jLabelT ) )
												.addComponent( jTextFieldTStart, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
												.addGroup( groupLayout.createSequentialGroup()
														.addGap( 2 )
														.addComponent( jLabelTo4 ) )
												.addComponent( jTextFieldTEnd, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE ) )
										.addGap( 74 )
										.addComponent( jButtonRefresh ) )
						);
				setLayout( groupLayout );

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
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
