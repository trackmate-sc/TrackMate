package fiji.plugin.trackmate.gui.panels.detector;

import static fiji.plugin.trackmate.detection.BlockLogDetectorFactory.KEY_NSPLIT;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.BlockLogDetector;
import fiji.plugin.trackmate.detection.BlockLogDetectorFactory;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.util.NumberParser;
import ij.gui.Overlay;
import ij.gui.Roi;
import net.imglib2.Interval;

public class BlockLogDetectorConfigurationPanel extends LogDetectorConfigurationPanel
{
	private static final long serialVersionUID = 1L;

	private JLabel jLabelNSplit;

	private JNumericTextField jTextFieldNSplit;

	private Collection< Roi > roiList = Collections.emptyList();

	public BlockLogDetectorConfigurationPanel( final Settings settings, final Model model, final String infoText, final String detectorName )
	{
		super( settings, model, infoText, detectorName );
	}

	@SuppressWarnings( "rawtypes" )
	@Override
	protected BlockLogDetectorFactory< ? > getDetectorFactory()
	{
		return new BlockLogDetectorFactory();
	}

	@Override
	protected void initGUI()
	{
		super.initGUI();
		this.setPreferredSize( new java.awt.Dimension( 300, 461 ) );
		// Remove sub-pixel localization checkbox
		remove( jCheckSubPixel );
		remove( jCheckBoxMedianFilter );

		// Add down sampling text and textfield
		{
			jLabelNSplit = new JLabel();
			layout.putConstraint( SpringLayout.NORTH, jLabelNSplit, 290, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jLabelNSplit, 16, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.EAST, jLabelNSplit, 160, SpringLayout.WEST, this );

			jLabelNSplit.setText( "Split factor:" );
			jLabelNSplit.setFont( FONT );
			add( jLabelNSplit );
		}
		{
			jTextFieldNSplit = new JNumericTextField();
			jTextFieldNSplit.setHorizontalAlignment( SwingConstants.CENTER );
			jTextFieldNSplit.setText( "4" );

			layout.putConstraint( SpringLayout.NORTH, jTextFieldNSplit, 290, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jTextFieldNSplit, 168, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.EAST, jTextFieldNSplit, -100, SpringLayout.EAST, this );
			jTextFieldNSplit.setFont( FONT );
			add( jTextFieldNSplit );
		}
		{
			remove( jLabelThreshold );
			layout.putConstraint( SpringLayout.NORTH, jLabelThreshold, 270, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jLabelThreshold, 16, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.EAST, jLabelThreshold, 162, SpringLayout.WEST, this );
			add( jLabelThreshold );
		}
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > lSettings = super.getSettings();
		final int nsplit = NumberParser.parseInteger( jTextFieldNSplit.getText() );
		lSettings.put( KEY_NSPLIT, nsplit );
		return lSettings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		sliderChannel.setValue( ( Integer ) settings.get( KEY_TARGET_CHANNEL ) );
		jTextFieldBlobDiameter.setText( "" + ( 2 * ( Double ) settings.get( KEY_RADIUS ) ) );
		jTextFieldThreshold.setText( "" + settings.get( KEY_THRESHOLD ) );
		jTextFieldNSplit.setText( "" + settings.get( KEY_NSPLIT ) );
		jCheckSubPixel.setSelected( ( Boolean ) settings.get( KEY_DO_SUBPIXEL_LOCALIZATION ) );
		jCheckBoxMedianFilter.setSelected( ( Boolean ) settings.get( KEY_DO_MEDIAN_FILTERING ) );
	}

	@Override
	protected void preview()
	{
		clean();
		Overlay overlay = imp.getOverlay();
		if ( null == overlay )
		{
			overlay = new Overlay();
		}

		final int nsplit = NumberParser.parseInteger( jTextFieldNSplit.getText() );
		roiList = new ArrayList<>(nsplit*nsplit);
		
		final Interval interval = TMUtils.getInterval( TMUtils.rawWraps( imp ), settings );
		for ( int i = 0; i < nsplit; i++ )
		{
			for ( int j = 0; j < nsplit; j++ )
			{
				final Interval block = BlockLogDetector.getBlock( interval, nsplit, i, j );
				final Roi roi = new Roi( 0.5 + block.min( 0 ), 0.5 + block.min( 1 ), block.dimension( 0 ) - 1, block.dimension( 1 ) - 1 );
				roiList.add( roi );
				overlay.add( roi );
			}
		}
		overlay.setStrokeColor( Color.CYAN.darker() );
		imp.setOverlay( overlay );
		super.preview();
	}

	@Override
	public void clean()
	{
		super.clean();
		final Overlay overlay = imp.getOverlay();
		if ( null != overlay )
		{
			for ( final Roi roi : roiList )
			{
				overlay.remove( roi );
			}
		}
	}
}
