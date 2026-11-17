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
package fiji.plugin.trackmate.io;

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import fiji.plugin.trackmate.Logger;
import ij.IJ;

/**
 * A collection of static utilities for the input/output of xml files.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class IOUtils
{

	private static final Pattern ILLEGAL_XML_CHARS = Pattern.compile(
			"[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F-\\x84\\x86-\\x9F\\uD800-\\uDFFF\\uFDD0-\\uFDEF\\uFFFE\\uFFFF\\u2FFFE\\u2FFFF\\u3FFFE\\u3FFFF\\u4FFFE\\u4FFFF\\u5FFFE\\u5FFFF\\u6FFFE\\u6FFFF\\u7FFFE\\u7FFFF\\u8FFFE\\u8FFFF\\u9FFFE\\u9FFFF\\uAFFFE\\uAFFFF\\uBFFFE\\uBFFFF\\uCFFFE\\uCFFFF\\uDFFFE\\uDFFFF\\uEFFFE\\uEFFFF\\uFFFFE\\uFFFFF\\u10FFFE\\u10FFFF]" );

	private static final Pattern ILLEGAL_SURROGATE_PAIRS = Pattern.compile(
			"[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?:[^\uD800-\uDBFF])[\uDC00-\uDFFF]" );

	public static String cleanInvalidXmlChars( final String input )
	{
		if ( input == null )
			return null;
		final String cleaned = ILLEGAL_XML_CHARS.matcher( input ).replaceAll( "" );
		return ILLEGAL_SURROGATE_PAIRS.matcher( cleaned ).replaceAll( "" );
	}

	public static final boolean canReadFile( final String path, final StringBuilder errorHolder )
	{
		if ( path.isEmpty() )
		{
			errorHolder.append( "The path to the file is empty." );
			return false;
		}
		final File file = new File( path );
		if ( !file.exists() )
		{
			errorHolder.append( "The file " + path + " does not exist." );
			return false;
		}
		if ( !file.isFile() )
		{
			errorHolder.append( "The path" + path + " is not a file." );
			return false;
		}
		if ( !file.canRead() )
		{
			errorHolder.append( "The file" + path + " cannot be read." );
			return false;
		}
		return true;
	}

	/**
	 * Prompts the user for a GEFF file to save to.
	 *
	 * @param file
	 *            a default file, will be used to display a default choice in
	 *            the file chooser.
	 * @param parent
	 *            the {@link Frame} to lock on this dialog. It can be
	 *            <code>null</code>; in that case, native dialogs will not be
	 *            used on Macs.
	 * @return the selected file, or <code>null</code> if the user pressed the
	 *         "cancel" button.
	 */
	public static File askForFileForSaving( final File file, final Frame parent )
	{
		return askForFileForSaving( file, parent, "geff" );
	}

	/**
	 * Prompts the user for a file to save to.
	 *
	 * @param file
	 *            a default file, will be used to display a default choice in
	 *            the file chooser.
	 * @param parent
	 *            the {@link Frame} to lock on this dialog. It can be
	 *            <code>null</code>; in that case, native dialogs will not be
	 *            used on Macs.
	 * @param extension
	 *            the extension to use for the file filter.
	 * @return the selected file, or <code>null</code> if the user pressed the
	 *         "cancel" button.
	 */
	public static File askForFileForSaving( final File file, final Frame parent, final String extension )
	{
		if ( IJ.isMacintosh() && parent != null )
		{
			// use the native file dialog on the mac
			final FileDialog dialog = new FileDialog( parent, "Save to a " + extension.toUpperCase() + " file", FileDialog.SAVE );
			dialog.setIconImage( TRACKMATE_ICON.getImage() );
			dialog.setDirectory( file.getParent() );
			dialog.setFile( file.getName() );
			final FilenameFilter filter = ( dir, name ) -> name.endsWith( '.' + extension );
			dialog.setFilenameFilter( filter );
			dialog.setVisible( true );
			String selectedFile = dialog.getFile();
			if ( null == selectedFile )
				return null;
			if ( !selectedFile.endsWith( '.' + extension ) )
				selectedFile += '.' + extension;
			return new File( dialog.getDirectory(), selectedFile );
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
			fileChooser.setSelectedFile( file );
			final FileNameExtensionFilter filter = new FileNameExtensionFilter( extension.toUpperCase() + " files", extension );
			fileChooser.setFileFilter( filter );

			final int returnVal = fileChooser.showSaveDialog( parent );
			if ( returnVal == JFileChooser.APPROVE_OPTION )
				return fileChooser.getSelectedFile();

			return null;
		}
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
	 * Possibly creates the whole directories needed to save a file with the
	 * specified path.
	 *
	 * @param path
	 *            the path.
	 * @return <code>true</code> if folders have actually been created.
	 */
	public static boolean mkdirs( final String path )
	{
		final File dir = new File( path ).getParentFile();
		return dir == null ? false : dir.mkdirs();
	}

	/**
	 * Deletes the content at the given path, recursively if it is a directory.
	 * Does nothing if nothing exists there.
	 *
	 * @param target
	 *            the path to the file or directory to delete.
	 * @throws IOException
	 *             if an I/O error occurs while deleting.
	 */
	public static void deleteRecursively( final Path target ) throws IOException
	{
		if ( !Files.exists( target ) )
			return;

		if ( !Files.isDirectory( target ) )
		{
			Files.delete( target );
			return;
		}

		// Children first (reverse order), then the directory itself.
		final List< Path > paths;
		try (final Stream< Path > stream = Files.walk( target ))
		{
			paths = stream.sorted( Comparator.reverseOrder() ).collect( Collectors.toList() );
		}
		for ( final Path path : paths )
			Files.delete( path );
	}
}
