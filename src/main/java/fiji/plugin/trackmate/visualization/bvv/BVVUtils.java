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
package fiji.plugin.trackmate.visualization.bvv;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.scijava.ui.behaviour.io.InputTriggerConfig;

import bdv.BigDataViewer;
import bdv.cache.CacheControl.CacheControls;
import bdv.tools.brightness.ConverterSetup;
import bdv.ui.appearance.AppearanceManager;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.util.RandomAccessibleIntervalSource4D;
import bdv.viewer.ConverterSetups;
import bdv.viewer.DisplayMode;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bvv.core.BigVolumeViewer;
import bvv.core.VolumeViewerOptions;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.util.TMUtils;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.LUT;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.mesh.Mesh;
import net.imglib2.mesh.Meshes;
import net.imglib2.mesh.impl.nio.BufferMesh;
import net.imglib2.mesh.util.Icosahedron;
import net.imglib2.mesh.view.TranslateMesh;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

public class BVVUtils
{

	public static final StupidMesh createMesh( final Spot spot )
	{
		if ( spot instanceof SpotMesh )
		{
			final SpotMesh sm = ( SpotMesh ) spot;
			final Mesh mesh = TranslateMesh.translate( sm.getMesh(), spot );
			final BufferMesh bm = new BufferMesh( mesh.vertices().size(), mesh.triangles().size() );
			Meshes.copy( mesh, bm );
			return new StupidMesh( bm );
		}
		return new StupidMesh( Icosahedron.sphere( spot, spot.getFeature( Spot.RADIUS ).doubleValue() ) );
	}

	public static final < T extends RealType< T > > BigVolumeViewer createBvv( final GuiModel guiModel )
	{

		/*
		 * Wire BVV options to TrackMate config objects.
		 */

		final ImagePlus imp = guiModel.getSettings().imp;
		final BVVKeymapManager bvvKeymapManager = guiModel.getBvvKeymapManager();
		final InputTriggerConfig config = bvvKeymapManager.getForwardSelectedKeymap().getConfig();
		final AppearanceManager appearanceManager = guiModel.getAppearanceManager();

		final VolumeViewerOptions options = VolumeViewerOptions.options()
				.inputTriggerConfig( config )
				.maxAllowedStepInVoxels( 0 )
				.renderWidth( 1024 )
				.renderHeight( 1024 )
				.keymapManager( bvvKeymapManager )
				.appearanceManager( appearanceManager )
				.shareKeyPressedEvents( guiModel.getKeyPressedManager() )
				.height( 512 )
				.width( 512 );

		/*
		 * Create BVV sources
		 */

		// Scaling
		final Calibration cal = imp.getCalibration();
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.set(
				cal.pixelWidth, 0, 0, 0,
				0, cal.pixelHeight, 0, 0,
				0, 0, cal.pixelDepth, 0 );

		// Image data
		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final int cAxis = img.dimensionIndex( Axes.CHANNEL );
		final int nChannels = ( int ) ( ( cAxis < 0 ) ? 1 : img.dimension( cAxis ) );
		final int tAxis = img.dimensionIndex( Axes.TIME );
		final int nTimePoints = ( int ) ( ( tAxis < 0 ) ? 0 : img.dimension( tAxis ) );

		// Source and converter setup
		final List< SourceAndConverter< ? > > sources = new ArrayList<>( nChannels );
		final List< ConverterSetup > setups = new ArrayList< ConverterSetup >( nChannels );
		for ( int c = 0; c < nChannels; c++ )
		{
			final RandomAccessibleInterval< T > channelRai =
					( cAxis < 0 )
							? img
							: img.view().slice( cAxis, c );
			
			final String sourceName = ( cAxis < 0 ) ? "" : "Ch " + ( c + 1 );
			final Source< T > source;
			if ( nTimePoints > 1 )
			{
				source = new RandomAccessibleIntervalSource4D<>(
						channelRai,
						channelRai.getType(),
						sourceTransform,
						sourceName );
			}
			else
			{
				source = new RandomAccessibleIntervalSource<>(
						channelRai,
						channelRai.getType(),
						sourceTransform,
						sourceName );
			}

			final Converter< T, ARGBType > converterToARGB = BigDataViewer.createConverterToARGB( channelRai.getType() );
			final SourceAndConverter< T > soc = new SourceAndConverter< T >( source, converterToARGB );
			sources.add( soc );
			final ConverterSetup setup = BigDataViewer.createConverterSetup( soc, c );
			setups.add( setup );
		}

		final CacheControls cacheControl = new CacheControls();
		final String title = "3D view " + imp.getShortTitle();
		final BigVolumeViewer bvv = new BigVolumeViewer( setups, sources, nTimePoints, cacheControl, title, options );
		syncDisplayAndLUTs( bvv, sources, imp );
		return bvv;
	}

	public static void syncDisplayAndLUTs(
			final BigVolumeViewer bvv,
			final List< SourceAndConverter< ? > > sources,
			final ImagePlus imp )
	{
		// Display mode.
		final DisplayMode displayMode = getDisplayMode( imp );
		bvv.getViewer().setDisplayMode( displayMode );

		// LUT & min max
		final ConverterSetups converterSetups = bvv.getConverterSetups();
		final int nChannels = sources.size();
		for ( int c = 0; c < nChannels; c++ )
		{
			final List< ConverterSetup > css = converterSetups.getConverterSetups( sources );
			final ConverterSetup setup = css.get( c );

			double minRange;
			double maxRange;
			Color channelColor;
			if ( imp instanceof CompositeImage )
			{
				final CompositeImage ci = ( CompositeImage ) imp;
				final LUT lut = ci.getChannelLut( c + 1 );
				minRange = lut.min;
				maxRange = lut.max;
				channelColor = new Color( lut.getRGB( 255 ) );
			}
			else
			{
				imp.setPosition( c + 1, 1, 1 );
				minRange = imp.getDisplayRangeMin();
				maxRange = imp.getDisplayRangeMax();

				final LUT lut = imp.getProcessor().getLut();
				if ( lut != null )
					channelColor = new Color( lut.getRGB( 255 ) );
				else
					channelColor = Color.WHITE;
			}

			// Apply
			setup.setDisplayRange( minRange, maxRange );
			final int argb = ARGBType.rgba(
					channelColor.getRed(),
					channelColor.getGreen(),
					channelColor.getBlue(),
					255 );
			setup.setColor( new ARGBType( argb ) );
		}
	}

	/**
	 * Determines the BVV DisplayMode based on an ImagePlus instance.
	 */
	public static DisplayMode getDisplayMode( final ImagePlus imp )
	{
		// Check if the ImagePlus is a CompositeImage (multi-channel UI mode)
		if ( imp instanceof CompositeImage )
		{
			final CompositeImage ci = ( CompositeImage ) imp;

			switch ( ci.getMode() )
			{
			case CompositeImage.COMPOSITE:
				return DisplayMode.FUSED;

			case CompositeImage.COLOR:
			case CompositeImage.GRAYSCALE:
				return DisplayMode.SINGLE;

			default:
				return DisplayMode.FUSED;
			}
		}

		if ( imp.getNChannels() > 1 )
			return DisplayMode.FUSED;
		else
			return DisplayMode.SINGLE;
	}
}
