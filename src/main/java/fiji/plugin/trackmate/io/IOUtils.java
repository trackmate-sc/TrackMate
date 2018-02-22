package fiji.plugin.trackmate.io;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DOWNSAMPLE_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TRACKMATE_ICON;
import fiji.plugin.trackmate.Logger;
import fiji.util.NumberParser;
import ij.IJ;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;

/**
 * A collection of static utilities for the input/output of xml files.
 * @author Jean-Yves Tinevez
 *
 */
public class IOUtils {




	/**
	 * Prompts the user for a xml file to save to.
	 *
	 * @param file
	 *            a default file, will be used to display a default choice in
	 *            the file chooser.
	 * @param parent
	 *            the {@link Frame} to lock on this dialog. It can be
	 *            <code>null</code>; in that case, native dialogs will not be
	 *            used on Macs.
	 * @param logger
	 *            a {@link Logger} to report what is happening.
	 * @return the selected file, or <code>null</code> if the user pressed the
	 *         "cancel" button.
	 */
	public static File askForFileForSaving(File file, final Frame parent, final Logger logger) {

		if (IJ.isMacintosh() && parent != null) {
			// use the native file dialog on the mac
			final FileDialog dialog =	new FileDialog(parent, "Save to a XML file", FileDialog.SAVE);
			dialog.setIconImage(TRACKMATE_ICON.getImage());
			dialog.setDirectory(file.getParent());
			dialog.setFile(file.getName());
			final FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					return name.endsWith(".xml");
				}
			};
			dialog.setFilenameFilter(filter);
			dialog.setVisible(true);
			String selectedFile = dialog.getFile();
			if (null == selectedFile) {
				logger.log("Save data aborted.\n");
				return null;
			}
			if (!selectedFile.endsWith(".xml"))
				selectedFile += ".xml";
			file = new File(dialog.getDirectory(), selectedFile);
		} else {
			final JFileChooser fileChooser = new JFileChooser(file.getParent()) {
				private static final long serialVersionUID = 1L;
				@Override
			    protected JDialog createDialog( final Component lParent ) throws HeadlessException {
			        final JDialog dialog = super.createDialog( lParent );
			        dialog.setIconImage( TRACKMATE_ICON.getImage() );
			        return dialog;
			    }
			};
			fileChooser.setSelectedFile(file);
			final FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
			fileChooser.setFileFilter(filter);

			final int returnVal = fileChooser.showSaveDialog(parent);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
			} else {
				logger.log("Save data aborted.\n");
				return null;
			}
		}
		return file;
	}

	/**
	 * Prompts the user for a xml file to load from.
	 *
	 * @param file  a default file, will be used to display a default choice in the file chooser.
	 * @param title  the title to display on the file chooser window
	 * @param parent  the {@link Frame} to lock on this dialog.
	 * @param logger  a {@link Logger} to report what is happening.
	 * @return  the selected file, or <code>null</code> if the user pressed the "cancel" button.
	 */
	public static File askForFileForLoading(File file, final String title, final Frame parent, final Logger logger) {

		if(IJ.isMacintosh()) {
			// use the native file dialog on the mac
			final FileDialog dialog =	new FileDialog(parent, title, FileDialog.LOAD);
			dialog.setIconImage(TRACKMATE_ICON.getImage());
			dialog.setDirectory(file.getParent());
			dialog.setFile(file.getName());
			final FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					return name.endsWith(".xml");
				}
			};
			dialog.setFilenameFilter(filter);
			dialog.setVisible(true);
			String selectedFile = dialog.getFile();
			if (null == selectedFile) {
				logger.log("Load data aborted.\n");
				return null;
			}
			if (!selectedFile.endsWith(".xml"))
				selectedFile += ".xml";
			file = new File(dialog.getDirectory(), selectedFile);
		} else {
			final JFileChooser fileChooser = new JFileChooser(file.getParent()) {
				private static final long serialVersionUID = 1L;
				@Override
			    protected JDialog createDialog( final Component lParent ) throws HeadlessException {
			        final JDialog dialog = super.createDialog( lParent );
			        dialog.setIconImage( TRACKMATE_ICON.getImage() );
			        return dialog;
			    }
			};
			fileChooser.setName(title);
			fileChooser.setSelectedFile(file);
			final FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
			fileChooser.setFileFilter(filter);

			final int returnVal = fileChooser.showOpenDialog(parent);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				file = fileChooser.getSelectedFile();
			} else {
				logger.log("Load data aborted.\n");
				return null;
			}
		}
		return file;
	}


	/**
	 * Prompts the user for a target folder.
	 * 
	 * @param file
	 *            a default file, will be used to display a default choice in
	 *            the file chooser.
	 * @param title
	 *            the title to display on the file chooser window
	 * @param parent
	 *            the {@link Frame} to lock on this dialog.
	 * @param logger
	 *            a {@link Logger} to report what is happening.
	 * @return the selected file, or <code>null</code> if the user pressed the
	 *         "cancel" button.
	 */
	public static File askForFolder( File file, final String title, final Frame parent, final Logger logger )
	{

		if ( IJ.isMacintosh() )
		{
			// use the native file dialog on the mac
			System.setProperty( "apple.awt.fileDialogForDirectories", "true" );
			final FileDialog dialog = new FileDialog( parent, title, FileDialog.LOAD );
			dialog.setIconImage( TRACKMATE_ICON.getImage() );
			dialog.setDirectory( file.getParent() );
			dialog.setFile( file.getName() );
			dialog.setVisible( true );
			final String selectedFile = dialog.getFile();
			System.setProperty( "apple.awt.fileDialogForDirectories", "false" );

			if ( null == selectedFile )
			{
				logger.log( "Load data aborted.\n" );
				return null;
			}
			file = new File( dialog.getDirectory(), dialog.getFile() );
		}
		else
		{
			final JFileChooser fileChooser = new JFileChooser( file.getParent() )
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected JDialog createDialog( final Component lParent ) throws HeadlessException
				{
					final JDialog dialog = super.createDialog( lParent );
					dialog.setIconImage( TRACKMATE_ICON.getImage() );
					return dialog;
				}
			};
			fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
			fileChooser.setName( title );
			fileChooser.setSelectedFile( file );

			final int returnVal = fileChooser.showOpenDialog( parent );
			if ( returnVal == JFileChooser.APPROVE_OPTION )
			{
				file = fileChooser.getSelectedFile();
			}
			else
			{
				logger.log( "Load data aborted.\n" );
				return null;
			}
		}
		return file;
	}


	/**
	 * Read and return an integer attribute from a JDom {@link Element}, and substitute a default value of 0
	 * if the attribute is not found or of the wrong type.
	 */
	public static final int readIntAttribute(final Element element, final String name, final Logger logger) {
		return readIntAttribute(element, name, logger, 0);
	}

	public static final int readIntAttribute(final Element element, final String name, final Logger logger, final int defaultValue) {
		int val = defaultValue;
		final Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value: "+defaultValue+".\n");
			return val;
		}
		try {
			val = att.getIntValue();
		} catch (final DataConversionException e) {
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value: "+defaultValue+".\n");
		}
		return val;
	}

	public static final double readFloatAttribute(final Element element, final String name, final Logger logger) {
		double val = 0;
		final Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value.\n");
			return val;
		}
		try {
			val = att.getFloatValue();
		} catch (final DataConversionException e) {
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value.\n");
		}
		return val;
	}

	public static final double readDoubleAttribute(final Element element, final String name, final Logger logger) {
		double val = 0;
		final Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value.\n");
			return val;
		}
		try {
			val = att.getDoubleValue();
		} catch (final DataConversionException e) {
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value.\n");
		}
		return val;
	}

	public static final boolean readBooleanAttribute(final Element element, final String name, final Logger logger) {
		boolean val = false;
		final Attribute att = element.getAttribute(name);
		if (null == att) {
			logger.error("Could not find attribute "+name+" for element "+element.getName()+", substituting default value.\n");
			return val;
		}
		try {
			val = att.getBooleanValue();
		} catch (final DataConversionException e) {
			logger.error("Cannot read the attribute "+name+" of the element "+element.getName()+", substituting default value.\n");
		}
		return val;
	}

	/*
	 * EXTRA UN-MARSHALLING UTILS Using another syntax.
	 */

	public static final boolean readDoubleAttribute( final Element element, final Map< String, Object > settings, final String parameterKey, final StringBuilder errorHolder )
	{
		final String str = element.getAttributeValue( parameterKey );
		if ( null == str )
		{
			errorHolder.append( "Attribute " + parameterKey + " could not be found in XML element.\n" );
			return false;
		}
		try
		{
			final double val = NumberParser.parseDouble( str );
			settings.put( parameterKey, val );
		}
		catch ( final NumberFormatException nfe )
		{
			errorHolder.append( "Could not read " + parameterKey + " attribute as a double value. Got " + str + ".\n" );
			return false;
		}
		return true;
	}

	public static final boolean readIntegerAttribute( final Element element, final Map< String, Object > settings, final String parameterKey, final StringBuilder errorHolder )
	{
		final String str = element.getAttributeValue( parameterKey );
		if ( null == str )
		{
			errorHolder.append( "Attribute " + parameterKey + " could not be found in XML element.\n" );
			return false;
		}
		try
		{
			final int val = NumberParser.parseInteger( str );
			settings.put( parameterKey, val );
		}
		catch ( final NumberFormatException nfe )
		{
			errorHolder.append( "Could not read " + parameterKey + " attribute as an integer value. Got " + str + ".\n" );
			return false;
		}
		return true;
	}

	public static final boolean readBooleanAttribute( final Element element, final Map< String, Object > settings, final String parameterKey, final StringBuilder errorHolder )
	{
		final String str = element.getAttributeValue( parameterKey );
		if ( null == str )
		{
			errorHolder.append( "Attribute " + parameterKey + " could not be found in XML element.\n" );
			return false;
		}
		try
		{
			final boolean val = Boolean.parseBoolean( str );
			settings.put( parameterKey, val );
		}
		catch ( final NumberFormatException nfe )
		{
			errorHolder.append( "Could not read " + parameterKey + " attribute as an boolean value. Got " + str + "." );
			return false;
		}
		return true;
	}

	/**
	 * Unmarshall the attributes of a JDom element in a map of doubles. Mappings
	 * are <b>added</b> to the specified map. If a value is found not to be a
	 * double, an error is returned.
	 *
	 * @return <code>true</code> if all values were found and mapped as doubles,
	 *         <code>false</code> otherwise and the error holder is updated.
	 */
	public static boolean unmarshallMap( final Element element, final Map< String, Double > map, final StringBuilder errorHolder )
	{
		boolean ok = true;
		final List< Attribute > attributes = element.getAttributes();
		for ( final Attribute att : attributes )
		{
			final String key = att.getName();
			try
			{
				final double val = att.getDoubleValue();
				map.put( key, val );
			}
			catch ( final DataConversionException e )
			{
				errorHolder.append( "Could not convert the " + key + " attribute to double. Got " + att.getValue() + ".\n" );
				ok = false;
			}
		}
		return ok;
	}

	/*
	 * MARSHALLING UTILS
	 */

	public static final boolean writeTargetChannel( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
	}

	public static final boolean writeRadius( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_RADIUS, Double.class, errorHolder );
	}

	public static final boolean writeThreshold( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_THRESHOLD, Double.class, errorHolder );
	}

	public static final boolean writeDoMedian( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_DO_MEDIAN_FILTERING, Boolean.class, errorHolder );
	}

	public static final boolean writeDoSubPixel( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_DO_SUBPIXEL_LOCALIZATION, Boolean.class, errorHolder );
	}

	public static final boolean writeDownsamplingFactor( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_DOWNSAMPLE_FACTOR, Integer.class, errorHolder );
	}

	/**
	 * Add a parameter attribute to the given element, taken from the given
	 * settings map. Basic checks are made to ensure that the parameter value
	 * can be found and is of the right class.
	 * 
	 * @param settings
	 *            the map to take the parameter value from
	 * @param element
	 *            the JDom element to update
	 * @param parameterKey
	 *            the key to the parameter value in the map
	 * @param expectedClass
	 *            the expected class for the value
	 * @return <code>true</code> if the parameter was found, of the right class,
	 *         and was successfully added to the element, <code>false</code> if
	 *         not, and updated the specified error holder.
	 */
	public static final boolean writeAttribute( final Map< String, Object > settings, final Element element, final String parameterKey, final Class< ? > expectedClass, final StringBuilder errorHolder )
	{
		final Object obj = settings.get( parameterKey );

		if ( null == obj )
		{
			errorHolder.append( "Could not find parameter " + parameterKey + " in settings map.\n" );
			return false;
		}

		if ( !expectedClass.isInstance( obj ) )
		{
			errorHolder.append( "Exoected " + parameterKey + " parameter to be a " + expectedClass.getName() + " but was a " + obj.getClass().getName() + ".\n" );
			return false;
		}

		element.setAttribute( parameterKey, "" + obj );
		return true;
	}

	/**
	 * Stores the given mapping in a given JDom element, using attributes in a
	 * KEY="VALUE" fashion.
	 */
	public static void marshallMap( final Map< String, Double > map, final Element element )
	{
		for ( final String key : map.keySet() )
		{
			element.setAttribute( key, map.get( key ).toString() );
		}
	}
}
