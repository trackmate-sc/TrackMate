package fiji.plugin.trackmate.gui.components.detector;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

/**
 * Configuration panel for spot detectors based on LoG detector.
 * 
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; 2010 - 2014
 */
public class ManualDetectorConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final NumberFormat FORMAT = new DecimalFormat( "#.##" );

	protected final JFormattedTextField ftfDiameter;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Creates a new {@link ManualDetectorConfigurationPanel}.
	 * 
	 * @param settings
	 *
	 * @param infoText
	 *            the detector info text, will be displayed on the panel.
	 * @param detectorName
	 *            the detector name, will be displayed on the panel.
	 */
	public ManualDetectorConfigurationPanel( final String infoText, final String detectorName )
	{
		this.setPreferredSize( new java.awt.Dimension( 300, 461 ) );
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 1.0 };
		setLayout( gridBagLayout );

		final JLabel jLabelPreTitle = new JLabel();
		final GridBagConstraints gbcLabelPreTitle = new GridBagConstraints();
		gbcLabelPreTitle.anchor = GridBagConstraints.NORTH;
		gbcLabelPreTitle.fill = GridBagConstraints.HORIZONTAL;
		gbcLabelPreTitle.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelPreTitle.gridwidth = 3;
		gbcLabelPreTitle.gridx = 0;
		gbcLabelPreTitle.gridy = 0;
		this.add( jLabelPreTitle, gbcLabelPreTitle );
		jLabelPreTitle.setText( "Settings for detector:" );
		jLabelPreTitle.setFont( FONT );

		final JLabel jLabelSegmenterName = new JLabel();
		final GridBagConstraints gbcLabelSegmenterName = new GridBagConstraints();
		gbcLabelSegmenterName.anchor = GridBagConstraints.NORTH;
		gbcLabelSegmenterName.fill = GridBagConstraints.HORIZONTAL;
		gbcLabelSegmenterName.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelSegmenterName.gridwidth = 3;
		gbcLabelSegmenterName.gridx = 0;
		gbcLabelSegmenterName.gridy = 1;
		this.add( jLabelSegmenterName, gbcLabelSegmenterName );
		jLabelSegmenterName.setFont( BIG_FONT );
		jLabelSegmenterName.setText( detectorName );

		final JLabel jLabelHelpText = new JLabel();
		final GridBagConstraints gbcLabelHelpText = new GridBagConstraints();
		gbcLabelHelpText.fill = GridBagConstraints.BOTH;
		gbcLabelHelpText.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelHelpText.gridwidth = 3;
		gbcLabelHelpText.gridx = 0;
		gbcLabelHelpText.gridy = 2;
		this.add( jLabelHelpText, gbcLabelHelpText );
		jLabelHelpText.setFont( FONT.deriveFont( Font.ITALIC ) );
		jLabelHelpText.setText( infoText.replace( "<br>", "" ).replace( "<p>", "<p align=\"justify\">" ).replace( "<html>", "<html><p align=\"justify\">" ) );

		final JLabel jLabelEstimDiameter = new JLabel();
		final GridBagConstraints gbc_jLabel2 = new GridBagConstraints();
		gbc_jLabel2.anchor = GridBagConstraints.EAST;
		gbc_jLabel2.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabel2.gridx = 0;
		gbc_jLabel2.gridy = 3;
		this.add( jLabelEstimDiameter, gbc_jLabel2 );
		jLabelEstimDiameter.setText( "Spot diameter to use:" );
		jLabelEstimDiameter.setFont( SMALL_FONT );

		ftfDiameter = new JFormattedTextField( FORMAT );
		ftfDiameter.setValue( Double.valueOf( 10. ) );
		ftfDiameter.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcTextFieldBlobDiameter = new GridBagConstraints();
		gbcTextFieldBlobDiameter.anchor = GridBagConstraints.SOUTH;
		gbcTextFieldBlobDiameter.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldBlobDiameter.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldBlobDiameter.gridx = 1;
		gbcTextFieldBlobDiameter.gridy = 3;
		this.add( ftfDiameter, gbcTextFieldBlobDiameter );
		ftfDiameter.setFont( SMALL_FONT );

		final JLabel jLabelBlobDiameterUnit = new JLabel();
		final GridBagConstraints gbcLabelBlobDiameterUnit = new GridBagConstraints();
		gbcLabelBlobDiameterUnit.anchor = GridBagConstraints.WEST;
		gbcLabelBlobDiameterUnit.fill = GridBagConstraints.VERTICAL;
		gbcLabelBlobDiameterUnit.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelBlobDiameterUnit.gridx = 2;
		gbcLabelBlobDiameterUnit.gridy = 3;
		this.add( jLabelBlobDiameterUnit, gbcLabelBlobDiameterUnit );
		jLabelBlobDiameterUnit.setFont( SMALL_FONT );
		jLabelBlobDiameterUnit.setText( "pixels" );

		final JLabel lblSpacer = new JLabel( "   " );
		final GridBagConstraints gbc_lblSpacer = new GridBagConstraints();
		gbc_lblSpacer.gridwidth = 3;
		gbc_lblSpacer.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblSpacer.gridx = 0;
		gbc_lblSpacer.gridy = 4;
		add( lblSpacer, gbc_lblSpacer );
	}

	/*
	 * METHODS
	 */

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > settings = new HashMap<>( 1 );
		settings.put( KEY_RADIUS, ( ( Number ) ftfDiameter.getValue() ).doubleValue() );
		return settings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		ftfDiameter.setValue( Double.valueOf( ( double ) Optional.ofNullable( settings.get( KEY_RADIUS ) ).orElse( Double.valueOf( 5. ) ) * 2. ) );
	}

	@Override
	public void clean()
	{}
}
