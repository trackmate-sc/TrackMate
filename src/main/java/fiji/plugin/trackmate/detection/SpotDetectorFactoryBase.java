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
package fiji.plugin.trackmate.detection;

import java.util.Map;

import org.jdom2.Element;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModule;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Mother interface for detector factories. It is also responsible for
 * loading/saving the detector parameters from/to XML, and of generating a
 * suitable configuration panel for GUI interaction.
 */
public interface SpotDetectorFactoryBase< T extends RealType< T > & NativeType< T > > extends TrackMateModule
{

	/**
	 * Configure this factory to operate on the given source image (possibly
	 * 5D), with the given settings map.
	 * <p>
	 * Also checks the validity of the given settings map for this factory. If
	 * check fails, return <code>false</code>, an error message can be obtained
	 * through {@link #getErrorMessage()}.
	 *
	 * @param img
	 *            the {@link ImgPlus} to operate on, possible 5D.
	 * @param settings
	 *            the settings map, must be suitable for this detector factory.
	 * @return <code>false</code> is the given settings map is not suitable for
	 *         this detector factory.
	 * @see SpotDetectorFactory#getErrorMessage()
	 */
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings );

	/**
	 * Returns a meaningful error message for the last action on this factory.
	 *
	 * @see #setTarget(ImgPlus, Map)
	 * @see #marshall(Map, Element)
	 * @see #unmarshall(Element, Map)
	 * @return an error message.
	 */
	public String getErrorMessage();

	/**
	 * Marshals a settings map to a JDom element, ready for saving to XML. The
	 * element is <b>updated</b> with new attributes.
	 * <p>
	 * Only parameters specific to the specific detector factory are marshalled.
	 * The element also always receive an attribute named
	 * {@value DetectorKeys#XML_ATTRIBUTE_DETECTOR_NAME} that saves the target
	 * {@link SpotDetectorFactory} key.
	 *
	 * @param settings
	 *            the settings map to marshal.
	 * @param element
	 *            the element to marshal to.
	 * @return <code>true</code> if marshalling was successful. If not, check
	 *         {@link #getErrorMessage()}
	 * 
	 */
	public boolean marshall( final Map< String, Object > settings, final Element element );

	/**
	 * Un-marshals a JDom element to update a settings map.
	 *
	 * @param element
	 *            the JDom element to read from.
	 * @param settings
	 *            the map to update. Is cleared prior to updating, so that it
	 *            contains only the parameters specific to the target detector
	 *            factory.
	 * @return <code>true</code> if un-marshalling was successful. If not, check
	 *         {@link #getErrorMessage()}
	 */
	public boolean unmarshall( final Element element, final Map< String, Object > settings );

	/**
	 * Returns a new GUI panel able to configure the settings suitable for this
	 * specific detector factory.
	 *
	 * @param settings
	 *            the current settings, used to get info to display on the GUI
	 *            panel.
	 * @param model
	 *            the current model, used to get info to display on the GUI
	 *            panel.
	 * @return a new configuration panel.
	 */
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model );

	/**
	 * Returns a new default settings map suitable for the target detector.
	 * Settings are instantiated with default values.
	 *
	 * @return a new map.
	 */
	public Map< String, Object > getDefaultSettings();

	/**
	 * Checks that the given settings map is suitable for target detector.
	 *
	 * @param settings
	 *            the map to test.
	 * @return <code>true</code> if the settings map is valid.
	 */
	public boolean checkSettings( final Map< String, Object > settings );

	/**
	 * Returns <code>true</code> for the detectors that can provide a spot with
	 * a 2D <code>SpotRoi</code> when they operate on 2D images.
	 * <p>
	 * This flag may be used by clients to exploit the fact that the spots
	 * created with this detector will have a contour that can be used
	 * <i>e.g.</i> to compute 2D morphological features. The default is
	 * <code>false</code>, indicating that this detector provides spots as a X,
	 * Y, Z, radius tuple.
	 *
	 * @return <code>true</code> if the spots created by this detector have a 2D
	 *         contour.
	 */
	public default boolean has2Dsegmentation()
	{
		return false;
	}

	/**
	 * Returns <code>true</code> for the detectors that can provide a spot with
	 * a 3D <code>SpotMesh</code> when they operate on 3D images.
	 * <p>
	 * This flag may be used by clients to exploit the fact that the spots
	 * created with this detector will have a 3D mesh that can be used
	 * <i>e.g.</i> to compute 3D morphological features. The default is
	 * <code>false</code>, indicating that this detector provides spots as a X,
	 * Y, Z, radius tuple.
	 *
	 * @return <code>true</code> if the spots created by this detector have a 2D
	 *         contour.
	 */
	public default boolean has3Dsegmentation()
	{
		return false;
	}

	/**
	 * Returns a copy the current instance.
	 *
	 * @return a new instance of this detector factory.
	 */
	public SpotDetectorFactoryBase< T > copy();
}
